package com.example.konteksto.ui.sessions.studio

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.konteksto.data.repository.GlossaryTermItem
import com.example.konteksto.domain.model.HostSession
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
    onBookmarkConcept: (LiveConceptExplanationItem) -> Unit = {},
    onToggleBookmarkGlossaryTerm: (GlossaryTermItem) -> Unit = {},
    domainTerms: List<DomainTermItem> = emptyList(),
) {
    val context = LocalContext.current
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var termsList by remember(domainTerms) { mutableStateOf(domainTerms) }
    var termToDelete by remember { mutableStateOf<DomainTermItem?>(null) }
    var termToEdit by remember { mutableStateOf<DomainTermItem?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    var previousConceptIds by remember { mutableStateOf<Set<String>?>(null) }

    LaunchedEffect(transcriptState.conceptExplanations) {
        val currentConcepts = transcriptState.conceptExplanations
        if (currentConcepts.isNotEmpty()) {
            val currentIds = currentConcepts.map { it.id }.toSet()
            val prev = previousConceptIds
            if (prev != null) {
                val newlyAdded = currentConcepts.filter { it.id !in prev }
                if (newlyAdded.isNotEmpty()) {
                    val newest = newlyAdded.first()
                    val result = snackbarHostState.showSnackbar(
                        message = "New live concept detected: ${newest.title}",
                        actionLabel = "View",
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        selectedTab = 0
                    }
                }
            }
            previousConceptIds = currentIds
        }
    }

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

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                glossaryCount = transcriptState.sessionGlossaryTerms.size.coerceAtLeast(1),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
                            onBookmarkConcept = onBookmarkConcept,
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
                            showEditDeleteActions = false,
                        )
                    }
                    2 -> {
                        // Glossary Tab
                        SessionGlossaryView(
                            glossaryTerms = transcriptState.sessionGlossaryTerms,
                            domainTerms = termsList,
                            onToggleBookmark = onToggleBookmarkGlossaryTerm,
                        )
                    }
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
