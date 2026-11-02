package com.example.konteksto.ui.main

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.konteksto.ui.glossary.GlossaryScreen
import com.example.konteksto.ui.host.HostSessionsScreen
import com.example.konteksto.ui.navigation.MainTab
import com.example.konteksto.ui.profile.ProfileScreen
import com.example.konteksto.ui.sessions.SessionsScreen
import com.example.konteksto.ui.theme.KontekstoTheme

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    onSignOut: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            KontekstoBottomBar(navController = navController)
        },
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
                SessionsScreen()
            }
            composable(MainTab.HostSessions.route) {
                HostSessionsScreen()
            }
            composable(MainTab.Glossary.route) {
                GlossaryScreen()
            }
            composable(MainTab.Profile.route) {
                ProfileScreen(onSignOutClick = onSignOut)
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
