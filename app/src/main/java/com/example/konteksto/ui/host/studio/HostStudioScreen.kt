package com.example.konteksto.ui.host.studio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.theme.KontekstoTheme

@Composable
fun HostStudioScreen(
    session: HostSession,
    onBackClick: () -> Unit,
    onPresentQrClick: (HostSession) -> Unit,
    onToggleLiveStatus: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    uiState: HostStudioUiState? = null,
    onTabSelected: (Int) -> Unit = {},
    onToggleAudioBroadcasting: () -> Unit = {},
    onAddTranscript: (String) -> Unit = {},
    onAddContextNote: (title: String, content: String) -> Unit = { _, _ -> },
) {
    val scrollState = rememberScrollState()

    var showStartConfirmDialog by remember { mutableStateOf(false) }
    var showEndConfirmDialog by remember { mutableStateOf(false) }
    var showAddContextDialog by remember { mutableStateOf(false) }

    var localSelectedTab by remember { mutableIntStateOf(0) }
    val selectedTab = uiState?.selectedTab ?: localSelectedTab

    var localIsAudioBroadcasting by remember { mutableStateOf(session.isLive) }
    val isAudioBroadcasting = uiState?.isAudioBroadcasting ?: localIsAudioBroadcasting

    var localTranscripts by remember(session.id) {
        mutableStateOf(emptyList<TranscriptEntry>())
    }
    val transcripts = uiState?.transcripts ?: localTranscripts

    var localContextNotes by remember(session.id) {
        mutableStateOf(
            listOf(
                ContextNoteItem(
                    id = "c1",
                    category = "ARCHITECTURE DIAGRAM",
                    title = "Latency Benchmark & Edge Buffer Thresholds",
                    content = "US-West edge latency threshold target < 15ms. Local frame processing buffer set to 128kb batch size.",
                    timestamp = "10:02 AM",
                ),
            ),
        )
    }
    val contextNotes = uiState?.contextNotes ?: localContextNotes

    // Start Live Session Dialog
    if (showStartConfirmDialog) {
        StartSessionConfirmDialog(
            onConfirm = {
                showStartConfirmDialog = false
                localIsAudioBroadcasting = true
                onToggleLiveStatus(true)
            },
            onDismiss = { showStartConfirmDialog = false },
        )
    }

    // End Live Session Dialog
    if (showEndConfirmDialog) {
        EndSessionConfirmDialog(
            onConfirm = {
                showEndConfirmDialog = false
                localIsAudioBroadcasting = false
                onToggleLiveStatus(false)
            },
            onDismiss = { showEndConfirmDialog = false },
        )
    }

    // Add Context Note Dialog
    if (showAddContextDialog) {
        AddContextNoteDialog(
            onAddNoteSubmit = { title, content ->
                if (uiState != null) {
                    onAddContextNote(title, content)
                } else {
                    val newItem = ContextNoteItem(
                        id = System.currentTimeMillis().toString(),
                        category = "BACKGROUND NOTE",
                        title = title,
                        content = content,
                        timestamp = "Just now",
                    )
                    localContextNotes += newItem
                }
            },
            onDismiss = { showAddContextDialog = false },
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
            // Top Bar
            HostStudioTopBar(
                session = session,
                onBackClick = onBackClick,
                onPresentQrClick = onPresentQrClick,
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Segmented Tabs Row
            HostStudioSegmentedTabs(
                selectedTab = selectedTab,
                contextNotesCount = contextNotes.size,
                onTabSelected = { tab ->
                    localSelectedTab = tab
                    onTabSelected(tab)
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Session Header Card
            HostStudioHeaderCard(
                session = session,
                isAudioBroadcasting = isAudioBroadcasting,
                onStartSessionClick = { showStartConfirmDialog = true },
                onEndSessionClick = { showEndConfirmDialog = true },
                onToggleAudioBroadcasting = {
                    localIsAudioBroadcasting = !localIsAudioBroadcasting
                    onToggleAudioBroadcasting()
                },
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Tab View Content
            if (selectedTab == 0) {
                HostStudioBroadcastView(
                    isSessionLive = session.isLive,
                    isAudioBroadcasting = isAudioBroadcasting,
                    transcripts = transcripts,
                    partialTranscript = uiState?.partialTranscript ?: "",
                    onAddTranscript = { text ->
                        if (uiState != null) {
                            onAddTranscript(text)
                        } else {
                            val newEntry = TranscriptEntry(
                                id = System.currentTimeMillis().toString(),
                                speakerName = session.speakerName.ifBlank { "Host" },
                                timestamp = "Just now",
                                text = text,
                            )
                            localTranscripts += newEntry
                        }
                    },
                )
            } else {
                HostStudioSlidesView(
                    contextNotes = contextNotes,
                    onAddNoteClick = { showAddContextDialog = true },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HostStudioScreenLightPreview() {
    val sampleSession = HostSession(
        id = "1",
        venue = "Tech Summit Hall B",
        time = "10:00 AM",
        talkCode = "TALK–415",
        title = "Edge AI & Distributed Cloud Architecture: Scaling to Zero Latency",
        speakerName = "Engr. Marco Valerio",
        attendeesCount = 890,
        isLive = true,
    )
    KontekstoTheme(darkTheme = false) {
        HostStudioScreen(
            session = sampleSession,
            onBackClick = {},
            onPresentQrClick = {},
            onToggleLiveStatus = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HostStudioScreenDarkPreview() {
    val sampleSession = HostSession(
        id = "1",
        venue = "Tech Summit Hall B",
        time = "10:00 AM",
        talkCode = "TALK–415",
        title = "Edge AI & Distributed Cloud Architecture: Scaling to Zero Latency",
        speakerName = "Engr. Marco Valerio",
        attendeesCount = 890,
        isLive = true,
    )
    KontekstoTheme(darkTheme = true) {
        HostStudioScreen(
            session = sampleSession,
            onBackClick = {},
            onPresentQrClick = {},
            onToggleLiveStatus = {},
        )
    }
}
