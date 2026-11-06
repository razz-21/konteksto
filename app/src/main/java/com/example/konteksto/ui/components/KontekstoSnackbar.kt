package com.example.konteksto.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.withTimeoutOrNull

/** How long action feedback stays on screen. Material 3 only offers ~4s, ~10s, or indefinite. */
const val KONTEKSTO_SNACKBAR_DURATION_MS = 6_000L

/**
 * Shows action feedback as a Material 3 snackbar for [KONTEKSTO_SNACKBAR_DURATION_MS].
 * The newest message replaces any snackbar already showing, so rapid actions never queue up.
 */
suspend fun SnackbarHostState.showKontekstoSnackbar(
    message: String,
    actionLabel: String? = null,
): SnackbarResult {
    currentSnackbarData?.dismiss()
    // Cancelling showSnackbar on timeout removes the snackbar from the host.
    return withTimeoutOrNull(KONTEKSTO_SNACKBAR_DURATION_MS) {
        showSnackbar(
            message = message,
            actionLabel = actionLabel,
            withDismissAction = actionLabel == null,
            duration = SnackbarDuration.Indefinite,
        )
    } ?: SnackbarResult.Dismissed
}
