package com.example.konteksto.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

data class GlossaryTermItem(
    val id: String,
    val term: String,
    val category: String = "TECHNICAL TERM",
    val definition: String,
    val source: String = "transcript",
    val sessionId: String = "",
    val sessionTitle: String = "",
    val isBookmarked: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
)

interface GlossaryRepository {
    fun getSavedGlossaryTermsStream(userUid: String): Flow<List<GlossaryTermItem>>
    fun getSessionGlossaryStream(sessionId: String): Flow<List<GlossaryTermItem>>
    suspend fun saveTermToGlossary(
        userUid: String,
        sessionId: String,
        sessionTitle: String,
        term: GlossaryTermItem,
    ): Result<Unit>
    suspend fun removeTermFromGlossary(
        userUid: String,
        sessionId: String,
        termId: String,
    ): Result<Unit>
    suspend fun isTermBookmarked(userUid: String, termId: String): Boolean
}

class FirebaseGlossaryRepository(
    private val context: Context? = null,
    private val authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() },
) : GlossaryRepository {

    private val localSavedTerms = MutableStateFlow<List<GlossaryTermItem>>(loadLocalSavedTerms())

    private val firestoreOrNull: FirebaseFirestore?
        get() = try {
            firestoreProvider()
        } catch (_: Exception) {
            null
        }

    override fun getSavedGlossaryTermsStream(userUid: String): Flow<List<GlossaryTermItem>> = callbackFlow {
        val firestore = firestoreOrNull
        val currentUid = userUid.ifBlank {
            try { authProvider().currentUser?.uid.orEmpty() } catch (_: Exception) { "" }
        }

        if (firestore == null || currentUid.isBlank()) {
            val job = launch {
                localSavedTerms.collect { trySend(it) }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        trySend(localSavedTerms.value)

        val listenerRegistration = try {
            firestore.collection("users").document(currentUid)
                .collection("saved_glossary")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(localSavedTerms.value)
                        return@addSnapshotListener
                    }

                    val remoteTerms = snapshot?.documents?.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val term = doc.getString("term") ?: ""
                        val category = doc.getString("category") ?: "TECHNICAL TERM"
                        val definition = doc.getString("definition") ?: doc.getString("description") ?: ""
                        val source = doc.getString("source") ?: "transcript"
                        val sessionId = doc.getString("sessionId") ?: ""
                        val sessionTitle = doc.getString("sessionTitle") ?: ""
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                        if (term.isNotBlank()) {
                            GlossaryTermItem(
                                id = id,
                                term = term,
                                category = category,
                                definition = definition,
                                source = source,
                                sessionId = sessionId,
                                sessionTitle = sessionTitle,
                                isBookmarked = true,
                                timestamp = timestamp,
                            )
                        } else null
                    } ?: emptyList()

                    localSavedTerms.value = remoteTerms
                    saveLocalSavedTerms(remoteTerms)
                    trySend(remoteTerms)
                }
        } catch (_: Exception) {
            null
        }

        awaitClose { listenerRegistration?.remove() }
    }

    override fun getSessionGlossaryStream(sessionId: String): Flow<List<GlossaryTermItem>> = callbackFlow {
        if (sessionId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val firestore = firestoreOrNull
        if (firestore == null) {
            val job = launch {
                localSavedTerms.collect { terms ->
                    trySend(terms.filter { it.sessionId == sessionId })
                }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        val listenerRegistration = try {
            firestore.collection("sessions").document(sessionId)
                .collection("glossary")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(localSavedTerms.value.filter { it.sessionId == sessionId })
                        return@addSnapshotListener
                    }

                    val remoteTerms = snapshot?.documents?.mapNotNull { doc ->
                        val id = doc.getString("termId") ?: doc.getString("id") ?: doc.id
                        val term = doc.getString("term") ?: ""
                        val category = doc.getString("category") ?: "TECHNICAL TERM"
                        val definition = doc.getString("definition") ?: doc.getString("description") ?: ""
                        val source = doc.getString("source") ?: "transcript"
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                        if (term.isNotBlank()) {
                            GlossaryTermItem(
                                id = id,
                                term = term,
                                category = category,
                                definition = definition,
                                source = source,
                                sessionId = sessionId,
                                isBookmarked = localSavedTerms.value.any { it.id == id },
                                timestamp = timestamp,
                            )
                        } else null
                    } ?: emptyList()

                    trySend(remoteTerms)
                }
        } catch (_: Exception) {
            null
        }

        awaitClose { listenerRegistration?.remove() }
    }

    override suspend fun saveTermToGlossary(
        userUid: String,
        sessionId: String,
        sessionTitle: String,
        term: GlossaryTermItem,
    ): Result<Unit> {
        val currentUid = userUid.ifBlank {
            try { authProvider().currentUser?.uid.orEmpty() } catch (_: Exception) { "" }
        }
        val updatedTerm = term.copy(sessionId = sessionId, sessionTitle = sessionTitle, isBookmarked = true)

        val updatedLocal = (listOf(updatedTerm) + localSavedTerms.value).distinctBy { it.id }
        localSavedTerms.value = updatedLocal
        saveLocalSavedTerms(updatedLocal)

        try {
            val firestore = firestoreOrNull
            if (firestore != null) {
                // 1. Save to session glossary (sessions/{sessionId}/glossary/{termId})
                if (sessionId.isNotBlank()) {
                    val sessionGlossaryRef = firestore.collection("sessions").document(sessionId)
                        .collection("glossary").document(term.id)

                    sessionGlossaryRef.set(
                        mapOf(
                            "termId" to term.id,
                            "term" to term.term,
                            "normalizedKey" to term.term.lowercase().trim(),
                            "category" to term.category,
                            "definition" to term.definition,
                            "source" to term.source,
                            "hostEdited" to false,
                            "hidden" to false,
                            "timestamp" to term.timestamp,
                        ),
                    ).await()
                }

                // 2. Save to user personal glossary (users/{uid}/saved_glossary/{termId})
                if (currentUid.isNotBlank()) {
                    val userGlossaryRef = firestore.collection("users").document(currentUid)
                        .collection("saved_glossary").document(term.id)

                    userGlossaryRef.set(
                        mapOf(
                            "id" to term.id,
                            "term" to term.term,
                            "category" to term.category,
                            "definition" to term.definition,
                            "source" to term.source,
                            "sessionId" to sessionId,
                            "sessionTitle" to sessionTitle,
                            "timestamp" to term.timestamp,
                        ),
                    ).await()
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseGlossaryRepo", "Failed to save term to Firestore: ${e.message}")
        }

        return Result.success(Unit)
    }

    override suspend fun removeTermFromGlossary(
        userUid: String,
        sessionId: String,
        termId: String,
    ): Result<Unit> {
        val currentUid = userUid.ifBlank {
            try { authProvider().currentUser?.uid.orEmpty() } catch (_: Exception) { "" }
        }
        val updatedLocal = localSavedTerms.value.filter { it.id != termId }
        localSavedTerms.value = updatedLocal
        saveLocalSavedTerms(updatedLocal)

        try {
            val firestore = firestoreOrNull
            if (firestore != null && currentUid.isNotBlank()) {
                firestore.collection("users").document(currentUid)
                    .collection("saved_glossary").document(termId)
                    .delete().await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseGlossaryRepo", "Failed to remove term from user glossary: ${e.message}")
        }

        return Result.success(Unit)
    }

    override suspend fun isTermBookmarked(userUid: String, termId: String): Boolean {
        return localSavedTerms.value.any { it.id == termId }
    }

    private fun loadLocalSavedTerms(): List<GlossaryTermItem> {
        val prefs = context?.getSharedPreferences("konteksto_glossary_prefs", Context.MODE_PRIVATE)
        val jsonString = prefs?.getString("saved_terms", null)
        if (jsonString.isNullOrBlank()) return emptyList()

        return try {
            val array = JSONArray(jsonString)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                GlossaryTermItem(
                    id = obj.getString("id"),
                    term = obj.getString("term"),
                    category = obj.optString("category", "TECHNICAL TERM"),
                    definition = obj.getString("definition"),
                    source = obj.optString("source", "transcript"),
                    sessionId = obj.optString("sessionId", ""),
                    sessionTitle = obj.optString("sessionTitle", ""),
                    isBookmarked = true,
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveLocalSavedTerms(terms: List<GlossaryTermItem>) {
        val prefs = context?.getSharedPreferences("konteksto_glossary_prefs", Context.MODE_PRIVATE) ?: return
        try {
            val array = JSONArray()
            terms.forEach { item ->
                array.put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("term", item.term)
                        put("category", item.category)
                        put("definition", item.definition)
                        put("source", item.source)
                        put("sessionId", item.sessionId)
                        put("sessionTitle", item.sessionTitle)
                        put("timestamp", item.timestamp)
                    },
                )
            }
            prefs.edit().putString("saved_terms", array.toString()).apply()
        } catch (_: Exception) { }
    }
}
