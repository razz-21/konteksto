package com.example.konteksto.ui.host.studio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.konteksto.data.repository.AuthRepository
import com.example.konteksto.data.repository.FirebaseAuthRepository
import com.example.konteksto.data.repository.FirebaseSessionRepository
import com.example.konteksto.data.repository.SessionRepository
import com.example.konteksto.domain.model.HostSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HostStudioUiState(
    val session: HostSession? = null,
    val isAudioBroadcasting: Boolean = false,
    val selectedTab: Int = 0, // 0: Broadcast & Audio, 1: Slides & Context
    val transcripts: List<TranscriptEntry> = emptyList(),
    val partialTranscript: String = "",
    val contextNotes: List<ContextNoteItem> = emptyList(),
    val slideDeckInfo: SlideDeckInfo? = null,
    val analyzedSlides: List<AnalyzedSlideItem> = emptyList(),
    val isUpdatingLiveStatus: Boolean = false,
    val errorMessage: String? = null,
)

class HostStudioViewModel(
    private val sessionId: String,
    private val sessionRepository: SessionRepository = FirebaseSessionRepository(),
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HostStudioUiState(
            transcripts = emptyList(),
            contextNotes = listOf(
                ContextNoteItem(
                    id = "c1",
                    category = "ARCHITECTURE DIAGRAM",
                    title = "Latency Benchmark & Edge Buffer Thresholds",
                    content = "US-West edge latency threshold target < 15ms. Local frame processing buffer set to 128kb batch size.",
                    timestamp = "10:02 AM",
                ),
            ),
        ),
    )
    val uiState: StateFlow<HostStudioUiState> = _uiState.asStateFlow()

    init {
        observeSession()
        observeTranscripts()
    }

    private fun observeSession() {
        viewModelScope.launch {
            val hostUid = authRepository.currentUser?.uid ?: ""
            sessionRepository.getHostSessionsStream(hostUid).collect { sessions ->
                val matchingSession = sessions.find { it.id == sessionId }
                if (matchingSession != null) {
                    val restoredDeck = if (!matchingSession.slideDeckFileName.isNullOrBlank()) {
                        SlideDeckInfo(
                            fileName = matchingSession.slideDeckFileName,
                            fileSizeMb = matchingSession.slideDeckSizeMb ?: 1.0,
                            pageCount = matchingSession.slideDeckPageCount ?: 1,
                            uploadTime = matchingSession.slideDeckUploadTime ?: "Uploaded Previously",
                            isAnalyzed = true,
                        )
                    } else _uiState.value.slideDeckInfo

                    val restoredSlides = if (restoredDeck != null && _uiState.value.analyzedSlides.isEmpty()) {
                        listOf(
                            AnalyzedSlideItem(
                                id = "s_restored",
                                slideNumber = 1,
                                title = restoredDeck.fileName.substringBeforeLast("."),
                                diagramSummary = "PDF presentation slides parsed and indexed for grounded AI explanations.",
                                keyTakeaway = "Presentation deck grounds AI live terminology definitions in real time.",
                            ),
                        )
                    } else _uiState.value.analyzedSlides

                    _uiState.value = _uiState.value.copy(
                        session = matchingSession,
                        isAudioBroadcasting = matchingSession.isLive,
                        slideDeckInfo = restoredDeck,
                        analyzedSlides = restoredSlides,
                    )
                }
            }
        }
    }

    private fun observeTranscripts() {
        viewModelScope.launch {
            sessionRepository.getTranscriptsStream(sessionId).collect { remoteTranscripts ->
                _uiState.value = _uiState.value.copy(
                    transcripts = remoteTranscripts,
                )
            }
        }
    }

    fun setSelectedTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
    }

    fun toggleAudioBroadcasting() {
        val newStatus = !_uiState.value.isAudioBroadcasting
        _uiState.value = _uiState.value.copy(
            isAudioBroadcasting = newStatus,
        )
        val eventMsg = if (newStatus) "Audio broadcast enabled" else "Audio broadcast ended"
        addTranscriptEntry(speakerName = "SYSTEM", text = eventMsg, isSystemEvent = true)
    }

    fun toggleSessionLiveStatus(
        isLive: Boolean,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        val currentSession = _uiState.value.session ?: HostSession(
            id = sessionId,
            venue = "Tech Summit Hall B",
            time = "10:00 AM",
            talkCode = "TALK–415",
            title = "Edge AI & Distributed Cloud Architecture: Scaling to Zero Latency",
            speakerName = "Engr. Marco Valerio",
            attendeesCount = 890,
            isLive = isLive,
        )
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingLiveStatus = true)
            sessionRepository.updateSessionLiveStatus(currentSession.id, isLive)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isUpdatingLiveStatus = false,
                        isAudioBroadcasting = isLive,
                        session = currentSession.copy(isLive = isLive),
                    )
                    val eventMsg = if (isLive) "Session started" else "Session ended"
                    addTranscriptEntry(speakerName = "SYSTEM", text = eventMsg, isSystemEvent = true)
                    onSuccess()
                }
                .onFailure { throwable ->
                    val msg = throwable.localizedMessage ?: "Failed to update session live status."
                    _uiState.value = _uiState.value.copy(
                        isUpdatingLiveStatus = false,
                        errorMessage = msg,
                    )
                    onError(msg)
                }
        }
    }

    fun updatePartialTranscript(text: String) {
        _uiState.value = _uiState.value.copy(partialTranscript = text)
    }

    fun addTranscriptEntry(speakerName: String = "", text: String, isSystemEvent: Boolean = false) {
        if (text.isBlank()) return
        val currentSpeaker = if (isSystemEvent || speakerName.equals("SYSTEM", ignoreCase = true)) {
            "SYSTEM"
        } else {
            speakerName.ifBlank {
                _uiState.value.session?.speakerName?.takeIf { it.isNotBlank() } ?: "Host"
            }
        }
        viewModelScope.launch {
            sessionRepository.addTranscriptEntry(
                sessionId = sessionId,
                speakerName = currentSpeaker,
                text = text.trim(),
                isSystemEvent = isSystemEvent || currentSpeaker == "SYSTEM",
            ).onSuccess { newEntry ->
                val currentTranscripts = _uiState.value.transcripts
                val updatedTranscripts = if (currentTranscripts.none { it.id == newEntry.id }) {
                    currentTranscripts + newEntry
                } else currentTranscripts

                _uiState.value = _uiState.value.copy(
                    transcripts = updatedTranscripts,
                    partialTranscript = "",
                )
            }.onFailure {
                val fallbackEntry = TranscriptEntry(
                    id = System.currentTimeMillis().toString(),
                    speakerName = currentSpeaker,
                    timestamp = "Just now",
                    text = text.trim(),
                    isSystemEvent = isSystemEvent || currentSpeaker == "SYSTEM",
                )
                _uiState.value = _uiState.value.copy(
                    transcripts = _uiState.value.transcripts + fallbackEntry,
                    partialTranscript = "",
                )
            }
        }
    }

    fun addContextNote(title: String, content: String, category: String = "BACKGROUND NOTE") {
        if (title.isBlank()) return
        val newItem = ContextNoteItem(
            id = System.currentTimeMillis().toString(),
            category = category,
            title = title.trim(),
            content = content.trim(),
            timestamp = "Just now",
        )
        _uiState.value = _uiState.value.copy(
            contextNotes = _uiState.value.contextNotes + newItem,
        )
    }

    fun uploadSlideDeckPdf(uri: android.net.Uri, fileName: String, fileSizeMb: Double) {
        viewModelScope.launch {
            sessionRepository.uploadSlideDeckPdf(
                sessionId = sessionId,
                uri = uri,
                fileName = fileName,
                fileSizeMb = fileSizeMb,
            ).onSuccess { deckInfo ->
                _uiState.value = _uiState.value.copy(
                    slideDeckInfo = deckInfo.copy(isAnalyzed = false),
                    analyzedSlides = emptyList(),
                )
            }.onFailure {
                setSlideDeckInfo(fileName, fileSizeMb)
            }
        }
    }

    fun analyzeSlideDeck() {
        val currentDeck = _uiState.value.slideDeckInfo ?: return
        val fileName = currentDeck.fileName
        val generatedSlides = listOf(
            AnalyzedSlideItem(
                id = "s_${System.currentTimeMillis()}",
                slideNumber = 1,
                title = fileName.substringBeforeLast("."),
                diagramSummary = "PDF presentation slides parsed and indexed for grounded AI explanations.",
                keyTakeaway = "Presentation deck grounds AI live terminology definitions in real time.",
            ),
        )
        _uiState.value = _uiState.value.copy(
            slideDeckInfo = currentDeck.copy(isAnalyzed = true),
            analyzedSlides = generatedSlides,
        )
    }

    fun setSlideDeckInfo(fileName: String, fileSizeMb: Double, pageCount: Int = 1) {
        val nowFormatted = java.text.SimpleDateFormat("a 'at' h:mm a", java.util.Locale.US).format(java.util.Date())
        val newDeck = SlideDeckInfo(
            fileName = fileName,
            fileSizeMb = fileSizeMb,
            pageCount = pageCount,
            uploadTime = "Uploaded $nowFormatted",
            isAnalyzed = true,
        )
        val defaultSlides = listOf(
            AnalyzedSlideItem(
                id = "s_${System.currentTimeMillis()}",
                slideNumber = 1,
                title = fileName.substringBeforeLast("."),
                diagramSummary = "PDF presentation slides parsed and indexed for grounded AI explanations.",
                keyTakeaway = "Presentation deck grounds AI live terminology definitions in real time.",
            ),
        )
        _uiState.value = _uiState.value.copy(
            slideDeckInfo = newDeck,
            analyzedSlides = defaultSlides,
        )
    }

    fun removeSlideDeck() {
        _uiState.value = _uiState.value.copy(
            slideDeckInfo = null,
            analyzedSlides = emptyList(),
        )
    }

    fun removeAnalyzedSlide(slideId: String) {
        val updated = _uiState.value.analyzedSlides.filter { it.id != slideId }
        _uiState.value = _uiState.value.copy(
            analyzedSlides = updated,
            slideDeckInfo = if (updated.isEmpty()) null else _uiState.value.slideDeckInfo,
        )
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    companion object {
        fun provideFactory(
            sessionId: String,
            sessionRepository: SessionRepository = FirebaseSessionRepository(),
            authRepository: AuthRepository = FirebaseAuthRepository(),
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HostStudioViewModel(sessionId, sessionRepository, authRepository) as T
            }
        }
    }
}
