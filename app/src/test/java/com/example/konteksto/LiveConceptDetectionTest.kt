package com.example.konteksto

import com.example.konteksto.data.repository.ExplanationRepository
import com.example.konteksto.data.repository.FirebaseSessionRepository
import com.example.konteksto.data.repository.GlossaryRepository
import com.example.konteksto.data.repository.GlossaryTermItem
import com.example.konteksto.ui.host.studio.TranscriptEntry
import com.example.konteksto.ui.sessions.studio.CompanionTranscriptItem
import com.example.konteksto.ui.sessions.studio.LiveConceptExplanationItem
import com.example.konteksto.ui.sessions.studio.SessionStudioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val CONCEPT_SESSION_ID = "session_concepts"

private class FakeExplanationRepository : ExplanationRepository {
    val detectionCalls = mutableListOf<List<String>>()
    var nextDetected: List<LiveConceptExplanationItem> = emptyList()

    override suspend fun explainSelection(
        sessionId: String,
        segmentIds: List<String>,
        selectedTexts: List<String>,
        speakerName: String,
    ) = Result.success(concept("Selection Explanation"))

    override suspend fun detectLiveConcepts(
        sessionId: String,
        transcripts: List<TranscriptEntry>,
    ): Result<List<LiveConceptExplanationItem>> {
        detectionCalls += transcripts.map { it.text }
        return Result.success(nextDetected)
    }
}

private class FakeGlossaryRepository : GlossaryRepository {
    val saved = MutableStateFlow<List<GlossaryTermItem>>(emptyList())

    override fun getSavedGlossaryTermsStream(userUid: String): Flow<List<GlossaryTermItem>> = saved
    override fun getSessionGlossaryStream(sessionId: String): Flow<List<GlossaryTermItem>> = error("Room glossary is bookmarks only")

    override suspend fun saveTermToGlossary(userUid: String, sessionId: String, sessionTitle: String, term: GlossaryTermItem): Result<Unit> {
        saved.value = saved.value + term.copy(sessionId = sessionId)
        return Result.success(Unit)
    }

    override suspend fun removeTermFromGlossary(userUid: String, sessionId: String, termId: String): Result<Unit> {
        saved.value = saved.value.filterNot { it.id == termId }
        return Result.success(Unit)
    }

    override suspend fun isTermBookmarked(userUid: String, termId: String) = saved.value.any { it.id == termId }
}

private fun concept(title: String) = LiveConceptExplanationItem(id = "concept_${title.lowercase().replace(" ", "_")}", title = title)

@OptIn(ExperimentalCoroutinesApi::class)
class LiveConceptDetectionTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val sessionRepository = FirebaseSessionRepository(
        context = null,
        firestoreProvider = { throw IllegalStateException("No firestore in unit test") },
    )
    private val explanations = FakeExplanationRepository()
    private val glossary = FakeGlossaryRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun attendee() = SessionStudioViewModel(CONCEPT_SESSION_ID, sessionRepository, explanations, glossary)

    @Test
    fun onlyLinesSpokenAfterJoiningTriggerDetection() = runTest(testDispatcher) {
        sessionRepository.addTranscriptEntry(CONCEPT_SESSION_ID, "Host", "Earlier line")
        val viewModel = attendee()
        viewModel.startObserving()
        assertTrue(explanations.detectionCalls.isEmpty())

        explanations.nextDetected = listOf(concept("Encapsulation"))
        sessionRepository.addTranscriptEntry(CONCEPT_SESSION_ID, "Host", "Encapsulation hides state")

        assertEquals(1, explanations.detectionCalls.size)
        assertEquals(listOf("Encapsulation"), viewModel.uiState.value.conceptExplanations.map { it.title })
        viewModel.stopObserving()
    }

    @Test
    fun explainingASelectionDoesNotAddALiveConcept() = runTest(testDispatcher) {
        val viewModel = attendee()
        viewModel.startObserving()

        viewModel.explainSelection(listOf(CompanionTranscriptItem("tr_1", "Host", "10:00", "Some line")))

        assertEquals("Selection Explanation", viewModel.uiState.value.activeExplanation?.explanation?.title)
        assertTrue(viewModel.uiState.value.conceptExplanations.isEmpty())
        viewModel.stopObserving()
    }

    @Test
    fun glossaryIsOnlyBookmarkedConceptsFromThisSession() = runTest(testDispatcher) {
        glossary.saved.value = listOf(GlossaryTermItem(id = "other", term = "Other", definition = "", sessionId = "another_session"))
        val viewModel = attendee()
        viewModel.startObserving()
        assertTrue(viewModel.uiState.value.sessionGlossaryTerms.isEmpty())

        explanations.nextDetected = listOf(concept("Inheritance"))
        sessionRepository.addTranscriptEntry(CONCEPT_SESSION_ID, "Host", "Inheritance reuses code")
        val detected = viewModel.uiState.value.conceptExplanations.single()

        viewModel.bookmarkConceptExplanation(detected)
        assertEquals(listOf("Inheritance"), viewModel.uiState.value.sessionGlossaryTerms.map { it.term })
        assertTrue(viewModel.uiState.value.conceptExplanations.single().isBookmarked)

        viewModel.removeGlossaryTerm(viewModel.uiState.value.sessionGlossaryTerms.single())
        assertTrue(viewModel.uiState.value.sessionGlossaryTerms.isEmpty())
        assertFalse(viewModel.uiState.value.conceptExplanations.single().isBookmarked)
        viewModel.stopObserving()
    }
}
