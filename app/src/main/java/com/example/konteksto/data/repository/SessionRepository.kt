package com.example.konteksto.data.repository

import android.content.Context
import android.util.Log
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.host.studio.TranscriptEntry
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

interface SessionRepository {
    fun getHostSessionsStream(hostUid: String): Flow<List<HostSession>>
    fun getAllLiveSessionsStream(): Flow<List<HostSession>>
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
    fun getTranscriptsStream(sessionId: String): Flow<List<TranscriptEntry>>
    suspend fun addTranscriptEntry(
        sessionId: String,
        speakerName: String,
        text: String,
        isSystemEvent: Boolean = false,
    ): Result<TranscriptEntry>
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

    private val firestoreOrNull: FirebaseFirestore?
        get() = try {
            firestoreProvider()
        } catch (_: Exception) {
            null
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

        // Send local cached sessions for this hostUid immediately for fast initial rendering
        val initialFiltered = localCreatedSessions.value.filter { it.hostUid == hostUid }
        trySend(initialFiltered)

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
                        // Remote is SSOT for hostUid: update local persistence
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
            isLive = false, // Newly created session is not live yet
            bannerImageUrl = bannerImageUrl,
            category = category,
            hostUid = hostUid,
            startTime = startTime,
            endTime = endTime,
            status = "not_live",
        )

        // Store locally and persist to device disk immediately
        val updatedLocal = listOf(newSession) + localCreatedSessions.value.filter { it.id != sessionId }
        localCreatedSessions.value = updatedLocal
        saveLocalSessions(updatedLocal)

        // Try syncing to Firestore in background
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

    override fun getTranscriptsStream(sessionId: String): Flow<List<TranscriptEntry>> = callbackFlow {
        if (sessionId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val firestore = firestoreOrNull
        if (firestore == null) {
            val job = launch {
                localTranscriptsMap.collect { map ->
                    trySend(map[sessionId] ?: emptyList())
                }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        val initialLocal = localTranscriptsMap.value[sessionId] ?: emptyList()
        trySend(initialLocal)

        val listenerRegistration = try {
            firestore.collection("sessions").document(sessionId)
                .collection("transcripts")
                .orderBy("createdAt")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(localTranscriptsMap.value[sessionId] ?: emptyList())
                        return@addSnapshotListener
                    }

                    val remoteTranscripts = snapshot?.documents?.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val speaker = doc.getString("speakerName") ?: "Speaker"
                        val time = doc.getString("timestamp") ?: ""
                        val text = doc.getString("text") ?: ""
                        val isSystem = doc.getBoolean("isSystemEvent") ?: speaker.equals("SYSTEM", ignoreCase = true)
                        if (text.isNotBlank()) TranscriptEntry(id, speaker, time, text, isSystem) else null
                    } ?: emptyList()

                    val updatedMap = localTranscriptsMap.value + (sessionId to remoteTranscripts)
                    localTranscriptsMap.value = updatedMap
                    trySend(remoteTranscripts)
                }
        } catch (_: Exception) {
            null
        }

        awaitClose {
            listenerRegistration?.remove()
        }
    }

    override suspend fun addTranscriptEntry(
        sessionId: String,
        speakerName: String,
        text: String,
        isSystemEvent: Boolean,
    ): Result<TranscriptEntry> {
        val transcriptId = "tr_${System.currentTimeMillis()}"
        val nowFormatted = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US).format(java.util.Date())
        val entry = TranscriptEntry(
            id = transcriptId,
            speakerName = speakerName.ifBlank { "Speaker" },
            timestamp = nowFormatted,
            text = text.trim(),
            isSystemEvent = isSystemEvent || speakerName.equals("SYSTEM", ignoreCase = true),
        )

        val currentList = localTranscriptsMap.value[sessionId] ?: emptyList()
        val updatedList = currentList + entry
        localTranscriptsMap.value = localTranscriptsMap.value + (sessionId to updatedList)

        try {
            val firestore = firestoreOrNull
            if (firestore != null) {
                val docRef = firestore.collection("sessions").document(sessionId)
                    .collection("transcripts").document(transcriptId)
                val data = hashMapOf<String, Any?>(
                    "id" to transcriptId,
                    "speakerName" to entry.speakerName,
                    "timestamp" to entry.timestamp,
                    "text" to entry.text,
                    "isSystemEvent" to entry.isSystemEvent,
                    "createdAt" to System.currentTimeMillis(),
                )
                docRef.set(data).await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Failed to add transcript to Firestore: ${e.message}")
        }

        return Result.success(entry)
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
                            "analyzedContextNotesSignature" to FieldValue.delete(),
                        ),
                    ).await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Failed to clear slide deck metadata: ${e.message}")
            return Result.failure(e)
        }

        // Only delete the file once nothing points at it, so a failed clear leaves a working deck.
        if (!fileName.isNullOrBlank()) {
            deleteSlideDeckFile(sessionId, fileName)
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
            Log.e("FirebaseSessionRepo", "Failed to save analyzed context notes signature: ${e.message}")
            return Result.failure(e)
        }

        return Result.success(Unit)
    }

    override suspend fun saveAnalyzedSlides(
        sessionId: String,
        analyzedSlides: List<com.example.konteksto.ui.host.studio.AnalyzedSlideItem>,
    ): Result<Unit> {
        val updatedLocal = localCreatedSessions.value.map { session ->
            if (session.id == sessionId) {
                session.copy(isSlideDeckAnalyzed = analyzedSlides.isNotEmpty())
            } else session
        }
        localCreatedSessions.value = updatedLocal
        saveLocalSessions(updatedLocal)

        val slidesList = analyzedSlides.map { slide ->
            mapOf(
                "id" to slide.id,
                "slideNumber" to slide.slideNumber,
                "title" to slide.title,
                "summary" to (slide.summary ?: ""),
                "explanation" to (slide.explanation ?: ""),
                "keyTakeaway" to (slide.keyTakeaway ?: ""),
            )
        }

        try {
            val firestore = firestoreOrNull
            if (firestore != null && sessionId.isNotBlank()) {
                firestore.collection("sessions").document(sessionId)
                    .update(
                        mapOf(
                            "analyzedSlides" to slidesList,
                            "isSlideDeckAnalyzed" to analyzedSlides.isNotEmpty(),
                            "analyzedSlidesCount" to analyzedSlides.size,
                            "analyzedAt" to System.currentTimeMillis(),
                        ),
                    ).await()

                val slidesRef = firestore.collection("sessions").document(sessionId)
                    .collection("analyzed_slides")
                val keptIds = analyzedSlides.map { it.id }.toSet()
                val existingSlides = slidesRef.get().await()

                val batch = firestore.batch()
                existingSlides.documents
                    .filter { it.id !in keptIds }
                    .forEach { batch.delete(it.reference) }
                analyzedSlides.forEach { slide ->
                    val docRef = slidesRef.document(slide.id)
                    batch.set(
                        docRef,
                        mapOf(
                            "id" to slide.id,
                            "slideNumber" to slide.slideNumber,
                            "title" to slide.title,
                            "summary" to (slide.summary ?: ""),
                            "explanation" to (slide.explanation ?: ""),
                            "keyTakeaway" to (slide.keyTakeaway ?: ""),
                            "analyzedAt" to System.currentTimeMillis(),
                        ),
                    )
                }
                batch.commit().await()
                Log.d("FirebaseSessionRepo", "Saved ${analyzedSlides.size} analyzed slides to Firestore for session $sessionId")
            }
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Failed to save analyzed slides to Firestore: ${e.message}")
            return Result.failure(e)
        }

        return Result.success(Unit)
    }

    override fun getAnalyzedSlidesStream(sessionId: String): Flow<List<com.example.konteksto.ui.host.studio.AnalyzedSlideItem>> = callbackFlow {
        val firestore = firestoreOrNull
        if (firestore == null || sessionId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore.collection("sessions").document(sessionId)
            .collection("analyzed_slides")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val slides = snapshot?.documents?.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val slideNumber = doc.getLong("slideNumber")?.toInt() ?: 1
                    val title = doc.getString("title") ?: "Slide $slideNumber"
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
                }?.sortedBy { it.slideNumber } ?: emptyList()

                trySend(slides)
            }

        awaitClose { listener.remove() }
    }

    private val localContextNotesMap =
        MutableStateFlow<Map<String, List<com.example.konteksto.ui.host.studio.ContextNoteItem>>>(emptyMap())

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
                .orderBy("createdAt")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(localContextNotesMap.value[sessionId] ?: emptyList())
                        return@addSnapshotListener
                    }

                    val remoteNotes = snapshot?.documents?.mapNotNull { doc ->
                        val title = doc.getString("title") ?: return@mapNotNull null
                        com.example.konteksto.ui.host.studio.ContextNoteItem(
                            id = doc.getString("id") ?: doc.id,
                            category = doc.getString("category") ?: "BACKGROUND NOTE",
                            title = title,
                            content = doc.getString("content") ?: "",
                            timestamp = doc.getString("timestamp") ?: "",
                        )
                    } ?: emptyList()

                    localContextNotesMap.value = localContextNotesMap.value + (sessionId to remoteNotes)
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
        val currentNotes = localContextNotesMap.value[sessionId] ?: emptyList()
        localContextNotesMap.value = localContextNotesMap.value + (sessionId to currentNotes + note)

        try {
            val firestore = firestoreOrNull
            if (firestore != null && sessionId.isNotBlank()) {
                firestore.collection("sessions").document(sessionId)
                    .collection("context_notes").document(note.id)
                    .set(
                        mapOf(
                            "id" to note.id,
                            "category" to note.category,
                            "title" to note.title,
                            "content" to note.content,
                            "timestamp" to note.timestamp,
                            "createdAt" to System.currentTimeMillis(),
                        ),
                    ).await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Failed to save context note to Firestore: ${e.message}")
            val remainingNotes = (localContextNotesMap.value[sessionId] ?: emptyList()).filter { it.id != note.id }
            localContextNotesMap.value = localContextNotesMap.value + (sessionId to remainingNotes)
            return Result.failure(e)
        }

        return Result.success(Unit)
    }

    override suspend fun updateContextNote(
        sessionId: String,
        note: com.example.konteksto.ui.host.studio.ContextNoteItem,
    ): Result<Unit> {
        val previousNotes = localContextNotesMap.value[sessionId] ?: emptyList()
        val updatedNotes = previousNotes.map { if (it.id == note.id) note else it }
        localContextNotesMap.value = localContextNotesMap.value + (sessionId to updatedNotes)

        try {
            val firestore = firestoreOrNull
            if (firestore != null && sessionId.isNotBlank()) {
                // Leaves createdAt untouched so the note keeps its place in the list.
                firestore.collection("sessions").document(sessionId)
                    .collection("context_notes").document(note.id)
                    .update(
                        mapOf(
                            "title" to note.title,
                            "content" to note.content,
                        ),
                    ).await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Failed to update context note in Firestore: ${e.message}")
            localContextNotesMap.value = localContextNotesMap.value + (sessionId to previousNotes)
            return Result.failure(e)
        }

        return Result.success(Unit)
    }

    override suspend fun deleteContextNote(sessionId: String, noteId: String): Result<Unit> {
        val previousNotes = localContextNotesMap.value[sessionId] ?: emptyList()
        val remainingNotes = previousNotes.filter { it.id != noteId }
        localContextNotesMap.value = localContextNotesMap.value + (sessionId to remainingNotes)

        try {
            val firestore = firestoreOrNull
            if (firestore != null && sessionId.isNotBlank()) {
                firestore.collection("sessions").document(sessionId)
                    .collection("context_notes").document(noteId)
                    .delete().await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Failed to delete context note from Firestore: ${e.message}")
            localContextNotesMap.value = localContextNotesMap.value + (sessionId to previousNotes)
            return Result.failure(e)
        }

        return Result.success(Unit)
    }

    private val localDomainTermsMap =
        MutableStateFlow<Map<String, List<com.example.konteksto.ui.host.studio.DomainTermItem>>>(emptyMap())

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

        trySend(localDomainTermsMap.value[sessionId] ?: emptyList())

        val listenerRegistration = try {
            firestore.collection("sessions").document(sessionId)
                .collection("glossary")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(localDomainTermsMap.value[sessionId] ?: emptyList())
                        return@addSnapshotListener
                    }

                    val remoteTerms = snapshot?.documents?.mapNotNull { doc ->
                        val term = doc.getString("term") ?: return@mapNotNull null
                        val definition = doc.getString("definition") ?: return@mapNotNull null
                        com.example.konteksto.ui.host.studio.DomainTermItem(
                            id = doc.getString("termId") ?: doc.id,
                            term = term,
                            category = doc.getString("category") ?: "TECHNICAL TERM",
                            description = definition,
                        )
                    }?.sortedBy { it.term.lowercase() } ?: emptyList()

                    localDomainTermsMap.value = localDomainTermsMap.value + (sessionId to remoteTerms)
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
        localDomainTermsMap.value = localDomainTermsMap.value + (sessionId to terms)

        try {
            val firestore = firestoreOrNull
            if (firestore != null && sessionId.isNotBlank()) {
                val glossaryRef = firestore.collection("sessions").document(sessionId)
                    .collection("glossary")
                val keptIds = terms.map { it.id }.toSet()
                // Only replaces terms from slide analysis, leaving any added from other sources.
                val existingTerms = glossaryRef.whereEqualTo("source", "slides").get().await()

                val batch = firestore.batch()
                existingTerms.documents
                    .filter { it.id !in keptIds }
                    .forEach { batch.delete(it.reference) }
                terms.forEach { term ->
                    batch.set(
                        glossaryRef.document(term.id),
                        mapOf(
                            "termId" to term.id,
                            "term" to term.term,
                            "category" to term.category,
                            "definition" to term.description,
                            "source" to "slides",
                            "timestamp" to System.currentTimeMillis(),
                        ),
                    )
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Failed to save domain terms to Firestore: ${e.message}")
            return Result.failure(e)
        }

        return Result.success(Unit)
    }

    override suspend fun updateDomainTerm(
        sessionId: String,
        term: com.example.konteksto.ui.host.studio.DomainTermItem,
    ): Result<Unit> {
        val previousTerms = localDomainTermsMap.value[sessionId] ?: emptyList()
        val updatedTerms = previousTerms.map { if (it.id == term.id) term else it }
            .sortedBy { it.term.lowercase() }
        localDomainTermsMap.value = localDomainTermsMap.value + (sessionId to updatedTerms)

        try {
            val firestore = firestoreOrNull
            if (firestore != null && sessionId.isNotBlank()) {
                firestore.collection("sessions").document(sessionId)
                    .collection("glossary").document(term.id)
                    .update(
                        mapOf(
                            "term" to term.term,
                            "category" to term.category,
                            "definition" to term.description,
                        ),
                    ).await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Failed to update domain term in Firestore: ${e.message}")
            localDomainTermsMap.value = localDomainTermsMap.value + (sessionId to previousTerms)
            return Result.failure(e)
        }

        return Result.success(Unit)
    }

    override suspend fun deleteDomainTerm(sessionId: String, termId: String): Result<Unit> {
        val previousTerms = localDomainTermsMap.value[sessionId] ?: emptyList()
        val remainingTerms = previousTerms.filter { it.id != termId }
        localDomainTermsMap.value = localDomainTermsMap.value + (sessionId to remainingTerms)

        try {
            val firestore = firestoreOrNull
            if (firestore != null && sessionId.isNotBlank()) {
                firestore.collection("sessions").document(sessionId)
                    .collection("glossary").document(termId)
                    .delete().await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSessionRepo", "Failed to delete domain term from Firestore: ${e.message}")
            localDomainTermsMap.value = localDomainTermsMap.value + (sessionId to previousTerms)
            return Result.failure(e)
        }

        return Result.success(Unit)
    }

    private fun loadLocalSessions(): List<HostSession> {
        val prefs = context?.getSharedPreferences("konteksto_sessions_prefs", Context.MODE_PRIVATE)
            ?: return emptyList()
        val jsonString = prefs.getString("saved_sessions", null) ?: return emptyList()
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
