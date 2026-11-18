package com.example.konteksto

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.konteksto.data.repository.GeminiLiveTranscriptionRepository
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** 100 ms of 16 kHz mono 16-bit PCM. */
private const val CHUNK_BYTES = 3_200
private const val CHUNK_MS = 100L

/**
 * Talks to the real Gemini Live API through Firebase AI Logic, so it needs a network connection and
 * the Firebase project's AI Logic APIs enabled. The sample says: "Today we are going to talk about
 * retrieval augmented generation. The model looks up relevant documents before it answers a
 * question."
 */
@RunWith(AndroidJUnit4::class)
class GeminiLiveTranscriptionTest {

    @Test
    fun transcribesSample_geminiDeveloperApi() {
        assertTranscribesSample(GenerativeBackend.googleAI())
    }

    @Test
    fun transcribesSample_agentPlatform() {
        assertTranscribesSample(GenerativeBackend.agentPlatform())
    }

    private fun assertTranscribesSample(backend: GenerativeBackend) = runBlocking {
        val pcm = InstrumentationRegistry.getInstrumentation().context.assets
            .open("transcribe_sample_16k.pcm")
            .use { it.readBytes() }
        // Sent at real-time pace, like the mic, then followed by silence so the speaker counts as paused.
        val audio = flow {
            for (offset in pcm.indices step CHUNK_BYTES) {
                emit(pcm.copyOfRange(offset, minOf(offset + CHUNK_BYTES, pcm.size)))
                delay(CHUNK_MS)
            }
            repeat(30) {
                emit(ByteArray(CHUNK_BYTES))
                delay(CHUNK_MS)
            }
        }

        val segments = mutableListOf<String>()
        withTimeoutOrNull(30_000) {
            GeminiLiveTranscriptionRepository(backend = backend).transcribe(audio).first { segment ->
                Log.i("GeminiLiveTranscriptionTest", "Segment: $segment")
                segments += segment
                segment.contains("question", ignoreCase = true)
            }
        }

        val transcript = segments.joinToString(" ")
        Log.i("GeminiLiveTranscriptionTest", "Transcript: $transcript")
        assertTrue(
            "Unexpected transcript: \"$transcript\"",
            transcript.contains("retrieval", ignoreCase = true) &&
                transcript.contains("documents", ignoreCase = true),
        )
    }
}
