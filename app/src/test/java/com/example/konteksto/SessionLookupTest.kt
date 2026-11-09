package com.example.konteksto

import com.example.konteksto.data.repository.FirebaseSessionRepository
import com.example.konteksto.data.repository.SessionRepository
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.components.SessionLookup
import com.example.konteksto.ui.host.PresentQrViewModel
import com.example.konteksto.ui.sessions.studio.SessionStudioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private const val LOOKUP_SESSION_ID = "session_lookup"

private val liveSession = HostSession(
    id = LOOKUP_SESSION_ID,
    venue = "Hall A",
    time = "10:00 AM",
    talkCode = "ABC123",
    title = "Session",
    speakerName = "Speaker",
    attendeesCount = 0,
    isLive = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SessionLookupTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    /** Stands in for the session document listener; a thrown error is a rejected listener. */
    private val sessionDocument = MutableSharedFlow<HostSession?>()
    private var failListener = false
    private val sessionRepository = object : SessionRepository by FirebaseSessionRepository(
        context = null,
        firestoreProvider = { throw IllegalStateException("No firestore in unit test") },
    ) {
        override fun getSessionStream(sessionId: String): Flow<HostSession?> =
            if (failListener) flow { error("Permission denied") } else sessionDocument
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun attendee() = SessionStudioViewModel(LOOKUP_SESSION_ID, sessionRepository)

    @Test
    fun staysLoadingUntilTheSessionArrives() = runTest(testDispatcher) {
        val viewModel = attendee()
        viewModel.startObserving()
        assertEquals(SessionLookup.Loading, viewModel.uiState.value.sessionLookup)

        sessionDocument.emit(liveSession)

        assertEquals(SessionLookup.Found(liveSession), viewModel.uiState.value.sessionLookup)
        viewModel.stopObserving()
    }

    @Test
    fun aMissingSessionIsNotFound() = runTest(testDispatcher) {
        val viewModel = attendee()
        viewModel.startObserving()

        sessionDocument.emit(null)

        assertEquals(SessionLookup.NotFound, viewModel.uiState.value.sessionLookup)
        viewModel.stopObserving()
    }

    @Test
    fun aSessionDeletedWhileOpenBecomesNotFound() = runTest(testDispatcher) {
        val viewModel = attendee()
        viewModel.startObserving()
        sessionDocument.emit(liveSession)

        sessionDocument.emit(null)

        assertEquals(SessionLookup.NotFound, viewModel.uiState.value.sessionLookup)
        viewModel.stopObserving()
    }

    @Test
    fun aRejectedListenerCanBeRetried() = runTest(testDispatcher) {
        failListener = true
        val viewModel = attendee()
        viewModel.startObserving()
        assertEquals(SessionLookup.Failed, viewModel.uiState.value.sessionLookup)

        failListener = false
        viewModel.retrySessionLookup()
        sessionDocument.emit(liveSession)

        assertEquals(SessionLookup.Found(liveSession), viewModel.uiState.value.sessionLookup)
        viewModel.stopObserving()
    }

    @Test
    fun presentQrShowsOnlyARealSession() = runTest(testDispatcher) {
        val viewModel = PresentQrViewModel(LOOKUP_SESSION_ID, sessionRepository)
        assertEquals(SessionLookup.Loading, viewModel.sessionLookup.value)

        sessionDocument.emit(null)
        assertEquals(SessionLookup.NotFound, viewModel.sessionLookup.value)

        sessionDocument.emit(liveSession)
        assertEquals(SessionLookup.Found(liveSession), viewModel.sessionLookup.value)
    }
}
