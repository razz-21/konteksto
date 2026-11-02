package com.example.konteksto.ui.main

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                ProfileScreen()
            }
        }
    }
}

@Composable
private fun KontekstoBottomBar(
    navController: NavHostController,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 3.dp,
    ) {
        MainTab.tabs.forEach { tab ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == tab.route } == true

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentDestination?.route != tab.route) {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    if ((tab.badgeCount != null) && (tab.badgeCount > 0)) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ) {
                                    Text(
                                        text = tab.badgeCount.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(id = tab.iconRes),
                                contentDescription = tab.title,
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(id = tab.iconRes),
                            contentDescription = tab.title,
                        )
                    }
                },
                label = {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        ),
                    )
                },
            )
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
