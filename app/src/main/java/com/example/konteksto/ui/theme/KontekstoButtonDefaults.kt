package com.example.konteksto.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Shared shape and size for every Material 3 button in the app. Buttons are fully rounded (pill),
 * while other components keep the corners defined in [Shapes].
 */
object KontekstoButtonDefaults {
    val Height = 48.dp

    val shape: Shape = CircleShape
}
