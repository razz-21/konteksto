package com.example.konteksto.ui.sessions.studio

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.R
import com.example.konteksto.ui.components.JumpToLiveButton
import com.example.konteksto.ui.components.LiveSpeakingBubble
import com.example.konteksto.ui.components.rememberLiveTranscriptFollowState
import com.example.konteksto.domain.model.HostSession

data class CompanionTranscriptItem(
    val id: String,
    val speakerName: String,
    val timestamp: String,
    val text: String,
    val isSystemEvent: Boolean = false,
)

data class LiveConceptExplanationItem(
    val id: String,
    val type: String = "Core Neural Mechanics",
    val timestamp: String = "At 14m 20s",
    val percentGrounded: Int = 98,
    val title: String = "Self-Attention Weights",
    val definition: String = "Mathematical scores calculated by the model that determine how much focus or weight each word gives to other words in the sentence.",
    val analogy: String = "Like a highlighter pen that dynamically emphasizes the pronouns and their antecedents while you read.",
    val whyItMatters: String = "Without self-attention, the model would treat words in isolation and miss contextual nuance or sentence-level logic.",
    val isBookmarked: Boolean = false,
)

private val DEFAULT_CONCEPT_EXPLANATIONS = listOf(
    LiveConceptExplanationItem(
        id = "concept_1",
        type = "Core Neural Mechanics",
        timestamp = "At 14m 20s",
        percentGrounded = 98,
        title = "Self-Attention Weights",
        definition = "Mathematical scores calculated by the model that determine how much focus or weight each word gives to other words in the sentence.",
        analogy = "Like a highlighter pen that dynamically emphasizes the pronouns and their antecedents while you read.",
        whyItMatters = "Without self-attention, the model would treat words in isolation and miss contextual nuance or sentence-level logic.",
    ),
    LiveConceptExplanationItem(
        id = "concept_2",
        type = "Generative Architectures",
        timestamp = "At 09:35 AM",
        percentGrounded = 95,
        title = "Sequential Dependency Bottleneck",
        definition = "The processing delay in traditional recurrent networks where tokens must be processed one after another in strictly ordered steps.",
        analogy = "Like waiting in a single-file line at a grocery store checkout rather than opening parallel registers.",
        whyItMatters = "Eliminating sequential dependency allows massive parallel processing during training and lower latency during inference.",
    ),
    LiveConceptExplanationItem(
        id = "concept_3",
        type = "Model Optimization",
        timestamp = "At 09:41 AM",
        percentGrounded = 92,
        title = "Parameter Activation Efficiency",
        definition = "Selective execution of sub-networks within multi-billion parameter models to compute outputs without activating every neuron.",
        analogy = "Like turning on lights only in the active rooms of a house rather than powering the entire grid.",
        whyItMatters = "Reduces hardware energy consumption and enables large models to run smoothly under tight real-time constraints.",
    ),
)

@Composable
fun SessionLiveCompanionView(
    session: HostSession,
    modifier: Modifier = Modifier,
    isAudioBroadcasting: Boolean = true,
    customTranscripts: List<CompanionTranscriptItem> = emptyList(),
    livePartialText: String = "",
    hasEarlierTranscripts: Boolean = false,
    isLoadingEarlierTranscripts: Boolean = false,
    didEarlierTranscriptsFail: Boolean = false,
    didLiveTranscriptFail: Boolean = false,
    onLoadEarlierTranscripts: () -> Unit = {},
    onRetryLiveTranscript: () -> Unit = {},
    conceptExplanations: List<LiveConceptExplanationItem> = DEFAULT_CONCEPT_EXPLANATIONS,
    isExplaining: Boolean = false,
    explainError: String? = null,
    onRetryExplain: () -> Unit = {},
    onAskWhatTheyMean: (selectedItems: List<CompanionTranscriptItem>, clearSelection: () -> Unit) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val items = remember(customTranscripts, session.speakerName) {
        customTranscripts.map { item ->
            if (!item.isSystemEvent && session.speakerName.isNotBlank()) {
                item.copy(speakerName = session.speakerName)
            } else item
        }
    }

    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val transcriptListState = rememberLazyListState()
    val transcriptItemCount = (if (hasEarlierTranscripts) 1 else 0) + items.size +
        (if (livePartialText.isNotBlank()) 1 else 0)
    val followState = rememberLiveTranscriptFollowState(
        listState = transcriptListState,
        itemCount = transcriptItemCount,
        liveText = livePartialText,
        isPaused = selectedIds.isNotEmpty(),
    )

    // Pulsing animation for wave icon when audio is live
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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Main Container Card for Live Audio Transcript
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
            ) {
                // Header Row: Live Audio Transcript Title + Wave Icon on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Live Audio Transcript",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Animated Audio Waveform Icon on the right of title
                    Icon(
                        painter = painterResource(id = R.drawable.ic_audio_waveform),
                        contentDescription = "Audio Waveform",
                        tint = if (isAudioBroadcasting) Color(0xFFFF4D4D) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer {
                                alpha = if (isAudioBroadcasting) waveAlpha else 1.0f
                            },
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle Row: Instruction + Multi-select tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Tap statements to select & ask what they mean",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        ),
                    ) {
                        Text(
                            text = "Multi-select",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Live Transcript Statements Panel
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
                        TextButton(onClick = onRetryLiveTranscript) {
                            Text(text = stringResource(id = R.string.transcript_retry))
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    LazyColumn(
                        state = transcriptListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (hasEarlierTranscripts) {
                            item(key = "load_earlier") {
                                TextButton(
                                    onClick = onLoadEarlierTranscripts,
                                    enabled = !isLoadingEarlierTranscripts,
                                    modifier = Modifier.fillMaxWidth(),
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

                        if (items.isEmpty() && livePartialText.isBlank()) {
                            if (!didLiveTranscriptFail) {
                                item(key = "empty") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp, horizontal = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            text = "No live transcripts yet",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Transcripts will appear in real-time as the speaker talks during the session.",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        } else {
                            items(items, key = { it.id }) { item ->
                                if (item.isSystemEvent || item.speakerName.equals("SYSTEM", ignoreCase = true)) {
                                    // System Event Pill Chip Badge
                                    val isPositiveEvent = item.text.contains("started", ignoreCase = true) ||
                                        item.text.contains("enabled", ignoreCase = true)

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            border = BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            ),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(
                                                            color = if (isPositiveEvent) Color(0xFF00C853) else Color(0xFFDC2626),
                                                            shape = CircleShape,
                                                        ),
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${item.text}  ·  ${item.timestamp}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 10.5.sp,
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
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
                                        shape = MaterialTheme.shapes.small,
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            },
                                        ),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) {
                                                Color(0xFF3B52D4)
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                            },
                                        ),
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                        ) {
                                            // Top Row: Speaker Name, Timestamp, Select Checkbox
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = item.speakerName,
                                                        style = MaterialTheme.typography.labelLarge.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.5.sp,
                                                        ),
                                                        color = Color(0xFF3B52D4),
                                                    )

                                                    Spacer(modifier = Modifier.width(6.dp))

                                                    Text(
                                                        text = item.timestamp,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 10.sp,
                                                        ),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }

                                                // Select Checkbox Tag (Icon only)
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = { checked ->
                                                        selectedIds = if (checked) {
                                                            selectedIds + item.id
                                                        } else {
                                                            selectedIds - item.id
                                                        }
                                                    },
                                                    modifier = Modifier.size(20.dp),
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = Color(0xFF3B52D4),
                                                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    ),
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Transcript Body Content
                                            Text(
                                                text = item.text,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontSize = 12.sp,
                                                    lineHeight = 16.5.sp,
                                                ),
                                                color = MaterialTheme.colorScheme.onBackground,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (livePartialText.isNotBlank()) {
                            item(key = "live_partial") {
                                LiveSpeakingBubble(text = livePartialText)
                            }
                        }
                    }

                    if (followState.showJumpToLive) {
                        JumpToLiveButton(
                            listState = transcriptListState,
                            followState = followState,
                            itemCount = transcriptItemCount,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }
        }

        // Bottom Action Button: ONLY "What did they mean?" (NO Snap Slide)
        val selectedItems = remember(selectedIds, items) {
            val selected = items.filter { it.id in selectedIds }
            // Ensure they are sorted by their order in the items list
            selected.sortedBy { items.indexOf(it) }
        }

        // Validate selection: max 5, and must be contiguous
        val isValidSelection = remember(selectedItems) {
            if (selectedItems.isEmpty()) false
            else if (selectedItems.size > 5) false
            else {
                val indices = selectedItems.map { items.indexOf(it) }
                val isContiguous = indices.zipWithNext().all { (a, b) -> b == a + 1 }
                isContiguous
            }
        }
        val showSelectionWarning = selectedItems.isNotEmpty() && !isValidSelection

        if (showSelectionWarning) {
            Text(
                text = "Please select up to 5 consecutive statements.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Button(
            onClick = {
                if (isValidSelection) {
                    onAskWhatTheyMean(selectedItems) {
                        selectedIds = emptySet()
                    }
                }
            },
            enabled = isValidSelection && !isExplaining,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFDCE2FF),
                contentColor = Color(0xFF152270),
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            ),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_question_help),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isValidSelection) Color(0xFF152270) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "What did they mean?",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                ),
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Live Concept Explanations Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_sparkle),
                    contentDescription = null,
                    tint = Color(0xFF3B52D4),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Live Concept Explanations (${conceptExplanations.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Text(
                text = "Auto-detected",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Live Concept Explanations Cards List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (isExplaining) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Analyzing selected statements...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (explainError != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(explainError, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                        TextButton(onClick = onRetryExplain) {
                            Text("Retry", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
            conceptExplanations.forEach { concept ->
                LiveConceptExplanationCard(
                    item = concept,
                    onBookmarkToggle = { conceptId ->
                        Toast.makeText(context, "Toggled bookmark for concept", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }
}

@Composable
fun LiveConceptExplanationCard(
    item: LiveConceptExplanationItem,
    onBookmarkToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isBookmarked by remember(item.isBookmarked) { mutableStateOf(item.isBookmarked) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header Row: Type / Category, Timestamp, Bookmark button
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
                        text = item.type,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                        ),
                        color = Color(0xFF3B52D4),
                    )

                    Text(
                        text = "  ·  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Text(
                        text = item.timestamp,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Bookmark Button
                IconButton(
                    onClick = {
                        isBookmarked = !isBookmarked
                        onBookmarkToggle(item.id)
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bookmark),
                        contentDescription = "Bookmark Concept",
                        tint = if (isBookmarked) Color(0xFF3B52D4) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Percent Grounded Badge Tag
            Surface(
                shape = CircleShape,
                color = Color(0xFFE8F8F0),
            ) {
                Text(
                    text = "${item.percentGrounded}% Grounded",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    ),
                    color = Color(0xFF00875A),
                )
            }

            // Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.5.sp,
                    lineHeight = 22.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )

            // Definition Body Text
            Text(
                text = item.definition,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.5.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Analogy Container Box
            if (item.analogy.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                    border = BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_lightbulb),
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(13.dp),
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "ANALOGY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.5.sp,
                                    letterSpacing = 0.8.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Text(
                            text = "\"${item.analogy}\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp,
                                fontStyle = FontStyle.Italic,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            // Why It Matters Paragraph
            if (item.whyItMatters.isNotBlank()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 12.5.sp,
                                ),
                            ) {
                                append("Why it matters: ")
                            }
                            withStyle(
                                style = SpanStyle(
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.5.sp,
                                ),
                            ) {
                                append(item.whyItMatters)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                    )
                }
            }
        }
    }
}
