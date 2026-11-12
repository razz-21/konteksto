package com.example.konteksto

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.host.studio.HostStudioBroadcastView
import com.example.konteksto.ui.host.studio.TranscriptEntry
import com.example.konteksto.ui.sessions.studio.CompanionTranscriptItem
import com.example.konteksto.ui.sessions.studio.SessionLiveCompanionView
import com.example.konteksto.ui.theme.KontekstoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveTranscriptScrollTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val session = HostSession(
        id = "1",
        venue = "Hall",
        time = "10:00 AM",
        talkCode = "TALK-1",
        title = "Talk",
        speakerName = "Speaker",
        attendeesCount = 0,
        isLive = true,
    )

    private fun lines(count: Int) = (1..count).map { index ->
        CompanionTranscriptItem(id = "id_$index", speakerName = "Speaker", timestamp = "10:00", text = "Line $index")
    }

    private var transcripts by mutableStateOf(emptyList<CompanionTranscriptItem>())
    private var partial by mutableStateOf("")

    private fun showAttendeeView(lineCount: Int = 30) {
        transcripts = lines(lineCount)
        composeRule.setContent {
            KontekstoTheme {
                SessionLiveCompanionView(
                    session = session,
                    customTranscripts = transcripts,
                    livePartialText = partial,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertDisplayed(text: String) {
        composeRule.waitForIdle()
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }

    private fun scrollUp() {
        composeRule.onNodeWithText("Line ${transcripts.size - 1}").performTouchInput { swipeDown() }
        composeRule.waitForIdle()
    }

    @Test
    fun followsNewLinesAndInProgressTextWhileAtTheBottom() {
        showAttendeeView()
        assertDisplayed("Line 30")
        // Lazy: lines far outside the panel aren't composed at all.
        composeRule.onNodeWithText("Line 1").assertDoesNotExist()

        partial = "and the reason"
        assertDisplayed("and the reason")

        partial = ""
        transcripts = lines(31)
        assertDisplayed("Line 31")
    }

    @Test
    fun followsAnInProgressSentenceTallerThanThePanel() {
        showAttendeeView()
        val words = (1..400).joinToString(" ") { "word$it" }
        partial = "$words finalword"
        composeRule.waitForIdle()
        // Stays at the bottom of the bubble rather than stopping at its top edge.
        composeRule.onNodeWithText("Jump to live").assertDoesNotExist()
    }

    @Test
    fun staysPutWhenScrolledUpUntilJumpToLive() {
        showAttendeeView()
        scrollUp()
        composeRule.onNodeWithText("Line 30").assertDoesNotExist()

        transcripts = lines(31)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Line 31").assertDoesNotExist()

        composeRule.onNodeWithText("Jump to live").performClick()
        assertDisplayed("Line 31")
        composeRule.onNodeWithText("Jump to live").assertDoesNotExist()

        // Following resumes afterwards.
        transcripts = lines(32)
        assertDisplayed("Line 32")
    }

    @Test
    fun keepsReadingPositionWhileInProgressTextGrows() {
        showAttendeeView()
        partial = "first words"
        assertDisplayed("Line 30")

        // New words keep updating the same live bubble, but the list must not follow each change.
        partial = (1..400).joinToString(" ") { "word$it" }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Line 30").assertIsDisplayed()
        composeRule.onNodeWithText("LIVE SPEAKING…").assertIsDisplayed()
    }

    @Test
    fun staysPutWhileSelectingLines() {
        showAttendeeView()
        composeRule.onNodeWithText("Line 30").performClick()

        transcripts = lines(31)
        composeRule.waitForIdle()
        // The list may compose the next line ahead of time, but it must not scroll to it.
        composeRule.onNodeWithText("Line 31").assertIsNotDisplayed()
        composeRule.onNodeWithText("Line 30").assertIsDisplayed()
        composeRule.onNodeWithText("Jump to live").assertIsDisplayed()

        composeRule.onNodeWithText("Line 30").performClick()
        assertDisplayed("Line 31")
    }

    @Test
    fun selectionSurvivesNewLines() {
        showAttendeeView()
        composeRule.onNodeWithText("Line 30").performClick()
        transcripts = lines(40)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("What did they mean?").assertIsEnabled()
    }

    @Test
    fun hostViewShowsInProgressTextAndFollows() {
        var hostLines by mutableStateOf(
            (1..30).map { TranscriptEntry(id = "id_$it", speakerName = "Host", timestamp = "10:00", text = "Host line $it") },
        )
        var hostPartial by mutableStateOf("")
        composeRule.setContent {
            KontekstoTheme {
                HostStudioBroadcastView(
                    isAudioBroadcasting = true,
                    transcripts = hostLines,
                    partialTranscript = hostPartial,
                    onAddTranscript = {},
                )
            }
        }
        assertDisplayed("Host line 30")

        hostPartial = "speaking now"
        assertDisplayed("speaking now")

        hostPartial = ""
        hostLines = hostLines + TranscriptEntry(id = "id_31", speakerName = "Host", timestamp = "10:00", text = "Host line 31")
        assertDisplayed("Host line 31")
    }
}
