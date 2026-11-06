package com.example.konteksto.ui.theme

import androidx.compose.ui.graphics.Color

// Light
val PrimaryLight = Color(0xFF2A5BD7)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFDBE1FF)
val OnPrimaryContainerLight = Color(0xFF00174B)
val SecondaryLight = Color(0xFF575E71)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFDCE2F9)
val OnSecondaryContainerLight = Color(0xFF151B2C)
val BackgroundLight = Color(0xFFF3F3FA)          // page behind cards and lists
val OnBackgroundLight = Color(0xFF191C20)
val SurfaceLight = Color(0xFFF9F9FF)             // full-screen forms and dialogs
val OnSurfaceLight = Color(0xFF191C20)
val SurfaceVariantLight = Color(0xFFE1E2EC)
val OnSurfaceVariantLight = Color(0xFF44474F)
val OutlineLight = Color(0xFF74777F)
val OutlineVariantLight = Color(0xFFC4C6D0)
val SurfaceDimLight = Color(0xFFD9D9E0)
val SurfaceBrightLight = Color(0xFFF9F9FF)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF3F3FA)
val SurfaceContainerLight = Color(0xFFEDEDF4)
val SurfaceContainerHighLight = Color(0xFFE7E8EE)
val SurfaceContainerHighestLight = Color(0xFFE2E2E9)
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

// Dark
val PrimaryDark = Color(0xFFB3C5FF)
val OnPrimaryDark = Color(0xFF002B75)
val PrimaryContainerDark = Color(0xFF1F44A8)
val OnPrimaryContainerDark = Color(0xFFDBE1FF)
val SecondaryDark = Color(0xFFBFC6DC)
val OnSecondaryDark = Color(0xFF293041)
val SecondaryContainerDark = Color(0xFF3E4759)
val OnSecondaryContainerDark = Color(0xFFDAE2F9)
val BackgroundDark = Color(0xFF0D0F13)          // darker than the Material default so cards stand out
val OnBackgroundDark = Color(0xFFE2E2E9)
val SurfaceDark = Color(0xFF0D0F13)
val OnSurfaceDark = Color(0xFFE2E2E9)
val SurfaceVariantDark = Color(0xFF44474F)
val OnSurfaceVariantDark = Color(0xFFC4C6D0)
val OutlineDark = Color(0xFF8E9099)
val OutlineVariantDark = Color(0xFF44474F)
val SurfaceDimDark = Color(0xFF0D0F13)
val SurfaceBrightDark = Color(0xFF37393E)
val SurfaceContainerLowestDark = Color(0xFF090B0E)
val SurfaceContainerLowDark = Color(0xFF191C20)
val SurfaceContainerDark = Color(0xFF1D2024)
val SurfaceContainerHighDark = Color(0xFF282A2F)
val SurfaceContainerHighestDark = Color(0xFF33353A)
val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// Cards and sections in dark mode: between surfaceContainer and surfaceContainerHigh, so the page,
// the card, and the tiles inside it (surfaceContainerHigh) are three distinct steps.
val CardContainerDark = Color(0xFF202328)

// Success (grounding badges, live status dots). Material 3 has no success role, so these are extended colors.
val SuccessLight = Color(0xFF00875A)
val SuccessContainerLight = Color(0xFFE8F8F0)
val SuccessDark = Color(0xFF6DDBA8)
val SuccessContainerDark = Color(0xFF003D26)

// Avatar colors (Edit profile)
val AvatarBlue = Color(0xFF2A5BD7)
val AvatarPurple = Color(0xFF7148C2)
val AvatarTeal = Color(0xFF00796B)
val AvatarOrange = Color(0xFFC2410C)
val AvatarPink = Color(0xFFB4235F)

// Compatibility aliases
val KontekstoBlue = PrimaryLight
val KontekstoLightBlue = PrimaryDark
val KontekstoDarkNavy = OnBackgroundLight