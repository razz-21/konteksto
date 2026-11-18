package com.example.konteksto.ui.host.studio

import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import com.example.konteksto.data.repository.LiveTranscriptionRepository
import com.example.konteksto.ui.components.LIVE_SPEAKING_PLACEHOLDER
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

/** 100 ms of 16 kHz mono 16-bit audio. */
private const val AUDIO_CHUNK_BYTES = MIC_SAMPLE_RATE_HZ / 10 * 2
private const val AUDIO_CHUNK_MS = 100L

/** A pause after the last words, so Gemini treats the sentence as finished and sends it back. */
private const val TRAILING_SILENCE_CHUNKS = 10

/** How long stopping waits for Gemini to send back the last sentence. */
private const val FINAL_SEGMENT_WAIT_MS = 4_000L

/** Speech within this long ago means the host is still talking. */
private const val SPEECH_HANGOVER_MS = 800L

/** Heard speech that no sentence came back for, such as a cough, stops showing after this long. */
private const val SPEECH_PENDING_TIMEOUT_MS = 6_000L

private const val SPEAKING_CHECK_INTERVAL_MS = 150L

private const val MAX_CONSECUTIVE_FAILURES = 3
private const val RETRY_DELAY_MS = 1_000L

/**
 * Transcribes the host with Gemini, opening a new session each time the server ends one (about every
 * 10 minutes). When Gemini is switched off in Remote Config, the mic can't be opened, or Gemini keeps
 * failing, the [fallback] engine takes over until the host stops the broadcast.
 */
class GeminiTranscriptionEngine(
    private val repository: LiveTranscriptionRepository,
    private val fallback: TranscriptionEngine,
    private val onSegmentComplete: (String) -> Unit,
    private val fallbackMessage: String,
) : TranscriptionEngine {
    // Not Main.immediate, so a job is always stored before its body runs.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val microphone = MicrophoneAudioSource()
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val _activeEngine = MutableStateFlow(TranscriptionEngineType.Gemini)

    private var listeningJob: Job? = null
    private var finishingJob: Job? = null
    private var usesFallback = false
    private var isListening = false
    private var speakingJob: Job? = null

    private val speechDetector = SpeechActivityDetector()

    // Written by the audio thread, read on the main thread.
    @Volatile private var lastSpeechAtMs = 0L
    @Volatile private var hasPendingSpeech = false

    /** The placeholder while the host speaks and Gemini, which only sends whole sentences, hasn't answered. */
    private val speakingPlaceholder = MutableStateFlow("")

    override val partialText: StateFlow<String> =
        combine(fallback.partialText, speakingPlaceholder) { fallbackText, placeholder ->
            fallbackText.ifEmpty { placeholder }
        }.stateIn(scope, SharingStarted.Eagerly, "")

    override val activeEngine: StateFlow<TranscriptionEngineType> = _activeEngine

    override val messages: Flow<String> = _messages

    override fun startListening() {
        if (isListening) return
        isListening = true
        // Each broadcast tries Gemini again, even if the last one fell back.
        _activeEngine.value = TranscriptionEngineType.Gemini
        // Starting again drops the last sentence of the previous broadcast if it hasn't arrived yet.
        finishingJob?.cancel()
        finishingJob = null
        // Recording starts now, and the audio is held until the Gemini session is ready.
        if (!microphone.start()) {
            switchToFallback(notifyHost = true)
            return
        }
        speakingJob?.cancel()
        hasPendingSpeech = false
        speakingJob = scope.launch { showWhileSpeaking() }
        listeningJob = scope.launch {
            if (!repository.isGeminiTranscriptionEnabled()) {
                switchToFallback(notifyHost = false)
                return@launch
            }
            var failures = 0
            // Each pass is one session. After stopping, the last one finishes without a new one opening.
            while (isListening) {
                try {
                    repository.transcribe(microphoneAudio()).collect { segment ->
                        failures = 0
                        onSegmentComplete(segment)
                        // Unless the host already went on to the next sentence, the one heard just arrived.
                        if (SystemClock.elapsedRealtime() - lastSpeechAtMs > SPEECH_HANGOVER_MS) {
                            hasPendingSpeech = false
                            speakingPlaceholder.value = ""
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("GeminiTranscription", "Gemini session failed: ${e.message}")
                    if (++failures >= MAX_CONSECUTIVE_FAILURES) {
                        switchToFallback(notifyHost = true)
                        return@launch
                    }
                    delay(RETRY_DELAY_MS)
                }
            }
        }
    }

    override fun stopListening() {
        if (!isListening) return
        isListening = false
        if (usesFallback) {
            usesFallback = false
            fallback.stopListening()
            return
        }
        val job = listeningJob ?: return
        listeningJob = null
        // Ends the audio stream, whose trailing silence lets Gemini send back the last sentence.
        microphone.stop()
        val speaking = speakingJob
        speakingJob = null
        finishingJob = scope.launch {
            try {
                delay(FINAL_SEGMENT_WAIT_MS)
            } finally {
                job.cancel()
                speaking?.cancel()
            }
        }
    }

    private fun switchToFallback(notifyHost: Boolean) {
        microphone.stop()
        listeningJob = null
        speakingJob?.cancel()
        speakingJob = null
        // The host stopped while Gemini was still being tried.
        if (!isListening) return
        usesFallback = true
        _activeEngine.value = TranscriptionEngineType.PhoneRecognizer
        if (notifyHost) _messages.tryEmit(fallbackMessage)
        fallback.startListening()
    }

    /** Keeps the placeholder up from the first words until Gemini sends the sentence back. */
    private suspend fun showWhileSpeaking() {
        try {
            while (true) {
                val sinceSpeechMs = SystemClock.elapsedRealtime() - lastSpeechAtMs
                if (hasPendingSpeech && sinceSpeechMs > SPEECH_PENDING_TIMEOUT_MS) hasPendingSpeech = false
                speakingPlaceholder.value = if (hasPendingSpeech) LIVE_SPEAKING_PLACEHOLDER else ""
                delay(SPEAKING_CHECK_INTERVAL_MS)
            }
        } finally {
            speakingPlaceholder.value = ""
        }
    }

    /** One session's audio: the mic from now on, including what was held since the last session. */
    private fun microphoneAudio(): Flow<ByteArray> = flow {
        ParcelFileDescriptor.AutoCloseInputStream(microphone.openStream()).use { input ->
            val buffer = ByteArray(AUDIO_CHUNK_BYTES)
            while (true) {
                val read = try {
                    input.read(buffer)
                } catch (e: IOException) {
                    // The mic closed the stream while stopping.
                    -1
                }
                if (read < 0) break
                if (read > 0) {
                    val chunk = buffer.copyOf(read)
                    if (speechDetector.isSpeech(chunk)) {
                        lastSpeechAtMs = SystemClock.elapsedRealtime()
                        hasPendingSpeech = true
                    }
                    emit(chunk)
                }
            }
        }
        repeat(TRAILING_SILENCE_CHUNKS) {
            emit(ByteArray(AUDIO_CHUNK_BYTES))
            delay(AUDIO_CHUNK_MS)
        }
    }.flowOn(Dispatchers.IO)
}
