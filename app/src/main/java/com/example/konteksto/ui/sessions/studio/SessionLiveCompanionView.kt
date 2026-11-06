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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.R
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.components.JumpToLiveButton
import com.example.konteksto.ui.components.LiveSpeakingBubble
import com.example.konteksto.ui.components.MarkdownText
import com.example.konteksto.ui.components.rememberLiveTranscriptFollowState

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
)

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
    conceptExplanations: List<LiveConceptExplanationItem> = DEFAULT_CONCEPT_EXPLANATIONS,
    isExplaining: Boolean = false,
    explainError: String? = null,
    onRetryExplain: () -> Unit = {},
    onBookmarkConcept: (LiveConceptExplanationItem) -> Unit = {},
    onAskWhatTheyMean: (selectedItems: List<CompanionTranscriptItem>, clearSelection: () -> Unit) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current

    // Sample default transcript entries matching the design screenshot
    val defaultTranscripts = remember(session.speakerName) {
        val name = session.speakerName.ifBlank { "ERNESTO RAZO" }
        listOf(
            CompanionTranscriptItem(
                id = "demo_1",
                speakerName = name,
                timestamp = "7:35 PM",
                text = "so here is the take a key takeaways first oop organized organizes program around objects that bundle date with behavior class is a blueprint an object is a living instance build with new so you're going to use new keyword if you want to instantiate the class to object at the four pillars that the following are encapsulation and abstraction make code reusable and easy to maintain",
            ),
            CompanionTranscriptItem(
                id = "demo_sys_1",
                speakerName = "SYSTEM",
                timestamp = "7:35 PM",
                text = "Audio broadcast ended",
                isSystemEvent = true,
            ),
        )
    }

    val activeTranscripts = remember(customTranscripts, defaultTranscripts) {
        if (customTranscripts.isNotEmpty()) customTranscripts else defaultTranscripts
    }

    val items = remember(activeTranscripts, session.speakerName) {
        activeTranscripts.map { item ->
            if (!item.isSystemEvent && session.speakerName.isNotBlank()) {
                item.copy(speakerName = session.speakerName.uppercase())
            } else item
        }
    }

    // Default select statement 1 in demo mode to match design screenshot
    var selectedIds by remember {
        mutableStateOf(
            if (customTranscripts.isEmpty()) setOf("demo_1") else emptySet(),
        )
    }

    val transcriptListState = rememberLazyListState()
    val transcriptItemCount = (if (hasEarlierTranscripts) 1 else 0) + items.size +
        (if (livePartialText.isNotBlank()) 1 else 0)
    val followState = rememberLiveTranscriptFollowState(
        listState = transcriptListState,
        itemCount = transcriptItemCount,
        liveText = livePartialText,
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
        LazyColumn(
            state = transcriptListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (selectedIds.isNotEmpty()) 80.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // PANEL 1: SEPARATE WHITE CARD PANEL FOR LIVE TRANSCRIPT
            item(key = "live_transcript_panel") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White,
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color(0xFFEAECF0),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                color = Color(0xFFF2F4F7),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_audio_waveform),
                                        contentDescription = null,
                                        tint = if (isAudioBroadcasting) Color(0xFF2563EB) else Color(0xFF475467),
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
                                        color = Color(0xFF344054),
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
                            color = Color(0xFF667085),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Box for Transcript Statements with JumpToLiveButton strictly inside this panel
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
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

                                if (hasEarlierTranscripts) {
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

                                items.forEach { item ->
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
                                                containerColor = if (isSelected) Color(0xFFF8FAFF) else Color(0xFFFAFAFA),
                                            ),
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) Color(0xFF2563EB) else Color(0xFFEAECF0),
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
                                                            color = Color(0xFF2563EB),
                                                        )

                                                        Spacer(modifier = Modifier.width(8.dp))

                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = Color(0xFFF2F4F7),
                                                        ) {
                                                            Text(
                                                                text = item.timestamp,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    fontSize = 11.sp,
                                                                ),
                                                                color = Color(0xFF475467),
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
                                    LiveSpeakingBubble(text = livePartialText)
                                }

                                if (isExplaining) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFFAFAFA),
                                        ),
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text("Analyzing selected statements...", color = Color(0xFF667085))
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
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(explainError, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                                            TextButton(onClick = onRetryExplain) {
                                                Text("Retry", color = MaterialTheme.colorScheme.onErrorContainer)
                                            }
                                        }
                                    }
                                }
                            }

                            // Jump To Live button constrained strictly inside Panel 1
                            if (followState.showJumpToLive) {
                                JumpToLiveButton(
                                    listState = transcriptListState,
                                    followState = followState,
                                    itemCount = transcriptItemCount,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 4.dp),
                                )
                            }
                        }
                    }
                }
            }

            // PANEL 2: SEPARATE WHITE CARD PANEL FOR LIVE CONCEPT EXPLANATIONS
            if (conceptExplanations.isNotEmpty()) {
                item(key = "live_concept_explanations_panel") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White,
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = Color(0xFFEAECF0),
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            // Concept Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_sparkle),
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
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
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = Color(0xFF667085),
                                )
                            }

                            conceptExplanations.forEach { concept ->
                                LiveConceptExplanationCard(
                                    item = concept,
                                    onBookmarkToggle = { _ ->
                                        onBookmarkConcept(concept)
                                        val msg = if (!concept.isBookmarked) {
                                            "Saved to My Glossary & Session Glossary"
                                        } else {
                                            "Removed from My Glossary"
                                        }
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Sticky Action Button at Bottom: "What did they mean? (1)"
        val selectedItems = remember(selectedIds, items) {
            items.filter { it.id in selectedIds }.sortedBy { items.indexOf(it) }
        }

        if (selectedIds.isNotEmpty()) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_sparkle),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "What did they mean?",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            ),
                            color = Color.White,
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // White Circle Badge with selected count
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = selectedItems.size.toString(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp,
                                ),
                                color = Color(0xFF2563EB),
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
            color = Color(0xFFF2F4F7),
            border = BorderStroke(
                width = 1.dp,
                color = Color(0xFFEAECF0),
            ),
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
                            color = if (isOffOrEnded) Color(0xFFFF8A80) else Color(0xFF00C853),
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
                    color = Color(0xFF344054),
                )

                if (timestamp.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))

                    // Inner Timestamp Pill Badge
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE2E8F0),
                    ) {
                        Text(
                            text = timestamp,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                            ),
                            color = Color(0xFF475467),
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
            .background(if (checked) Color(0xFF2563EB) else Color.Transparent)
            .border(
                width = 1.5.dp,
                color = if (checked) Color(0xFF2563EB) else Color(0xFF667085),
                shape = RoundedCornerShape(4.dp),
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check),
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(13.dp),
            )
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
            containerColor = Color(0xFFFAFAFA),
        ),
        border = BorderStroke(
            1.dp,
            Color(0xFFEAECF0),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
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
                        color = Color(0xFF2563EB),
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
                        tint = if (isBookmarked) Color(0xFF2563EB) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

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

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.5.sp,
                    lineHeight = 22.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )

            MarkdownText(
                markdown = item.definition,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.5.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (item.analogy.isNotBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_sparkle),
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(16.dp).padding(top = 2.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Analogy",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB),
                                ),
                            )
                            Text(
                                text = item.analogy,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontStyle = FontStyle.Italic,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (item.whyItMatters.isNotBlank()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Why it matters:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.whyItMatters,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
