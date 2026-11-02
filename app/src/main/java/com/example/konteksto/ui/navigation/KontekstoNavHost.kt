package com.example.konteksto.ui.navigation

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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.konteksto.data.repository.AuthRepository
import com.example.konteksto.data.repository.FirebaseAuthRepository
import com.example.konteksto.domain.model.AuthUser
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
            )
        }
    }
}
