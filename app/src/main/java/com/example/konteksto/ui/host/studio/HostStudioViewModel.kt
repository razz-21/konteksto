package com.example.konteksto.ui.host.studio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.konteksto.data.repository.AuthRepository
import com.example.konteksto.data.repository.FirebaseAuthRepository
import com.example.konteksto.data.repository.FirebaseSessionRepository
import com.example.konteksto.data.repository.GeminiSlideAnalyzer
import com.example.konteksto.data.repository.SessionRepository
import com.example.konteksto.domain.model.HostSession
import kotlinx.coroutines.Job
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
    val isUploadingPdf: Boolean = false,
    val isAnalyzingPdf: Boolean = false,
    val isUpdatingLiveStatus: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * The in-memory deck holds the PDF bytes and the latest analysis state, neither of which a session
 * update carries, so it is only replaced when the session points at a different file.
 */
internal fun resolveSlideDeck(currentDeck: SlideDeckInfo?, session: HostSession): SlideDeckInfo? {
    val fileName = session.slideDeckFileName
    if (fileName.isNullOrBlank() || currentDeck?.fileName == fileName) return currentDeck
    return SlideDeckInfo(
        fileName = fileName,
        fileSizeMb = session.slideDeckSizeMb ?: 1.0,
        pageCount = session.slideDeckPageCount ?: 1,
        uploadTime = session.slideDeckUploadTime ?: "Uploaded Previously",
        isAnalyzed = session.isSlideDeckAnalyzed,
    )
}

class HostStudioViewModel(
    private val sessionId: String,
    private val sessionRepository: SessionRepository = FirebaseSessionRepository(),
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val geminiSlideAnalyzer: GeminiSlideAnalyzer = GeminiSlideAnalyzer(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HostStudioUiState(
            transcripts = emptyList(),
        ),
    )
    val uiState: StateFlow<HostStudioUiState> = _uiState.asStateFlow()

    private var removeSlideDeckJob: Job? = null

    init {
        observeSession()
        observeTranscripts()
        observeAnalyzedSlides()
        observeContextNotes()
    }

    private fun observeContextNotes() {
        viewModelScope.launch {
            sessionRepository.getContextNotesStream(sessionId).collect { remoteNotes ->
                _uiState.value = _uiState.value.copy(contextNotes = remoteNotes)
            }
        }
    }

    private fun observeAnalyzedSlides() {
        viewModelScope.launch {
            sessionRepository.getAnalyzedSlidesStream(sessionId).collect { remoteSlides ->
                if (remoteSlides.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        analyzedSlides = remoteSlides,
                        slideDeckInfo = _uiState.value.slideDeckInfo?.copy(isAnalyzed = true),
                    )
                }
            }
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            val hostUid = authRepository.currentUser?.uid ?: ""
            sessionRepository.getHostSessionsStream(hostUid).collect { sessions ->
                val matchingSession = sessions.find { it.id == sessionId }
                if (matchingSession != null) {
                    _uiState.value = _uiState.value.copy(
                        session = matchingSession,
                        isAudioBroadcasting = matchingSession.isLive,
                        slideDeckInfo = resolveSlideDeck(_uiState.value.slideDeckInfo, matchingSession),
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
            id = "note_${System.currentTimeMillis()}",
            category = category,
            title = title.trim(),
            content = content.trim(),
            timestamp = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US).format(java.util.Date()),
        )
        _uiState.value = _uiState.value.copy(
            contextNotes = _uiState.value.contextNotes + newItem,
        )
        viewModelScope.launch {
            sessionRepository.addContextNote(sessionId, newItem).onFailure {
                _uiState.value = _uiState.value.copy(
                    contextNotes = _uiState.value.contextNotes.filter { it.id != newItem.id },
                    errorMessage = "Couldn't save the note '${newItem.title}'. Check your connection and try again.",
                )
            }
        }
    }

    fun updateContextNote(editedNote: ContextNoteItem) {
        val originalNote = _uiState.value.contextNotes.find { it.id == editedNote.id } ?: return
        _uiState.value = _uiState.value.copy(
            contextNotes = _uiState.value.contextNotes.map { if (it.id == editedNote.id) editedNote else it },
        )
        viewModelScope.launch {
            sessionRepository.updateContextNote(sessionId, editedNote).onFailure {
                _uiState.value = _uiState.value.copy(
                    contextNotes = _uiState.value.contextNotes.map {
                        if (it.id == originalNote.id) originalNote else it
                    },
                    errorMessage = "Couldn't save your changes to '${originalNote.title}'. " +
                        "Check your connection and try again.",
                )
            }
        }
    }

    fun deleteContextNote(noteId: String) {
        val notes = _uiState.value.contextNotes
        val index = notes.indexOfFirst { it.id == noteId }
        if (index == -1) return
        val deletedNote = notes[index]
        _uiState.value = _uiState.value.copy(contextNotes = notes.filter { it.id != noteId })
        viewModelScope.launch {
            sessionRepository.deleteContextNote(sessionId, noteId).onFailure {
                val currentNotes = _uiState.value.contextNotes
                _uiState.value = _uiState.value.copy(
                    contextNotes = if (currentNotes.any { it.id == noteId }) {
                        currentNotes
                    } else {
                        currentNotes.toMutableList().apply { add(index.coerceAtMost(size), deletedNote) }
                    },
                    errorMessage = "Couldn't delete '${deletedNote.title}'. Check your connection and try again.",
                )
            }
        }
    }

    fun uploadSlideDeckPdf(
        uri: android.net.Uri,
        fileName: String,
        fileSizeMb: Double,
        pageCount: Int = 1,
        pdfBytes: ByteArray? = null,
    ) {
        val nowFormatted = java.text.SimpleDateFormat("a 'at' h:mm a", java.util.Locale.US).format(java.util.Date())
        val deckInfo = SlideDeckInfo(
            fileName = fileName,
            fileSizeMb = fileSizeMb,
            pageCount = pageCount,
            uploadTime = "Uploaded $nowFormatted",
            isAnalyzed = false,
            pdfBytes = pdfBytes,
        )
        _uiState.value = _uiState.value.copy(
            slideDeckInfo = deckInfo,
            analyzedSlides = emptyList(),
            isUploadingPdf = true,
        )
        viewModelScope.launch {
            // A pending removal deletes the stored file, which could be this same path.
            removeSlideDeckJob?.join()
            sessionRepository.uploadSlideDeckPdf(
                sessionId = sessionId,
                uri = uri,
                fileName = fileName,
                fileSizeMb = fileSizeMb,
                pageCount = pageCount,
            ).onSuccess { repoDeck ->
                _uiState.value = _uiState.value.copy(
                    slideDeckInfo = repoDeck.copy(
                        pageCount = pageCount,
                        isAnalyzed = false,
                        pdfBytes = pdfBytes,
                    ),
                    isUploadingPdf = false,
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    slideDeckInfo = null,
                    isUploadingPdf = false,
                    errorMessage = "Couldn't upload '$fileName'. Check your connection and try again.",
                )
            }
        }
    }

    fun analyzeSlideDeck() {
        val currentDeck = _uiState.value.slideDeckInfo ?: return
        val currentSession = _uiState.value.session
        _uiState.value = _uiState.value.copy(isAnalyzingPdf = true)
        viewModelScope.launch {
            val pdfBytes = currentDeck.pdfBytes?.takeIf { it.isNotEmpty() }
                ?: sessionRepository.downloadSlideDeckPdf(sessionId, currentDeck.fileName).getOrElse {
                    _uiState.value = _uiState.value.copy(
                        isAnalyzingPdf = false,
                        errorMessage = "Couldn't load '${currentDeck.fileName}', so it wasn't analyzed. " +
                            "Check your connection and try again. If this keeps happening, remove the deck and upload it again.",
                    )
                    return@launch
                }
            val deckWithPdf = currentDeck.copy(pdfBytes = pdfBytes)

            geminiSlideAnalyzer.analyzeSlideDeck(
                slideDeckInfo = deckWithPdf,
                sessionTitle = currentSession?.title ?: "",
                category = currentSession?.category ?: "",
                contextNotes = _uiState.value.contextNotes,
            ).onSuccess { generatedSlides ->
                _uiState.value = _uiState.value.copy(
                    slideDeckInfo = deckWithPdf.copy(isAnalyzed = true),
                    analyzedSlides = generatedSlides,
                    isAnalyzingPdf = false,
                )
                sessionRepository.saveAnalyzedSlides(sessionId, generatedSlides)
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    slideDeckInfo = deckWithPdf,
                    isAnalyzingPdf = false,
                    errorMessage = "Couldn't analyze '${currentDeck.fileName}', so nothing was saved. Please try again.",
                )
            }
        }
    }

    fun removeSlideDeck() {
        val fileName = _uiState.value.slideDeckInfo?.fileName
        _uiState.value = _uiState.value.copy(
            slideDeckInfo = null,
            analyzedSlides = emptyList(),
        )
        removeSlideDeckJob = viewModelScope.launch {
            sessionRepository.clearSlideDeck(sessionId, fileName).onFailure {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Couldn't remove '$fileName'. Check your connection and try again.",
                )
                return@launch
            }
            sessionRepository.saveAnalyzedSlides(sessionId, emptyList())
        }
    }

    fun updateAnalyzedSlide(editedSlide: AnalyzedSlideItem) {
        val originalSlide = _uiState.value.analyzedSlides.find { it.id == editedSlide.id } ?: return
        val updated = _uiState.value.analyzedSlides.map { if (it.id == editedSlide.id) editedSlide else it }
        _uiState.value = _uiState.value.copy(analyzedSlides = updated)
        viewModelScope.launch {
            sessionRepository.saveAnalyzedSlides(sessionId, updated).onFailure {
                _uiState.value = _uiState.value.copy(
                    analyzedSlides = _uiState.value.analyzedSlides.map {
                        if (it.id == originalSlide.id) originalSlide else it
                    },
                    errorMessage = "Couldn't save your changes to Slide ${originalSlide.slideNumber}. " +
                        "Check your connection and try again.",
                )
            }
        }
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
