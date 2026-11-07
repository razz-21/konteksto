package com.example.konteksto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.konteksto.data.audio.AudioInputRepository
import com.example.konteksto.data.repository.AuthRepository
import com.example.konteksto.domain.model.AudioInputDevice
import com.example.konteksto.domain.model.AudioInputType
import com.example.konteksto.domain.model.AuthUser
import com.example.konteksto.data.repository.FirebaseSessionRepository
import com.example.konteksto.data.repository.SessionRepository
import com.example.konteksto.ui.host.studio.HostStudioViewModel
import com.example.konteksto.ui.host.studio.TranscriptEntry
import com.example.konteksto.ui.host.studio.TranscriptionEngine
import com.example.konteksto.ui.host.studio.publishThrottled
import com.example.konteksto.ui.sessions.studio.SessionStudioViewModel
import com.example.konteksto.ui.sessions.studio.TRANSCRIPT_PAGE_SIZE
import com.example.konteksto.ui.sessions.studio.expireStalePartial
import com.example.konteksto.ui.sessions.studio.mergeTranscriptWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val SESSION_ID = "session_1"

private class FakeEngine(private val onSegmentComplete: (String) -> Unit) : TranscriptionEngine {
    private val partial = MutableStateFlow("")
    override val partialText: StateFlow<String> = partial
    var startCount = 0

    override fun startListening() {
        startCount++
    }

    override fun stopListening() {
        finishSegment()
    }

    fun speak(text: String) {
        partial.value = text
    }

    fun finishSegment() {
        val text = partial.value
        partial.value = ""
        if (text.isNotBlank()) onSegmentComplete(text)
    }
}

private object SignedOutAuthRepository : AuthRepository {
    override val currentUser: AuthUser? = null
    override fun getAuthStateStream(): Flow<AuthUser?> = emptyFlow()
    override suspend fun signInWithGoogleIdToken(idToken: String) = error("Not used")
    override suspend fun signInWithEmailAndPassword(email: String, password: String) = error("Not used")
    override suspend fun signUpWithEmailAndPassword(email: String, password: String) = error("Not used")
    override suspend fun signInAnonymously() = error("Not used")
    override fun signOut() = Unit
}

private object BuiltInMicRepository : AudioInputRepository {
    override val currentInputDevice: Flow<AudioInputDevice> =
        flowOf(AudioInputDevice(AudioInputType.BuiltInMic))
    override fun setBluetoothMicEnabled(enabled: Boolean) = Unit
}

private fun localRepository() = FirebaseSessionRepository(
    context = null,
    firestoreProvider = { throw IllegalStateException("No firestore in unit test") },
)

private fun entry(createdAt: Long) = TranscriptEntry(
    id = "tr_$createdAt",
    speakerName = "Speaker",
    timestamp = "10:00 AM",
    text = "Line $createdAt",
    createdAt = createdAt,
)

@OptIn(ExperimentalCoroutinesApi::class)
class LiveTranscriptSyncTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun hostViewModel(repository: SessionRepository): Pair<HostStudioViewModel, FakeEngine> {
        lateinit var engine: FakeEngine
        val viewModel = HostStudioViewModel(
            sessionId = SESSION_ID,
            transcriptionEngineFactory = { onSegmentComplete ->
                FakeEngine(onSegmentComplete).also { engine = it }
            },
            audioInputRepository = BuiltInMicRepository,
            sessionRepository = repository,
            authRepository = SignedOutAuthRepository,
        )
        return viewModel to engine
    }

    @Test
    fun publishesAtMostOncePerIntervalAndAlwaysTheLatestText() = runTest {
        val partial = MutableStateFlow("")
        val published = mutableListOf<String>()
        val job = launch { partial.publishThrottled(1_000L) { published += it } }
        runCurrent()

        partial.value = "the"
        runCurrent()
        partial.value = "the reason"
        runCurrent()
        partial.value = "the reason this"
        runCurrent()
        assertEquals(listOf("the"), published)

        advanceTimeBy(1_001L)
        assertEquals(listOf("the", "the reason this"), published)
        job.cancel()
    }

    @Test
    fun throttledTextFinishedBeforeItsTurnIsNeverPublished() = runTest {
        val partial = MutableStateFlow("")
        val published = mutableListOf<String>()
        val job = launch { partial.publishThrottled(1_000L) { published += it } }
        runCurrent()

        partial.value = "first"
        runCurrent()
        partial.value = "first words"
        runCurrent()
        // Saving the finished line clears the text while the throttle is still waiting.
        partial.value = ""
        advanceTimeBy(1_001L)

        assertEquals(listOf("first"), published)
        job.cancel()
    }

    @Test
    fun abandonedPartialExpiresButActiveSpeechDoesNot() = runTest {
        val source = MutableSharedFlow<String>()
        val seen = mutableListOf<String>()
        val job = launch { source.expireStalePartial(staleAfterMs = 1_000L).collect { seen += it } }
        runCurrent()

        source.emit("still")
        advanceTimeBy(900L)
        source.emit("still talking")
        advanceTimeBy(900L)
        assertEquals(listOf("still", "still talking"), seen)

        advanceTimeBy(101L)
        assertEquals(listOf("still", "still talking", ""), seen)
        job.cancel()
    }

    @Test
    fun mergeKeepsLinesTheListenerWindowHasMovedPast() {
        val merged = mergeTranscriptWindow(
            known = listOf(entry(1), entry(2), entry(3)),
            window = listOf(entry(2), entry(3), entry(4)),
            windowSize = 3,
        )
        assertEquals(listOf(1L, 2L, 3L, 4L), merged.map { it.createdAt })
    }

    @Test
    fun mergeDoesNotHideAGapFromLinesMissedInTheBackground() {
        val merged = mergeTranscriptWindow(
            known = listOf(entry(1), entry(2)),
            window = listOf(entry(10), entry(11)),
            windowSize = 2,
        )
        assertEquals(listOf(10L, 11L), merged.map { it.createdAt })
    }

    @Test
    fun finishingALineClearsTheLivePartialButOtherLinesDoNot() = runBlocking {
        val repository = localRepository()
        repository.updateLivePartialTranscript(SESSION_ID, "still talking")

        repository.addTranscriptEntry(SESSION_ID, "Host", "A typed note")
        assertEquals("still talking", repository.getLivePartialTranscriptStream(SESSION_ID).first())

        repository.addTranscriptEntry(SESSION_ID, "Host", "still talking", clearsLivePartial = true)
        assertEquals("", repository.getLivePartialTranscriptStream(SESSION_ID).first())
    }

    @Test
    fun rapidLinesGetUniqueIdsAndStrictlyIncreasingOrder() = runBlocking {
        val repository = localRepository()
        val entries = (1..200).map { index ->
            repository.addTranscriptEntry(SESSION_ID, "Host", "Line $index").getOrThrow()
        }

        assertEquals(200, entries.map { it.id }.toSet().size)
        assertEquals(entries.map { it.createdAt }.sorted(), entries.map { it.createdAt })
        assertEquals(200, entries.map { it.createdAt }.toSet().size)
    }

    @Test
    fun streamLimitAndEarlierPagesCoverTheWholeTranscript() = runBlocking {
        val repository = localRepository()
        val entries = (1..5).map { index ->
            repository.addTranscriptEntry(SESSION_ID, "Host", "Line $index").getOrThrow()
        }

        val window = repository.getTranscriptsStream(SESSION_ID, limit = 2).first()
        assertEquals(listOf("Line 4", "Line 5"), window.map { it.text })

        val earlier = repository.getEarlierTranscripts(SESSION_ID, entries[3].createdAt, limit = 2).getOrThrow()
        assertEquals(listOf("Line 2", "Line 3"), earlier.map { it.text })
    }

    @Test
    fun speechReachesAttendeesAndFinishedTextIsNotLeftInTheBubble() = runTest(testDispatcher) {
        val repository = localRepository()
        val (host, engine) = hostViewModel(repository)
        val attendee = SessionStudioViewModel(SESSION_ID, repository)
        attendee.startObserving()

        engine.speak("and the reason")
        assertEquals("and the reason", host.uiState.value.partialTranscript)
        assertEquals("and the reason", attendee.uiState.value.livePartialText)

        engine.finishSegment()
        assertEquals("", host.uiState.value.partialTranscript)
        assertEquals("", attendee.uiState.value.livePartialText)
        assertEquals(listOf("and the reason"), attendee.uiState.value.transcripts.map { it.text })
        assertEquals(listOf("and the reason"), host.uiState.value.transcripts.map { it.text })
        attendee.stopObserving()
    }

    @Test
    fun turningTheMicOffSavesTheLastWordsBeforeTheEndedEvent() = runTest(testDispatcher) {
        val (host, engine) = hostViewModel(localRepository())
        host.toggleAudioBroadcasting()
        assertEquals(1, engine.startCount)

        engine.speak("my last words")
        host.toggleAudioBroadcasting()

        assertEquals(
            listOf("Audio broadcast enabled", "my last words", "Audio broadcast ended"),
            host.uiState.value.transcripts.map { it.text },
        )
    }

    @Test
    fun switchingToTheSlidesTabKeepsTheMicRunning() = runTest(testDispatcher) {
        val (host, engine) = hostViewModel(localRepository())
        host.toggleAudioBroadcasting()

        host.setSelectedTab(1)
        engine.speak("still broadcasting")
        engine.finishSegment()

        assertTrue(host.uiState.value.isAudioBroadcasting)
        assertEquals(1, engine.startCount)
        assertTrue(host.uiState.value.transcripts.any { it.text == "still broadcasting" })
    }

    @Test
    fun leavingTheStudioStillSavesWordsInProgress() = runTest(testDispatcher) {
        val repository = localRepository()
        lateinit var engine: FakeEngine
        val store = ViewModelStore()
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HostStudioViewModel(
                sessionId = SESSION_ID,
                transcriptionEngineFactory = { onSegmentComplete ->
                    FakeEngine(onSegmentComplete).also { engine = it }
                },
                audioInputRepository = BuiltInMicRepository,
                sessionRepository = repository,
                authRepository = SignedOutAuthRepository,
            ) as T
        }
        ViewModelProvider(store, factory)[HostStudioViewModel::class.java]

        engine.speak("words in progress")
        store.clear()

        assertEquals(
            listOf("words in progress"),
            repository.getTranscriptsStream(SESSION_ID).first().map { it.text },
        )
    }

    @Test
    fun rejectedWriteIsReportedAndNotShownAsSent() = runTest(testDispatcher) {
        val repository = object : SessionRepository by localRepository() {
            override suspend fun addTranscriptEntry(
                sessionId: String,
                speakerName: String,
                text: String,
                isSystemEvent: Boolean,
                clearsLivePartial: Boolean,
            ): Result<TranscriptEntry> = Result.failure(IllegalStateException("Denied"))
        }
        val (host, _) = hostViewModel(repository)

        host.addTranscriptEntry(text = "A line")

        assertNotNull(host.uiState.value.errorMessage)
        assertTrue(host.uiState.value.transcripts.isEmpty())
    }

    @Test
    fun rejectedLivePartialIsOnlyReportedOnce() = runTest(testDispatcher) {
        val repository = object : SessionRepository by localRepository() {
            override suspend fun updateLivePartialTranscript(sessionId: String, text: String) =
                Result.failure<Unit>(IllegalStateException("Denied"))
        }
        val (host, engine) = hostViewModel(repository)

        engine.speak("first")
        assertNotNull(host.uiState.value.errorMessage)

        host.clearErrorMessage()
        testScheduler.advanceTimeBy(1_001L)
        engine.speak("second")
        testScheduler.advanceTimeBy(1_001L)
        assertNull(host.uiState.value.errorMessage)
    }

    @Test
    fun attendeeKeepsOlderLinesPagesBackAndStopsAtTheStart() = runTest(testDispatcher) {
        val repository = localRepository()
        val total = (TRANSCRIPT_PAGE_SIZE * 2).toInt() + 5
        repeat(total) { index ->
            repository.addTranscriptEntry(SESSION_ID, "Host", "Line $index")
        }
        val attendee = SessionStudioViewModel(SESSION_ID, repository)
        attendee.startObserving()
        assertEquals(TRANSCRIPT_PAGE_SIZE.toInt(), attendee.uiState.value.transcripts.size)
        assertTrue(attendee.uiState.value.hasEarlierTranscripts)

        // A new line pushes the oldest out of the listener's window, but the attendee keeps it.
        repository.addTranscriptEntry(SESSION_ID, "Host", "Newest")
        assertEquals(TRANSCRIPT_PAGE_SIZE.toInt() + 1, attendee.uiState.value.transcripts.size)
        assertEquals("Newest", attendee.uiState.value.transcripts.last().text)

        attendee.loadEarlierTranscripts()
        assertTrue(attendee.uiState.value.hasEarlierTranscripts)
        attendee.loadEarlierTranscripts()
        assertFalse(attendee.uiState.value.hasEarlierTranscripts)

        val texts = attendee.uiState.value.transcripts.map { it.text }
        assertEquals((0 until total).map { "Line $it" } + "Newest", texts)
        attendee.stopObserving()
    }

    @Test
    fun attendeeSeesARejectedListenerAndCanRetry() = runTest(testDispatcher) {
        var shouldFail = true
        val delegate = localRepository()
        val repository = object : SessionRepository by delegate {
            override fun getTranscriptsStream(sessionId: String, limit: Long?) = flow {
                if (shouldFail) throw IllegalStateException("Denied")
                emit(listOf(entry(1)))
            }
        }
        val attendee = SessionStudioViewModel(SESSION_ID, repository)

        attendee.startObserving()
        assertTrue(attendee.uiState.value.didLiveTranscriptFail)

        shouldFail = false
        attendee.retryLiveTranscript()
        assertFalse(attendee.uiState.value.didLiveTranscriptFail)
        assertEquals(1, attendee.uiState.value.transcripts.size)
        attendee.stopObserving()
    }
}
