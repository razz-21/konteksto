package com.example.konteksto.ui.navigation

import android.widget.Toast
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.konteksto.data.preferences.OnboardingPreferences
import com.example.konteksto.data.repository.AuthRepository
import com.example.konteksto.data.repository.FirebaseAuthRepository
import com.example.konteksto.data.repository.FirebaseSessionRepository
import com.example.konteksto.data.repository.SessionRepository
import com.example.konteksto.domain.model.AuthUser
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.host.CreateSessionScreen
import com.example.konteksto.ui.host.HostViewModel
import com.example.konteksto.ui.host.PresentQrScreen
import com.example.konteksto.ui.host.studio.AddContextNoteRoute
import com.example.konteksto.ui.host.studio.EditContextNoteRoute
import com.example.konteksto.ui.host.studio.EditDomainTermRoute
import com.example.konteksto.ui.host.studio.EditSlideRoute
import com.example.konteksto.ui.host.studio.HostStudioRoute
import com.example.konteksto.ui.host.studio.HostStudioViewModel
import com.example.konteksto.ui.login.LoginRoute
import com.example.konteksto.ui.main.MainScreen
import com.example.konteksto.ui.onboarding.OnboardingScreen
import kotlinx.coroutines.launch

private sealed interface SessionBootstrapState {
    data object Loading : SessionBootstrapState
    data class Authenticated(val user: AuthUser) : SessionBootstrapState
    data object Unauthenticated : SessionBootstrapState
}

@Composable
fun KontekstoNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    authRepository: AuthRepository = remember { FirebaseAuthRepository() },
    startDestination: String? = null,
    isDarkMode: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sessionRepository: SessionRepository = remember(context) {
        FirebaseSessionRepository(context = context.applicationContext)
    }
    val onboardingPreferences = remember(context) {
        OnboardingPreferences(context.applicationContext)
    }
    val isOnboardingCompleted by onboardingPreferences.isOnboardingCompleted
        .collectAsStateWithLifecycle(initialValue = null)

    val hostViewModel: HostViewModel = viewModel(
        factory = HostViewModel.provideFactory(
            sessionRepository = sessionRepository,
            authRepository = authRepository,
        ),
    )
    val hostSessions by hostViewModel.hostSessions.collectAsStateWithLifecycle()
    val isHostRefreshing by hostViewModel.isRefreshing.collectAsStateWithLifecycle()

    val sessionState by produceState<SessionBootstrapState>(
        initialValue = authRepository.currentUser?.let {
            SessionBootstrapState.Authenticated(it)
        } ?: SessionBootstrapState.Loading,
        key1 = authRepository,
    ) {
        authRepository.getAuthStateStream().collect { user ->
            value = if (user != null) {
                SessionBootstrapState.Authenticated(user)
            } else {
                SessionBootstrapState.Unauthenticated
            }
        }
    }

    // Wait for Firebase Auth and DataStore onboarding preferences to finish restoring
    // so screen selection never flashes or overrides state on app restart.
    if (startDestination == null && (sessionState is SessionBootstrapState.Loading || isOnboardingCompleted == null)) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }
        return
    }

    val initialRoute = remember(startDestination, sessionState, isOnboardingCompleted) {
        startDestination ?: when (sessionState) {
            is SessionBootstrapState.Authenticated -> {
                if (isOnboardingCompleted == true) Screen.Main.route else Screen.Onboarding.route
            }
            else -> Screen.Login.route
        }
    }

    LaunchedEffect(sessionState, isOnboardingCompleted) {
        val currentRoute = navController.currentDestination?.route ?: return@LaunchedEffect
        val onboardingDone = isOnboardingCompleted ?: false
        when (sessionState) {
            is SessionBootstrapState.Authenticated -> {
                if (currentRoute == Screen.Login.route) {
                    val targetRoute = if (onboardingDone) Screen.Main.route else Screen.Onboarding.route
                    navController.navigate(targetRoute) {
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }
            is SessionBootstrapState.Unauthenticated -> {
                if (currentRoute != Screen.Login.route) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }
            is SessionBootstrapState.Loading -> Unit
        }
    }

    NavHost(
        navController = navController,
        startDestination = initialRoute,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(400),
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(400),
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(400),
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(400),
            )
        },
    ) {
        composable(Screen.Login.route) {
            LoginRoute(
                onSignInSuccess = {
                    val onboardingDone = isOnboardingCompleted ?: false
                    val targetRoute = if (onboardingDone) Screen.Main.route else Screen.Onboarding.route
                    if (navController.currentDestination?.route != targetRoute) {
                        navController.navigate(targetRoute) {
                            popUpTo(Screen.Login.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    coroutineScope.launch {
                        onboardingPreferences.setOnboardingCompleted(true)
                    }
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                hostSessions = hostSessions,
                isHostRefreshing = isHostRefreshing,
                onHostRefresh = { hostViewModel.refreshSessions() },
                isDarkMode = isDarkMode,
                onDarkModeChange = onDarkModeChange,
                onSignOut = {
                    if (navController.currentDestination?.route != Screen.Login.route) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                },
                onNavigateToCreateSession = {
                    navController.navigate(Screen.CreateSession.route)
                },
                onNavigateToPresentQr = { session ->
                    navController.navigate(Screen.PresentQr.createRoute(session.id))
                },
                onNavigateToHostStudio = { session ->
                    navController.navigate(Screen.HostStudio.createRoute(session.id))
                },
            )
        }

        composable(
            route = Screen.HostStudio.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val studioViewModel: HostStudioViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HostStudioViewModel.provideFactory(
                    sessionId = sessionId,
                    sessionRepository = sessionRepository,
                    authRepository = authRepository,
                ),
            )
            val session = hostSessions.find { it.id == sessionId } ?: HostSession(
                id = sessionId,
                venue = "Tech Summit Hall B",
                time = "10:00 AM",
                talkCode = "TALK–415",
                title = "Edge AI & Distributed Cloud Architecture: Scaling to Zero Latency",
                speakerName = "Engr. Marco Valerio",
                attendeesCount = 890,
                isLive = true,
            )

            HostStudioRoute(
                viewModel = studioViewModel,
                initialSession = session,
                onBackClick = {
                    navController.popBackStack()
                },
                onPresentQrClick = { hostSession ->
                    navController.navigate(Screen.PresentQr.createRoute(hostSession.id))
                },
                onEditSlideClick = { slideId ->
                    navController.navigate(Screen.EditSlide.createRoute(sessionId, slideId))
                },
                onEditDomainTermClick = { termId ->
                    navController.navigate(Screen.EditDomainTerm.createRoute(sessionId, termId))
                },
                onAddContextNoteClick = {
                    navController.navigate(Screen.AddContextNote.createRoute(sessionId))
                },
                onEditContextNoteClick = { noteId ->
                    navController.navigate(Screen.EditContextNote.createRoute(sessionId, noteId))
                },
            )
        }

        composable(
            route = Screen.EditSlide.route,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("slideId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val slideId = backStackEntry.arguments?.getString("slideId") ?: ""
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.HostStudio.createRoute(sessionId))
            }
            val studioViewModel: HostStudioViewModel = viewModel(
                viewModelStoreOwner = parentEntry,
                factory = HostStudioViewModel.provideFactory(
                    sessionId = sessionId,
                    sessionRepository = sessionRepository,
                    authRepository = authRepository,
                ),
            )

            EditSlideRoute(
                viewModel = studioViewModel,
                slideId = slideId,
                onBackClick = {
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = Screen.EditDomainTerm.route,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("termId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val termId = backStackEntry.arguments?.getString("termId") ?: ""
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.HostStudio.createRoute(sessionId))
            }
            val studioViewModel: HostStudioViewModel = viewModel(
                viewModelStoreOwner = parentEntry,
                factory = HostStudioViewModel.provideFactory(
                    sessionId = sessionId,
                    sessionRepository = sessionRepository,
                    authRepository = authRepository,
                ),
            )

            EditDomainTermRoute(
                viewModel = studioViewModel,
                termId = termId,
                onBackClick = {
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = Screen.AddContextNote.route,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.HostStudio.createRoute(sessionId))
            }
            val studioViewModel: HostStudioViewModel = viewModel(
                viewModelStoreOwner = parentEntry,
                factory = HostStudioViewModel.provideFactory(
                    sessionId = sessionId,
                    sessionRepository = sessionRepository,
                    authRepository = authRepository,
                ),
            )

            AddContextNoteRoute(
                viewModel = studioViewModel,
                onBackClick = {
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = Screen.EditContextNote.route,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("noteId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.HostStudio.createRoute(sessionId))
            }
            val studioViewModel: HostStudioViewModel = viewModel(
                viewModelStoreOwner = parentEntry,
                factory = HostStudioViewModel.provideFactory(
                    sessionId = sessionId,
                    sessionRepository = sessionRepository,
                    authRepository = authRepository,
                ),
            )

            EditContextNoteRoute(
                viewModel = studioViewModel,
                noteId = noteId,
                onBackClick = {
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = Screen.PresentQr.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val session = hostSessions.find { it.id == sessionId } ?: HostSession(
                id = sessionId,
                venue = "Tech Summit Hall B",
                time = "10:00 AM",
                talkCode = "TALK–415",
                title = "Edge AI & Distributed Cloud Architecture: Scaling to Zero Latency",
                speakerName = "Engr. Marco Valerio",
                attendeesCount = 890,
                isLive = true,
            )

            PresentQrScreen(
                session = session,
                onBackClick = {
                    navController.popBackStack()
                },
            )
        }

        composable(Screen.CreateSession.route) {
            val context = LocalContext.current
            val currentUser = (sessionState as? SessionBootstrapState.Authenticated)?.user
                ?: authRepository.currentUser
            val currentSpeakerName = currentUser?.displayName?.takeIf { it.isNotBlank() }
                ?: currentUser?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                ?: ""

            CreateSessionScreen(
                initialSpeakerName = currentSpeakerName,
                onBackClick = {
                    navController.popBackStack()
                },
                onCreateSessionSubmit = { title, talkCode, speaker, category, room, startTime, endTime, banner ->
                    hostViewModel.createSession(
                        title = title,
                        speakerName = speaker,
                        category = category,
                        venue = room,
                        startTime = startTime,
                        endTime = endTime,
                        bannerImageUrl = banner.imageUrl,
                        talkCode = talkCode,
                        onSuccess = {
                            Toast.makeText(context, "Session created successfully!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        onError = { message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        },
                    )
                },
            )
        }
    }
}
