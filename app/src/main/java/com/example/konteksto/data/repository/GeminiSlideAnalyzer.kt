package com.example.konteksto.data.repository

import android.util.Log
import com.example.konteksto.BuildConfig
import com.example.konteksto.ui.host.studio.AnalyzedSlideItem
import com.example.konteksto.ui.host.studio.SlideDeckInfo
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.firebase.Firebase
import com.google.firebase.vertexai.vertexAI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray

class GeminiSlideAnalyzer(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
) {

    suspend fun analyzeSlideDeck(
        slideDeckInfo: SlideDeckInfo,
        sessionTitle: String = "",
        category: String = "",
    ): Result<List<AnalyzedSlideItem>> = withContext(Dispatchers.IO) {
        val fileName = slideDeckInfo.fileName
        val pageCount = slideDeckInfo.pageCount.coerceAtLeast(1)
        val pdfBytes = slideDeckInfo.pdfBytes
        if (pdfBytes == null || pdfBytes.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Slide deck PDF is not available."))
        }

        val prompt = """
            You are an expert AI assistant analyzing the complete presentation slide deck titled '$fileName' ($pageCount slides total) for a talk session titled '$sessionTitle' ($category).
            
            CRITICAL INSTRUCTIONS:
            1. Analyze ALL $pageCount slides across this entire presentation slide deck from slide 1 to slide $pageCount.
            2. Extract key concepts, summaries, and explanations for EACH slide in the deck.
            3. Return a JSON array with exactly $pageCount slide objects (one object per slide in the presentation deck).
            
            Each slide object in the array MUST contain:
            - "slideNumber": integer representing the slide number (1, 2, 3, ..., up to $pageCount)
            - "title": concise slide title or topic explanation for this slide
            - "summary": clear summary of what is covered on this slide
            - "explanation": detailed explanation of the concepts, mechanisms, and content presented on this slide
            - "keyTakeaway": core actionable takeaway, key terminology, or definition for the live audience
            
            Return ONLY raw valid JSON array, with no markdown formatting or additional text.
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
                modelLoop@ for (modelName in candidateModelNames) {
                    for (attempt in 1..2) {
                        try {
                            val model = GenerativeModel(modelName, apiKey = apiKey)
                            val inputContent = content {
                                blob("application/pdf", pdfBytes)
                                text(prompt)
                            }
                            val response = model.generateContent(inputContent)
                            val textResult = response.text?.trim()
                            if (!textResult.isNullOrBlank()) {
                                responseText = textResult
                                Log.d("GeminiSlideAnalyzer", "Successfully generated slide analysis using model: $modelName")
                                break@modelLoop
                            }
                        } catch (e: Exception) {
                            val errMsg = e.message ?: ""
                            if (errMsg.contains("503") || errMsg.contains("high demand") || errMsg.contains("UNAVAILABLE")) {
                                Log.w("GeminiSlideAnalyzer", "Model $modelName 503 high demand on attempt $attempt. Retrying in 1s...")
                                delay(1000)
                            } else {
                                Log.w("GeminiSlideAnalyzer", "Model $modelName failed: $errMsg, trying next candidate...")
                                break // Skip retry for 404 / unsupported model errors
                            }
                        }
                    }
                }
            } else {
                for (modelName in candidateModelNames) {
                    try {
                        val model = Firebase.vertexAI.generativeModel(modelName)
                        val inputContent = com.google.firebase.vertexai.type.content {
                            inlineData(pdfBytes, "application/pdf")
                            text(prompt)
                        }
                        val response = model.generateContent(inputContent)
                        val textResult = response.text?.trim()
                        if (!textResult.isNullOrBlank()) {
                            responseText = textResult
                            break
                        }
                    } catch (e: Exception) {
                        Log.w("GeminiSlideAnalyzer", "Firebase model $modelName failed: ${e.message}")
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
                val results = mutableListOf<AnalyzedSlideItem>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    results.add(
                        AnalyzedSlideItem(
                            id = "s_${System.currentTimeMillis()}_$i",
                            slideNumber = obj.optInt("slideNumber", i + 1),
                            title = obj.optString("title", "Slide ${i + 1}"),
                            summary = obj.optString("summary").ifBlank { null },
                            explanation = obj.optString("explanation").ifBlank { null },
                            keyTakeaway = obj.optString("keyTakeaway").ifBlank { null },
                        ),
                    )
                }

                if (results.isNotEmpty()) {
                    return@withContext Result.success(results)
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiSlideAnalyzer", "Gemini slide analysis parsing error: ${e.message}", e)
        }

        Result.failure(IllegalStateException("Gemini did not return a usable slide analysis."))
    }
}
