package com.example.konteksto.ui.host.studio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechToTextManager(
    private val context: Context,
    private val onParagraphComplete: (String) -> Unit,
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isBroadcastActive = false
    private var isCurrentlyListening = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var lastCommittedText = ""

    private val pauseCommitRunnable = Runnable {
        commitCurrentPartialAsParagraph()
    }

    private val recognitionIntent by lazy {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("SpeechToTextManager", "Speech recognition is not available on this device.")
            return
        }

        if (isBroadcastActive && isCurrentlyListening) {
            return
        }

        isBroadcastActive = true
        _isListening.value = true

        mainHandler.post {
            ensureRecognizerCreated()
            startListeningInternal()
        }
    }

    private fun ensureRecognizerCreated() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }
        }
    }

    private fun startListeningInternal() {
        if (!isBroadcastActive || isCurrentlyListening) return
        try {
            ensureRecognizerCreated()
            isCurrentlyListening = true
            speechRecognizer?.startListening(recognitionIntent)
        } catch (e: Exception) {
            Log.e("SpeechToTextManager", "Error starting listening: ${e.message}")
            isCurrentlyListening = false
            recreateAndRestart()
        }
    }

    private fun recreateAndRestart() {
        if (!isBroadcastActive) return
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            isCurrentlyListening = false
            ensureRecognizerCreated()
            isCurrentlyListening = true
            speechRecognizer?.startListening(recognitionIntent)
        } catch (e: Exception) {
            isCurrentlyListening = false
            Log.e("SpeechToTextManager", "Error in recreateAndRestart: ${e.message}")
        }
    }

    private fun commitCurrentPartialAsParagraph() {
        val currentText = _partialText.value.trim()
        if (currentText.isNotBlank() && currentText != lastCommittedText) {
            lastCommittedText = currentText
            _partialText.value = ""
            onParagraphComplete(currentText)
        }
    }

    fun stopListening() {
        isBroadcastActive = false
        isCurrentlyListening = false
        _isListening.value = false
        _partialText.value = ""
        lastCommittedText = ""
        mainHandler.removeCallbacks(pauseCommitRunnable)

        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.e("SpeechToTextManager", "Error stopping speech recognition: ${e.message}")
            }
        }
    }

    private fun createListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            isCurrentlyListening = true
        }

        override fun onBeginningOfSpeech() {
            isCurrentlyListening = true
        }

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            mainHandler.postDelayed(pauseCommitRunnable, 800)
        }

        override fun onError(error: Int) {
            isCurrentlyListening = false
            mainHandler.removeCallbacks(pauseCommitRunnable)

            // Only commit if there was actual spoken text accumulated
            if (_partialText.value.isNotBlank()) {
                commitCurrentPartialAsParagraph()
            }

            if (isBroadcastActive) {
                // If it's a silent timeout/no match error, wait 1200ms backoff before restarting to prevent rapid toggling
                val delayMs = if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH) {
                    1200L
                } else {
                    600L
                }

                mainHandler.postDelayed({
                    if (isBroadcastActive && !isCurrentlyListening) {
                        startListeningInternal()
                    }
                }, delayMs)
            }
        }

        override fun onResults(results: Bundle?) {
            isCurrentlyListening = false
            mainHandler.removeCallbacks(pauseCommitRunnable)

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val finalText = matches?.firstOrNull()?.trim() ?: _partialText.value.trim()
            if (finalText.isNotBlank() && finalText != lastCommittedText) {
                lastCommittedText = finalText
                _partialText.value = ""
                onParagraphComplete(finalText)
            }

            if (isBroadcastActive) {
                mainHandler.postDelayed({
                    if (isBroadcastActive && !isCurrentlyListening) {
                        startListeningInternal()
                    }
                }, 400)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.trim() ?: ""
            if (text.isNotBlank()) {
                _partialText.value = text
                mainHandler.removeCallbacks(pauseCommitRunnable)
                mainHandler.postDelayed(pauseCommitRunnable, 1200)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
