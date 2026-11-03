package com.example.konteksto.data.repository

import android.content.Context
import android.util.Log
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.host.studio.TranscriptEntry
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
}

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
        )
    }
}
