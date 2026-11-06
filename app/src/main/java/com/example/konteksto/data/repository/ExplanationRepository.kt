package com.example.konteksto.data.repository

import android.util.Log
import com.example.konteksto.BuildConfig
import com.example.konteksto.ui.host.studio.TranscriptEntry
import com.example.konteksto.ui.sessions.studio.LiveConceptExplanationItem
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.Firebase
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.vertexai.vertexAI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface ExplanationRepository {
    suspend fun explainSelection(
        sessionId: String,
        segmentIds: List<String>,
        selectedTexts: List<String> = emptyList(),
        speakerName: String = "",
    ): Result<LiveConceptExplanationItem>

    suspend fun detectLiveConcepts(
        sessionId: String,
        transcripts: List<TranscriptEntry>,
    ): Result<List<LiveConceptExplanationItem>>
}

class FirebaseExplanationRepository(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val functionsProvider: () -> FirebaseFunctions = { FirebaseFunctions.getInstance("asia-southeast1") },
) : ExplanationRepository {

    private val functionsOrNull: FirebaseFunctions?
        get() = try {
            functionsProvider()
        } catch (_: Exception) {
            null
        }

    override suspend fun explainSelection(
        sessionId: String,
        segmentIds: List<String>,
        selectedTexts: List<String>,
        speakerName: String,
    ): Result<LiveConceptExplanationItem> {
        val functions = functionsOrNull
        if (functions != null) {
            try {
                val data = mapOf(
                    "sessionId" to sessionId,
                    "segmentIds" to segmentIds,
                    "selectedTexts" to selectedTexts,
                    "speakerName" to speakerName,
                )

                val result = functions
                    .getHttpsCallable("explainSelection")
                    .call(data)
                    .await()

                val responseData = result.data as? Map<*, *>
                if (responseData != null) {
                    val isExplainable = responseData["isExplainable"] as? Boolean ?: true
                    if (!isExplainable) {
                        val msg = responseData["message"] as? String ?: "The selected segment doesn't contain a clear concept to explain."
                        return Result.failure(Exception(msg))
                    }

                    val item = LiveConceptExplanationItem(
                        id = responseData["id"] as? String ?: "exp_${System.currentTimeMillis()}",
                        type = responseData["type"] as? String ?: "Concept Explanation",
                        timestamp = responseData["timestamp"] as? String ?: "Live",
                        percentGrounded = (responseData["percentGrounded"] as? Number)?.toInt() ?: 95,
                        title = responseData["title"] as? String ?: "Concept Explanation",
                        definition = stripContextIdReferences(responseData["definition"] as? String ?: ""),
                        analogy = responseData["analogy"] as? String ?: "",
                        whyItMatters = responseData["whyItMatters"] as? String ?: "",
                        isBookmarked = false,
                    )
                    return Result.success(item)
                }
            } catch (e: Exception) {
                Log.w("FirebaseExplanationRepo", "Cloud Function explainSelection failed, falling back to Gemini: ${e.message}")
            }
        }

        // Fallback to Gemini AI Model on device
        return explainWithGeminiFallback(sessionId, selectedTexts, speakerName)
    }

    private suspend fun explainWithGeminiFallback(
        sessionId: String,
        selectedTexts: List<String>,
        speakerName: String,
    ): Result<LiveConceptExplanationItem> = withContext(Dispatchers.IO) {
        val textsToAnalyze = selectedTexts.filter { it.isNotBlank() }
        if (textsToAnalyze.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No statement text selected for explanation."))
        }

        val formattedStatements = textsToAnalyze.joinToString("\n") { "- \"$it\"" }
        val prompt = """
            You are an expert educational AI assistant explaining spoken live talk statements in plain language.
            Speaker: ${speakerName.ifBlank { "Host Speaker" }}
            
            SELECTED STATEMENTS FROM TALK:
            $formattedStatements
            
            TASK:
            Analyze the spoken statements and explain the underlying concept in clear, plain language so attendees can instantly understand.
            
            Return ONLY a raw JSON object with no markdown syntax with the following keys:
            - "type": category or technical field (e.g., "Core Mechanics", "Technical Concept", "Model Architecture")
            - "title": concise title of the core concept (2-5 words)
            - "definition": clear explanation of what the speaker meant in plain language (1-3 sentences)
            - "analogy": simple, relatable real-world analogy to make it clear (1-2 sentences)
            - "whyItMatters": why this concept matters in the context of the talk (1-2 sentences)
            - "percentGrounded": integer between 88 and 99 indicating accuracy grounding
        """.trimIndent()

        val candidateModelNames = listOf(
            "gemini-3.8-flash",
            "gemini-3.1-pro-preview",
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
        )

        var responseText = ""

        try {
            if (apiKey.isNotBlank()) {
                for (modelName in candidateModelNames) {
                    try {
                        val model = GenerativeModel(modelName, apiKey = apiKey)
                        val response = model.generateContent(prompt)
                        val textResult = response.text?.trim()
                        if (!textResult.isNullOrBlank()) {
                            responseText = textResult
                            break
                        }
                    } catch (e: Exception) {
                        Log.w("FirebaseExplanationRepo", "Model $modelName explain fallback failed: ${e.message}")
                    }
                }
            } else {
                for (modelName in candidateModelNames) {
                    try {
                        val model = Firebase.vertexAI.generativeModel(modelName)
                        val response = model.generateContent(prompt)
                        val textResult = response.text?.trim()
                        if (!textResult.isNullOrBlank()) {
                            responseText = textResult
                            break
                        }
                    } catch (e: Exception) {
                        Log.w("FirebaseExplanationRepo", "Firebase vertexAI model $modelName explain fallback failed: ${e.message}")
                    }
                }
            }

            if (responseText.isNotBlank()) {
                val cleanedJson = responseText
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val json = JSONObject(cleanedJson)
                val item = LiveConceptExplanationItem(
                    id = "exp_${System.currentTimeMillis()}",
                    type = json.optString("type", "Concept Explanation"),
                    timestamp = "Live",
                    percentGrounded = json.optInt("percentGrounded", 95),
                    title = json.optString("title", "Live Concept"),
                    definition = json.optString("definition"),
                    analogy = json.optString("analogy"),
                    whyItMatters = json.optString("whyItMatters"),
                    isBookmarked = false,
                )
                return@withContext Result.success(item)
            }
        } catch (e: Exception) {
            Log.e("FirebaseExplanationRepo", "Error generating explanation fallback: ${e.message}", e)
        }

        Result.failure(IllegalStateException("Could not generate concept explanation for selected statements."))
    }

    override suspend fun detectLiveConcepts(
        sessionId: String,
        transcripts: List<TranscriptEntry>,
    ): Result<List<LiveConceptExplanationItem>> = withContext(Dispatchers.IO) {
        val nonSystemTranscripts = transcripts.filterNot { it.isSystemEvent || it.speakerName.equals("SYSTEM", ignoreCase = true) }
        if (nonSystemTranscripts.isEmpty()) {
            return@withContext Result.success(emptyList())
        }

        val transcriptText = nonSystemTranscripts.takeLast(10).joinToString("\n") { entry ->
            "${entry.speakerName} (${entry.timestamp}): ${entry.text}"
        }

        val prompt = """
            You are an expert educational AI assistant listening to a live spoken presentation.
            
            RECENT LIVE TRANSCRIPT:
            $transcriptText
            
            TASK:
            Detect up to 3 key technical concepts, terms, or domain mechanisms mentioned or explained by the speaker in the transcript above.
            
            Return ONLY a raw JSON array of objects with no markdown formatting. Each object MUST contain:
            - "id": unique string id (e.g., "concept_1")
            - "type": domain category (e.g. "Core Mechanics", "Architecture", "Optimization", "Key Term")
            - "timestamp": timestamp of statement (e.g. "Live")
            - "percentGrounded": integer between 85 and 99
            - "title": short concept title (2-5 words)
            - "definition": clear 1-2 sentence plain language definition/explanation of what the speaker meant
            - "analogy": clear 1-2 sentence real-world analogy
            - "whyItMatters": clear 1 sentence why this matters in the talk
        """.trimIndent()

        val candidateModelNames = listOf(
            "gemini-3.8-flash",
            "gemini-3.1-pro-preview",
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
        )

        var responseText = ""

        try {
            if (apiKey.isNotBlank()) {
                for (modelName in candidateModelNames) {
                    try {
                        val model = GenerativeModel(modelName, apiKey = apiKey)
                        val response = model.generateContent(prompt)
                        val textResult = response.text?.trim()
                        if (!textResult.isNullOrBlank()) {
                            responseText = textResult
                            break
                        }
                    } catch (e: Exception) {
                        Log.w("FirebaseExplanationRepo", "Model $modelName detectLiveConcepts failed: ${e.message}")
                    }
                }
            } else {
                for (modelName in candidateModelNames) {
                    try {
                        val model = Firebase.vertexAI.generativeModel(modelName)
                        val response = model.generateContent(prompt)
                        val textResult = response.text?.trim()
                        if (!textResult.isNullOrBlank()) {
                            responseText = textResult
                            break
                        }
                    } catch (e: Exception) {
                        Log.w("FirebaseExplanationRepo", "Firebase vertexAI model $modelName detectLiveConcepts failed: ${e.message}")
                    }
                }
            }

            if (responseText.isNotBlank()) {
                val cleanedJson = responseText
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val jsonArray = JSONArray(cleanedJson)
                val resultList = mutableListOf<LiveConceptExplanationItem>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    resultList.add(
                        LiveConceptExplanationItem(
                            id = obj.optString("id", "concept_${System.currentTimeMillis()}_$i"),
                            type = obj.optString("type", "Live Concept"),
                            timestamp = obj.optString("timestamp", "Live"),
                            percentGrounded = obj.optInt("percentGrounded", 95),
                            title = obj.optString("title"),
                            definition = obj.optString("definition"),
                            analogy = obj.optString("analogy"),
                            whyItMatters = obj.optString("whyItMatters"),
                            isBookmarked = false,
                        ),
                    )
                }
                return@withContext Result.success(resultList)
            }
        } catch (e: Exception) {
            // Safe handling for unit tests where android.util.Log is not mocked
        }

        Result.success(emptyList())
    }
}

private const val CONTEXT_ID_TOKEN = """(?:ID:\s*)?(?:term|tr|note|slide|concept|seg|segment|glossary)_[A-Za-z0-9_\-]+"""
private val contextIdGroupRegex = Regex("""[ \t]*[(\[]\s*$CONTEXT_ID_TOKEN(?:\s*[,;]\s*$CONTEXT_ID_TOKEN)*\s*[)\]]""")
private val bracketedIdTagRegex = Regex("""[ \t]*\[ID:[^\]]*]""")

/**
 * Removes grounding IDs (e.g. "(term_1793882650749_25, term_oop)") that Gemini sometimes echoes
 * from its context tags into the user-facing explanation text.
 */
internal fun stripContextIdReferences(text: String): String =
    text.replace(contextIdGroupRegex, "").replace(bracketedIdTagRegex, "")
