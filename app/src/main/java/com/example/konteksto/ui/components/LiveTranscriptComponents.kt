package com.example.konteksto.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.R
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

/** Tracks whether a live transcript should stay pinned to its newest line. */
@Stable
class LiveTranscriptFollowState {
    var isFollowing by mutableStateOf(true)
        internal set
    var showJumpToLive by mutableStateOf(false)
        internal set
}

private suspend fun LazyListState.scrollToNewest(itemCount: Int) {
    if (itemCount == 0) return
    animateScrollToItem(itemCount - 1)
    // A line taller than the panel stops at its top edge, which would hide the words being added to it.
    if (canScrollForward) scrollBy(layoutInfo.viewportSize.height.toFloat())
}

/**
 * Keeps the list on its newest line when a new line is added, until the viewer scrolls up, then
 * waits for them to return to the bottom. It does not follow changes inside the line the speaker is
 * still saying, so the list stays still while new words are generated. [isPaused] also holds it in
 * place, such as while the viewer is selecting lines.
 */
@Composable
fun rememberLiveTranscriptFollowState(
    listState: LazyListState,
    itemCount: Int,
    isPaused: Boolean = false,
): LiveTranscriptFollowState {
    val followState = remember { LiveTranscriptFollowState() }

    LaunchedEffect(listState) {
        listState.interactionSource.interactions
            .filterIsInstance<DragInteraction.Start>()
            .collect { followState.isFollowing = false }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { isScrolling ->
            if (!isScrolling && !listState.canScrollForward) followState.isFollowing = true
        }
    }
    LaunchedEffect(listState, isPaused) {
        snapshotFlow { (!followState.isFollowing || isPaused) && listState.canScrollForward }
            .collect { followState.showJumpToLive = it }
    }

    val shouldFollow = followState.isFollowing && !isPaused
    LaunchedEffect(shouldFollow, itemCount) {
        if (shouldFollow) listState.scrollToNewest(itemCount)
    }

    return followState
}

@Composable
fun JumpToLiveButton(
    listState: LazyListState,
    followState: LiveTranscriptFollowState,
    itemCount: Int,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    Surface(
        onClick = {
            followState.isFollowing = true
            coroutineScope.launch { listState.scrollToNewest(itemCount) }
        },
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceBright,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_forward),
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = 90f },
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(id = R.string.transcript_jump_to_live),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** The sentence the speaker is still saying, which the recognizer can revise until it's finished. */
@Composable
fun LiveSpeakingBubble(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                color = MaterialTheme.colorScheme.error,
                                shape = CircleShape,
                            ),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE SPEAKING…",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                        ),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Text(
                    text = "Streaming real-time",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontStyle = FontStyle.Italic,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
