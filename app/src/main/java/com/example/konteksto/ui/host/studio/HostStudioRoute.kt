package com.example.konteksto.ui.host.studio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.components.SessionUnavailableScreen

@Composable
fun HostStudioRoute(
    viewModel: HostStudioViewModel,
    onBackClick: () -> Unit,
    onPresentQrClick: (HostSession) -> Unit,
    onEditSlideClick: (slideId: String) -> Unit,
    onEditDomainTermClick: (termId: String) -> Unit,
    onAddContextNoteClick: () -> Unit,
    onEditContextNoteClick: (noteId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val session = uiState.session
    if (session == null) {
        SessionUnavailableScreen(
            lookup = uiState.sessionLookup,
            onBackClick = onBackClick,
            onRetryClick = viewModel::retrySessionLookup,
            modifier = modifier,
        )
        return
    }

    HostStudioScreen(
        session = session,
        uiState = uiState,
        onBackClick = onBackClick,
        onPresentQrClick = onPresentQrClick,
        onToggleLiveStatus = { isLive ->
            viewModel.toggleSessionLiveStatus(isLive = isLive)
        },
        onTabSelected = { tab -> viewModel.setSelectedTab(tab) },
        onToggleAudioBroadcasting = { viewModel.toggleAudioBroadcasting() },
        onAddTranscript = { text -> viewModel.addTranscriptEntry(text = text) },
        onEditDomainTermClick = onEditDomainTermClick,
        onDeleteDomainTermClick = { termId -> viewModel.deleteDomainTerm(termId) },
        onAddContextNoteClick = onAddContextNoteClick,
        onUploadPdfSubmitted = { uri, fileName, fileSizeMb, pageCount, pdfBytes ->
            viewModel.uploadSlideDeckPdf(uri, fileName, fileSizeMb, pageCount, pdfBytes)
        },
        onRemoveSlideDeckClicked = { viewModel.removeSlideDeck() },
        onAnalyzeSlideDeckClicked = { viewModel.analyzeSlideDeck() },
        onEditSlideClick = onEditSlideClick,
        onEditContextNoteClick = onEditContextNoteClick,
        onDeleteContextNoteClick = { noteId -> viewModel.deleteContextNote(noteId) },
        onErrorDismissed = { viewModel.clearErrorMessage() },
        modifier = modifier,
    )
}
