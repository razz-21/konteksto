package com.example.konteksto

import com.example.konteksto.ui.navigation.MainTab
import com.example.konteksto.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationTest {
    @Test
    fun testRootRoutes() {
        assertEquals("login", Screen.Login.route)
        assertEquals("main", Screen.Main.route)
    }

    @Test
    fun testEditSlideRoute() {
        assertEquals("edit_slide/session_1/s_2", Screen.EditSlide.createRoute("session_1", "s_2"))
    }

    @Test
    fun testMainTabRoutesAndBadge() {
        assertEquals(4, MainTab.tabs.size)
        assertEquals("sessions", MainTab.Sessions.route)
        assertEquals("host_sessions", MainTab.HostSessions.route)
        assertEquals("glossary", MainTab.Glossary.route)
        assertEquals("profile", MainTab.Profile.route)

        assertEquals(1, MainTab.Glossary.badgeCount)
    }
}
