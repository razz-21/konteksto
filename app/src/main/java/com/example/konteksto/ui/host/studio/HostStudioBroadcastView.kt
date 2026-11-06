package com.example.konteksto.ui.host.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.R
import com.example.konteksto.ui.components.JumpToLiveButton
import com.example.konteksto.ui.components.LiveSpeakingBubble
import com.example.konteksto.ui.components.rememberLiveTranscriptFollowState
import com.example.konteksto.ui.sessions.studio.SquareSelectCheckbox
import com.example.konteksto.ui.sessions.studio.SystemEventPill
import com.example.konteksto.ui.theme.extendedColors

@Composable
fun HostStudioBroadcastView(
    isAudioBroadcasting: Boolean,
    transcripts: List<TranscriptEntry>,
    onAddTranscript: (String) -> Unit,
    modifier: Modifier = Modifier,
    partialTranscript: String = "",
) {
    var newTranscriptText by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val transcriptListState = rememberLazyListState()
    val transcriptItemCount = transcripts.size + if (partialTranscript.isNotBlank()) 1 else 0
    val followState = rememberLiveTranscriptFollowState(
        listState = transcriptListState,
        itemCount = transcriptItemCount,
        liveText = partialTranscript,
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Outer Container Card with Border for the Whole Transcript Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.extendedColors.cardContainer,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Header Row: Title & Entry Count Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isAudioBroadcasting) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(MaterialTheme.extendedColors.success, CircleShape),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = "Live transcript",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text = "${transcripts.size} entries",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.5.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    LazyColumn(
                        state = transcriptListState,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (transcripts.isEmpty() && partialTranscript.isBlank()) {
                            item(key = "empty") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 28.dp, horizontal = 16.dp),
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
                                SystemEventPill(
                                    text = entry.text,
                                    timestamp = entry.timestamp,
                                )
                            } else {
                                val isSelected = selectedIds.contains(entry.id)

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedIds = if (isSelected) {
                                                selectedIds - entry.id
                                            } else {
                                                selectedIds + entry.id
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
                                                    text = entry.speakerName,
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.5.sp,
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary,
                                                )

                                                if (entry.timestamp.isNotBlank()) {
                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                    ) {
                                                        Text(
                                                            text = entry.timestamp,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = 11.sp,
                                                            ),
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        )
                                                    }
                                                }
                                            }

                                            // Square Checkbox on Top Right
                                            SquareSelectCheckbox(
                                                checked = isSelected,
                                                onCheckedChange = { checked ->
                                                    selectedIds = if (checked) {
                                                        selectedIds + entry.id
                                                    } else {
                                                        selectedIds - entry.id
                                                    }
                                                },
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Transcript Text
                                        Text(
                                            text = entry.text,
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

                        // Live Spoken Stream Bubble
                        if (partialTranscript.isNotBlank()) {
                            item(key = "live_partial") {
                                LiveSpeakingBubble(text = partialTranscript)
                            }
                        }
                    }

                    // Floating "Jump to live" pill button over bottom of list
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

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Transcript Input Row: Pill TextField & Circular Send Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = newTranscriptText,
                onValueChange = { newTranscriptText = it },
                placeholder = {
                    Text(
                        text = "Type what the speaker said",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                modifier = Modifier.weight(1f),
                shape = CircleShape,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
            )

            IconButton(
                onClick = {
                    if (newTranscriptText.isNotBlank()) {
                        onAddTranscript(newTranscriptText.trim())
                        newTranscriptText = ""
                    }
                },
                enabled = newTranscriptText.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (newTranscriptText.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        shape = CircleShape,
                    ),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_forward),
                    contentDescription = "Send",
                    tint = if (newTranscriptText.isNotBlank()) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
