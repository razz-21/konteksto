package com.example.konteksto.ui.navigation

import androidx.annotation.DrawableRes
import com.example.konteksto.R

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Onboarding : Screen("onboarding")
    object Main : Screen("main")
    object CreateSession : Screen("create_session")
    object PresentQr : Screen("present_qr/{sessionId}") {
        fun createRoute(sessionId: String) = "present_qr/$sessionId"
    }
    object HostStudio : Screen("host_studio/{sessionId}") {
        fun createRoute(sessionId: String) = "host_studio/$sessionId"
    }
    object EditSlide : Screen("edit_slide/{sessionId}/{slideId}") {
        fun createRoute(sessionId: String, slideId: String) = "edit_slide/$sessionId/$slideId"
    }
    object EditDomainTerm : Screen("edit_domain_term/{sessionId}/{termId}") {
        fun createRoute(sessionId: String, termId: String) = "edit_domain_term/$sessionId/$termId"
    }
    object AddContextNote : Screen("add_context_note/{sessionId}") {
        fun createRoute(sessionId: String) = "add_context_note/$sessionId"
    }
    object EditContextNote : Screen("edit_context_note/{sessionId}/{noteId}") {
        fun createRoute(sessionId: String, noteId: String) = "edit_context_note/$sessionId/$noteId"
    }
}

sealed class MainTab(
    val route: String,
    val title: String,
    @param:DrawableRes val iconRes: Int,
    val badgeCount: Int? = null,
) {
    object Sessions : MainTab(
        route = "sessions",
        title = "Sessions",
        iconRes = R.drawable.ic_nav_sessions,
    )

    object HostSessions : MainTab(
        route = "host_sessions",
        title = "Host Sessions",
        iconRes = R.drawable.ic_nav_host,
    )

    object Glossary : MainTab(
        route = "glossary",
        title = "Glossary",
        iconRes = R.drawable.ic_nav_glossary,
        badgeCount = 1,
    )

    object Profile : MainTab(
        route = "profile",
        title = "Profile",
        iconRes = R.drawable.ic_nav_profile,
    )

    companion object {
        val tabs = listOf(Sessions, HostSessions, Glossary, Profile)
    }
}
