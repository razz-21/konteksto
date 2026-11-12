package com.example.konteksto.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.konteksto.data.repository.AuthRepository
import com.example.konteksto.data.repository.FirebaseAuthRepository
import com.example.konteksto.data.repository.FirebaseSessionRepository
import com.example.konteksto.data.repository.FirebaseUserProfileRepository
import com.example.konteksto.data.repository.SessionRepository
import com.example.konteksto.data.repository.UserProfileRepository
import com.example.konteksto.domain.model.AuthUser
import com.example.konteksto.ui.components.showKontekstoSnackbar
import com.example.konteksto.ui.host.CreateSessionScreen
import com.example.konteksto.ui.host.HostViewModel
import com.example.konteksto.ui.host.PresentQrRoute
import com.example.konteksto.ui.host.PresentQrViewModel
import com.example.konteksto.ui.host.studio.AddContextNoteRoute
import com.example.konteksto.ui.host.studio.EditContextNoteRoute
import com.example.konteksto.ui.host.studio.EditDomainTermRoute
import com.example.konteksto.ui.host.studio.EditSlideRoute
import com.example.konteksto.ui.host.studio.HostStudioRoute
import com.example.konteksto.ui.host.studio.HostStudioViewModel
import com.example.konteksto.ui.login.LoginRoute
import com.example.konteksto.ui.main.MainScreen
import com.example.konteksto.ui.sessions.studio.SessionStudioRoute
import com.example.konteksto.ui.sessions.studio.SessionStudioViewModel
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
    val mainSnackbarHostState = remember { SnackbarHostState() }
    val navScope = rememberCoroutineScope()
    val sessionRepository: SessionRepository = remember(context) {
        FirebaseSessionRepository(context = context.applicationContext)
    }
    val userProfileRepository: UserProfileRepository = remember(context) {
        FirebaseUserProfileRepository(context.applicationContext)
    }
    val hostViewModel: HostViewModel = viewModel(
        factory = HostViewModel.provideFactory(
            sessionRepository = sessionRepository,
            authRepository = authRepository,
            userProfileRepository = userProfileRepository,
        ),
    )
    val hostSessions by hostViewModel.hostSessions.collectAsStateWithLifecycle()
    val currentProfile by hostViewModel.currentProfile.collectAsStateWithLifecycle()
    val isHostRefreshing by hostViewModel.isRefreshing.collectAsStateWithLifecycle()
    val isLoadingHostSessions by hostViewModel.isLoadingSessions.collectAsStateWithLifecycle()

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

    // Wait for Firebase Auth to finish restoring any persisted session from disk
    // so the Login screen never flashes or overrides a logged-in user on app restart.
    if (startDestination == null && sessionState is SessionBootstrapState.Loading) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }
        return
    }

    val initialRoute = remember(startDestination) {
        startDestination ?: if (sessionState is SessionBootstrapState.Authenticated) {
            Screen.Main.route
        } else {
            Screen.Login.route
        }
    }

    LaunchedEffect(sessionState) {
        val currentRoute = navController.currentDestination?.route ?: return@LaunchedEffect
        when (sessionState) {
            is SessionBootstrapState.Authenticated -> {
                if (currentRoute == Screen.Login.route) {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }
            is SessionBootstrapState.Unauthenticated -> {
                if (currentRoute == Screen.Main.route) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) {
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
                    if (navController.currentDestination?.route != Screen.Main.route) {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Login.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                snackbarHostState = mainSnackbarHostState,
                hostSessions = hostSessions,
                isLoadingHostSessions = isLoadingHostSessions,
                currentProfile = currentProfile,
                isHostRefreshing = isHostRefreshing,
                onHostRefresh = { hostViewModel.refreshSessions() },
                isDarkMode = isDarkMode,
                onDarkModeChange = onDarkModeChange,
                onSignOut = {
                    if (navController.currentDestination?.route != Screen.Login.route) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Main.route) {
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
                onNavigateToAttendeeRoom = { sessionId ->
                    navController.navigate(Screen.SessionStudio.createRoute(sessionId))
                },
            )
        }

        composable(
            route = Screen.PresentQr.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val presentQrViewModel: PresentQrViewModel = viewModel(
                key = sessionId,
                factory = PresentQrViewModel.provideFactory(
                    sessionId = sessionId,
                    sessionRepository = sessionRepository,
                ),
            )

            PresentQrRoute(
                viewModel = presentQrViewModel,
                onBackClick = {
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = Screen.SessionStudio.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
            val transcriptViewModel: SessionStudioViewModel = viewModel(
                key = sessionId,
                factory = SessionStudioViewModel.provideFactory(
                    sessionId = sessionId,
                    sessionRepository = sessionRepository,
                    authRepository = authRepository,
                    userProfileRepository = userProfileRepository,
                ),
            )

            val dbDomainTerms by remember(sessionId) {
                sessionRepository.getDomainTermsStream(sessionId)
            }.collectAsStateWithLifecycle(initialValue = emptyList())

            SessionStudioRoute(
                viewModel = transcriptViewModel,
                domainTerms = dbDomainTerms,
                onBackClick = {
                    navController.popBackStack()
                },
                onLeaveConfirm = {
                    val currentUid = authRepository.currentUser?.uid ?: "guest_user"
                    coroutineScope.launch {
                        sessionRepository.leaveSession(currentUid, sessionId)
                    }
                    navController.popBackStack()
                },
                onPresentQrClick = { targetSession ->
                    navController.navigate(Screen.PresentQr.createRoute(targetSession.id))
                },
            )
        }

        composable(
            route = Screen.AttendeeRoom.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
            val transcriptViewModel: SessionStudioViewModel = viewModel(
                key = sessionId,
                factory = SessionStudioViewModel.provideFactory(
                    sessionId = sessionId,
                    sessionRepository = sessionRepository,
                    authRepository = authRepository,
                    userProfileRepository = userProfileRepository,
                ),
            )

            val dbDomainTerms by remember(sessionId) {
                sessionRepository.getDomainTermsStream(sessionId)
            }.collectAsStateWithLifecycle(initialValue = emptyList())

            SessionStudioRoute(
                viewModel = transcriptViewModel,
                domainTerms = dbDomainTerms,
                onBackClick = {
                    navController.popBackStack()
                },
                onLeaveConfirm = {
                    val currentUid = authRepository.currentUser?.uid ?: "guest_user"
                    coroutineScope.launch {
                        sessionRepository.leaveSession(currentUid, sessionId)
                    }
                    navController.popBackStack()
                },
                onPresentQrClick = { targetSession ->
                    navController.navigate(Screen.PresentQr.createRoute(targetSession.id))
                },
            )
        }

        composable(
            route = Screen.HostStudio.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val studioViewModel: HostStudioViewModel = viewModel(
                key = sessionId,
                factory = HostStudioViewModel.provideFactory(
                    context = context,
                    sessionId = sessionId,
                    sessionRepository = sessionRepository,
                    authRepository = authRepository,
                ),
            )
            HostStudioRoute(
                viewModel = studioViewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onPresentQrClick = { targetSession ->
                    navController.navigate(Screen.PresentQr.createRoute(targetSession.id))
                },
                onEditSlideClick = { slideId ->
                    navController.navigate(Screen.EditSlide.createRoute(sessionId, slideId)) {
                        launchSingleTop = true
                    }
                },
                onEditDomainTermClick = { termId ->
                    navController.navigate(Screen.EditDomainTerm.createRoute(sessionId, termId)) {
                        launchSingleTop = true
                    }
                },
                onAddContextNoteClick = {
                    navController.navigate(Screen.AddContextNote.createRoute(sessionId)) {
                        launchSingleTop = true
                    }
                },
                onEditContextNoteClick = { noteId ->
                    navController.navigate(Screen.EditContextNote.createRoute(sessionId, noteId)) {
                        launchSingleTop = true
                    }
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

            EditSlideRoute(
                viewModel = sharedStudioViewModel(
                    navController = navController,
                    backStackEntry = backStackEntry,
                    sessionId = sessionId,
                    sessionRepository = sessionRepository,
                    authRepository = authRepository,
                ),
                slideId = slideId,
                onBackClick = {
                    navController.popBackStack(Screen.EditSlide.route, inclusive = true)
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

            EditDomainTermRoute(
                viewModel = sharedStudioViewModel(
                    navController = navController,
                    backStackEntry = backStackEntry,
                    sessionId = sessionId,
                    sessionRepository = sessionRepository,
                    authRepository = authRepository,
                ),
                termId = termId,
                onBackClick = {
                    navController.popBackStack(Screen.EditDomainTerm.route, inclusive = true)
                },
            )
        }

        composable(
            route = Screen.AddContextNote.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""

            AddContextNoteRoute(
                viewModel = sharedStudioViewModel(
                    navController = navController,
                    backStackEntry = backStackEntry,
                    sessionId = sessionId,
                    sessionRepository = sessionRepository,
                    authRepository = authRepository,
                ),
                onBackClick = {
                    navController.popBackStack(Screen.AddContextNote.route, inclusive = true)
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

            EditContextNoteRoute(
                viewModel = sharedStudioViewModel(
                    navController = navController,
                    backStackEntry = backStackEntry,
                    sessionId = sessionId,
                    sessionRepository = sessionRepository,
                    authRepository = authRepository,
                ),
                noteId = noteId,
                onBackClick = {
                    navController.popBackStack(Screen.EditContextNote.route, inclusive = true)
                },
            )
        }

        composable(Screen.CreateSession.route) {
            val createSnackbarHostState = remember { SnackbarHostState() }
            val createScope = rememberCoroutineScope()
            val currentUser = (sessionState as? SessionBootstrapState.Authenticated)?.user
                ?: authRepository.currentUser
            // Use the name saved in Profile & Settings, then fall back to Firebase Auth.
            val currentSpeakerName = currentProfile?.name?.takeIf { it.isNotBlank() }
                ?: currentUser?.displayName?.takeIf { it.isNotBlank() }
                ?: currentUser?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                ?: ""

            CreateSessionScreen(
                initialSpeakerName = currentSpeakerName,
                snackbarHostState = createSnackbarHostState,
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
                            navController.popBackStack()
                            navScope.launch { mainSnackbarHostState.showKontekstoSnackbar("Session created successfully!") }
                        },
                        onError = { message ->
                            createScope.launch { createSnackbarHostState.showKontekstoSnackbar(message) }
                        },
                    )
                },
            )
        }
    }
}

/** Edit screens share the studio's ViewModel so saved changes appear in its lists straight away. */
@Composable
private fun sharedStudioViewModel(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sessionId: String,
    sessionRepository: SessionRepository,
    authRepository: AuthRepository,
): HostStudioViewModel {
    val studioEntry = remember(backStackEntry) {
        navController.getBackStackEntry(Screen.HostStudio.route)
    }
    return viewModel(
        viewModelStoreOwner = studioEntry,
        key = sessionId,
        factory = HostStudioViewModel.provideFactory(
            context = LocalContext.current,
            sessionId = sessionId,
            sessionRepository = sessionRepository,
            authRepository = authRepository,
        ),
    )
}
