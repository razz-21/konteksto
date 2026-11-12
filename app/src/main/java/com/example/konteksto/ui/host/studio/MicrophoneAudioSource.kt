package com.example.konteksto.ui.host.studio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.IOException
import java.io.OutputStream

internal const val MIC_SAMPLE_RATE_HZ = 16_000
internal const val MIC_CHANNEL_COUNT = 1
internal const val MIC_ENCODING = AudioFormat.ENCODING_PCM_16BIT

/** 100 ms of 16-bit mono audio. */
private const val READ_CHUNK_BYTES = MIC_SAMPLE_RATE_HZ / 10 * 2

/** Audio held while no recognizer run is listening, enough to cover a restart without losing words. */
private const val MAX_HELD_BYTES = MIC_SAMPLE_RATE_HZ * 2 * 5

private const val READER_STOP_TIMEOUT_MS = 500L

/**
 * Keeps the microphone open for the whole broadcast and streams its audio to one recognizer run at
 * a time. Left to itself, the recognizer opens and closes the mic on every run, so the mic would turn
 * off and on between runs and lose the words said meanwhile. Audio recorded while no run is reading
 * is held and handed to the next run.
 */
internal class MicrophoneAudioSource {
    private val lock = Any()

    @Volatile
    private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private var readerThread: Thread? = null

    // Guarded by lock.
    private var output: OutputStream? = null
    private var readEnd: ParcelFileDescriptor? = null
    private val heldChunks = ArrayDeque<ByteArray>()
    private var heldBytes = 0

    /** Returns false when the mic can't be opened, so the recognizer has to record by itself. */
    @SuppressLint("MissingPermission") // The broadcast only starts once the mic permission is granted.
    fun start(): Boolean {
        if (isRecording) return true
        val minBufferBytes = AudioRecord.getMinBufferSize(
            MIC_SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            MIC_ENCODING,
        )
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                MIC_SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                MIC_ENCODING,
                maxOf(minBufferBytes, READ_CHUNK_BYTES * 4),
            )
        } catch (e: IllegalArgumentException) {
            Log.e("MicrophoneAudioSource", "Couldn't set up the mic: ${e.message}")
            return false
        } catch (e: SecurityException) {
            Log.e("MicrophoneAudioSource", "No permission to record: ${e.message}")
            return false
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return false
        }
        record.startRecording()
        if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            record.release()
            return false
        }

        audioRecord = record
        isRecording = true
        readerThread = Thread({ pumpAudio(record) }, "KontekstoMicrophone").apply { start() }
        return true
    }

    /** Opens a fresh stream for the next recognizer run, which first gets the audio held since the last one. */
    fun openStream(): ParcelFileDescriptor {
        val (newReadEnd, newWriteEnd) = ParcelFileDescriptor.createPipe()
        val (oldOutput, oldReadEnd) = synchronized(lock) {
            val old = output to readEnd
            output = ParcelFileDescriptor.AutoCloseOutputStream(newWriteEnd)
            // Kept open until the next run, because the recognizer receives it after startListening returns.
            readEnd = newReadEnd
            old
        }
        oldOutput?.closeQuietly()
        oldReadEnd?.closeQuietly()
        return newReadEnd
    }

    fun stop() {
        if (!isRecording) return
        isRecording = false
        val (oldOutput, oldReadEnd) = synchronized(lock) {
            val old = output to readEnd
            output = null
            readEnd = null
            heldChunks.clear()
            heldBytes = 0
            old
        }
        // Closing the stream also ends the recognizer run reading it.
        oldOutput?.closeQuietly()
        oldReadEnd?.closeQuietly()
        audioRecord?.stop()
        readerThread?.join(READER_STOP_TIMEOUT_MS)
        audioRecord?.release()
        audioRecord = null
        readerThread = null
    }

    private fun pumpAudio(record: AudioRecord) {
        val buffer = ByteArray(READ_CHUNK_BYTES)
        while (isRecording) {
            val read = record.read(buffer, 0, buffer.size)
            if (read < 0) {
                Log.e("MicrophoneAudioSource", "The mic stopped giving audio: error $read")
                return
            }
            if (read > 0) deliver(buffer.copyOf(read))
        }
    }

    /** Holds the chunk, then writes everything held to the current run's stream, if a run is reading. */
    private fun deliver(chunk: ByteArray) {
        val target = synchronized(lock) {
            hold(chunk)
            output
        } ?: return

        while (true) {
            val next = synchronized(lock) {
                // A new run took over, so what's left is held for it.
                if (output !== target) return
                heldChunks.removeFirstOrNull()?.also { heldBytes -= it.size }
            } ?: return
            try {
                target.write(next)
            } catch (e: IOException) {
                // The run stopped reading, so this chunk waits for the next one.
                synchronized(lock) {
                    heldChunks.addFirst(next)
                    heldBytes += next.size
                    if (output === target) output = null
                }
                target.closeQuietly()
                return
            }
        }
    }

    private fun hold(chunk: ByteArray) {
        heldChunks.addLast(chunk)
        heldBytes += chunk.size
        while (heldBytes > MAX_HELD_BYTES) {
            heldBytes -= heldChunks.removeFirst().size
        }
    }
}

private fun java.io.Closeable.closeQuietly() {
    try {
        close()
    } catch (e: IOException) {
        // Already closed by the other side.
    }
}
