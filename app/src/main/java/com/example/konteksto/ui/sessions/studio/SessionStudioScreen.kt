package com.example.konteksto.ui.sessions.studio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.konteksto.data.repository.GlossaryTermItem
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.domain.model.LiveConceptExplanationItem
import com.example.konteksto.ui.components.FillRemainingHeightColumn
import com.example.konteksto.ui.components.LiveTranscriptMinHeight
import com.example.konteksto.ui.components.showKontekstoSnackbar
import com.example.konteksto.ui.host.studio.DeleteDomainTermDialog
import com.example.konteksto.ui.host.studio.DomainTermItem
import com.example.konteksto.ui.theme.KontekstoButtonDefaults
import com.example.konteksto.ui.theme.KontekstoTheme
import kotlinx.coroutines.launch

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
    onRemoveGlossaryTerm: (GlossaryTermItem) -> Unit = {},
    onConceptAnnouncementShown: (String) -> Unit = {},
    domainTerms: List<DomainTermItem> = emptyList(),
) {
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(SessionStudioTab.LiveCompanion) }
    var termsList by remember(domainTerms) { mutableStateOf(domainTerms) }
    var termToDelete by remember { mutableStateOf<DomainTermItem?>(null) }
    var termToEdit by remember { mutableStateOf<DomainTermItem?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { message ->
        snackbarScope.launch { snackbarHostState.showKontekstoSnackbar(message) }
    }
    val newConcept = transcriptState.newConceptAnnouncement
    LaunchedEffect(newConcept?.id) {
        newConcept ?: return@LaunchedEffect
        val result = snackbarHostState.showKontekstoSnackbar(
            message = "New live concept detected: ${newConcept.title}",
            actionLabel = "View",
        )
        if (result == SnackbarResult.ActionPerformed) {
            selectedTab = SessionStudioTab.ConceptExplanation
        }
        onConceptAnnouncementShown(newConcept.id)
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
                    shape = KontekstoButtonDefaults.shape,
                    modifier = Modifier.height(KontekstoButtonDefaults.Height),
                ) {
                    Text("Leave", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLeaveConfirmDialog = false },
                    shape = KontekstoButtonDefaults.shape,
                    modifier = Modifier.height(KontekstoButtonDefaults.Height),
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
                showMessage("Deleted ${term.term}")
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
                showMessage("Saved changes to ${updated.term}")
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
        // The tabs take the height left under the header, but never less than the transcript
        // minimum; on a short phone the whole page scrolls instead of squeezing the transcript.
        FillRemainingHeightColumn(
            minBodyHeight = LiveTranscriptMinHeight + LiveCompanionActionAreaHeight,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            header = {
                // Top Bar: Back button navigates directly back, Leave button opens prompt
                SessionStudioTopBar(
                    session = session,
                    onBackClick = onBackClick,
                    onPresentQrClick = onPresentQrClick,
                    onLeaveClick = { showLeaveConfirmDialog = true },
                    onShowMessage = showMessage,
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Hero Banner Card & Speaker Info Card with session details
                SessionStudioHeaderCard(
                    session = session,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Material Segmented Tabs directly below speaker section
                // Contains 4 tabs: Live Companion, Concept Explanation, Terms, Glossary
                SessionStudioSegmentedTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    conceptCount = transcriptState.conceptExplanations.size,
                    termsCount = termsList.size,
                    glossaryCount = transcriptState.sessionGlossaryTerms.size,
                )

                Spacer(modifier = Modifier.height(12.dp))
            },
        ) {
            // Tab Content
            when (selectedTab) {
                SessionStudioTab.LiveCompanion -> {
                    // Live Companion Tab View (Transcripts, What did they mean button)
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
                        onAskWhatTheyMean = { selectedItems, clearSelection ->
                            onAskWhatTheyMean(selectedItems)
                            clearSelection()
                        },
                    )
                }
                SessionStudioTab.ConceptExplanation -> {
                    // Concept Explanation Tab: concepts detected while the speaker talks
                    SessionConceptExplanationsView(
                        conceptExplanations = transcriptState.conceptExplanations,
                        isSessionLive = session.isLive,
                        onBookmarkConcept = onBookmarkConcept,
                        onShowMessage = showMessage,
                    )
                }
                SessionStudioTab.Terms -> {
                    // Terms Tab
                    SessionTermsView(
                        domainTerms = termsList,
                        showEditDeleteActions = false,
                    )
                }
                SessionStudioTab.Glossary -> {
                    // Glossary Tab: only the live concepts this attendee bookmarked
                    SessionGlossaryView(
                        glossaryTerms = transcriptState.sessionGlossaryTerms,
                        onRemoveTerm = onRemoveGlossaryTerm,
                        onShowMessage = showMessage,
                    )
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
        venue = "CDO Ayala",
        time = "10:00 – 11:30 AM",
        talkCode = "4VY4N6",
        title = "Object Oriented Programming",
        speakerName = "Ernesto Razo",
        category = "Technology & digital",
        attendeesCount = 42,
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
        venue = "CDO Ayala",
        time = "10:00 – 11:30 AM",
        talkCode = "4VY4N6",
        title = "Object Oriented Programming",
        speakerName = "Ernesto Razo",
        category = "Technology & digital",
        attendeesCount = 42,
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
