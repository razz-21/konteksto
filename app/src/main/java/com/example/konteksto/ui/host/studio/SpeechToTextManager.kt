package com.example.konteksto.ui.host.studio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.os.SystemClock
import android.util.Log
import com.example.konteksto.BuildConfig
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/** A run that hasn't become ready by now failed to start without saying so. */
private const val STARTUP_TIMEOUT_MS = 5_000L

// TODO: Remove once pause detection is tuned. Filter Logcat by this tag to see what resets the pause timer.
private const val PAUSE_DEBUG_TAG = "PauseDebug"

/** Logs pause timing in debug builds only. Never pass transcript words here, only counts. */
private fun logPause(message: String) {
    if (BuildConfig.DEBUG) Log.d(PAUSE_DEBUG_TAG, message)
}

class SpeechToTextManager(
    private val context: Context,
    onParagraphComplete: (String) -> Unit,
) : TranscriptionEngine {
    private val tracker = RecognitionRunTracker(onParagraphComplete)
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val startRun = Runnable { startRunInternal() }
    private val commitOnPause = Runnable {
        val quietMs = SystemClock.uptimeMillis() - lastResetAtMs
        logPause("TIMER FIRED after ${quietMs}ms quiet -> saving ${tracker.partialText.value.wordCount()} words as a card")
        tracker.onPause()
    }
    private var lastResetAtMs = 0L

    private val microphone = MicrophoneAudioSource()

    /**
     * Only the on-device recognizer is sure to read audio the app records, so only it keeps the mic
     * open from the moment the broadcast starts until it stops. The default recognizer records by
     * itself, turning the mic off and on between runs.
     */
    private var useOnDeviceRecognizer = SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
    private var usesOwnMicrophone = false

    override val partialText: StateFlow<String> = tracker.partialText

    private fun buildRecognitionIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putStringArrayListExtra(RecognizerIntent.EXTRA_BIASING_STRINGS, arrayListOf("Konteksto"))
        if (usesOwnMicrophone) {
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, microphone.openStream())
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, MIC_CHANNEL_COUNT)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, MIC_ENCODING)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, MIC_SAMPLE_RATE_HZ)
            // The run keeps listening through pauses until the mic stream closes, handing back each
            // finished stretch of speech as a segment instead of ending.
            putExtra(RecognizerIntent.EXTRA_SEGMENTED_SESSION, RecognizerIntent.EXTRA_AUDIO_SOURCE)
        }
    }

    override fun startListening() {
        if (!useOnDeviceRecognizer && !SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("SpeechToTextManager", "Speech recognition is not available on this device.")
            return
        }
        if (tracker.isActive) return

        tracker.start()
        usesOwnMicrophone = useOnDeviceRecognizer && microphone.start()
        logPause(
            if (usesOwnMicrophone) {
                "MIC ON: app holds the mic until the broadcast stops (on-device recognizer)"
            } else {
                "MIC ON: recognizer records by itself, so it may restart the mic (default recognizer)"
            },
        )
        scheduleRun(0L)
    }

    override fun stopListening() {
        tracker.stop()

        mainHandler.removeCallbacksAndMessages(null)
        microphone.stop()
        usesOwnMicrophone = false
        logPause("MIC OFF: broadcast stopped")
        mainHandler.post { destroyRecognizer() }
    }

    private fun scheduleRun(delayMs: Long) {
        mainHandler.removeCallbacks(startRun)
        mainHandler.postDelayed(startRun, delayMs)
    }

    private fun startRunInternal() {
        if (!tracker.isActive) return
        try {
            ensureRecognizerCreated()
            logPause(
                if (usesOwnMicrophone) {
                    "recognizer run starting (mic stays on)"
                } else {
                    "MIC (RE)START: recognizer run starting"
                },
            )
            val runId = tracker.onRunStarting()
            speechRecognizer?.startListening(buildRecognitionIntent())
            mainHandler.postDelayed({
                if (tracker.isStuckStarting(runId)) recreateAndRestart()
            }, STARTUP_TIMEOUT_MS)
        } catch (e: Exception) {
            Log.e("SpeechToTextManager", "Error starting listening: ${e.message}")
            recreateAndRestart()
        }
    }

    private fun recreateAndRestart() {
        if (!tracker.isActive) return
        destroyRecognizer()
        scheduleRun(ERROR_RESTART_DELAY_MS)
    }

    private fun ensureRecognizerCreated() {
        if (speechRecognizer == null) {
            speechRecognizer = createRecognizer().apply {
                setRecognitionListener(listener)
            }
        }
    }

    private fun createRecognizer(): SpeechRecognizer {
        if (useOnDeviceRecognizer) {
            try {
                return SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } catch (e: UnsupportedOperationException) {
                Log.w("SpeechToTextManager", "On-device recognition is unavailable: ${e.message}")
                switchToDefaultRecognizer()
            }
        }
        return SpeechRecognizer.createSpeechRecognizer(context)
    }

    /** The default recognizer records by itself, so the app lets go of the mic for it. */
    private fun switchToDefaultRecognizer() {
        logPause("FALLBACK: on-device recognizer can't be used, so the default one records by itself")
        useOnDeviceRecognizer = false
        usesOwnMicrophone = false
        microphone.stop()
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("SpeechToTextManager", "Error stopping speech recognition: ${e.message}")
        }
        speechRecognizer = null
    }

    /**
     * The recognizer can keep a run open through the speaker's pauses, so a card is saved once no new
     * words have come for a moment instead of only when the run ends. Only recognized words count as
     * speaking, because the mic's sound level can't tell the speaker apart from background noise.
     */
    private fun onSpeakerActive(reason: String) {
        val now = SystemClock.uptimeMillis()
        logPause("timer reset by $reason (${now - lastResetAtMs}ms since last reset)")
        lastResetAtMs = now
        mainHandler.removeCallbacks(commitOnPause)
        mainHandler.postDelayed(commitOnPause, PAUSE_COMMIT_DELAY_MS)
    }

    private fun handle(next: RecognitionRunTracker.Next) {
        mainHandler.removeCallbacks(commitOnPause)
        when (next) {
            RecognitionRunTracker.Next.KeepGoing -> Unit
            is RecognitionRunTracker.Next.Restart -> scheduleRun(next.delayMs)
            RecognitionRunTracker.Next.UseDefaultRecognizer -> if (useOnDeviceRecognizer) {
                switchToDefaultRecognizer()
                destroyRecognizer()
                scheduleRun(0L)
            } else {
                scheduleRun(ERROR_RESTART_DELAY_MS)
            }
        }
    }

    private fun String.wordCount(): Int = split(' ').count { it.isNotBlank() }

    private fun Bundle?.firstResult(): String =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            logPause("recognizer run ready")
            tracker.onReady()
        }

        override fun onBeginningOfSpeech() {
            logPause("recognizer heard speech begin")
        }

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            logPause("recognizer heard speech end")
        }

        override fun onError(error: Int) {
            logPause("recognizer error $error")
            handle(tracker.onError(error))
        }

        override fun onResults(results: Bundle?) {
            logPause("recognizer final result, ${results.firstResult().wordCount()} words in run")
            handle(tracker.onResults(results.firstResult()))
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults.firstResult()
            val changed = tracker.onPartial(text)
            logPause(
                "partial: ${text.wordCount()} words from recognizer, " +
                    "${tracker.partialText.value.wordCount()} unsaved, changed=$changed",
            )
            if (changed) onSpeakerActive("NEW WORDS")
        }

        override fun onSegmentResults(segmentResults: Bundle) {
            logPause("recognizer segment, ${segmentResults.firstResult().wordCount()} words")
            mainHandler.removeCallbacks(commitOnPause)
            tracker.onSegment(segmentResults.firstResult())
        }

        override fun onEndOfSegmentedSession() {
            logPause("recognizer segmented run ended")
            handle(tracker.onEndOfSegmentedSession())
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
