package com.example.konteksto.ui.host.studio

import android.net.Uri
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.max
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.theme.KontekstoTheme

// The Slides & Context lists scroll on their own, so that tab is sized to the screen. This keeps
// them usable on short screens, where the rest of the tab scrolls instead.
private val MIN_SLIDES_TAB_HEIGHT = 640.dp

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
    onAddContextNoteClick: () -> Unit = {},
    onEditDomainTermClick: (termId: String) -> Unit = {},
    onDeleteDomainTermClick: (termId: String) -> Unit = {},
    onUploadPdfSubmitted: (uri: Uri, fileName: String, fileSizeMb: Double, pageCount: Int, pdfBytes: ByteArray?) -> Unit = { _, _, _, _, _ -> },
    onRemoveSlideDeckClicked: () -> Unit = {},
    onAnalyzeSlideDeckClicked: () -> Unit = {},
    onEditSlideClick: (slideId: String) -> Unit = {},
    onEditContextNoteClick: (noteId: String) -> Unit = {},
    onDeleteContextNoteClick: (noteId: String) -> Unit = {},
    onErrorDismissed: () -> Unit = {},
) {
    val scrollState = rememberScrollState()

    var showStartConfirmDialog by remember { mutableStateOf(false) }
    var showEndConfirmDialog by remember { mutableStateOf(false) }

    var localSelectedTab by remember { mutableIntStateOf(0) }
    val selectedTab = uiState?.selectedTab ?: localSelectedTab

    var localIsAudioBroadcasting by remember { mutableStateOf(session.isLive) }
    val isAudioBroadcasting = uiState?.isAudioBroadcasting ?: localIsAudioBroadcasting

    var localTranscripts by remember(session.id) {
        mutableStateOf(emptyList<TranscriptEntry>())
    }
    val transcripts = uiState?.transcripts ?: localTranscripts

    var localSlideDeckInfo by remember(session.id) {
        mutableStateOf<SlideDeckInfo?>(null)
    }
    val slideDeckInfo = uiState?.slideDeckInfo ?: localSlideDeckInfo

    var localAnalyzedSlides by remember(session.id) {
        mutableStateOf<List<AnalyzedSlideItem>>(emptyList())
    }
    val analyzedSlides = uiState?.analyzedSlides ?: localAnalyzedSlides

    val contextNotes = uiState?.contextNotes ?: emptyList()

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

    uiState?.errorMessage?.let { message ->
        SlideDeckErrorDialog(
            message = message,
            onDismiss = onErrorDismissed,
        )
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Kept outside the scrolling content so it stays pinned to the top.
            HostStudioTopBar(
                session = session,
                onBackClick = onBackClick,
                onPresentQrClick = onPresentQrClick,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                        .then(
                            if (selectedTab == 1) {
                                Modifier.height(max(maxHeight - 8.dp, MIN_SLIDES_TAB_HEIGHT))
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Segmented Tabs Row
                    HostStudioSegmentedTabs(
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            localSelectedTab = tab
                            onTabSelected(tab)
                        },
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab View Content
                    if (selectedTab == 0) {
                        // Session Header Card (Title, Venue, Start Session & Audio Broadcast)
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

                        HostStudioBroadcastView(
                            isAudioBroadcasting = isAudioBroadcasting,
                            transcripts = transcripts,
                            partialTranscript = uiState?.partialTranscript ?: "",
                            inputDevice = uiState?.inputDevice,
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
                            modifier = Modifier.weight(1f),
                            contextNotes = contextNotes,
                            slideDeckInfo = slideDeckInfo,
                            hasUnappliedContextNotes = uiState?.hasUnappliedContextNotes ?: false,
                            analyzedSlides = analyzedSlides,
                            domainTerms = uiState?.domainTerms ?: emptyList(),
                            isUploadingPdf = uiState?.isUploadingPdf ?: false,
                            isAnalyzingPdf = uiState?.isAnalyzingPdf ?: false,
                            onAddNoteClick = onAddContextNoteClick,
                            onUploadPdfSubmitted = { uri, fileName, fileSizeMb, pageCount, pdfBytes ->
                                if (uiState != null) {
                                    onUploadPdfSubmitted(uri, fileName, fileSizeMb, pageCount, pdfBytes)
                                } else {
                                    localSlideDeckInfo = SlideDeckInfo(
                                        fileName = fileName,
                                        fileSizeMb = fileSizeMb,
                                        pageCount = pageCount,
                                        uploadTime = "Uploaded Just now",
                                        isAnalyzed = false,
                                        pdfBytes = pdfBytes,
                                    )
                                    localAnalyzedSlides = emptyList()
                                }
                            },
                            onRemoveSlideDeckClick = {
                                if (uiState != null) {
                                    onRemoveSlideDeckClicked()
                                } else {
                                    localSlideDeckInfo = null
                                    localAnalyzedSlides = emptyList()
                                }
                            },
                            onAnalyzeSlideDeckClick = {
                                if (uiState != null) {
                                    onAnalyzeSlideDeckClicked()
                                } else {
                                    val currentDeckName = localSlideDeckInfo?.fileName ?: "Presentation.pdf"
                                    val pageCount = localSlideDeckInfo?.pageCount ?: 13
                                    localSlideDeckInfo = localSlideDeckInfo?.copy(isAnalyzed = true)
                                    localAnalyzedSlides = (1..pageCount).map { i ->
                                        AnalyzedSlideItem(
                                            id = "s_$i",
                                            slideNumber = i,
                                            title = "${currentDeckName.substringBeforeLast(".")}: Slide $i",
                                            summary = "Overview summary of topics and main structure presented on Slide $i.",
                                            explanation = "In-depth explanation of Slide $i concepts, mechanics, and context for live session listeners.",
                                            keyTakeaway = "Slide $i grounds AI live terminology definitions in real time.",
                                        )
                                    }
                                }
                            },
                            onEditSlideClick = onEditSlideClick,
                            onEditDomainTermClick = onEditDomainTermClick,
                            onDeleteDomainTermClick = onDeleteDomainTermClick,
                            onEditContextNoteClick = onEditContextNoteClick,
                            onDeleteContextNoteClick = onDeleteContextNoteClick,
                        )
                    }
                }
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
