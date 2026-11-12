package com.example.konteksto.ui.host.studio

import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal const val SILENCE_RESTART_DELAY_MS = 300L
internal const val ERROR_RESTART_DELAY_MS = 600L

/** A speaker who stops for this long has finished a thought, so their words become a card. */
internal const val PAUSE_COMMIT_DELAY_MS = 2_000L

/**
 * The recognizer can still fix a word or two it already sent, so a new text whose opening differs
 * from the saved words by at most this share of them still continues them.
 */
private const val SAVED_WORDS_ALLOWED_CHANGES = 0.2

/** Seams only ever repeat a short run of words, so a longer match is real speech said twice. */
private const val MAX_REPEATED_WORDS = 20

/** A single repeated word at a seam is as likely to be the speaker as the recognizer, so it stays. */
private const val MIN_REPEATED_WORDS = 2

private val WHITESPACE = Regex("\\s+")

/**
 * Decides what each speech recognizer callback means for the transcript. It is kept apart from the
 * Android recognizer so these rules can be unit tested.
 *
 * Each recognizer run ends with a result or an error, and some recognizers send a stray error just
 * after the result. Strays land while the next run is still starting, so they are ignored there.
 * Acting on them used to save the next run's unfinished words early and restart over it, which
 * split, repeated and dropped words where one transcript card met the next.
 */
internal class RecognitionRunTracker(private val onSegmentComplete: (String) -> Unit) {

    private enum class Phase { STOPPED, STARTING, LISTENING }

    /** What the recognizer should do after a callback. */
    sealed interface Next {
        data object KeepGoing : Next
        data class Restart(val delayMs: Long) : Next

        /** The on-device recognizer can't serve the speaker's language, so the default one should take over. */
        data object UseDefaultRecognizer : Next
    }

    private var phase = Phase.STOPPED
    private var runId = 0

    /** The last words saved, for spotting a new segment that opens by repeating them. */
    private var savedTail = emptyList<String>()

    /**
     * This run's words already saved at a pause. Some recognizers keep sending the whole run's text,
     * so these are left out of its later partials and its final result. Others start fresh text
     * after a pause, which then doesn't open with these words and is kept whole.
     */
    private var runSavedWords = emptyList<String>()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    val isActive: Boolean get() = phase != Phase.STOPPED

    fun start() {
        if (isActive) return
        savedTail = emptyList()
        phase = Phase.STARTING
    }

    /** Saves the words still in progress, and ignores whatever the recognizer sends after this. */
    fun stop() {
        if (!isActive) return
        save(_partialText.value)
        phase = Phase.STOPPED
    }

    /** Returns the new run's id, for checking later whether it ever got going. */
    fun onRunStarting(): Int {
        phase = Phase.STARTING
        runSavedWords = emptyList()
        return ++runId
    }

    fun isStuckStarting(id: Int): Boolean = phase == Phase.STARTING && id == runId

    fun onReady() {
        if (phase == Phase.STARTING) phase = Phase.LISTENING
    }

    /**
     * Partials before the run is ready are leftovers from the previous run, whose words are already saved.
     * Returns whether the words in progress changed, which means the speaker is still talking.
     */
    fun onPartial(text: String): Boolean {
        if (phase != Phase.LISTENING || text.isBlank()) return false
        val unsaved = text.withoutSavedRunWords()
        if (unsaved == _partialText.value) return false
        _partialText.value = unsaved
        return true
    }

    /**
     * The speaker stopped talking but the recognizer kept the run open, so the words so far are saved
     * now instead of waiting for the run to end, which can take until the broadcast stops.
     */
    fun onPause() {
        if (phase != Phase.LISTENING) return
        val text = _partialText.value
        if (text.isBlank()) return
        runSavedWords = runSavedWords + text.words()
        save(text)
    }

    /** A segmented run finished part of the speech and is still listening. */
    fun onSegment(text: String) {
        if (!isActive) return
        save(text.withoutSavedRunWords().ifBlank { _partialText.value })
        runSavedWords = emptyList()
    }

    fun onResults(text: String): Next {
        if (!isActive) return Next.KeepGoing
        save(text.withoutSavedRunWords().ifBlank { _partialText.value })
        // A late result while the next run starts must not restart over that run.
        if (phase == Phase.STARTING) return Next.KeepGoing
        phase = Phase.STARTING
        return Next.Restart(0L)
    }

    fun onEndOfSegmentedSession(): Next {
        if (phase != Phase.LISTENING) return Next.KeepGoing
        save(_partialText.value)
        phase = Phase.STARTING
        return Next.Restart(0L)
    }

    fun onError(error: Int): Next {
        if (!isActive) return Next.KeepGoing
        if (error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
            error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
        ) {
            save(_partialText.value)
            phase = Phase.STARTING
            return Next.UseDefaultRecognizer
        }
        // Errors before the run is ready are strays from the finished run. A start that truly
        // failed never becomes ready, and the recognizer's startup check catches that instead.
        if (phase == Phase.STARTING) return Next.KeepGoing

        // No final result follows an error, so the words in progress are all there will be.
        save(_partialText.value)
        phase = Phase.STARTING
        val isSilence = error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
        return Next.Restart(if (isSilence) SILENCE_RESTART_DELAY_MS else ERROR_RESTART_DELAY_MS)
    }

    private fun String.withoutSavedRunWords(): String {
        val words = words()
        if (!words.continues(runSavedWords)) {
            // The recognizer started fresh text, so none of it was saved yet.
            runSavedWords = emptyList()
            return words.joinToString(" ")
        }
        return words.drop(runSavedWords.size).joinToString(" ")
    }

    private fun save(text: String) {
        _partialText.value = ""
        val words = text.words()
        val newWords = words.drop(repeatedOpeningLength(savedTail, words))
        if (newWords.isEmpty()) return
        savedTail = (savedTail + newWords).takeLast(MAX_REPEATED_WORDS)
        onSegmentComplete(newWords.joinToString(" "))
    }
}

/** How many of [next]'s first words repeat the end of [previous], ignoring case and punctuation. */
private fun repeatedOpeningLength(previous: List<String>, next: List<String>): Int {
    val longest = minOf(previous.size, next.size, MAX_REPEATED_WORDS)
    for (count in longest downTo MIN_REPEATED_WORDS) {
        val previousEnd = previous.takeLast(count).map { it.comparable() }
        if (previousEnd == next.take(count).map { it.comparable() }) return count
    }
    return 0
}

/** Whether these words open with [saved], allowing for the few words the recognizer may have fixed since. */
private fun List<String>.continues(saved: List<String>): Boolean {
    if (saved.isEmpty()) return true
    if (size < saved.size) return false
    val changed = saved.indices.count { saved[it].comparable() != this[it].comparable() }
    return changed <= (saved.size * SAVED_WORDS_ALLOWED_CHANGES).toInt()
}

private fun String.words(): List<String> = trim().split(WHITESPACE).filter { it.isNotEmpty() }

private fun String.comparable(): String = filter { it.isLetterOrDigit() }.lowercase()
