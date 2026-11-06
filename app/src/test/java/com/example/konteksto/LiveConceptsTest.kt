package com.example.konteksto

import com.example.konteksto.data.repository.ExplanationRepository
import com.example.konteksto.data.repository.FirebaseSessionRepository
import com.example.konteksto.data.repository.GlossaryRepository
import com.example.konteksto.data.repository.GlossaryTermItem
import com.example.konteksto.data.repository.SessionRepository
import com.example.konteksto.domain.model.LiveConceptExplanationItem
import com.example.konteksto.ui.sessions.studio.CompanionTranscriptItem
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val CONCEPT_SESSION_ID = "session_concepts"

private class FakeExplanationRepository : ExplanationRepository {
    override suspend fun explainSelection(
        sessionId: String,
        segmentIds: List<String>,
        selectedTexts: List<String>,
        speakerName: String,
    ) = Result.success(concept("Selection Explanation"))
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

private const val OPENED_AT = 1_000L

private fun concept(title: String, createdAt: Long = 0L) =
    LiveConceptExplanationItem(id = "concept_${title.lowercase().replace(" ", "_")}", title = title, createdAt = createdAt)

@OptIn(ExperimentalCoroutinesApi::class)
class LiveConceptsTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    /** Stands in for live_concepts, which only the server's concept engine writes. */
    private val serverConcepts = MutableStateFlow<List<LiveConceptExplanationItem>>(emptyList())
    private val sessionRepository = object : SessionRepository by FirebaseSessionRepository(
        context = null,
        firestoreProvider = { throw IllegalStateException("No firestore in unit test") },
    ) {
        override fun getLiveConceptsStream(sessionId: String): Flow<List<LiveConceptExplanationItem>> = serverConcepts
    }
    private val glossary = FakeGlossaryRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun attendee() =
        SessionStudioViewModel(CONCEPT_SESSION_ID, sessionRepository, FakeExplanationRepository(), glossary, clock = { OPENED_AT })

    @Test
    fun savedConceptsShowWhenReturningToTheSession() = runTest(testDispatcher) {
        serverConcepts.value = listOf(concept("Polymorphism"), concept("Encapsulation"))

        val viewModel = attendee()
        viewModel.startObserving()

        assertEquals(listOf("Polymorphism", "Encapsulation"), viewModel.uiState.value.conceptExplanations.map { it.title })
        viewModel.stopObserving()
    }

    @Test
    fun conceptsTheServerAddsAppearLive() = runTest(testDispatcher) {
        val viewModel = attendee()
        viewModel.startObserving()
        sessionRepository.addTranscriptEntry(CONCEPT_SESSION_ID, "Host", "Encapsulation hides state")
        assertTrue(viewModel.uiState.value.conceptExplanations.isEmpty())

        serverConcepts.value = listOf(concept("Encapsulation"))

        assertEquals(listOf("Encapsulation"), viewModel.uiState.value.conceptExplanations.map { it.title })
        viewModel.stopObserving()
    }

    @Test
    fun conceptsSavedBeforeOpeningTheStudioAreNotAnnounced() = runTest(testDispatcher) {
        serverConcepts.value = listOf(concept("Encapsulation", createdAt = OPENED_AT - 1))

        val viewModel = attendee()
        viewModel.startObserving()

        assertEquals(1, viewModel.uiState.value.conceptExplanations.size)
        assertNull(viewModel.uiState.value.newConceptAnnouncement)
        viewModel.stopObserving()
    }

    @Test
    fun aConceptSavedWhileInTheStudioIsAnnouncedOnce() = runTest(testDispatcher) {
        serverConcepts.value = listOf(concept("Encapsulation", createdAt = OPENED_AT - 1))
        val viewModel = attendee()
        viewModel.startObserving()

        serverConcepts.value = listOf(concept("Inheritance", createdAt = OPENED_AT + 1)) + serverConcepts.value
        val announced = viewModel.uiState.value.newConceptAnnouncement
        assertEquals("Inheritance", announced?.title)

        viewModel.onConceptAnnouncementShown(announced!!.id)
        viewModel.stopObserving()
        viewModel.startObserving()
        assertNull(viewModel.uiState.value.newConceptAnnouncement)
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

        serverConcepts.value = listOf(concept("Inheritance"))
        val detected = viewModel.uiState.value.conceptExplanations.single()

        viewModel.bookmarkConceptExplanation(detected)
        assertEquals(listOf("Inheritance"), viewModel.uiState.value.sessionGlossaryTerms.map { it.term })
        assertTrue(viewModel.uiState.value.conceptExplanations.single().isBookmarked)

        viewModel.removeGlossaryTerm(viewModel.uiState.value.sessionGlossaryTerms.single())
        assertTrue(viewModel.uiState.value.sessionGlossaryTerms.isEmpty())
        assertFalse(viewModel.uiState.value.conceptExplanations.single().isBookmarked)
        viewModel.stopObserving()
    }

    @Test
    fun bookmarksSurviveTheServerUpdatingTheList() = runTest(testDispatcher) {
        serverConcepts.value = listOf(concept("Inheritance"))
        val viewModel = attendee()
        viewModel.startObserving()
        viewModel.bookmarkConceptExplanation(viewModel.uiState.value.conceptExplanations.single())

        serverConcepts.value = listOf(concept("Composition"), concept("Inheritance"))

        assertEquals(
            listOf(false, true),
            viewModel.uiState.value.conceptExplanations.map { it.isBookmarked },
        )
        viewModel.stopObserving()
    }
}
