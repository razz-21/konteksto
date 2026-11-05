package com.example.konteksto.data.repository

import com.example.konteksto.ui.sessions.studio.LiveConceptExplanationItem
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await

interface ExplanationRepository {
    suspend fun explainSelection(sessionId: String, segmentIds: List<String>): Result<LiveConceptExplanationItem>
}

class FirebaseExplanationRepository(
    private val functionsProvider: () -> FirebaseFunctions = { FirebaseFunctions.getInstance("asia-southeast1") }
) : ExplanationRepository {
    
    private val functionsOrNull: FirebaseFunctions?
        get() = try {
            functionsProvider()
        } catch (_: Exception) {
            null
        }

    override suspend fun explainSelection(
        sessionId: String,
        segmentIds: List<String>
    ): Result<LiveConceptExplanationItem> {
        val functions = functionsOrNull
            ?: return Result.failure(IllegalStateException("Firebase Functions is not available."))
            
        try {
            val data = mapOf(
                "sessionId" to sessionId,
                "segmentIds" to segmentIds
            )
            
            val result = functions
                .getHttpsCallable("explainSelection")
                .call(data)
                .await()
                
            val responseData = result.data as? Map<*, *>
                ?: return Result.failure(IllegalStateException("Invalid response from server."))
                
            val isExplainable = responseData["isExplainable"] as? Boolean ?: true
            if (!isExplainable) {
                val msg = responseData["message"] as? String ?: "The selected segment doesn't contain a clear concept to explain."
                return Result.failure(Exception(msg))
            }
                
            val item = LiveConceptExplanationItem(
                id = responseData["id"] as? String ?: "",
                type = responseData["type"] as? String ?: "",
                timestamp = responseData["timestamp"] as? String ?: "",
                percentGrounded = (responseData["percentGrounded"] as? Number)?.toInt() ?: 0,
                title = responseData["title"] as? String ?: "",
                definition = responseData["definition"] as? String ?: "",
                analogy = responseData["analogy"] as? String ?: "",
                whyItMatters = responseData["whyItMatters"] as? String ?: "",
                isBookmarked = false
            )
            
            return Result.success(item)
        } catch (e: Exception) {
            val msg = if (e is FirebaseFunctionsException) {
                if (e.code == FirebaseFunctionsException.Code.NOT_FOUND) {
                    "Cloud Function not found. Please deploy 'explainSelection' to Firebase."
                } else {
                    e.message ?: "An error occurred with the AI service."
                }
            } else {
                e.message ?: "Failed to generate explanation."
            }
            return Result.failure(Exception(msg))
        }
    }
}
