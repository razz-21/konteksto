package com.example.konteksto.ui.sessions.studio

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.konteksto.domain.model.HostSession

@Composable
fun SessionStudioRoute(
    session: HostSession,
    customTranscripts: List<CompanionTranscriptItem> = emptyList(),
    onBackClick: () -> Unit,
    onLeaveConfirm: () -> Unit,
    onPresentQrClick: (HostSession) -> Unit,
    modifier: Modifier = Modifier,
) {
    SessionStudioScreen(
        session = session,
        customTranscripts = customTranscripts,
        onBackClick = onBackClick,
        onLeaveConfirm = onLeaveConfirm,
        onPresentQrClick = onPresentQrClick,
        modifier = modifier,
    )
}
