package com.example.konteksto

import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.host.studio.resolveAudioBroadcasting
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioBroadcastStateTest {
    private fun session(isLive: Boolean) = HostSession(
        id = "1",
        venue = "Hall",
        time = "10:00 AM",
        talkCode = "TALK-1",
        title = "Talk",
        speakerName = "Speaker",
        attendeesCount = 0,
        isLive = isLive,
    )

    @Test
    fun testUnrelatedSessionUpdateKeepsMicOffDuringLiveSession() {
        val previous = session(isLive = true)
        val updated = previous.copy(isSlideDeckAnalyzed = true)

        assertFalse(resolveAudioBroadcasting(false, previous, updated))
    }

    @Test
    fun testUnrelatedSessionUpdateKeepsMicOnDuringLiveSession() {
        val previous = session(isLive = true)
        val updated = previous.copy(isSlideDeckAnalyzed = true)

        assertTrue(resolveAudioBroadcasting(true, previous, updated))
    }

    @Test
    fun testStartingSessionTurnsMicOn() {
        assertTrue(resolveAudioBroadcasting(false, session(isLive = false), session(isLive = true)))
    }

    @Test
    fun testEndingSessionTurnsMicOffEvenIfItWasOn() {
        assertFalse(resolveAudioBroadcasting(true, session(isLive = true), session(isLive = false)))
    }

    @Test
    fun testFirstLoadFollowsSessionStatus() {
        assertTrue(resolveAudioBroadcasting(false, null, session(isLive = true)))
        assertFalse(resolveAudioBroadcasting(false, null, session(isLive = false)))
    }
}
