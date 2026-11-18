package com.example.konteksto

import com.example.konteksto.ui.host.studio.SpeechActivityDetector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

/** 100 ms of a 220 Hz tone at [amplitude], as 16 kHz 16-bit little-endian PCM. */
private fun tone(amplitude: Int): ByteArray {
    val samples = 1_600
    val bytes = ByteArray(samples * 2)
    for (i in 0 until samples) {
        val value = (amplitude * sin(2 * PI * 220 * i / 16_000)).toInt()
        bytes[2 * i] = value.toByte()
        bytes[2 * i + 1] = (value shr 8).toByte()
    }
    return bytes
}

class SpeechActivityDetectorTest {

    @Test
    fun silence_isNotSpeech() {
        val detector = SpeechActivityDetector()
        repeat(20) { assertFalse(detector.isSpeech(ByteArray(3_200))) }
    }

    @Test
    fun sustainedVoiceLevel_isSpeech_fromTheSecondChunk() {
        val detector = SpeechActivityDetector()
        assertFalse(detector.isSpeech(tone(amplitude = 6_000)))
        assertTrue(detector.isSpeech(tone(amplitude = 6_000)))
        assertTrue(detector.isSpeech(tone(amplitude = 6_000)))
    }

    @Test
    fun singleClick_isNotSpeech() {
        val detector = SpeechActivityDetector()
        assertFalse(detector.isSpeech(tone(amplitude = 10_000)))
        assertFalse(detector.isSpeech(ByteArray(3_200)))
        assertFalse(detector.isSpeech(tone(amplitude = 10_000)))
    }

    @Test
    fun steadyRoomHum_stopsCountingAsSpeech_onceTheFloorAdapts() {
        val detector = SpeechActivityDetector()
        // A hum just under the threshold raises the background level...
        repeat(100) { detector.isSpeech(tone(amplitude = 650)) }
        // ...so a sound that would count in a quiet room no longer does.
        repeat(5) { assertFalse(detector.isSpeech(tone(amplitude = 1_200))) }
        // Real speech is still far enough above it.
        detector.isSpeech(tone(amplitude = 8_000))
        assertTrue(detector.isSpeech(tone(amplitude = 8_000)))
    }
}
