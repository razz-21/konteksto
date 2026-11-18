package com.example.konteksto.ui.host.studio

import kotlin.math.sqrt

/** Quietest level that can count as speech, about -36 dBFS, so a silent room never does. */
private const val MIN_SPEECH_RMS = 500.0

/** How much louder than the room's background speech has to be. */
private const val SPEECH_TO_NOISE_RATIO = 3.0

private const val INITIAL_NOISE_FLOOR_RMS = 150.0

/** How quickly the background level follows the room getting louder or quieter. */
private const val NOISE_FLOOR_ADAPT_RATE = 0.05

/** A single click or bump is one loud chunk; speech keeps going for at least this many. */
private const val MIN_LOUD_CHUNKS = 2

/**
 * Tells speech from background noise by loudness alone, adapting to how loud the room is. It is
 * only meant to show that someone is talking, not to decide what gets transcribed.
 */
internal class SpeechActivityDetector {
    private var noiseFloorRms = INITIAL_NOISE_FLOOR_RMS
    private var loudChunksInARow = 0

    /** Returns true when [chunk], 16-bit little-endian mono PCM, sounds like ongoing speech. */
    fun isSpeech(chunk: ByteArray): Boolean {
        val rms = rms(chunk)
        val isLoud = rms > maxOf(MIN_SPEECH_RMS, noiseFloorRms * SPEECH_TO_NOISE_RATIO)
        if (isLoud) {
            loudChunksInARow++
        } else {
            loudChunksInARow = 0
            noiseFloorRms += (rms - noiseFloorRms) * NOISE_FLOOR_ADAPT_RATE
        }
        return loudChunksInARow >= MIN_LOUD_CHUNKS
    }

    private fun rms(chunk: ByteArray): Double {
        val sampleCount = chunk.size / 2
        if (sampleCount == 0) return 0.0
        var sumOfSquares = 0.0
        for (i in 0 until sampleCount) {
            val sample = ((chunk[2 * i].toInt() and 0xFF) or (chunk[2 * i + 1].toInt() shl 8)).toShort()
            sumOfSquares += sample.toDouble() * sample
        }
        return sqrt(sumOfSquares / sampleCount)
    }
}
