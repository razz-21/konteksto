package com.example.konteksto.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.semantics.Role

/**
 * Standard debounce window duration in milliseconds to prevent double-tapping.
 * Matches the 400ms transition duration for interactive button actions across the app.
 */
const val DEFAULT_DEBOUNCE_INTERVAL_MS = 400L

/**
 * 400ms animation spec for button interaction and loading-state transitions.
 */
fun <T> buttonTransitionSpec() = tween<T>(durationMillis = 400)

private fun getMonotonicTimeMs(): Long {
    return try {
        val uptime = SystemClock.uptimeMillis()
        if (uptime != 0L) uptime else System.currentTimeMillis()
    } catch (_: Throwable) {
        System.currentTimeMillis()
    }
}

/**
 * Remembers a debounced version of [onClick] that ignores rapid subsequent taps
 * occurring within [debounceIntervalMs] (default 400ms).
 */
@Composable
fun rememberDebouncedClick(
    debounceIntervalMs: Long = DEFAULT_DEBOUNCE_INTERVAL_MS,
    onClick: (() -> Unit)?,
): () -> Unit {
    if (onClick == null) return {}
    val latestOnClick by rememberUpdatedState(onClick)
    var lastClickTime by remember { mutableLongStateOf(0L) }
    return remember(debounceIntervalMs) {
        {
            val currentTime = getMonotonicTimeMs()
            if (currentTime - lastClickTime >= debounceIntervalMs) {
                lastClickTime = currentTime
                latestOnClick()
            }
        }
    }
}

/**
 * Extension for [Modifier.clickable] that debounces clicks to prevent rapid duplicate taps.
 */
fun Modifier.debouncedClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    debounceIntervalMs: Long = DEFAULT_DEBOUNCE_INTERVAL_MS,
    onClick: () -> Unit,
): Modifier = composed {
    val debouncedOnClick = rememberDebouncedClick(
        debounceIntervalMs = debounceIntervalMs,
        onClick = onClick,
    )
    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = debouncedOnClick,
    )
}
