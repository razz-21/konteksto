package com.example.konteksto.ui.sessions.studio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.konteksto.data.repository.FirebaseGlossaryRepository
import com.example.konteksto.data.repository.GlossaryRepository
import com.example.konteksto.data.repository.GlossaryTermItem
import com.example.konteksto.data.repository.SessionRepository
import com.example.konteksto.ui.host.studio.TranscriptEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.transformLatest
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionStudioUiState())
    val uiState: StateFlow<SessionStudioUiState> = _uiState.asStateFlow()

    private var knownTranscripts = emptyList<TranscriptEntry>()
    private var observeJob: Job? = null
    private var lastDetectedTranscriptCount = 0

    /** Live updates only run while the screen is visible, so a backgrounded attendee costs no reads. */
    fun startObserving() {
        if (observeJob?.isActive == true) return
        _uiState.value = _uiState.value.copy(didLiveTranscriptFail = false)
        observeJob = viewModelScope.launch {
            launch {
                sessionRepository.getTranscriptsStream(sessionId, limit = TRANSCRIPT_PAGE_SIZE)
                    .catch { _uiState.value = _uiState.value.copy(didLiveTranscriptFail = true) }
                    .collect(::onTranscriptWindow)
            }
            launch {
                sessionRepository.getLivePartialTranscriptStream(sessionId)
                    .expireStalePartial()
                    .collect { text ->
                        _uiState.value = _uiState.value.copy(livePartialText = text)
                    }
            }
            launch {
                glossaryRepository.getSessionGlossaryStream(sessionId)
                    .collect { glossaryTerms ->
                        _uiState.value = _uiState.value.copy(sessionGlossaryTerms = glossaryTerms)
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
        _uiState.value = _uiState.value.copy(
            transcripts = companionItems,
            hasEarlierTranscripts = _uiState.value.hasEarlierTranscripts || window.size >= TRANSCRIPT_PAGE_SIZE,
        )

        // Trigger concept detection when new spoken transcript lines arrive during talk
        if (knownTranscripts.size > lastDetectedTranscriptCount && knownTranscripts.any { !it.isSystemEvent }) {
            lastDetectedTranscriptCount = knownTranscripts.size
            triggerLiveConceptDetection()
        }
    }

    private fun triggerLiveConceptDetection() {
        viewModelScope.launch {
            explanationRepository.detectLiveConcepts(sessionId, knownTranscripts)
                .onSuccess { detectedConcepts ->
                    if (detectedConcepts.isNotEmpty()) {
                        val existing = _uiState.value.conceptExplanations
                        val merged = (detectedConcepts + existing).distinctBy { it.title.lowercase().trim() }
                        _uiState.value = _uiState.value.copy(conceptExplanations = merged)
                    }
                }
        }
    }

    fun loadEarlierTranscripts() {
        val oldest = knownTranscripts.firstOrNull() ?: return
        if (_uiState.value.isLoadingEarlierTranscripts) return
        _uiState.value = _uiState.value.copy(
            isLoadingEarlierTranscripts = true,
            didEarlierTranscriptsFail = false,
        )
        viewModelScope.launch {
            sessionRepository.getEarlierTranscripts(sessionId, oldest.createdAt, TRANSCRIPT_PAGE_SIZE)
                .onSuccess { page ->
                    knownTranscripts = page + knownTranscripts
                    _uiState.value = _uiState.value.copy(
                        transcripts = knownTranscripts.map { it.toCompanionItem() },
                        hasEarlierTranscripts = page.size >= TRANSCRIPT_PAGE_SIZE,
                        isLoadingEarlierTranscripts = false,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoadingEarlierTranscripts = false,
                        didEarlierTranscriptsFail = true,
                    )
                }
        }
    }

    fun explainSelection(selectedItems: List<CompanionTranscriptItem>) {
        if (selectedItems.isEmpty()) return

        _uiState.value = _uiState.value.copy(
            activeExplanation = ActiveExplanationRequest(
                selectedItems = selectedItems,
                isLoading = true,
                error = null,
            ),
        )

        viewModelScope.launch {
            val segmentIds = selectedItems.map { it.id }
            val selectedTexts = selectedItems.map { it.text }
            val speakerName = selectedItems.firstOrNull()?.speakerName.orEmpty()

            explanationRepository.explainSelection(sessionId, segmentIds, selectedTexts, speakerName)
                .onSuccess { explanation ->
                    val updatedConcepts = (listOf(explanation) + _uiState.value.conceptExplanations)
                        .distinctBy { it.title.lowercase().trim() }
                    _uiState.value = _uiState.value.copy(
                        activeExplanation = _uiState.value.activeExplanation?.copy(
                            isLoading = false,
                            explanation = explanation,
                        ),
                        conceptExplanations = updatedConcepts,
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        activeExplanation = _uiState.value.activeExplanation?.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to generate explanation.",
                        ),
                    )
                }
        }
    }

    fun bookmarkConceptExplanation(concept: LiveConceptExplanationItem, sessionTitle: String = "") {
        viewModelScope.launch {
            val termId = concept.id.ifBlank { "term_${concept.title.lowercase().replace(" ", "_")}" }
            val termItem = GlossaryTermItem(
                id = termId,
                term = concept.title,
                category = concept.type,
                definition = concept.definition,
                source = "transcript",
                sessionId = sessionId,
                sessionTitle = sessionTitle,
                isBookmarked = !concept.isBookmarked,
            )

            if (concept.isBookmarked) {
                glossaryRepository.removeTermFromGlossary(userUid = "", sessionId = sessionId, termId = termId)
            } else {
                glossaryRepository.saveTermToGlossary(
                    userUid = "",
                    sessionId = sessionId,
                    sessionTitle = sessionTitle,
                    term = termItem,
                )
            }

            val updatedExplanations = _uiState.value.conceptExplanations.map {
                if (it.id == concept.id) it.copy(isBookmarked = !it.isBookmarked) else it
            }
            _uiState.value = _uiState.value.copy(conceptExplanations = updatedExplanations)
        }
    }

    fun toggleBookmarkGlossaryTerm(term: GlossaryTermItem, sessionTitle: String = "") {
        viewModelScope.launch {
            if (term.isBookmarked) {
                glossaryRepository.removeTermFromGlossary(userUid = "", sessionId = sessionId, termId = term.id)
            } else {
                glossaryRepository.saveTermToGlossary(
                    userUid = "",
                    sessionId = sessionId,
                    sessionTitle = sessionTitle,
                    term = term,
                )
            }
        }
    }

    fun dismissActiveExplanation() {
        _uiState.value = _uiState.value.copy(activeExplanation = null)
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
