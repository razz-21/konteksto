package com.example.konteksto

import com.example.konteksto.data.repository.FirebaseSessionRepository
import com.example.konteksto.domain.model.ConferenceThemeBanner
import com.example.konteksto.domain.model.HostSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HostSessionsTest {
    @Test
    fun testHostSessionModel() {
        val session = HostSession(
            id = "1",
            venue = "Tech Summit Hall B",
            time = "10:00 AM",
            talkCode = "TALK–415",
            title = "Edge AI & Distributed Cloud Architecture: Scaling to Zero Latency",
            speakerName = "Engr. Marco Valerio",
            attendeesCount = 890,
            isLive = true,
        )

        assertEquals("1", session.id)
        assertEquals("Tech Summit Hall B", session.venue)
        assertEquals("10:00 AM", session.time)
        assertEquals("TALK–415", session.talkCode)
        assertEquals("Engr. Marco Valerio", session.speakerName)
        assertEquals(890, session.attendeesCount)
        assertTrue(session.isLive)
    }

    @Test
    fun testConferenceThemeBannersCountAndCategories() {
        val banners = ConferenceThemeBanner.DefaultCategories
        assertEquals(12, banners.size)

        val categoryNames = banners.map { it.categoryName }
        assertTrue(categoryNames.contains("Business & Leadership"))
        assertTrue(categoryNames.contains("Health & Medicine"))
        assertTrue(categoryNames.contains("Education & Learning"))
        assertTrue(categoryNames.contains("Arts & Creative Design"))
        assertTrue(categoryNames.contains("Climate & Environment"))
        assertTrue(categoryNames.contains("Community & Society"))
        assertTrue(categoryNames.contains("Startups & Ventures"))
        assertTrue(categoryNames.contains("Science & Discovery"))
        assertTrue(categoryNames.contains("Media & Storytelling"))
        assertTrue(categoryNames.contains("Policy & Governance"))
        assertTrue(categoryNames.contains("Sports & Performance"))
        assertTrue(categoryNames.contains("Technology & Digital"))
    }

    @Test
    fun testGetHostSessionsStreamFiltersByHostUid() = runBlocking {
        val repository = FirebaseSessionRepository(
            context = null,
            firestoreProvider = { throw IllegalStateException("No firestore in unit test") },
        )

        repository.createSession(
            title = "User 1 Session",
            speakerName = "User 1",
            category = "Tech",
            venue = "Room A",
            startTime = "10:00 AM",
            endTime = "11:00 AM",
            bannerImageUrl = "",
            talkCode = "TALK1",
            hostUid = "user_1",
        )

        repository.createSession(
            title = "User 2 Session",
            speakerName = "User 2",
            category = "Tech",
            venue = "Room B",
            startTime = "11:00 AM",
            endTime = "12:00 PM",
            bannerImageUrl = "",
            talkCode = "TALK2",
            hostUid = "user_2",
        )

        val user1Sessions = repository.getHostSessionsStream("user_1").first()
        assertEquals(1, user1Sessions.size)
        assertEquals("User 1 Session", user1Sessions[0].title)
        assertEquals("user_1", user1Sessions[0].hostUid)

        val user2Sessions = repository.getHostSessionsStream("user_2").first()
        assertEquals(1, user2Sessions.size)
        assertEquals("User 2 Session", user2Sessions[0].title)
        assertEquals("user_2", user2Sessions[0].hostUid)

        val emptyUserSessions = repository.getHostSessionsStream("").first()
        assertEquals(0, emptyUserSessions.size)
    }
}
