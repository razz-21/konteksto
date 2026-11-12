package com.example.konteksto.ui.sessions.studio

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.R
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.components.JumpToLiveButton
import com.example.konteksto.ui.components.LiveSpeakingBubble
import com.example.konteksto.ui.components.rememberLiveTranscriptFollowState
import com.example.konteksto.ui.theme.KontekstoButtonDefaults
import com.example.konteksto.ui.theme.extendedColors

data class CompanionTranscriptItem(
    val id: String,
    val speakerName: String,
    val timestamp: String,
    val text: String,
    val isSystemEvent: Boolean = false,
)

/** Space kept under the transcript card for the floating "What did they mean?" button. */
internal val LiveCompanionActionAreaHeight = 80.dp

@Composable
fun SessionLiveCompanionView(
    session: HostSession,
    modifier: Modifier = Modifier,
    isAudioBroadcasting: Boolean = false,
    customTranscripts: List<CompanionTranscriptItem> = emptyList(),
    livePartialText: String = "",
    hasEarlierTranscripts: Boolean = false,
    isLoadingEarlierTranscripts: Boolean = false,
    didEarlierTranscriptsFail: Boolean = false,
    didLiveTranscriptFail: Boolean = false,
    onLoadEarlierTranscripts: () -> Unit = {},
    onRetryLiveTranscript: () -> Unit = {},
    isExplaining: Boolean = false,
    explainError: String? = null,
    onRetryExplain: () -> Unit = {},
    onAskWhatTheyMean: (selectedItems: List<CompanionTranscriptItem>, clearSelection: () -> Unit) -> Unit = { _, _ -> },
) {

    val items = remember(customTranscripts, session.speakerName) {
        customTranscripts.map { item ->
            if (!item.isSystemEvent && session.speakerName.isNotBlank()) {
                item.copy(speakerName = session.speakerName.uppercase())
            } else item
        }
    }

    var selectedIds by remember {
        mutableStateOf<Set<String>>(emptySet())
    }

    val transcriptListState = rememberLazyListState()
    val transcriptItemCount = (if (hasEarlierTranscripts) 1 else 0) + items.size +
        (if (livePartialText.isNotBlank()) 1 else 0) +
        (if (isExplaining) 1 else 0) +
        (if (explainError != null) 1 else 0)
    val followState = rememberLiveTranscriptFollowState(
        listState = transcriptListState,
        itemCount = transcriptItemCount,
        isPaused = selectedIds.isNotEmpty(),
    )

    // Pulsing animation for audio wave icon when audio is live
    val infiniteTransition = rememberInfiniteTransition(label = "wave_pulsing_transition")
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wave_alpha",
    )

    Box(modifier = modifier.fillMaxSize()) {
        // PANEL: WHITE CARD FOR LIVE TRANSCRIPT. The header stays put and only the lines scroll,
        // so the jump-to-live button floats over the visible bottom of the list.
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = LiveCompanionActionAreaHeight),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.extendedColors.cardContainer,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Header Row: "Live transcript" + Audio status badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Live transcript",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )

                    // Audio status pill badge
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_audio_waveform),
                                contentDescription = null,
                                tint = if (isAudioBroadcasting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(14.dp)
                                    .graphicsLayer {
                                        alpha = if (isAudioBroadcasting) waveAlpha else 1.0f
                                    },
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAudioBroadcasting) "Audio live" else "Audio off",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Subtitle Row: Instruction
                Text(
                    text = "Tap lines to select them, then ask what they mean.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.5.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (didLiveTranscriptFail) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(id = R.string.transcript_live_failed),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(
                            onClick = onRetryLiveTranscript,
                            shape = KontekstoButtonDefaults.shape,
                        ) {
                            Text(text = stringResource(id = R.string.transcript_retry))
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    if (items.isEmpty() && livePartialText.isBlank() && !hasEarlierTranscripts) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (isAudioBroadcasting) {
                                    "Listening to speaker...\nTranscripts will appear here soon."
                                } else {
                                    "No transcripts available yet."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    } else {
                        LazyColumn(
                            state = transcriptListState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                        if (hasEarlierTranscripts) {
                            item(key = "load_earlier") {
                                TextButton(
                                    onClick = onLoadEarlierTranscripts,
                                    enabled = !isLoadingEarlierTranscripts,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = KontekstoButtonDefaults.shape,
                                ) {
                                    Text(
                                        text = stringResource(
                                            id = if (didEarlierTranscriptsFail) {
                                                R.string.transcript_load_earlier_failed
                                            } else {
                                                R.string.transcript_load_earlier
                                            },
                                        ),
                                    )
                                }
                            }
                        }

                        items(items, key = { it.id }) { item ->
                            if (item.isSystemEvent || item.speakerName.equals("SYSTEM", ignoreCase = true)) {
                                SystemEventPill(
                                    text = item.text,
                                    timestamp = item.timestamp,
                                )
                            } else {
                                val isSelected = selectedIds.contains(item.id)

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedIds = if (isSelected) {
                                                selectedIds - item.id
                                            } else {
                                                selectedIds + item.id
                                            }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        // No border: selection shows as a full tint plus the checked box.
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                    ) {
                                        // Top Row: Speaker Name, Timestamp, Square Checkbox
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f, fill = false),
                                            ) {
                                                Text(
                                                    text = item.speakerName.uppercase(),
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 13.5.sp,
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary,
                                                )

                                                Spacer(modifier = Modifier.width(8.dp))

                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                ) {
                                                    Text(
                                                        text = item.timestamp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 11.sp,
                                                        ),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                            }

                                            // Square Checkbox on Top Right
                                            SquareSelectCheckbox(
                                                checked = isSelected,
                                                onCheckedChange = { checked ->
                                                    selectedIds = if (checked) {
                                                        selectedIds + item.id
                                                    } else {
                                                        selectedIds - item.id
                                                    }
                                                },
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Transcript Statement Text
                                        Text(
                                            text = item.text,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 13.sp,
                                                lineHeight = 18.5.sp,
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                    }
                                }
                            }
                        }

                        if (livePartialText.isNotBlank()) {
                            item(key = "live_partial") {
                                LiveSpeakingBubble(text = livePartialText)
                            }
                        }

                        if (isExplaining) {
                            item(key = "explaining") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    ),
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("Analyzing selected statements...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        if (explainError != null) {
                            item(key = "explain_error") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(explainError, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                                        TextButton(
                                            onClick = onRetryExplain,
                                            shape = KontekstoButtonDefaults.shape,
                                        ) {
                                            Text("Retry", color = MaterialTheme.colorScheme.onErrorContainer)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (followState.showJumpToLive) {
                        JumpToLiveButton(
                            listState = transcriptListState,
                            followState = followState,
                            itemCount = transcriptItemCount,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp),
                        )
                    }
                    }
                }
            }
        }

        // Floating Sticky Action Button at Bottom: "What did they mean? (1)"
        val selectedItems = remember(selectedIds, items) {
            items.filter { it.id in selectedIds }.sortedBy { items.indexOf(it) }
        }

        // Always visible so attendees can find it; enabled once at least one line is selected.
        val hasSelection = selectedItems.isNotEmpty()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
        ) {
            Button(
                onClick = {
                    onAskWhatTheyMean(selectedItems) {
                        selectedIds = emptySet()
                    }
                },
                enabled = hasSelection,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(KontekstoButtonDefaults.Height),
                shape = KontekstoButtonDefaults.shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, disabledElevation = 0.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    // Icon and text follow the button's content color, so they dim when disabled.
                    Icon(
                        painter = painterResource(id = R.drawable.ic_sparkle),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "What did they mean?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                    )

                    if (hasSelection) {
                        Spacer(modifier = Modifier.width(10.dp))

                        // White Circle Badge with selected count
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onPrimary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = selectedItems.size.toString(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemEventPill(
    text: String,
    timestamp: String,
    modifier: Modifier = Modifier,
) {
    val isOffOrEnded = text.contains("off", ignoreCase = true) ||
        text.contains("ended", ignoreCase = true) ||
        text.contains("stopped", ignoreCase = true)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Colored Status Indicator Dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (isOffOrEnded) MaterialTheme.colorScheme.error else MaterialTheme.extendedColors.success,
                            shape = CircleShape,
                        ),
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Event Text
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (timestamp.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))

                    // Inner Timestamp Pill Badge
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Text(
                            text = timestamp,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SquareSelectCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (checked) MaterialTheme.colorScheme.primary else Color.Transparent)
            .border(
                width = 1.5.dp,
                color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(4.dp),
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check),
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

