package com.example.konteksto.ui.sessions.studio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.host.studio.DomainTermItem

@Composable
fun SessionStudioRoute(
    viewModel: SessionStudioViewModel,
    session: HostSession,
    onBackClick: () -> Unit,
    onLeaveConfirm: () -> Unit,
    onPresentQrClick: (HostSession) -> Unit,
    modifier: Modifier = Modifier,
    domainTerms: List<DomainTermItem> = emptyList(),
) {
    val transcriptState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleStartEffect(viewModel) {
        viewModel.startObserving()
        onStopOrDispose { viewModel.stopObserving() }
    }

    SessionStudioScreen(
        session = session,
        onBackClick = onBackClick,
        onLeaveConfirm = onLeaveConfirm,
        onPresentQrClick = onPresentQrClick,
        modifier = modifier,
        transcriptState = transcriptState,
        onLoadEarlierTranscripts = viewModel::loadEarlierTranscripts,
        onRetryLiveTranscript = viewModel::retryLiveTranscript,
        onAskWhatTheyMean = viewModel::explainSelection,
        onDismissActiveExplanation = viewModel::dismissActiveExplanation,
        onBookmarkConcept = { concept -> viewModel.bookmarkConceptExplanation(concept, session.title) },
        onRemoveGlossaryTerm = viewModel::removeGlossaryTerm,
        onConceptAnnouncementShown = viewModel::onConceptAnnouncementShown,
        domainTerms = domainTerms,
    )
}
