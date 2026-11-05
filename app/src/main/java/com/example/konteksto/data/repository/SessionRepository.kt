package com.example.konteksto.data.repository

import android.content.Context
import android.util.Log
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.host.studio.TranscriptEntry
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

interface SessionRepository {
    fun getHostSessionsStream(hostUid: String): Flow<List<HostSession>>
    fun getAllLiveSessionsStream(): Flow<List<HostSession>>
    fun getAllSessionsStream(): Flow<List<HostSession>>
    fun getJoinedSessionIdsStream(userUid: String): Flow<Set<String>>
    suspend fun findSessionByCode(code: String): Result<HostSession>
    suspend fun joinSession(userUid: String, sessionId: String): Result<HostSession>
    suspend fun leaveSession(userUid: String, sessionId: String): Result<Unit>
    suspend fun createSession(
        title: String,
        speakerName: String,
        category: String,
        venue: String,
        startTime: String,
        endTime: String,
        bannerImageUrl: String,
        talkCode: String = "",
        hostUid: String,
    ): Result<String>
    suspend fun updateSessionLiveStatus(sessionId: String, isLive: Boolean): Result<Unit>

    /**
     * Streams the transcript in spoken order, limited to the latest [limit] lines when set. The flow
     * fails if the listener is rejected, because Firestore delivers nothing further after that.
     */
    fun getTranscriptsStream(sessionId: String, limit: Long? = null): Flow<List<TranscriptEntry>>
    suspend fun getEarlierTranscripts(
        sessionId: String,
        beforeCreatedAt: Long,
        limit: Long,
    ): Result<List<TranscriptEntry>>

    /** [clearsLivePartial] removes the in-progress text in the same write, so it never shows twice. */
    suspend fun addTranscriptEntry(
        sessionId: String,
        speakerName: String,
        text: String,
        isSystemEvent: Boolean = false,
        clearsLivePartial: Boolean = false,
    ): Result<TranscriptEntry>
    fun getLivePartialTranscriptStream(sessionId: String): Flow<String>
    suspend fun updateLivePartialTranscript(sessionId: String, text: String): Result<Unit>
    suspend fun uploadSlideDeckPdf(
        sessionId: String,
        uri: android.net.Uri,
        fileName: String,
        fileSizeMb: Double,
        pageCount: Int = 1,
    ): Result<com.example.konteksto.ui.host.studio.SlideDeckInfo>
    suspend fun downloadSlideDeckPdf(sessionId: String, fileName: String): Result<ByteArray>
    suspend fun clearSlideDeck(sessionId: String, fileName: String?): Result<Unit>
    suspend fun saveAnalyzedContextNotesSignature(sessionId: String, signature: String): Result<Unit>
    suspend fun saveAnalyzedSlides(
        sessionId: String,
        analyzedSlides: List<com.example.konteksto.ui.host.studio.AnalyzedSlideItem>,
    ): Result<Unit>
    fun getAnalyzedSlidesStream(sessionId: String): Flow<List<com.example.konteksto.ui.host.studio.AnalyzedSlideItem>>
    fun getContextNotesStream(sessionId: String): Flow<List<com.example.konteksto.ui.host.studio.ContextNoteItem>>
    suspend fun addContextNote(
        sessionId: String,
        note: com.example.konteksto.ui.host.studio.ContextNoteItem,
    ): Result<Unit>
    suspend fun updateContextNote(
        sessionId: String,
        note: com.example.konteksto.ui.host.studio.ContextNoteItem,
    ): Result<Unit>
    suspend fun deleteContextNote(sessionId: String, noteId: String): Result<Unit>
    fun getDomainTermsStream(sessionId: String): Flow<List<com.example.konteksto.ui.host.studio.DomainTermItem>>
    suspend fun saveDomainTerms(
        sessionId: String,
        terms: List<com.example.konteksto.ui.host.studio.DomainTermItem>,
    ): Result<Unit>
    suspend fun updateDomainTerm(
        sessionId: String,
        term: com.example.konteksto.ui.host.studio.DomainTermItem,
    ): Result<Unit>
    suspend fun deleteDomainTerm(sessionId: String, termId: String): Result<Unit>
}

private const val MAX_SLIDE_DECK_BYTES = 50L * 1024 * 1024

class FirebaseSessionRepository(
    private val context: Context? = null,
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() },
) : SessionRepository {

    private val localCreatedSessions = MutableStateFlow<List<HostSession>>(loadLocalSessions())
    private val joinedSessionIdsFlow = MutableStateFlow<Set<String>>(emptySet())

    private val firestoreOrNull: FirebaseFirestore?
        get() = try {
            firestoreProvider()
        } catch (_: Exception) {
            null
        }

    override fun getAllSessionsStream(): Flow<List<HostSession>> = callbackFlow {
        val firestore = firestoreOrNull
        if (firestore == null) {
            val localJob = launch {
                localCreatedSessions.collect { sessions ->
                    trySend(sessions)
                }
            }
            awaitClose { localJob.cancel() }
            return@callbackFlow
        }

        trySend(localCreatedSessions.value)

        val listenerRegistration = try {
            firestore.collection("sessions")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(localCreatedSessions.value)
                        return@addSnapshotListener
                    }

                    val remoteSessions = snapshot?.documents?.mapNotNull { doc ->
                        val createdAt = doc.getLong("createdAt") ?: 0L
                        val session = doc.toHostSession()
                        if (session != null) createdAt to session else null
                    }?.sortedByDescending { it.first }
                        ?.map { it.second } ?: emptyList()

                    if (snapshot != null) {
                        val merged = (remoteSessions + localCreatedSessions.value).distinctBy { it.id }
                        trySend(merged)
                    } else {
                        trySend(localCreatedSessions.value)
                    }
                }
        } catch (_: Exception) {
            null
        }

        awaitClose {
            listenerRegistration?.remove()
        }
    }

    override fun getJoinedSessionIdsStream(userUid: String): Flow<Set<String>> = callbackFlow {
        val initialSet = loadJoinedSessionIds(userUid)
        joinedSessionIdsFlow.value = initialSet
        trySend(initialSet)

        val localJob = launch {
            joinedSessionIdsFlow.collect { ids ->
                trySend(ids)
            }
        }

        val firestore = firestoreOrNull
        val listenerRegistration = if (firestore != null && userUid.isNotBlank()) {
            try {
                firestore.collection("users").document(userUid)
                    .collection("joined_sessions")
                    .addSnapshotListener { snapshot, error ->
                        if (error == null && snapshot != null) {
                            val remoteIds = snapshot.documents.map { it.id }.toSet()
                            val combined = joinedSessionIdsFlow.value + remoteIds
                            if (combined != joinedSessionIdsFlow.value) {
                                joinedSessionIdsFlow.value = combined
                                saveJoinedSessionIds(userUid, combined)
                            }
                        }
                    }
            } catch (_: Exception) {
                null
            }
        } else null

        awaitClose {
            localJob.cancel()
            listenerRegistration?.remove()
        }
    }

    override suspend fun findSessionByCode(code: String): Result<HostSession> {
        val cleanCode = code.trim().replace("–", "-").uppercase()
        if (cleanCode.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter a valid session room code."))
        }

        val localList = localCreatedSessions.value
        val match = localList.find { session ->
            val sTalkCode = session.talkCode.trim().replace("–", "-").uppercase()
            val sId = session.id.trim().uppercase()
            sTalkCode == cleanCode || sId == cleanCode
        }

        if (match != null) {
            return Result.success(match)
        }

        val firestore = firestoreOrNull
        if (firestore != null) {
            try {
                val querySnapshot = firestore.collection("sessions")
                    .whereEqualTo("talkCode", cleanCode)
                    .get()
                    .await()

                val doc = querySnapshot.documents.firstOrNull()
                if (doc != null) {
                    val session = doc.toHostSession()
                    if (session != null) {
                        if (localCreatedSessions.value.none { it.id == session.id }) {
                            val newLocal = localCreatedSessions.value + session
                            localCreatedSessions.value = newLocal
                            saveLocalSessions(newLocal)
                        }
                        return Result.success(session)
                    }
                }
            } catch (e: Exception) {
                Log.e("FirebaseSessionRepo", "Error searching session by code: ${e.message}")
            }
        }

        return Result.failure(IllegalArgumentException("No session found for code '$code'"))
    }

    override suspend fun joinSession(userUid: String, sessionId: String): Result<HostSession> {
        val currentJoined = loadJoinedSessionIds(userUid).toMutableSet()
        val isNewJoin = currentJoined.add(sessionId)
        saveJoinedSessionIds(userUid, currentJoined)
        joinedSessionIdsFlow.value = currentJoined

        val updatedLocal = localCreatedSessions.value.map { session ->
            if (session.id == sessionId) {
                val newCount = if (isNewJoin) session.attendeesCount + 1 else session.attendeesCount
                session.copy(attendeesCount = newCount)
            } else {
                session
            }
        }
        localCreatedSessions.value = updatedLocal
        saveLocalSessions(updatedLocal)

        val targetSession = updatedLocal.find { it.id == sessionId }
            ?: return Result.failure(IllegalArgumentException("Session not found."))

        try {
            val firestore = firestoreOrNull
            if (firestore != null && isNewJoin) {
                val sessionRef = firestore.collection("sessions").document(sessionId)
                sessionRef.update("attendeesCount", FieldValue.increment(1)).await()

                val attendeeDoc = sessionRef.collection("attendees").document(userUid)
                attendeeDoc.set(
                    mapOf(
                        "userUid" to userUid,
                        "joinedAt" to System.currentTimeMillis(),
                    ),
                ).await()

                val userJoinedDoc = firestore.collection("users").document(userUid)
                    .collection("joined_sessions").document(sessionId)
                userJoinedDoc.set(
                    mapOf(
                        "sessionId" to sessionId,
                        "joinedAt" to System.currentTimeMillis(),
                    ),
                ).await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Failed to sync attendance to Firestore: ${e.message}")
        }

        return Result.success(targetSession)
    }

    override suspend fun leaveSession(userUid: String, sessionId: String): Result<Unit> {
        val currentJoined = loadJoinedSessionIds(userUid).toMutableSet()
        val isRemoved = currentJoined.remove(sessionId)
        saveJoinedSessionIds(userUid, currentJoined)
        joinedSessionIdsFlow.value = currentJoined

        if (isRemoved) {
            val updatedLocal = localCreatedSessions.value.map { session ->
                if (session.id == sessionId) {
                    val newCount = (session.attendeesCount - 1).coerceAtLeast(0)
                    session.copy(attendeesCount = newCount)
                } else {
                    session
                }
            }
            localCreatedSessions.value = updatedLocal
            saveLocalSessions(updatedLocal)

            try {
                val firestore = firestoreOrNull
                if (firestore != null) {
                    val sessionRef = firestore.collection("sessions").document(sessionId)
                    sessionRef.update("attendeesCount", FieldValue.increment(-1)).await()

                    val attendeeDoc = sessionRef.collection("attendees").document(userUid)
                    attendeeDoc.delete().await()

                    val userJoinedDoc = firestore.collection("users").document(userUid)
                        .collection("joined_sessions").document(sessionId)
                    userJoinedDoc.delete().await()
                }
            } catch (e: Exception) {
                Log.e("FirebaseSessionRepo", "Failed to sync leave session to Firestore: ${e.message}")
            }
        }

        return Result.success(Unit)
    }

    override fun getHostSessionsStream(hostUid: String): Flow<List<HostSession>> = callbackFlow {
        if (hostUid.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val firestore = firestoreOrNull
        if (firestore == null) {
            val localJob = launch {
                localCreatedSessions.collect { sessions ->
                    trySend(sessions.filter { it.hostUid == hostUid })
                }
            }
            awaitClose { localJob.cancel() }
            return@callbackFlow
        }

        val listenerRegistration = try {
            firestore.collection("sessions")
                .whereEqualTo("hostUid", hostUid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(localCreatedSessions.value.filter { it.hostUid == hostUid })
                        return@addSnapshotListener
                    }

                    val remoteSessions = snapshot?.documents?.mapNotNull { doc ->
                        val createdAt = doc.getLong("createdAt") ?: 0L
                        val session = doc.toHostSession()
                        if (session != null) createdAt to session else null
                    }?.sortedByDescending { it.first }
                        ?.map { it.second } ?: emptyList()

                    if (snapshot != null) {
                        val otherUserSessions = localCreatedSessions.value.filter { it.hostUid != hostUid }
                        val updatedLocal = otherUserSessions + remoteSessions
                        localCreatedSessions.value = updatedLocal
                        saveLocalSessions(updatedLocal)
                        trySend(remoteSessions)
                    } else {
                        trySend(localCreatedSessions.value.filter { it.hostUid == hostUid })
                    }
                }
        } catch (_: Exception) {
            null
        }

        awaitClose {
            listenerRegistration?.remove()
        }
    }

    override fun getAllLiveSessionsStream(): Flow<List<HostSession>> = callbackFlow {
        val firestore = firestoreOrNull
        if (firestore == null) {
            val localJob = launch {
                localCreatedSessions.collect { sessions ->
                    trySend(sessions.filter { it.isLive })
                }
            }
            awaitClose { localJob.cancel() }
            return@callbackFlow
        }

        trySend(localCreatedSessions.value.filter { it.isLive })

        val listenerRegistration = try {
            firestore.collection("sessions")
                .whereIn("status", listOf("live", "LIVE"))
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(localCreatedSessions.value.filter { it.isLive })
                        return@addSnapshotListener
                    }

                    val remoteSessions = snapshot?.documents?.mapNotNull { doc ->
                        val createdAt = doc.getLong("createdAt") ?: 0L
                        val session = doc.toHostSession()
                        if (session != null) createdAt to session else null
                    }?.sortedByDescending { it.first }
                        ?.map { it.second } ?: emptyList()

                    if (snapshot != null) {
                        trySend(remoteSessions)
                    } else {
                        trySend(localCreatedSessions.value.filter { it.isLive })
                    }
                }
        } catch (_: Exception) {
            null
        }

        awaitClose {
            listenerRegistration?.remove()
        }
    }

    override suspend fun createSession(
        title: String,
        speakerName: String,
        category: String,
        venue: String,
        startTime: String,
        endTime: String,
        bannerImageUrl: String,
        talkCode: String,
        hostUid: String,
    ): Result<String> {
        val generatedTalkCode = talkCode.ifBlank { "TALK–${(100..999).random()}" }
        val sessionId = "session_${System.currentTimeMillis()}"
        val formattedTime = if (startTime.isNotBlank() && endTime.isNotBlank()) {
            "$startTime - $endTime"
        } else startTime.ifBlank { "10:00 AM" }

        val newSession = HostSession(
            id = sessionId,
            venue = venue.ifBlank { "Main Hall" },
            time = formattedTime,
            talkCode = generatedTalkCode,
            title = title.trim(),
            speakerName = speakerName.trim(),
            attendeesCount = 0,
            isLive = false,
            bannerImageUrl = bannerImageUrl,
            category = category,
            hostUid = hostUid,
            startTime = startTime,
            endTime = endTime,
            status = "not_live",
        )

        val updatedLocal = listOf(newSession) + localCreatedSessions.value.filter { it.id != sessionId }
        localCreatedSessions.value = updatedLocal
        saveLocalSessions(updatedLocal)

        try {
            val firestore = firestoreOrNull
            if (firestore != null) {
                val docRef = firestore.collection("sessions").document(sessionId)
                val sessionMap = hashMapOf<String, Any?>(
                    "sessionId" to sessionId,
                    "hostUid" to hostUid,
                    "title" to title.trim(),
                    "speakerName" to speakerName.trim(),
                    "category" to category.trim(),
                    "topic" to category.trim(),
                    "venue" to venue.ifBlank { "Main Hall" },
                    "talkCode" to generatedTalkCode,
                    "startTime" to startTime,
                    "endTime" to endTime,
                    "time" to formattedTime,
                    "bannerImageUrl" to bannerImageUrl,
                    "status" to "not_live",
                    "attendeesCount" to 0,
                    "createdAt" to System.currentTimeMillis(),
                    "endedAt" to null,
                )
                docRef.set(sessionMap).await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Firestore remote sync failed: ${e.message}", e)
        }

        return Result.success(sessionId)
    }

    override suspend fun updateSessionLiveStatus(sessionId: String, isLive: Boolean): Result<Unit> {
        val newStatus = if (isLive) "live" else "ended"
        val updatedList = localCreatedSessions.value.map { session ->
            if (session.id == sessionId) {
                session.copy(isLive = isLive, status = newStatus)
            } else {
                session
            }
        }
        localCreatedSessions.value = updatedList
        saveLocalSessions(updatedList)

        try {
            val firestore = firestoreOrNull
            if (firestore != null) {
                firestore.collection("sessions").document(sessionId)
                    .update(
                        mapOf(
                            "isLive" to isLive,
                            "status" to newStatus,
                        ),
                    ).await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Failed to update session live status: ${e.message}")
        }
        return Result.success(Unit)
    }

    private val localTranscriptsMap = MutableStateFlow<Map<String, List<TranscriptEntry>>>(emptyMap())
    private val localLivePartials = MutableStateFlow<Map<String, String>>(emptyMap())
    private val lastTranscriptCreatedAt = AtomicLong(0L)

    private fun transcriptsCollection(firestore: FirebaseFirestore, sessionId: String) =
        firestore.collection("sessions").document(sessionId).collection("transcripts")

    private fun livePartialDocument(firestore: FirebaseFirestore, sessionId: String): DocumentReference =
        firestore.collection("sessions").document(sessionId).collection("live").document("current")

    private fun livePartialData(text: String): Map<String, Any> = hashMapOf(
        "partialText" to text,
        "updatedAt" to System.currentTimeMillis(),
    )

    private fun DocumentSnapshot.toTranscriptEntry(): TranscriptEntry? {
        val speaker = getString("speakerName") ?: "Speaker"
        val text = getString("text") ?: ""
        if (text.isBlank()) return null
        return TranscriptEntry(
            id = id,
            speakerName = speaker,
            timestamp = getString("timestamp") ?: "",
            text = text,
            isSystemEvent = getBoolean("isSystemEvent") ?: speaker.equals("SYSTEM", ignoreCase = true),
            createdAt = getLong("createdAt") ?: 0L,
        )
    }

    override fun getTranscriptsStream(sessionId: String, limit: Long?): Flow<List<TranscriptEntry>> = callbackFlow {
        if (sessionId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        fun List<TranscriptEntry>.inWindow() = if (limit != null) takeLast(limit.toInt()) else this

        val firestore = firestoreOrNull
        if (firestore == null) {
            val job = launch {
                localTranscriptsMap.collect { map ->
                    trySend(map[sessionId].orEmpty().inWindow())
                }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        trySend(localTranscriptsMap.value[sessionId].orEmpty().inWindow())

        val orderedTranscripts = transcriptsCollection(firestore, sessionId).orderBy("createdAt")
        val query = if (limit != null) orderedTranscripts.limitToLast(limit) else orderedTranscripts
        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirebaseSessionRepo", "Transcript listener failed: ${error.message}")
                close(error)
                return@addSnapshotListener
            }

            val remoteTranscripts = snapshot?.documents?.mapNotNull { it.toTranscriptEntry() } ?: emptyList()
            localTranscriptsMap.value = localTranscriptsMap.value + (sessionId to remoteTranscripts)
            trySend(remoteTranscripts)
        }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    override suspend fun getEarlierTranscripts(
        sessionId: String,
        beforeCreatedAt: Long,
        limit: Long,
    ): Result<List<TranscriptEntry>> {
        val firestore = firestoreOrNull
            ?: return Result.success(
                localTranscriptsMap.value[sessionId].orEmpty()
                    .filter { it.createdAt < beforeCreatedAt }
                    .takeLast(limit.toInt()),
            )

        return try {
            val snapshot = transcriptsCollection(firestore, sessionId)
                .whereLessThan("createdAt", beforeCreatedAt)
                .orderBy("createdAt")
                .limitToLast(limit)
                .get()
                .await()
            Result.success(snapshot.documents.mapNotNull { it.toTranscriptEntry() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Failed to load earlier transcripts: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun addTranscriptEntry(
        sessionId: String,
        speakerName: String,
        text: String,
        isSystemEvent: Boolean,
        clearsLivePartial: Boolean,
    ): Result<TranscriptEntry> {
        // Lines sort by createdAt and page by it too, so no two lines may ever share a value.
        val createdAt = lastTranscriptCreatedAt.updateAndGet { maxOf(it + 1, System.currentTimeMillis()) }
        val transcriptId = "tr_${createdAt}_${UUID.randomUUID().toString().take(8)}"
        val nowFormatted = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US).format(java.util.Date())
        val entry = TranscriptEntry(
            id = transcriptId,
            speakerName = speakerName.ifBlank { "Speaker" },
            timestamp = nowFormatted,
            text = text.trim(),
            isSystemEvent = isSystemEvent || speakerName.equals("SYSTEM", ignoreCase = true),
            createdAt = createdAt,
        )

        val firestore = firestoreOrNull
        if (firestore == null) {
            val currentList = localTranscriptsMap.value[sessionId] ?: emptyList()
            localTranscriptsMap.value = localTranscriptsMap.value + (sessionId to currentList + entry)
            if (clearsLivePartial) {
                localLivePartials.value = localLivePartials.value + (sessionId to "")
            }
            return Result.success(entry)
        }

        return try {
            val docRef = transcriptsCollection(firestore, sessionId).document(transcriptId)
            val data = hashMapOf<String, Any?>(
                "id" to transcriptId,
                "speakerName" to entry.speakerName,
                "timestamp" to entry.timestamp,
                "text" to entry.text,
                "isSystemEvent" to entry.isSystemEvent,
                "createdAt" to createdAt,
            )
            if (clearsLivePartial) {
                writeTranscriptClearingLivePartial(firestore, sessionId, docRef, data)
            } else {
                docRef.set(data).await()
            }
            Result.success(entry)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Failed to add transcript to Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun writeTranscriptClearingLivePartial(
        firestore: FirebaseFirestore,
        sessionId: String,
        docRef: DocumentReference,
        data: Map<String, Any?>,
    ) {
        try {
            firestore.batch()
                .set(docRef, data)
                .set(livePartialDocument(firestore, sessionId), livePartialData(""))
                .commit()
                .await()
        } catch (e: FirebaseFirestoreException) {
            if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
            // Rules that don't cover live/current reject the whole batch, and that mustn't cost the line.
            docRef.set(data).await()
        }
    }

    override fun getLivePartialTranscriptStream(sessionId: String): Flow<String> = callbackFlow {
        if (sessionId.isBlank()) {
            trySend("")
            awaitClose { }
            return@callbackFlow
        }

        val firestore = firestoreOrNull
        if (firestore == null) {
            val job = launch {
                localLivePartials.collect { partials ->
                    trySend(partials[sessionId].orEmpty())
                }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        val listenerRegistration = livePartialDocument(firestore, sessionId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Finished lines still arrive through the transcript stream, so this only costs the preview.
                Log.w("FirebaseSessionRepo", "Live partial listener failed: ${error.message}")
                trySend("")
                return@addSnapshotListener
            }
            trySend(snapshot?.getString("partialText").orEmpty())
        }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    override suspend fun updateLivePartialTranscript(sessionId: String, text: String): Result<Unit> {
        val firestore = firestoreOrNull
        if (firestore == null) {
            localLivePartials.value = localLivePartials.value + (sessionId to text)
            return Result.success(Unit)
        }

        return try {
            livePartialDocument(firestore, sessionId).set(livePartialData(text)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("FirebaseSessionRepo", "Failed to update live partial: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun uploadSlideDeckPdf(
        sessionId: String,
        uri: android.net.Uri,
        fileName: String,
        fileSizeMb: Double,
        pageCount: Int,
    ): Result<com.example.konteksto.ui.host.studio.SlideDeckInfo> {
        val nowFormatted = java.text.SimpleDateFormat("'at' h:mm a", java.util.Locale.US).format(java.util.Date())
        val downloadUrl = try {
            val storageRef = slideDeckStorageRef(sessionId, fileName)
            storageRef.putFile(uri).await()
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Firebase Storage upload failed: ${e.message}")
            return Result.failure(e)
        }

        val deckInfo = com.example.konteksto.ui.host.studio.SlideDeckInfo(
            fileName = fileName,
            fileSizeMb = fileSizeMb,
            pageCount = pageCount,
            uploadTime = "Uploaded $nowFormatted",
            isAnalyzed = false,
        )

        try {
            val firestore = firestoreOrNull
            if (firestore != null && sessionId.isNotBlank()) {
                firestore.collection("sessions").document(sessionId)
                    .update(
                        mapOf(
                            "slideDeckUrl" to downloadUrl,
                            "slideDeckFileName" to fileName,
                            "slideDeckSizeMb" to fileSizeMb,
                            "slideDeckPageCount" to pageCount,
                            "slideDeckUploadTime" to deckInfo.uploadTime,
                            "slideDeckUploadedAt" to System.currentTimeMillis(),
                            "isSlideDeckAnalyzed" to false,
                        ),
                    ).await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Firestore slide deck metadata update failed: ${e.message}")
            deleteSlideDeckFile(sessionId, fileName)
            return Result.failure(e)
        }

        val updatedLocal = localCreatedSessions.value.map { session ->
            if (session.id == sessionId) {
                session.copy(
                    slideDeckFileName = fileName,
                    slideDeckSizeMb = fileSizeMb,
                    slideDeckPageCount = pageCount,
                    slideDeckUploadTime = deckInfo.uploadTime,
                    isSlideDeckAnalyzed = false,
                )
            } else session
        }
        localCreatedSessions.value = updatedLocal
        saveLocalSessions(updatedLocal)

        return Result.success(deckInfo)
    }

    override suspend fun downloadSlideDeckPdf(sessionId: String, fileName: String): Result<ByteArray> {
        return try {
            Result.success(slideDeckStorageRef(sessionId, fileName).getBytes(MAX_SLIDE_DECK_BYTES).await())
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Firebase Storage download failed: ${e.message}")
            Result.failure(e)
        }
    }

    private fun slideDeckStorageRef(sessionId: String, fileName: String): com.google.firebase.storage.StorageReference {
        val bucketUrl = com.example.konteksto.BuildConfig.FIREBASE_STORAGE_BUCKET
        val storage = if (bucketUrl.isNotBlank()) {
            com.google.firebase.storage.FirebaseStorage.getInstance(bucketUrl)
        } else {
            com.google.firebase.storage.FirebaseStorage.getInstance()
        }
        return storage.reference.child("sessions/$sessionId/slide_decks/$fileName")
    }

    private suspend fun deleteSlideDeckFile(sessionId: String, fileName: String) {
        try {
            slideDeckStorageRef(sessionId, fileName).delete().await()
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Failed to delete slide deck file: ${e.message}")
        }
    }

    override suspend fun clearSlideDeck(sessionId: String, fileName: String?): Result<Unit> {
        val updatedLocal = localCreatedSessions.value.map { session ->
            if (session.id == sessionId) {
                session.copy(
                    slideDeckFileName = null,
                    slideDeckSizeMb = null,
                    slideDeckPageCount = null,
                    slideDeckUploadTime = null,
                    isSlideDeckAnalyzed = false,
                    analyzedContextNotesSignature = null,
                )
            } else session
        }
        localCreatedSessions.value = updatedLocal
        saveLocalSessions(updatedLocal)

        try {
            if (fileName != null) {
                deleteSlideDeckFile(sessionId, fileName)
            }
            val firestore = firestoreOrNull
            if (firestore != null && sessionId.isNotBlank()) {
                firestore.collection("sessions").document(sessionId)
                    .update(
                        mapOf(
                            "slideDeckUrl" to FieldValue.delete(),
                            "slideDeckFileName" to FieldValue.delete(),
                            "slideDeckSizeMb" to FieldValue.delete(),
                            "slideDeckPageCount" to FieldValue.delete(),
                            "slideDeckUploadTime" to FieldValue.delete(),
                            "slideDeckUploadedAt" to FieldValue.delete(),
                            "isSlideDeckAnalyzed" to false,
                            "analyzedContextNotesSignature" to FieldValue.delete(),
                        ),
                    ).await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Failed to clear slide deck metadata in Firestore: ${e.message}")
        }

        return Result.success(Unit)
    }

    override suspend fun saveAnalyzedContextNotesSignature(sessionId: String, signature: String): Result<Unit> {
        val updatedLocal = localCreatedSessions.value.map { session ->
            if (session.id == sessionId) {
                session.copy(analyzedContextNotesSignature = signature)
            } else session
        }
        localCreatedSessions.value = updatedLocal
        saveLocalSessions(updatedLocal)

        try {
            val firestore = firestoreOrNull
            if (firestore != null && sessionId.isNotBlank()) {
                firestore.collection("sessions").document(sessionId)
                    .update("analyzedContextNotesSignature", signature)
                    .await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Failed to save context notes signature: ${e.message}")
        }

        return Result.success(Unit)
    }

    override suspend fun saveAnalyzedSlides(
        sessionId: String,
        analyzedSlides: List<com.example.konteksto.ui.host.studio.AnalyzedSlideItem>,
    ): Result<Unit> {
        val updatedLocal = localCreatedSessions.value.map { session ->
            if (session.id == sessionId) {
                session.copy(isSlideDeckAnalyzed = true)
            } else session
        }
        localCreatedSessions.value = updatedLocal
        saveLocalSessions(updatedLocal)

        try {
            val firestore = firestoreOrNull
            if (firestore != null && sessionId.isNotBlank()) {
                firestore.collection("sessions").document(sessionId)
                    .update("isSlideDeckAnalyzed", true)
                    .await()

                val batch = firestore.batch()
                val collectionRef = firestore.collection("sessions").document(sessionId).collection("analyzed_slides")
                analyzedSlides.forEach { item ->
                    val docRef = collectionRef.document(item.id)
                    batch.set(
                        docRef,
                        mapOf(
                            "id" to item.id,
                            "slideNumber" to item.slideNumber,
                            "title" to item.title,
                            "summary" to (item.summary ?: ""),
                            "explanation" to (item.explanation ?: ""),
                            "keyTakeaway" to (item.keyTakeaway ?: ""),
                            "updatedAt" to System.currentTimeMillis(),
                        ),
                    )
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Failed to save analyzed slides: ${e.message}")
            return Result.failure(e)
        }

        return Result.success(Unit)
    }

    private val localAnalyzedSlidesMap = MutableStateFlow<Map<String, List<com.example.konteksto.ui.host.studio.AnalyzedSlideItem>>>(emptyMap())

    override fun getAnalyzedSlidesStream(sessionId: String): Flow<List<com.example.konteksto.ui.host.studio.AnalyzedSlideItem>> = callbackFlow {
        if (sessionId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val firestore = firestoreOrNull
        if (firestore == null) {
            val job = launch {
                localAnalyzedSlidesMap.collect { map ->
                    trySend(map[sessionId] ?: emptyList())
                }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        val listenerRegistration = try {
            firestore.collection("sessions").document(sessionId)
                .collection("analyzed_slides")
                .orderBy("slideNumber")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(localAnalyzedSlidesMap.value[sessionId] ?: emptyList())
                        return@addSnapshotListener
                    }

                    val remoteSlides = snapshot?.documents?.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val slideNumber = doc.getLong("slideNumber")?.toInt() ?: 1
                        val title = doc.getString("title") ?: ""
                        val summary = doc.getString("summary")
                        val explanation = doc.getString("explanation")
                        val keyTakeaway = doc.getString("keyTakeaway")

                        com.example.konteksto.ui.host.studio.AnalyzedSlideItem(
                            id = id,
                            slideNumber = slideNumber,
                            title = title,
                            summary = summary,
                            explanation = explanation,
                            keyTakeaway = keyTakeaway,
                        )
                    } ?: emptyList()

                    val updatedMap = localAnalyzedSlidesMap.value + (sessionId to remoteSlides)
                    localAnalyzedSlidesMap.value = updatedMap
                    trySend(remoteSlides)
                }
        } catch (_: Exception) {
            null
        }

        awaitClose {
            listenerRegistration?.remove()
        }
    }

    private val localContextNotesMap = MutableStateFlow<Map<String, List<com.example.konteksto.ui.host.studio.ContextNoteItem>>>(emptyMap())

    override fun getContextNotesStream(sessionId: String): Flow<List<com.example.konteksto.ui.host.studio.ContextNoteItem>> = callbackFlow {
        if (sessionId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val firestore = firestoreOrNull
        if (firestore == null) {
            val job = launch {
                localContextNotesMap.collect { map ->
                    trySend(map[sessionId] ?: emptyList())
                }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        val listenerRegistration = try {
            firestore.collection("sessions").document(sessionId)
                .collection("context_notes")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(localContextNotesMap.value[sessionId] ?: emptyList())
                        return@addSnapshotListener
                    }

                    val remoteNotes = snapshot?.documents?.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val category = doc.getString("category") ?: "GENERAL"
                        val title = doc.getString("title") ?: ""
                        val content = doc.getString("content") ?: doc.getString("text") ?: ""
                        val timestamp = doc.getString("timestamp") ?: ""
                        if (title.isNotBlank() || content.isNotBlank()) {
                            com.example.konteksto.ui.host.studio.ContextNoteItem(
                                id = id,
                                category = category,
                                title = title,
                                content = content,
                                timestamp = timestamp,
                            )
                        } else null
                    } ?: emptyList()

                    val updatedMap = localContextNotesMap.value + (sessionId to remoteNotes)
                    localContextNotesMap.value = updatedMap
                    trySend(remoteNotes)
                }
        } catch (_: Exception) {
            null
        }

        awaitClose {
            listenerRegistration?.remove()
        }
    }

    override suspend fun addContextNote(
        sessionId: String,
        note: com.example.konteksto.ui.host.studio.ContextNoteItem,
    ): Result<Unit> {
        val currentList = localContextNotesMap.value[sessionId] ?: emptyList()
        val updatedList = currentList + note
        localContextNotesMap.value = localContextNotesMap.value + (sessionId to updatedList)

        try {
            val firestore = firestoreOrNull
            if (firestore != null && sessionId.isNotBlank()) {
                val docRef = firestore.collection("sessions").document(sessionId)
                    .collection("context_notes").document(note.id)
                docRef.set(
                    mapOf(
                        "id" to note.id,
                        "category" to note.category,
                        "title" to note.title,
                        "content" to note.content,
                        "timestamp" to note.timestamp,
                        "updatedAt" to System.currentTimeMillis(),
                    ),
                ).await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Failed to add context note: ${e.message}")
        }

        return Result.success(Unit)
    }

    override suspend fun updateContextNote(
        sessionId: String,
        note: com.example.konteksto.ui.host.studio.ContextNoteItem,
    ): Result<Unit> {
        val currentList = localContextNotesMap.value[sessionId] ?: emptyList()
        val updatedList = currentList.map { if (it.id == note.id) note else it }
        localContextNotesMap.value = localContextNotesMap.value + (sessionId to updatedList)

        try {
            val firestore = firestoreOrNull
            if (firestore != null && sessionId.isNotBlank()) {
                val docRef = firestore.collection("sessions").document(sessionId)
                    .collection("context_notes").document(note.id)
                docRef.set(
                    mapOf(
                        "id" to note.id,
                        "category" to note.category,
                        "title" to note.title,
                        "content" to note.content,
                        "timestamp" to note.timestamp,
                        "updatedAt" to System.currentTimeMillis(),
                    ),
                ).await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Failed to update context note: ${e.message}")
        }

        return Result.success(Unit)
    }

    override suspend fun deleteContextNote(sessionId: String, noteId: String): Result<Unit> {
        val currentList = localContextNotesMap.value[sessionId] ?: emptyList()
        val updatedList = currentList.filter { it.id != noteId }
        localContextNotesMap.value = localContextNotesMap.value + (sessionId to updatedList)

        try {
            val firestore = firestoreOrNull
            if (firestore != null && sessionId.isNotBlank()) {
                firestore.collection("sessions").document(sessionId)
                    .collection("context_notes").document(noteId)
                    .delete().await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Failed to delete context note: ${e.message}")
        }

        return Result.success(Unit)
    }

    private val localDomainTermsMap = MutableStateFlow<Map<String, List<com.example.konteksto.ui.host.studio.DomainTermItem>>>(emptyMap())

    override fun getDomainTermsStream(sessionId: String): Flow<List<com.example.konteksto.ui.host.studio.DomainTermItem>> = callbackFlow {
        if (sessionId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val firestore = firestoreOrNull
        if (firestore == null) {
            val job = launch {
                localDomainTermsMap.collect { map ->
                    trySend(map[sessionId] ?: emptyList())
                }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        val listenerRegistration = try {
            firestore.collection("sessions").document(sessionId)
                .collection("domain_terms")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(localDomainTermsMap.value[sessionId] ?: emptyList())
                        return@addSnapshotListener
                    }

                    val remoteTerms = snapshot?.documents?.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val term = doc.getString("term") ?: ""
                        val category = doc.getString("category") ?: "TECHNICAL TERM"
                        val description = doc.getString("description") ?: ""

                        if (term.isNotBlank()) {
                            com.example.konteksto.ui.host.studio.DomainTermItem(
                                id = id,
                                term = term,
                                category = category,
                                description = description,
                            )
                        } else null
                    } ?: emptyList()

                    val updatedMap = localDomainTermsMap.value + (sessionId to remoteTerms)
                    localDomainTermsMap.value = updatedMap
                    trySend(remoteTerms)
                }
        } catch (_: Exception) {
            null
        }

        awaitClose {
            listenerRegistration?.remove()
        }
    }

    override suspend fun saveDomainTerms(
        sessionId: String,
        terms: List<com.example.konteksto.ui.host.studio.DomainTermItem>,
    ): Result<Unit> {
        val cleaned = com.example.konteksto.ui.host.studio.cleanDomainTerms(terms)
        localDomainTermsMap.value = localDomainTermsMap.value + (sessionId to cleaned)

        try {
            val firestore = firestoreOrNull
            if (firestore != null && sessionId.isNotBlank()) {
                val batch = firestore.batch()
                val collectionRef = firestore.collection("sessions").document(sessionId).collection("domain_terms")
                terms.forEach { item ->
                    val docRef = collectionRef.document(item.id)
                    batch.set(
                        docRef,
                        mapOf(
                            "id" to item.id,
                            "term" to item.term,
                            "category" to item.category,
                            "description" to item.description,
                            "updatedAt" to System.currentTimeMillis(),
                        ),
                    )
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Failed to save domain terms: ${e.message}")
            return Result.failure(e)
        }

        return Result.success(Unit)
    }

    override suspend fun updateDomainTerm(
        sessionId: String,
        term: com.example.konteksto.ui.host.studio.DomainTermItem,
    ): Result<Unit> {
        val currentList = localDomainTermsMap.value[sessionId] ?: emptyList()
        val updatedList = com.example.konteksto.ui.host.studio.cleanDomainTerms(
            currentList.map { if (it.id == term.id) term else it }
        )
        localDomainTermsMap.value = localDomainTermsMap.value + (sessionId to updatedList)

        try {
            val firestore = firestoreOrNull
            if (firestore != null && sessionId.isNotBlank()) {
                val docRef = firestore.collection("sessions").document(sessionId)
                    .collection("domain_terms").document(term.id)
                docRef.set(
                    mapOf(
                        "id" to term.id,
                        "term" to term.term,
                        "category" to term.category,
                        "description" to term.description,
                        "updatedAt" to System.currentTimeMillis(),
                    ),
                ).await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Failed to update domain term: ${e.message}")
        }

        return Result.success(Unit)
    }

    override suspend fun deleteDomainTerm(sessionId: String, termId: String): Result<Unit> {
        val currentList = localDomainTermsMap.value[sessionId] ?: emptyList()
        val updatedList = currentList.filter { it.id != termId }
        localDomainTermsMap.value = localDomainTermsMap.value + (sessionId to updatedList)

        try {
            val firestore = firestoreOrNull
            if (firestore != null && sessionId.isNotBlank()) {
                firestore.collection("sessions").document(sessionId)
                    .collection("domain_terms").document(termId)
                    .delete().await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Failed to delete domain term: ${e.message}")
        }

        return Result.success(Unit)
    }

    private fun loadLocalSessions(): List<HostSession> {
        val prefs = context?.getSharedPreferences("konteksto_sessions_prefs", Context.MODE_PRIVATE)
        val jsonString = prefs?.getString("saved_sessions", null)
        if (jsonString.isNullOrBlank()) {
            return emptyList()
        }
        return try {
            val array = JSONArray(jsonString)
            (0 until array.length()).map { i ->
                array.getJSONObject(i).toHostSessionFromJsonObject()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveLocalSessions(sessions: List<HostSession>) {
        val prefs = context?.getSharedPreferences("konteksto_sessions_prefs", Context.MODE_PRIVATE)
            ?: return
        try {
            val array = JSONArray()
            sessions.forEach { array.put(it.toJsonObject()) }
            prefs.edit().putString("saved_sessions", array.toString()).apply()
        } catch (_: Exception) { }
    }

    private fun loadJoinedSessionIds(userUid: String): Set<String> {
        val key = if (userUid.isNotBlank()) "joined_ids_$userUid" else "joined_ids"
        val prefs = context?.getSharedPreferences("konteksto_joined_prefs", Context.MODE_PRIVATE)
            ?: return emptySet()
        val set = prefs.getStringSet(key, null)
        return set ?: emptySet()
    }

    private fun saveJoinedSessionIds(userUid: String, ids: Set<String>) {
        val key = if (userUid.isNotBlank()) "joined_ids_$userUid" else "joined_ids"
        val prefs = context?.getSharedPreferences("konteksto_joined_prefs", Context.MODE_PRIVATE)
            ?: return
        prefs.edit().putStringSet(key, ids).apply()
    }

    private fun HostSession.toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("venue", venue)
        put("time", time)
        put("talkCode", talkCode)
        put("title", title)
        put("speakerName", speakerName)
        put("attendeesCount", attendeesCount)
        put("isLive", isLive)
        put("bannerImageUrl", bannerImageUrl ?: "")
        put("category", category ?: "")
        put("hostUid", hostUid ?: "")
        put("startTime", startTime ?: "")
        put("endTime", endTime ?: "")
        put("status", status)
        put("slideDeckFileName", slideDeckFileName ?: "")
        put("slideDeckSizeMb", slideDeckSizeMb ?: 0.0)
        put("slideDeckPageCount", slideDeckPageCount ?: 1)
        put("slideDeckUploadTime", slideDeckUploadTime ?: "")
        put("isSlideDeckAnalyzed", isSlideDeckAnalyzed)
        put("analyzedContextNotesSignature", analyzedContextNotesSignature)
    }

    private fun JSONObject.toHostSessionFromJsonObject(): HostSession {
        val rawStatus = optString("status").ifEmpty { "not_live" }
        val statusLower = rawStatus.lowercase()
        return HostSession(
            id = getString("id"),
            venue = getString("venue"),
            time = getString("time"),
            talkCode = getString("talkCode"),
            title = getString("title"),
            speakerName = getString("speakerName"),
            attendeesCount = getInt("attendeesCount"),
            isLive = optBoolean("isLive", statusLower == "live"),
            bannerImageUrl = optString("bannerImageUrl").ifEmpty { null },
            category = optString("category").ifEmpty { null },
            hostUid = optString("hostUid").ifEmpty { null },
            startTime = optString("startTime").ifEmpty { null },
            endTime = optString("endTime").ifEmpty { null },
            status = rawStatus,
            slideDeckFileName = optString("slideDeckFileName").ifEmpty { null },
            slideDeckSizeMb = optDouble("slideDeckSizeMb").takeIf { !it.isNaN() && it > 0.0 },
            slideDeckPageCount = optInt("slideDeckPageCount", 1),
            slideDeckUploadTime = optString("slideDeckUploadTime").ifEmpty { null },
            isSlideDeckAnalyzed = optBoolean("isSlideDeckAnalyzed", false),
            analyzedContextNotesSignature = if (isNull("analyzedContextNotesSignature")) null else getString("analyzedContextNotesSignature"),
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toHostSession(): HostSession? {
        val id = getString("sessionId") ?: id
        val title = getString("title") ?: return null
        val speaker = getString("speakerName") ?: ""
        val venue = getString("venue") ?: "Main Hall"
        val talkCode = getString("talkCode") ?: "TALK-101"
        val startTime = getString("startTime") ?: ""
        val endTime = getString("endTime") ?: ""
        val rawTime = getString("time") ?: ""
        val formattedTime = if (startTime.isNotBlank() && endTime.isNotBlank()) {
            "$startTime - $endTime"
        } else rawTime.ifBlank { "10:00 AM" }

        val attendees = getLong("attendeesCount")?.toInt() ?: 0
        val rawStatus = getString("status") ?: "not_live"
        val statusLower = rawStatus.lowercase()
        val bannerUrl = getString("bannerImageUrl")
        val category = getString("category")
        val hostUid = getString("hostUid")
        val deckFileName = getString("slideDeckFileName")
        val deckSizeMb = getDouble("slideDeckSizeMb")
        val deckPageCount = getLong("slideDeckPageCount")?.toInt() ?: 1
        val deckUploadTime = getString("slideDeckUploadTime")
        val deckAnalyzed = getBoolean("isSlideDeckAnalyzed") ?: false

        return HostSession(
            id = id,
            venue = venue,
            time = formattedTime,
            talkCode = talkCode,
            title = title,
            speakerName = speaker,
            attendeesCount = attendees,
            isLive = statusLower == "live",
            bannerImageUrl = bannerUrl,
            category = category,
            hostUid = hostUid,
            startTime = startTime,
            endTime = endTime,
            status = rawStatus,
            slideDeckFileName = deckFileName,
            slideDeckSizeMb = deckSizeMb,
            slideDeckPageCount = deckPageCount,
            slideDeckUploadTime = deckUploadTime,
            isSlideDeckAnalyzed = deckAnalyzed,
            analyzedContextNotesSignature = getString("analyzedContextNotesSignature"),
        )
    }
}
