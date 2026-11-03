package com.example.konteksto.ui.host.studio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.konteksto.domain.model.HostSession

@Composable
fun HostStudioRoute(
    viewModel: HostStudioViewModel,
    initialSession: HostSession,
    onBackClick: () -> Unit,
    onPresentQrClick: (HostSession) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val session = uiState.session ?: initialSession

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
        onAddContextNote = { title, content -> viewModel.addContextNote(title = title, content = content) },
        modifier = modifier,
    )
}
