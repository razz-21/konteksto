package com.example.konteksto.ui.main

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.konteksto.data.repository.FirebaseSessionRepository
import com.example.konteksto.data.repository.SessionRepository
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.domain.model.UserProfile
import com.example.konteksto.ui.components.showKontekstoSnackbar
import com.example.konteksto.ui.glossary.GlossaryScreen
import com.example.konteksto.ui.host.HostSessionsScreen
import com.example.konteksto.ui.navigation.MainTab
import com.example.konteksto.ui.profile.ProfileScreen
import com.example.konteksto.ui.sessions.SessionsScreen
import com.example.konteksto.ui.theme.KontekstoTheme
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    // Shared with the rest of the app so leaving a session updates this dashboard immediately.
    sessionRepository: SessionRepository = remember { FirebaseSessionRepository() },
    hostSessions: List<HostSession> = emptyList(),
    isLoadingHostSessions: Boolean = false,
    currentProfile: UserProfile? = null,
    isHostRefreshing: Boolean = false,
    onHostRefresh: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onNavigateToCreateSession: () -> Unit = {},
    onNavigateToPresentQr: (HostSession) -> Unit = {},
    onNavigateToHostStudio: (HostSession) -> Unit = {},
    onNavigateToAttendeeRoom: (sessionId: String) -> Unit = {},
    isDarkMode: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val snackbarScope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { message ->
        snackbarScope.launch { snackbarHostState.showKontekstoSnackbar(message) }
    }
    val navigateToProfileTab: () -> Unit = {
        navController.navigate(MainTab.Profile.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            KontekstoBottomBar(navController = navController)
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainTab.Sessions.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable(MainTab.Sessions.route) {
                SessionsScreen(
                    onNavigateToAttendeeRoom = onNavigateToAttendeeRoom,
                    sessionRepository = sessionRepository,
                    onProfileClick = navigateToProfileTab,
                )
            }
            composable(MainTab.HostSessions.route) {
                HostSessionsScreen(
                    sessions = hostSessions,
                    isLoading = isLoadingHostSessions,
                    currentProfile = currentProfile,
                    isRefreshing = isHostRefreshing,
                    onRefresh = onHostRefresh,
                    onNewSessionClick = onNavigateToCreateSession,
                    onPresentQrClick = onNavigateToPresentQr,
                    onLiveRoomClick = onNavigateToHostStudio,
                    onProfileClick = navigateToProfileTab,
                    onShowMessage = showMessage,
                )
            }
            composable(MainTab.Glossary.route) {
                GlossaryScreen(onShowMessage = showMessage)
            }
            composable(MainTab.Profile.route) {
                ProfileScreen(
                    isDarkMode = isDarkMode,
                    onDarkModeChange = onDarkModeChange,
                    onSignOutClick = onSignOut,
                    onShowMessage = showMessage,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenLightPreview() {
    KontekstoTheme(darkTheme = false) {
        MainScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenDarkPreview() {
    KontekstoTheme(darkTheme = true) {
        MainScreen()
    }
}
