package com.example.konteksto

import com.example.konteksto.data.repository.FirebaseSessionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionLeaveTest {
    @Test
    fun testLeavingSessionRemovesItFromJoinedSessions() = runBlocking {
        val repository = FirebaseSessionRepository(
            context = null,
            firestoreProvider = { throw IllegalStateException("No firestore in unit test") },
        )
        val sessionId = repository.createSession(
            title = "Session To Leave",
            speakerName = "Speaker",
            category = "Tech",
            venue = "Room A",
            startTime = "10:00 AM",
            endTime = "11:00 AM",
            bannerImageUrl = "",
            talkCode = "TALK1",
            hostUid = "host_1",
        ).getOrThrow()

        val sawJoined = CompletableDeferred<Unit>()
        val sawLeave = CompletableDeferred<Set<String>>()
        var leaving = false
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            repository.getJoinedSessionIdsStream("user_1").collect { ids ->
                if (sessionId in ids) sawJoined.complete(Unit)
                if (leaving && sessionId !in ids) sawLeave.complete(ids)
            }
        }
        // Let the stream finish its initial load before joining.
        yield()

        repository.joinSession("user_1", sessionId).getOrThrow()
        withTimeout(5_000) { sawJoined.await() }

        leaving = true
        val result = repository.leaveSession("user_1", sessionId)

        assertTrue(result.isSuccess)
        assertTrue(withTimeout(5_000) { sawLeave.await() }.isEmpty())
        collector.cancel()
    }
}
