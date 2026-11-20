package com.example.konteksto.data.repository

import android.util.Log
import com.example.konteksto.R
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineData
import com.google.firebase.ai.type.LiveServerContent
import com.google.firebase.ai.type.LiveServerGoAway
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.activityDetectionConfig
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import com.google.firebase.ai.type.realtimeInputConfig
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val LIVE_TRANSCRIPTION_MODEL_KEY = "live_transcription_model"
private const val TRANSCRIPTION_ENGINE_KEY = "transcription_engine"
private const val GEMINI_ENGINE = "gemini"

/** 16 kHz mono 16-bit PCM, the format MicrophoneAudioSource records. */
private const val PCM_MIME_TYPE = "audio/pcm;rate=16000"

private const val CLOSE_TIMEOUT_MS = 2_000L

/** A pause this long ends the speaker's sentence, so Gemini sends it back and it becomes a card. */
const val TRANSCRIPT_PAUSE_MS = 1_000

/**
 * The SDK's transcription config has no language setting, so the languages hosts speak are given as
 * an instruction instead. Whether the transcription follows it is up to the model.
 */
private val TRANSCRIPTION_INSTRUCTION = """
    The speaker talks only in English, Tagalog (Filipino) and Cebuano (Bisaya), and often switches between them within a sentence.
    - Transcribe every word exactly as spoken, in the language it was spoken in.
    - Never translate, and never write the speech as any other language.
    - Keep English technical terms in English.
""".trimIndent()

/** Starting a broadcast waits at most this long for fresh Remote Config values. */
private const val REMOTE_CONFIG_FETCH_TIMEOUT_MS = 3_000L

/** Lets a switch in the Firebase console reach the next broadcast within a minute. */
private const val REMOTE_CONFIG_MIN_FETCH_INTERVAL_S = 60L

interface LiveTranscriptionRepository {
    /** Whether live transcription should use Gemini rather than the phone's own speech recognizer. */
    suspend fun isGeminiTranscriptionEnabled(): Boolean

    /**
     * Streams raw 16 kHz mono 16-bit PCM [audio] to Gemini and emits each finished stretch of
     * speech as it is transcribed. Completes when the server ends the session, which happens after
     * about 10 minutes, so the caller can open a new one.
     */
    fun transcribe(audio: Flow<ByteArray>): Flow<String>
}

@OptIn(PublicPreviewAPI::class)
class GeminiLiveTranscriptionRepository(
    private val backend: GenerativeBackend = GenerativeBackend.googleAI(),
    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LiveTranscriptionRepository {

    override suspend fun isGeminiTranscriptionEnabled(): Boolean {
        refreshRemoteConfig()
        return remoteConfig.getString(TRANSCRIPTION_ENGINE_KEY) == GEMINI_ENGINE
    }

    override fun transcribe(audio: Flow<ByteArray>): Flow<String> = channelFlow {
        refreshRemoteConfig()
        val model = Firebase.ai(backend = backend).liveModel(
            modelName = remoteConfig.getString(LIVE_TRANSCRIPTION_MODEL_KEY),
            generationConfig = liveGenerationConfig {
                responseModality = ResponseModality.TEXT
                inputAudioTranscription = AudioTranscriptionConfig()
                realtimeInputConfig = realtimeInputConfig {
                    automaticActivityDetection = activityDetectionConfig {
                        silenceDurationMs = TRANSCRIPT_PAUSE_MS
                    }
                }
            },
            systemInstruction = content { text(TRANSCRIPTION_INSTRUCTION) },
        )
        val session = model.connect()
        val sending = launch {
            audio.collect { chunk -> session.sendAudioRealtime(InlineData(chunk, PCM_MIME_TYPE)) }
        }
        try {
            launch {
                session.receive().collect { message ->
                    when (message) {
                        is LiveServerContent -> {
                            val text = message.inputTranscription?.text?.trim()
                            if (!text.isNullOrEmpty()) send(text)
                        }
                        // The session is about to end, so the caller moves on to a new one now.
                        is LiveServerGoAway -> session.stopReceiving()
                        else -> Unit
                    }
                }
            }.join()
        } finally {
            sending.cancel()
            // The SDK's receive loop only notices cancellation when the next server message
            // arrives, which can be a minute away. Stopping it directly ends the loop right away.
            session.stopReceiving()
            // The server can take up to a minute to acknowledge the close. Don't hold up the
            // caller for it; OkHttp drops the socket by itself once that wait runs out.
            withContext(NonCancellable) { withTimeoutOrNull(CLOSE_TIMEOUT_MS) { session.close() } }
        }
    }
        .catch { e ->
            Firebase.crashlytics.recordException(e)
            throw e
        }
        .flowOn(ioDispatcher)

    /** Falls back to the last fetched values, or the defaults, when the fetch fails or is slow. */
    private suspend fun refreshRemoteConfig() {
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults).await()
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings { minimumFetchIntervalInSeconds = REMOTE_CONFIG_MIN_FETCH_INTERVAL_S },
        ).await()
        try {
            withTimeoutOrNull(REMOTE_CONFIG_FETCH_TIMEOUT_MS) { remoteConfig.fetchAndActivate().await() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("LiveTranscription", "Couldn't refresh Remote Config: ${e.message}")
        }
    }
}
