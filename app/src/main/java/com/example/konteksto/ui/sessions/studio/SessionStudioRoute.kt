package com.example.konteksto.ui.sessions.studio

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.host.studio.DomainTermItem

@Composable
fun SessionStudioRoute(
    session: HostSession,
    onBackClick: () -> Unit,
    onLeaveConfirm: () -> Unit,
    onPresentQrClick: (HostSession) -> Unit,
    modifier: Modifier = Modifier,
    customTranscripts: List<CompanionTranscriptItem> = emptyList(),
    domainTerms: List<DomainTermItem> = emptyList(),
) {
    SessionStudioScreen(
        session = session,
        onBackClick = onBackClick,
        onLeaveConfirm = onLeaveConfirm,
        onPresentQrClick = onPresentQrClick,
        modifier = modifier,
        customTranscripts = customTranscripts,
        domainTerms = domainTerms,
    )
}
