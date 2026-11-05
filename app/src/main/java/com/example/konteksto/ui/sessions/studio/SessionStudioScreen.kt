package com.example.konteksto.ui.sessions.studio

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.host.studio.DOMAIN_TERM_CATEGORIES
import com.example.konteksto.ui.host.studio.DeleteDomainTermDialog
import com.example.konteksto.ui.host.studio.DomainTermItem
import com.example.konteksto.ui.theme.KontekstoTheme

@Composable
fun SessionStudioScreen(
    session: HostSession,
    onBackClick: () -> Unit,
    onLeaveConfirm: () -> Unit,
    onPresentQrClick: (HostSession) -> Unit,
    modifier: Modifier = Modifier,
    transcriptState: SessionStudioUiState = SessionStudioUiState(),
    onLoadEarlierTranscripts: () -> Unit = {},
    onRetryLiveTranscript: () -> Unit = {},
    onAskWhatTheyMean: (List<CompanionTranscriptItem>) -> Unit = {},
    onDismissActiveExplanation: () -> Unit = {},
    domainTerms: List<DomainTermItem> = emptyList(),
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var termsList by remember(domainTerms) { mutableStateOf(domainTerms) }
    var termToDelete by remember { mutableStateOf<DomainTermItem?>(null) }
    var termToEdit by remember { mutableStateOf<DomainTermItem?>(null) }

    if (showLeaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmDialog = false },
            title = {
                Text(
                    text = "Leave Session?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to leave this session?",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveConfirmDialog = false
                        onLeaveConfirm()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text("Leave", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLeaveConfirmDialog = false },
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    termToDelete?.let { term ->
        DeleteDomainTermDialog(
            term = term.term,
            onConfirm = {
                termsList = termsList.filterNot { it.id == term.id }
                termToDelete = null
                Toast.makeText(context, "Deleted ${term.term}", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { termToDelete = null },
        )
    }

    termToEdit?.let { term ->
        EditSessionDomainTermDialog(
            domainTerm = term,
            onConfirm = { updated ->
                termsList = termsList.map { if (it.id == updated.id) updated else it }
                termToEdit = null
                Toast.makeText(context, "Saved changes to ${updated.term}", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { termToEdit = null },
        )
    }

    val activeExplanation = transcriptState.activeExplanation
    if (activeExplanation != null) {
        ExplanationBottomSheet(
            request = activeExplanation,
            onDismiss = onDismissActiveExplanation,
            onRetry = { onAskWhatTheyMean(activeExplanation.selectedItems) },
        )
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Top Bar: Back button navigates directly back, Leave button opens prompt
            SessionStudioTopBar(
                session = session,
                onBackClick = onBackClick,
                onPresentQrClick = onPresentQrClick,
                onLeaveClick = { showLeaveConfirmDialog = true },
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Hero Banner Card & Speaker Info Card with session details
            SessionStudioHeaderCard(
                session = session,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Material Segmented Tabs directly below speaker section
            // Contains 3 tabs: Live Companion, Terms, Glossary
            SessionStudioSegmentedTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                termsCount = termsList.size,
                glossaryCount = 3,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                when (selectedTab) {
                    0 -> {
                        // Live Companion Tab View (Transcripts, What did they mean button, Live Concept Explanations)
                        SessionLiveCompanionView(
                            session = session,
                            isAudioBroadcasting = session.isLive,
                            customTranscripts = transcriptState.transcripts,
                            livePartialText = transcriptState.livePartialText.takeIf { session.isLive }.orEmpty(),
                            hasEarlierTranscripts = transcriptState.hasEarlierTranscripts,
                            isLoadingEarlierTranscripts = transcriptState.isLoadingEarlierTranscripts,
                            didEarlierTranscriptsFail = transcriptState.didEarlierTranscriptsFail,
                            didLiveTranscriptFail = transcriptState.didLiveTranscriptFail,
                            onLoadEarlierTranscripts = onLoadEarlierTranscripts,
                            onRetryLiveTranscript = onRetryLiveTranscript,
                            conceptExplanations = transcriptState.conceptExplanations,
                            onAskWhatTheyMean = { selectedItems, clearSelection ->
                                onAskWhatTheyMean(selectedItems)
                                clearSelection()
                            },
                        )
                    }
                    1 -> {
                        // Terms Tab
                        SessionTermsView(
                            domainTerms = termsList,
                            onEditTermClick = { termToEdit = it },
                            onDeleteTermClick = { termToDelete = it },
                        )
                    }
                    2 -> {
                        // Glossary Tab
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 16.dp),
                        ) {
                            Text(
                                text = "Session Glossary Entries",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditSessionDomainTermDialog(
    domainTerm: DomainTermItem,
    onConfirm: (DomainTermItem) -> Unit,
    onDismiss: () -> Unit,
) {
    var term by remember(domainTerm) { mutableStateOf(domainTerm.term) }
    var category by remember(domainTerm) { mutableStateOf(domainTerm.category) }
    var description by remember(domainTerm) { mutableStateOf(domainTerm.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Domain Term",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DOMAIN_TERM_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                        )
                    }
                }
                OutlinedTextField(
                    value = term,
                    onValueChange = { term = it },
                    label = { Text("Term Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(domainTerm.copy(term = term.trim(), category = category, description = description.trim()))
                },
                enabled = term.isNotBlank() && description.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplanationBottomSheet(
    request: ActiveExplanationRequest,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            androidx.compose.material3.BottomSheetDefaults.DragHandle()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFE8EAFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.konteksto.R.drawable.ic_sparkle),
                        contentDescription = null,
                        tint = Color(0xFF3B52D4),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "What Did They Mean? (${request.selectedItems.size} Selected)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Explained by AI",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selected Statements Card
            androidx.compose.material3.Card(
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "SELECTED STATEMENTS (${request.selectedItems.size}):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                        color = Color(0xFF3B52D4),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    request.selectedItems.forEachIndexed { index, item ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Blue left border
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(36.dp) // Simplified height, actually should match content but this works for demo, or use intrinsic size
                                    .background(Color(0xFF3B52D4))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.padding(bottom = if (index == request.selectedItems.size - 1) 0.dp else 12.dp)) {
                                Text(
                                    text = "${item.speakerName} (${item.timestamp})",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "\"${item.text}\"",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Result, Loading, or Error Card
            androidx.compose.material3.Card(
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    when {
                        request.isLoading -> {
                            Text("Analyzing selected statements...", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                        request.error != null -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(request.error, color = MaterialTheme.colorScheme.error, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = onRetry) {
                                    Text("Retry")
                                }
                            }
                        }
                        request.explanation != null -> {
                            val exp = request.explanation
                            // Combine definition and whyItMatters into a single synthesized breakdown paragraph
                            val breakdown = buildString {
                                append("Synthesized breakdown: ")
                                append(exp.definition)
                                if (exp.analogy.isNotBlank()) {
                                    append(" Think of it this way: ${exp.analogy}")
                                }
                                if (exp.whyItMatters.isNotBlank()) {
                                    append(" This matters because ${exp.whyItMatters.replaceFirstChar { it.lowercase() }}")
                                }
                            }
                            Text(
                                text = breakdown,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)
                            )
                        }
                    }
                }
            }

            if (request.explanation != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Source: Audio stream segment",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${request.explanation.percentGrounded}% Grounded",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF00875A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text("Clear Selection & Close")
                }
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B52D4)
                    )
                ) {
                    Text("Got it, back to session")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SessionStudioScreenLightPreview() {
    val sampleSession = HostSession(
        id = "1",
        venue = "Main Auditorium · Stage A",
        time = "75 min",
        talkCode = "TALK–892",
        title = "Revolutionizing Deep Learning: Advancements in Neural Architectures",
        speakerName = "Dr. Elena Santos",
        category = "Frontier",
        attendeesCount = 892,
        isLive = true,
    )
    KontekstoTheme(darkTheme = false) {
        SessionStudioScreen(
            session = sampleSession,
            onBackClick = {},
            onLeaveConfirm = {},
            onPresentQrClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SessionStudioScreenDarkPreview() {
    val sampleSession = HostSession(
        id = "1",
        venue = "Main Auditorium · Stage A",
        time = "75 min",
        talkCode = "TALK–892",
        title = "Revolutionizing Deep Learning: Advancements in Neural Architectures",
        speakerName = "Dr. Elena Santos",
        category = "Frontier",
        attendeesCount = 892,
        isLive = true,
    )
    KontekstoTheme(darkTheme = true) {
        SessionStudioScreen(
            session = sampleSession,
            onBackClick = {},
            onLeaveConfirm = {},
            onPresentQrClick = {},
        )
    }
}
