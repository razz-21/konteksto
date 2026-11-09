package com.example.konteksto

import android.speech.SpeechRecognizer
import com.example.konteksto.ui.host.studio.ERROR_RESTART_DELAY_MS
import com.example.konteksto.ui.host.studio.RecognitionRunTracker
import com.example.konteksto.ui.host.studio.RecognitionRunTracker.Next
import com.example.konteksto.ui.host.studio.SILENCE_RESTART_DELAY_MS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognitionRunTrackerTest {

    private val saved = mutableListOf<String>()
    private val tracker = RecognitionRunTracker { saved += it }

    private fun listeningRun(): Int {
        val runId = tracker.onRunStarting()
        tracker.onReady()
        return runId
    }

    @Test
    fun finalResultIsSavedAndTheNextRunStartsRightAway() {
        tracker.start()
        listeningRun()
        tracker.onPartial("edge computing moves")

        val next = tracker.onResults("Edge computing moves work closer to users.")

        assertEquals(listOf("Edge computing moves work closer to users."), saved)
        assertEquals(Next.Restart(0L), next)
        assertEquals("", tracker.partialText.value)
    }

    @Test
    fun strayErrorAfterAResultNeitherSavesNorRestartsOverTheNextRun() {
        tracker.start()
        listeningRun()
        tracker.onResults("We scale to zero latency.")
        tracker.onRunStarting()

        assertEquals(Next.KeepGoing, tracker.onError(SpeechRecognizer.ERROR_NO_MATCH))
        assertEquals(Next.KeepGoing, tracker.onError(SpeechRecognizer.ERROR_CLIENT))

        tracker.onReady()
        tracker.onPartial("the next sentence")
        assertEquals("the next sentence", tracker.partialText.value)
        assertEquals(listOf("We scale to zero latency."), saved)
    }

    @Test
    fun leftoverPartialFromTheFinishedRunIsNotShownWhileTheNextStarts() {
        tracker.start()
        listeningRun()
        tracker.onResults("First sentence here.")
        tracker.onRunStarting()

        tracker.onPartial("First sentence here")

        assertEquals("", tracker.partialText.value)
    }

    @Test
    fun errorWhileListeningSavesTheWordsInProgress() {
        tracker.start()
        listeningRun()
        tracker.onPartial("caching at the edge")

        val next = tracker.onError(SpeechRecognizer.ERROR_NETWORK)

        assertEquals(listOf("caching at the edge"), saved)
        assertEquals(Next.Restart(ERROR_RESTART_DELAY_MS), next)
    }

    @Test
    fun silenceRestartsSoonerThanOtherErrors() {
        tracker.start()
        listeningRun()

        assertEquals(Next.Restart(SILENCE_RESTART_DELAY_MS), tracker.onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
        assertTrue(saved.isEmpty())
    }

    @Test
    fun segmentedRunSavesEachSegmentAndKeepsListening() {
        tracker.start()
        listeningRun()

        tracker.onPartial("first part of the talk")
        tracker.onSegment("First part of the talk.")
        tracker.onPartial("second part")
        tracker.onSegment("Second part.")

        assertEquals(listOf("First part of the talk.", "Second part."), saved)
        assertEquals("", tracker.partialText.value)
        assertEquals(Next.Restart(0L), tracker.onEndOfSegmentedSession())
    }

    @Test
    fun wordsRepeatedAtTheStartOfTheNextSegmentAreDropped() {
        tracker.start()
        listeningRun()
        tracker.onSegment("We scale to zero latency")

        tracker.onSegment("Zero latency, at the edge.")

        assertEquals(listOf("We scale to zero latency", "at the edge."), saved)
    }

    @Test
    fun segmentThatOnlyRepeatsTheLastWordsIsNotSavedAgain() {
        tracker.start()
        listeningRun()
        tracker.onSegment("Edge computing matters.")
        tracker.onPartial("edge computing matters")

        tracker.onEndOfSegmentedSession()

        assertEquals(listOf("Edge computing matters."), saved)
    }

    @Test
    fun singleRepeatedWordIsKeptBecauseTheSpeakerMayHaveSaidItTwice() {
        tracker.start()
        listeningRun()
        tracker.onSegment("I think that")

        tracker.onSegment("that is right")

        assertEquals(listOf("I think that", "that is right"), saved)
    }

    @Test
    fun lateResultWhileTheNextRunStartsIsSavedWithoutRestarting() {
        tracker.start()
        listeningRun()
        tracker.onPartial("words cut short by an error")
        tracker.onError(SpeechRecognizer.ERROR_SERVER)
        tracker.onRunStarting()

        val next = tracker.onResults("words cut short by an error and the rest")

        assertEquals(listOf("words cut short by an error", "and the rest"), saved)
        assertEquals(Next.KeepGoing, next)
    }

    @Test
    fun languageTheOnDeviceRecognizerCannotServeSwitchesToTheDefaultOne() {
        tracker.start()
        tracker.onRunStarting()

        assertEquals(Next.UseDefaultRecognizer, tracker.onError(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE))
    }

    @Test
    fun runThatNeverBecomesReadyIsReportedAsStuck() {
        tracker.start()
        val stuckRun = tracker.onRunStarting()
        assertTrue(tracker.isStuckStarting(stuckRun))

        tracker.onReady()
        assertFalse(tracker.isStuckStarting(stuckRun))

        val olderRun = stuckRun
        tracker.onResults("Done.")
        tracker.onRunStarting()
        assertFalse(tracker.isStuckStarting(olderRun))
    }

    @Test
    fun stoppingSavesTheWordsInProgressAndIgnoresLaterCallbacks() {
        tracker.start()
        listeningRun()
        tracker.onPartial("thank you all for")

        tracker.stop()
        val next = tracker.onResults("thank you all for coming")
        tracker.onPartial("coming")

        assertEquals(listOf("thank you all for"), saved)
        assertEquals(Next.KeepGoing, next)
        assertEquals("", tracker.partialText.value)
        assertFalse(tracker.isActive)
    }

    @Test
    fun restartingTheMicDoesNotTrimAgainstTheEarlierBroadcast() {
        tracker.start()
        listeningRun()
        tracker.onSegment("good morning everyone")
        tracker.stop()

        tracker.start()
        listeningRun()
        tracker.onSegment("Good morning everyone, welcome back.")

        assertEquals(listOf("good morning everyone", "Good morning everyone, welcome back."), saved)
    }
}
