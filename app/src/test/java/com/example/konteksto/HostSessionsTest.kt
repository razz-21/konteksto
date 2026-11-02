package com.example.konteksto

import com.example.konteksto.domain.model.ConferenceThemeBanner
import com.example.konteksto.domain.model.HostSession
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
}
