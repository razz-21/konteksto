package com.example.konteksto.ui.sessions.studio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.konteksto.data.repository.FirebaseGlossaryRepository
import com.example.konteksto.data.repository.GlossaryRepository
import com.example.konteksto.data.repository.GlossaryTermItem
import com.example.konteksto.data.repository.SessionRepository
import com.example.konteksto.domain.model.LiveConceptExplanationItem
import com.example.konteksto.ui.host.studio.TranscriptEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val TRANSCRIPT_PAGE_SIZE = 50L
internal const val LIVE_PARTIAL_STALE_AFTER_MS = 15_000L

data class ActiveExplanationRequest(
    val selectedItems: List<CompanionTranscriptItem>,
    val explanation: LiveConceptExplanationItem? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class SessionStudioUiState(
    val transcripts: List<CompanionTranscriptItem> = emptyList(),
    val livePartialText: String = "",
    val hasEarlierTranscripts: Boolean = false,
    val isLoadingEarlierTranscripts: Boolean = false,
    val didEarlierTranscriptsFail: Boolean = false,
    val didLiveTranscriptFail: Boolean = false,
    val conceptExplanations: List<LiveConceptExplanationItem> = emptyList(),
    /** A concept the engine saved while this attendee was in the room, shown once as a snackbar. */
    val newConceptAnnouncement: LiveConceptExplanationItem? = null,
    val activeExplanation: ActiveExplanationRequest? = null,
    val sessionGlossaryTerms: List<GlossaryTermItem> = emptyList(),
)

/**
 * The listener only covers the newest lines, so lines it has moved past are kept here for the viewer.
 * A full window that starts after everything known means lines were missed while the app was in the
 * background, and those are left for paging rather than shown with a silent gap.
 */
internal fun mergeTranscriptWindow(
    known: List<TranscriptEntry>,
    window: List<TranscriptEntry>,
    windowSize: Long = TRANSCRIPT_PAGE_SIZE,
): List<TranscriptEntry> {
    val hasGap = known.isNotEmpty() &&
        window.size >= windowSize &&
        window.first().createdAt > known.last().createdAt
    if (hasGap) return window
    return (known + window).associateBy { it.id }.values.sortedBy { it.createdAt }
}

/**
 * Hides in-progress text that has stopped changing, because a host who loses their connection or
 * crashes mid-sentence never sends the write that clears it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun Flow<String>.expireStalePartial(staleAfterMs: Long = LIVE_PARTIAL_STALE_AFTER_MS): Flow<String> =
    transformLatest { text ->
        emit(text)
        if (text.isNotBlank()) {
            delay(staleAfterMs)
            emit("")
        }
    }

class SessionStudioViewModel(
    private val sessionId: String,
    private val sessionRepository: SessionRepository,
    private val explanationRepository: com.example.konteksto.data.repository.ExplanationRepository = com.example.konteksto.data.repository.FirebaseExplanationRepository(),
    private val glossaryRepository: GlossaryRepository = FirebaseGlossaryRepository(),
    clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionStudioUiState())
    val uiState: StateFlow<SessionStudioUiState> = _uiState.asStateFlow()

    private var knownTranscripts = emptyList<TranscriptEntry>()
    private var observeJob: Job? = null

    /**
     * Only concepts saved after the attendee opened the studio are announced, so saved ones loading
     * on entry are not, and IDs already seen are not announced again when observing restarts.
     */
    private val openedAt = clock()
    private var seenConceptIds = emptySet<String>()

    /** Live updates only run while the screen is visible, so a backgrounded attendee costs no reads. */
    fun startObserving() {
        if (observeJob?.isActive == true) return
        _uiState.update { it.copy(didLiveTranscriptFail = false) }
        observeJob = viewModelScope.launch {
            launch {
                sessionRepository.getTranscriptsStream(sessionId, limit = TRANSCRIPT_PAGE_SIZE)
                    .catch { _uiState.update { it.copy(didLiveTranscriptFail = true) } }
                    .collect(::onTranscriptWindow)
            }
            launch {
                sessionRepository.getLivePartialTranscriptStream(sessionId)
                    .expireStalePartial()
                    .catch { emit("") }
                    .collect { text ->
                        _uiState.update { it.copy(livePartialText = text) }
                    }
            }
            launch {
                // The server's concept engine detects and saves these, so they are never generated on the device.
                sessionRepository.getLiveConceptsStream(sessionId)
                    .catch { }
                    .collect { concepts ->
                        val newest = concepts.firstOrNull { it.id !in seenConceptIds && it.createdAt > openedAt }
                        seenConceptIds = seenConceptIds + concepts.map { it.id }
                        _uiState.update { state ->
                            val savedIds = state.sessionGlossaryTerms.map { it.id }.toSet()
                            state.copy(
                                conceptExplanations = concepts.map { it.copy(isBookmarked = it.id in savedIds) },
                                newConceptAnnouncement = newest ?: state.newConceptAnnouncement,
                            )
                        }
                    }
            }
            launch {
                // The room glossary is only what this attendee bookmarked from live concepts.
                glossaryRepository.getSavedGlossaryTermsStream(userUid = "")
                    .map { terms -> terms.filter { it.sessionId == sessionId } }
                    .catch { emit(emptyList()) }
                    .collect { savedTerms ->
                        val savedIds = savedTerms.map { it.id }.toSet()
                        _uiState.update { state ->
                            state.copy(
                                sessionGlossaryTerms = savedTerms,
                                conceptExplanations = state.conceptExplanations.map { concept ->
                                    concept.copy(isBookmarked = concept.id in savedIds)
                                },
                            )
                        }
                    }
            }
        }
    }

    fun stopObserving() {
        observeJob?.cancel()
        observeJob = null
    }

    fun retryLiveTranscript() {
        stopObserving()
        startObserving()
    }

    private fun onTranscriptWindow(window: List<TranscriptEntry>) {
        knownTranscripts = mergeTranscriptWindow(knownTranscripts, window)
        val companionItems = knownTranscripts.map { it.toCompanionItem() }
        _uiState.update {
            it.copy(
                transcripts = companionItems,
                hasEarlierTranscripts = it.hasEarlierTranscripts || window.size >= TRANSCRIPT_PAGE_SIZE,
            )
        }
    }

    fun loadEarlierTranscripts() {
        val oldest = knownTranscripts.firstOrNull() ?: return
        if (_uiState.value.isLoadingEarlierTranscripts) return
        _uiState.update {
            it.copy(
                isLoadingEarlierTranscripts = true,
                didEarlierTranscriptsFail = false,
            )
        }
        viewModelScope.launch {
            sessionRepository.getEarlierTranscripts(sessionId, oldest.createdAt, TRANSCRIPT_PAGE_SIZE)
                .onSuccess { page ->
                    knownTranscripts = page + knownTranscripts
                    _uiState.update {
                        it.copy(
                            transcripts = knownTranscripts.map { entry -> entry.toCompanionItem() },
                            hasEarlierTranscripts = page.size >= TRANSCRIPT_PAGE_SIZE,
                            isLoadingEarlierTranscripts = false,
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoadingEarlierTranscripts = false,
                            didEarlierTranscriptsFail = true,
                        )
                    }
                }
        }
    }

    /** Explanations answer the attendee's own selection, so they stay in the sheet and are not added to live concepts. */
    fun explainSelection(selectedItems: List<CompanionTranscriptItem>) {
        if (selectedItems.isEmpty()) return

        _uiState.update {
            it.copy(
                activeExplanation = ActiveExplanationRequest(
                    selectedItems = selectedItems,
                    isLoading = true,
                    error = null,
                ),
            )
        }

        viewModelScope.launch {
            val segmentIds = selectedItems.map { it.id }
            val selectedTexts = selectedItems.map { it.text }
            val speakerName = selectedItems.firstOrNull()?.speakerName.orEmpty()

            explanationRepository.explainSelection(sessionId, segmentIds, selectedTexts, speakerName)
                .onSuccess { explanation ->
                    _uiState.update {
                        it.copy(
                            activeExplanation = it.activeExplanation?.copy(
                                isLoading = false,
                                explanation = explanation,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            activeExplanation = it.activeExplanation?.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to generate explanation.",
                            ),
                        )
                    }
                }
        }
    }

    fun bookmarkConceptExplanation(concept: LiveConceptExplanationItem, sessionTitle: String = "") {
        val shouldBookmark = !concept.isBookmarked
        _uiState.update { state ->
            state.copy(
                conceptExplanations = state.conceptExplanations.map {
                    if (it.id == concept.id) it.copy(isBookmarked = shouldBookmark) else it
                },
            )
        }

        viewModelScope.launch {
            if (shouldBookmark) {
                glossaryRepository.saveTermToGlossary(
                    userUid = "",
                    sessionId = sessionId,
                    sessionTitle = sessionTitle,
                    term = GlossaryTermItem(
                        id = concept.id,
                        term = concept.title,
                        category = concept.type,
                        definition = concept.definition,
                        source = "transcript",
                        sessionId = sessionId,
                        sessionTitle = sessionTitle,
                    ),
                )
            } else {
                glossaryRepository.removeTermFromGlossary(userUid = "", sessionId = sessionId, termId = concept.id)
            }
        }
    }

    fun removeGlossaryTerm(term: GlossaryTermItem) {
        viewModelScope.launch {
            glossaryRepository.removeTermFromGlossary(userUid = "", sessionId = sessionId, termId = term.id)
        }
    }

    fun onConceptAnnouncementShown(conceptId: String) {
        _uiState.update {
            if (it.newConceptAnnouncement?.id == conceptId) it.copy(newConceptAnnouncement = null) else it
        }
    }

    fun dismissActiveExplanation() {
        _uiState.update { it.copy(activeExplanation = null) }
    }

    private fun TranscriptEntry.toCompanionItem() = CompanionTranscriptItem(
        id = id,
        speakerName = speakerName,
        timestamp = timestamp,
        text = text,
        isSystemEvent = isSystemEvent,
    )

    companion object {
        fun provideFactory(
            sessionId: String,
            sessionRepository: SessionRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SessionStudioViewModel(sessionId, sessionRepository) as T
            }
        }
    }
}
