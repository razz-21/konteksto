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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.example.konteksto.data.repository.SessionRepository
import com.example.konteksto.domain.model.AuthUser
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.host.CreateSessionScreen
import com.example.konteksto.ui.host.HostViewModel
import com.example.konteksto.ui.host.PresentQrScreen
import com.example.konteksto.ui.login.LoginRoute
import com.example.konteksto.ui.main.MainScreen

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
) {
    val context = LocalContext.current
    val sessionRepository: SessionRepository = remember(context) {
        FirebaseSessionRepository(context = context.applicationContext)
    }
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
                hostSessions = hostSessions,
                isHostRefreshing = isHostRefreshing,
                onHostRefresh = { hostViewModel.refreshSessions() },
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
