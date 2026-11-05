package com.example.konteksto.ui.host.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.R
import com.example.konteksto.ui.components.JumpToLiveButton
import com.example.konteksto.ui.components.LiveSpeakingBubble
import com.example.konteksto.ui.components.rememberLiveTranscriptFollowState

@Composable
fun HostStudioBroadcastView(
    isAudioBroadcasting: Boolean,
    transcripts: List<TranscriptEntry>,
    onAddTranscript: (String) -> Unit,
    modifier: Modifier = Modifier,
    partialTranscript: String = "",
) {
    var newTranscriptText by remember { mutableStateOf("") }
    val transcriptListState = rememberLazyListState()
    val transcriptItemCount = transcripts.size + if (partialTranscript.isNotBlank()) 1 else 0
    val followState = rememberLiveTranscriptFollowState(
        listState = transcriptListState,
        itemCount = transcriptItemCount,
        liveText = partialTranscript,
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (isAudioBroadcasting) Color(0xFF00C853) else MaterialTheme.colorScheme.outline,
                            shape = CircleShape,
                        ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.host_studio_live_transcript),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Text(
                text = stringResource(id = R.string.host_studio_entries_format, transcripts.size),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Scrollable Live Transcript Container with max height
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            ),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(
                    state = transcriptListState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (transcripts.isEmpty() && partialTranscript.isBlank()) {
                        item(key = "empty") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp, horizontal = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "No transcript entries yet",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Start Audio Broadcast to record and display live transcripts.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    items(transcripts, key = { it.id }) { entry ->
                        if (entry.isSystemEvent || entry.speakerName.equals("SYSTEM", ignoreCase = true)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        val isPositiveEvent = entry.text.contains("started", ignoreCase = true) ||
                                                entry.text.contains("enabled", ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(
                                                    color = if (isPositiveEvent) Color(0xFF00C853) else MaterialTheme.colorScheme.error,
                                                    shape = CircleShape,
                                                ),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${entry.text}  ·  ${entry.timestamp}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp,
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                ),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = entry.speakerName,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                            ),
                                            color = MaterialTheme.colorScheme.primary,
                                        )

                                        Text(
                                            text = entry.timestamp,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = entry.text,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                }
                            }
                        }
                    }

                    // Live Spoken Stream Box (Un-carded while the user is talking)
                    if (partialTranscript.isNotBlank()) {
                        item(key = "live_partial") {
                            LiveSpeakingBubble(text = partialTranscript)
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
                            .padding(bottom = 8.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = newTranscriptText,
                onValueChange = { newTranscriptText = it },
                placeholder = {
                    Text(
                        text = "Type spoken summary or transcript...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    )
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
                singleLine = true,
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (newTranscriptText.isNotBlank()) {
                        onAddTranscript(newTranscriptText.trim())
                        newTranscriptText = ""
                    }
                },
                enabled = newTranscriptText.isNotBlank(),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.height(52.dp),
            ) {
                Text(
                    text = "Post",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}
