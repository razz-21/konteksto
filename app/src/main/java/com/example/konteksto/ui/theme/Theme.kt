package com.example.konteksto.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    inversePrimary = PrimaryLight,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = SecondaryDark,
    onTertiary = OnSecondaryDark,
    tertiaryContainer = SecondaryContainerDark,
    onTertiaryContainer = OnSecondaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = BackgroundDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceTint = PrimaryDark,
    inverseSurface = OnBackgroundDark,
    inverseOnSurface = BackgroundDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceBright = SurfaceContainerHighestDark,
    surfaceDim = SurfaceContainerLowestDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    inversePrimary = PrimaryDark,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = SecondaryLight,
    onTertiary = OnSecondaryLight,
    tertiaryContainer = SecondaryContainerLight,
    onTertiaryContainer = OnSecondaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = BackgroundLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceTint = PrimaryLight,
    inverseSurface = OnBackgroundLight,
    inverseOnSurface = BackgroundLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceBright = SurfaceContainerLowestLight,
    surfaceDim = SurfaceContainerHighestLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

/** Theme colors Material 3 has no role for. Read them with `MaterialTheme.extendedColors`. */
@Immutable
data class KontekstoExtendedColors(
    /** Cards and sections: white on the light page, a step lighter than the dark page so they stand out. */
    val cardContainer: Color,
    val success: Color,
    val successContainer: Color,
)

private val LightExtendedColors = KontekstoExtendedColors(
    cardContainer = SurfaceContainerLowestLight,
    success = SuccessLight,
    successContainer = SuccessContainerLight,
)

private val DarkExtendedColors = KontekstoExtendedColors(
    cardContainer = CardContainerDark,
    success = SuccessDark,
    successContainer = SuccessContainerDark,
)

private val LocalKontekstoExtendedColors = staticCompositionLocalOf { LightExtendedColors }

val MaterialTheme.extendedColors: KontekstoExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalKontekstoExtendedColors.current

@Composable
fun KontekstoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disabled by default so Material components strictly align with Konteksto theme colors
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalKontekstoExtendedColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}