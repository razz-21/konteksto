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
) : TranscriptionEngine {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isBroadcastActive = false
    private var isCurrentlyListening = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _partialText = MutableStateFlow("")
    override val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val recognitionIntent by lazy {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putStringArrayListExtra(RecognizerIntent.EXTRA_BIASING_STRINGS, arrayListOf("Konteksto"))
        }
    }

    override fun startListening() {
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

    /**
     * Partial results are only a live preview, because the recognizer can still revise or retract
     * their words. This saves one when no final result is coming, so the speech isn't lost.
     */
    private fun commitPartialText() {
        val text = _partialText.value.trim()
        _partialText.value = ""
        if (text.isNotBlank()) {
            onParagraphComplete(text)
        }
    }

    override fun stopListening() {
        isBroadcastActive = false
        isCurrentlyListening = false
        _isListening.value = false
        commitPartialText()

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

        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            isCurrentlyListening = false
            commitPartialText()

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
            if (!isBroadcastActive) return
            isCurrentlyListening = false

            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let { _partialText.value = it }
            commitPartialText()

            mainHandler.postDelayed({
                if (isBroadcastActive && !isCurrentlyListening) {
                    startListeningInternal()
                }
            }, 100)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (!isBroadcastActive) return
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.trim() ?: ""
            if (text.isNotBlank()) {
                _partialText.value = text
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
