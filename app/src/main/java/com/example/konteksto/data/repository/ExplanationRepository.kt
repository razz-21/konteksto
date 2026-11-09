package com.example.konteksto.data.repository

import android.util.Log
import com.example.konteksto.BuildConfig
import com.example.konteksto.domain.model.ExplanationLanguage
import com.example.konteksto.domain.model.LiveConceptExplanationItem
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.Firebase
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.vertexai.vertexAI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

interface ExplanationRepository {
    suspend fun explainSelection(
        sessionId: String,
        segmentIds: List<String>,
        selectedTexts: List<String> = emptyList(),
        speakerName: String = "",
        language: ExplanationLanguage = ExplanationLanguage.Default,
    ): Result<LiveConceptExplanationItem>
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
        language: ExplanationLanguage,
    ): Result<LiveConceptExplanationItem> {
        val functions = functionsOrNull
        if (functions != null) {
            try {
                val data = mapOf(
                    "sessionId" to sessionId,
                    "segmentIds" to segmentIds,
                    "selectedTexts" to selectedTexts,
                    "speakerName" to speakerName,
                    "explanationLanguage" to language.storageValue,
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
        return explainWithGeminiFallback(sessionId, selectedTexts, speakerName, language)
    }

    private suspend fun explainWithGeminiFallback(
        sessionId: String,
        selectedTexts: List<String>,
        speakerName: String,
        language: ExplanationLanguage,
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
            
            OUTPUT LANGUAGE:
            ${explanationLanguageInstruction(language)}
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
}

/**
 * Tells Gemini which language to write the user-facing fields in. Keep in sync with
 * `explanationLanguageInstruction` in functions/src/index.ts.
 */
internal fun explanationLanguageInstruction(language: ExplanationLanguage): String = when (language) {
    ExplanationLanguage.ENGLISH ->
        "Write \"type\", \"title\", \"definition\", \"analogy\", and \"whyItMatters\" in clear, plain English."
    ExplanationLanguage.TAGALOG ->
        "Write \"type\", \"title\", \"definition\", \"analogy\", and \"whyItMatters\" in natural, conversational Tagalog (Filipino). " +
            "Keep technical terms, product names, and code identifiers in English when there is no common Tagalog equivalent, " +
            "and use everyday Filipino examples in the analogy. Even if the speaker talked in English, answer in Tagalog."
    ExplanationLanguage.TAGLISH ->
        "Write \"type\", \"title\", \"definition\", \"analogy\", and \"whyItMatters\" in natural Taglish, the way Filipinos casually mix " +
            "Tagalog and English in one sentence (e.g. \"Ang API ay parang waiter na nagdadala ng order mo sa kitchen.\"). " +
            "Keep technical terms in English, use Tagalog for the connecting words and explanation, and use everyday Filipino examples in the analogy."
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
