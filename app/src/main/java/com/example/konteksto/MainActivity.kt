package com.example.konteksto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.konteksto.data.preferences.ThemePreferences
import com.example.konteksto.ui.navigation.KontekstoNavHost
import com.example.konteksto.ui.theme.KontekstoTheme
import com.example.konteksto.ui.theme.ThemeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current

            val themePreferences = remember(context) {
                ThemePreferences(context.applicationContext)
            }

            val themeViewModel: ThemeViewModel = viewModel(
                factory = ThemeViewModel.provideFactory(themePreferences),
            )

            val isDarkMode by themeViewModel.isDarkMode.collectAsStateWithLifecycle()

            LaunchedEffect(isDarkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { isDarkMode },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { isDarkMode },
                )
            }

            KontekstoTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ) {
                    KontekstoNavHost(
                        isDarkMode = isDarkMode,
                        onDarkModeChange = { enabled -> themeViewModel.setDarkMode(enabled) },
                    )
                }
            }
        }
    }
}
