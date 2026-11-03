package com.example.konteksto

import com.example.konteksto.data.repository.FirebaseSessionRepository
import com.example.konteksto.data.repository.GeminiSlideAnalyzer
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.host.studio.AnalyzedSlideItem
import com.example.konteksto.ui.host.studio.SlideDeckInfo
import com.example.konteksto.ui.host.studio.resolveSlideDeck
import com.example.konteksto.ui.host.studio.withEdits
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SlideDeckStateTest {
    private fun session(
        fileName: String? = null,
        isAnalyzed: Boolean = false,
    ) = HostSession(
        id = "1",
        venue = "Hall",
        time = "10:00 AM",
        talkCode = "TALK-1",
        title = "Talk",
        speakerName = "Speaker",
        attendeesCount = 0,
        slideDeckFileName = fileName,
        slideDeckSizeMb = 2.0,
        slideDeckPageCount = 5,
        slideDeckUploadTime = "Uploaded earlier",
        isSlideDeckAnalyzed = isAnalyzed,
    )

    @Test
    fun testSessionUpdateKeepsPdfBytesAndUnanalyzedState() {
        val bytes = byteArrayOf(1, 2, 3)
        val current = SlideDeckInfo(fileName = "deck.pdf", isAnalyzed = false, pdfBytes = bytes)

        val resolved = resolveSlideDeck(current, session("deck.pdf", isAnalyzed = true))

        assertFalse(resolved!!.isAnalyzed)
        assertArrayEquals(bytes, resolved.pdfBytes)
    }

    @Test
    fun testRestoredDeckUsesPersistedAnalyzedState() {
        assertFalse(resolveSlideDeck(null, session("deck.pdf", isAnalyzed = false))!!.isAnalyzed)
        assertTrue(resolveSlideDeck(null, session("deck.pdf", isAnalyzed = true))!!.isAnalyzed)
    }

    @Test
    fun testSessionWithDifferentFileReplacesDeck() {
        val current = SlideDeckInfo(fileName = "old.pdf", pdfBytes = byteArrayOf(1))

        val resolved = resolveSlideDeck(current, session("new.pdf"))

        assertEquals("new.pdf", resolved!!.fileName)
        assertNull(resolved.pdfBytes)
    }

    @Test
    fun testSessionWithoutDeckKeepsInFlightUpload() {
        val current = SlideDeckInfo(fileName = "uploading.pdf", isAnalyzed = false)

        assertEquals(current, resolveSlideDeck(current, session(fileName = null)))
        assertNull(resolveSlideDeck(null, session(fileName = null)))
    }

    @Test
    fun testSlideEditsAreTrimmedAndBlankFieldsCleared() {
        val slide = AnalyzedSlideItem(id = "s_1", slideNumber = 3, title = "Old", summary = "Old summary")

        val edited = slide.withEdits(
            title = "  New title ",
            summary = "   ",
            explanation = " Why it works ",
            keyTakeaway = "",
        )

        assertEquals("s_1", edited.id)
        assertEquals(3, edited.slideNumber)
        assertEquals("New title", edited.title)
        assertNull(edited.summary)
        assertEquals("Why it works", edited.explanation)
        assertNull(edited.keyTakeaway)
    }

    @Test
    fun testAnalyzerRefusesToRunWithoutPdf() = runBlocking {
        val analyzer = GeminiSlideAnalyzer(apiKey = "unused")

        assertTrue(analyzer.analyzeSlideDeck(SlideDeckInfo(pdfBytes = null)).isFailure)
        assertTrue(analyzer.analyzeSlideDeck(SlideDeckInfo(pdfBytes = ByteArray(0))).isFailure)
    }

    @Test
    fun testAnalyzedStateIsTrackedInLocalSession() = runBlocking {
        val repository = FirebaseSessionRepository(
            context = null,
            firestoreProvider = { throw IllegalStateException("No firestore in unit test") },
        )
        val sessionId = repository.createSession(
            title = "Talk",
            speakerName = "Speaker",
            category = "Tech",
            venue = "Room A",
            startTime = "10:00 AM",
            endTime = "11:00 AM",
            bannerImageUrl = "",
            hostUid = "user_1",
        ).getOrThrow()
        val slide = AnalyzedSlideItem(id = "s_1", slideNumber = 1, title = "Slide 1")

        repository.saveAnalyzedSlides(sessionId, listOf(slide))
        assertTrue(repository.getHostSessionsStream("user_1").first().single().isSlideDeckAnalyzed)

        repository.clearSlideDeck(sessionId, fileName = null)
        assertFalse(repository.getHostSessionsStream("user_1").first().single().isSlideDeckAnalyzed)
    }
}
