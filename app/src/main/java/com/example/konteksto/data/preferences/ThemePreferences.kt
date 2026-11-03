package com.example.konteksto.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
private val Context.themeDataStore by preferencesDataStore(name = "theme_settings")

class ThemePreferences(private val context: Context) {
    private val darkModeKey = booleanPreferencesKey("dark_mode")

    // Reads the saved value. If nothing is saved yet, it returns false (Light).
    val isDarkMode: Flow<Boolean> = context.themeDataStore.data.map { preferences ->
        preferences[darkModeKey] ?: false // added
    }
    // Saves the user's selected theme.
    suspend fun setDarkMode(enabled: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[darkModeKey] = enabled
        }
    }
}