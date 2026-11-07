package com.example.konteksto.ui.host.studio

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.konteksto.data.audio.AndroidAudioInputRepository
import com.example.konteksto.data.audio.AudioInputRepository
import com.example.konteksto.data.repository.AuthRepository
import com.example.konteksto.data.repository.FirebaseAuthRepository
import com.example.konteksto.data.repository.FirebaseSessionRepository
import com.example.konteksto.data.repository.GeminiSlideAnalyzer
import com.example.konteksto.data.repository.SessionRepository
import com.example.konteksto.domain.model.AudioInputDevice
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.components.SessionLookup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HostStudioUiState(
    val sessionLookup: SessionLookup = SessionLookup.Loading,
    val isAudioBroadcasting: Boolean = false,
    val selectedTab: Int = 0, // 0: Broadcast & Audio, 1: Slides & Context
    val transcripts: List<TranscriptEntry> = emptyList(),
    val partialTranscript: String = "",
    val inputDevice: AudioInputDevice? = null,
    val contextNotes: List<ContextNoteItem> = emptyList(),
    val slideDeckInfo: SlideDeckInfo? = null,
    val analyzedSlides: List<AnalyzedSlideItem> = emptyList(),
    val domainTerms: List<DomainTermItem> = emptyList(),
    val isUploadingPdf: Boolean = false,
    val isAnalyzingPdf: Boolean = false,
    val isUpdatingLiveStatus: Boolean = false,
    val errorMessage: String? = null,
    val areContextNotesLoaded: Boolean = false,
) {
    val session: HostSession?
        get() = (sessionLookup as? SessionLookup.Found)?.session

    /** True when the analyzed slides and terms were produced from different notes than the current ones. */
    val hasUnappliedContextNotes: Boolean
        get() = areContextNotesLoaded &&
            slideDeckInfo?.isAnalyzed == true &&
            session?.analyzedContextNotesSignature.orEmpty() != contextNotesSignature(contextNotes)
}

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

/**
 * The host can turn the mic off during a live session, and the session document also changes for
 * unrelated reasons such as saving slides, so only starting or ending the session resets the mic.
 */
internal fun resolveAudioBroadcasting(
    isAudioBroadcasting: Boolean,
    previousSession: HostSession?,
    session: HostSession,
): Boolean = if (previousSession?.isLive == session.isLive) isAudioBroadcasting else session.isLive

private const val LIVE_PARTIAL_PUBLISH_INTERVAL_MS = 1_000L

/**
 * Publishes the latest in-progress text at most once per [intervalMs]. Blank text is skipped because
 * saving the finished line already clears it, in the same write.
 */
internal suspend fun StateFlow<String>.publishThrottled(
    intervalMs: Long,
    publish: suspend (String) -> Unit,
) {
    collect { text ->
        if (text.isNotBlank()) {
            publish(text)
            delay(intervalMs)
        }
    }
}

class HostStudioViewModel(
    private val sessionId: String,
    transcriptionEngineFactory: (onSegmentComplete: (String) -> Unit) -> TranscriptionEngine,
    private val audioInputRepository: AudioInputRepository,
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
    private var observeSessionJob: Job? = null
    private var hasReportedLivePartialFailure = false

    // Leaving the studio cancels viewModelScope, and the words still in progress must be saved after that.
    private val transcriptWriteScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val transcriptionEngine = transcriptionEngineFactory { segment ->
        addTranscriptEntry(text = segment, clearsLivePartial = true)
    }

    init {
        observeSession()
        observeTranscripts()
        observeLivePartial()
        observeAnalyzedSlides()
        observeContextNotes()
        observeDomainTerms()
        observeInputDevice()
    }

    private fun observeInputDevice() {
        viewModelScope.launch {
            audioInputRepository.currentInputDevice
                .catch { /* Without a device list the transcript header just leaves the mic name out. */ }
                .collect { device -> _uiState.update { it.copy(inputDevice = device) } }
        }
    }

    private fun observeDomainTerms() {
        viewModelScope.launch {
            sessionRepository.getDomainTermsStream(sessionId).collect { remoteTerms ->
                _uiState.value = _uiState.value.copy(domainTerms = remoteTerms)
            }
        }
    }

    private fun observeContextNotes() {
        viewModelScope.launch {
            sessionRepository.getContextNotesStream(sessionId).collect { remoteNotes ->
                _uiState.value = _uiState.value.copy(
                    contextNotes = remoteNotes,
                    areContextNotesLoaded = true,
                )
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
        observeSessionJob?.cancel()
        observeSessionJob = viewModelScope.launch {
            val hostUid = authRepository.currentUser?.uid
            sessionRepository.getSessionStream(sessionId)
                .catch {
                    // A session already on screen stays there; only a first load that fails is shown as one.
                    _uiState.update { state ->
                        if (state.sessionLookup is SessionLookup.Found) state else state.copy(sessionLookup = SessionLookup.Failed)
                    }
                }
                .collect { remoteSession ->
                    // Only the session's own host can run its studio.
                    val session = remoteSession?.takeIf { hostUid != null && it.hostUid == hostUid }
                    if (session == null) {
                        _uiState.update { it.copy(sessionLookup = SessionLookup.NotFound) }
                        setAudioBroadcasting(false)
                        return@collect
                    }
                    val isAudioBroadcasting = resolveAudioBroadcasting(
                        isAudioBroadcasting = _uiState.value.isAudioBroadcasting,
                        previousSession = _uiState.value.session,
                        session = session,
                    )
                    _uiState.update {
                        it.copy(
                            sessionLookup = SessionLookup.Found(session),
                            slideDeckInfo = resolveSlideDeck(it.slideDeckInfo, session),
                        )
                    }
                    setAudioBroadcasting(isAudioBroadcasting)
                }
        }
    }

    fun retrySessionLookup() {
        _uiState.update { it.copy(sessionLookup = SessionLookup.Loading) }
        observeSession()
    }

    private fun observeTranscripts() {
        viewModelScope.launch {
            sessionRepository.getTranscriptsStream(sessionId)
                .catch {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Couldn't load the live transcript. Reopen this session to try again.",
                    )
                }
                .collect { remoteTranscripts ->
                    _uiState.value = _uiState.value.copy(
                        transcripts = remoteTranscripts,
                    )
                }
        }
    }

    private fun observeLivePartial() {
        viewModelScope.launch {
            transcriptionEngine.partialText.collect { text ->
                _uiState.value = _uiState.value.copy(partialTranscript = text)
            }
        }
        viewModelScope.launch {
            transcriptionEngine.partialText.publishThrottled(LIVE_PARTIAL_PUBLISH_INTERVAL_MS) { text ->
                sessionRepository.updateLivePartialTranscript(sessionId, text).onFailure {
                    if (!hasReportedLivePartialFailure) {
                        hasReportedLivePartialFailure = true
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Attendees can't see your words as you speak. " +
                                "They'll still get each line once you finish it.",
                        )
                    }
                }
            }
        }
    }

    /**
     * The engine saves any words in progress when it stops, so this runs before logging the stop.
     * The Bluetooth route is set before listening starts, so the first words already use the headset.
     */
    private fun setAudioBroadcasting(isAudioBroadcasting: Boolean) {
        if (_uiState.value.isAudioBroadcasting == isAudioBroadcasting) return
        _uiState.value = _uiState.value.copy(isAudioBroadcasting = isAudioBroadcasting)
        if (isAudioBroadcasting) {
            audioInputRepository.setBluetoothMicEnabled(true)
            transcriptionEngine.startListening()
        } else {
            transcriptionEngine.stopListening()
            audioInputRepository.setBluetoothMicEnabled(false)
        }
    }

    override fun onCleared() {
        transcriptionEngine.stopListening()
        audioInputRepository.setBluetoothMicEnabled(false)
    }

    fun setSelectedTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
    }

    fun toggleAudioBroadcasting() {
        val newStatus = !_uiState.value.isAudioBroadcasting
        setAudioBroadcasting(newStatus)
        val eventMsg = if (newStatus) "Audio broadcast enabled" else "Audio broadcast ended"
        addTranscriptEntry(speakerName = "SYSTEM", text = eventMsg, isSystemEvent = true)
    }

    fun toggleSessionLiveStatus(
        isLive: Boolean,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        val currentSession = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingLiveStatus = true)
            sessionRepository.updateSessionLiveStatus(currentSession.id, isLive)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isUpdatingLiveStatus = false,
                            sessionLookup = SessionLookup.Found(currentSession.copy(isLive = isLive)),
                        )
                    }
                    setAudioBroadcasting(isLive)
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

    fun addTranscriptEntry(
        speakerName: String = "",
        text: String,
        isSystemEvent: Boolean = false,
        clearsLivePartial: Boolean = false,
    ) {
        if (text.isBlank()) return
        val currentSpeaker = if (isSystemEvent || speakerName.equals("SYSTEM", ignoreCase = true)) {
            "SYSTEM"
        } else {
            speakerName.ifBlank {
                _uiState.value.session?.speakerName?.takeIf { it.isNotBlank() } ?: "Host"
            }
        }
        transcriptWriteScope.launch {
            sessionRepository.addTranscriptEntry(
                sessionId = sessionId,
                speakerName = currentSpeaker,
                text = text.trim(),
                isSystemEvent = isSystemEvent || currentSpeaker == "SYSTEM",
                clearsLivePartial = clearsLivePartial,
            ).onFailure {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Couldn't publish a transcript line, so attendees won't see it.",
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
        val nowFormatted = java.text.SimpleDateFormat("'at' h:mm a", java.util.Locale.US).format(java.util.Date())
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
            val notesUsed = _uiState.value.contextNotes
            val notesSignature = contextNotesSignature(notesUsed)

            geminiSlideAnalyzer.analyzeSlideDeck(
                slideDeckInfo = deckWithPdf,
                sessionTitle = currentSession?.title ?: "",
                category = currentSession?.category ?: "",
                contextNotes = notesUsed,
            ).onSuccess { analysis ->
                _uiState.value = _uiState.value.copy(
                    slideDeckInfo = deckWithPdf.copy(isAnalyzed = true),
                    analyzedSlides = analysis.slides,
                    domainTerms = analysis.terms,
                    isAnalyzingPdf = false,
                    sessionLookup = _uiState.value.session
                        ?.let { SessionLookup.Found(it.copy(analyzedContextNotesSignature = notesSignature)) }
                        ?: _uiState.value.sessionLookup,
                )
                // Saved first so the session updates from the writes below already carry it.
                val signatureSaved = sessionRepository
                    .saveAnalyzedContextNotesSignature(sessionId, notesSignature).isSuccess
                val slidesSaved = sessionRepository.saveAnalyzedSlides(sessionId, analysis.slides).isSuccess
                val termsSaved = sessionRepository.saveDomainTerms(sessionId, analysis.terms).isSuccess
                if (!signatureSaved || !slidesSaved || !termsSaved) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "The analysis finished, but it couldn't be saved and may be lost when " +
                            "you leave this screen. Check your connection and analyze again.",
                    )
                }
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
            domainTerms = emptyList(),
        )
        removeSlideDeckJob = viewModelScope.launch {
            sessionRepository.clearSlideDeck(sessionId, fileName).onFailure {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Couldn't remove '$fileName'. Check your connection and try again.",
                )
                return@launch
            }
            sessionRepository.saveAnalyzedSlides(sessionId, emptyList())
            sessionRepository.saveDomainTerms(sessionId, emptyList())
        }
    }

    fun updateDomainTerm(editedTerm: DomainTermItem) {
        val originalTerm = _uiState.value.domainTerms.find { it.id == editedTerm.id } ?: return
        _uiState.value = _uiState.value.copy(
            domainTerms = _uiState.value.domainTerms
                .map { if (it.id == editedTerm.id) editedTerm else it }
                .sortedBy { it.term.lowercase() },
        )
        viewModelScope.launch {
            sessionRepository.updateDomainTerm(sessionId, editedTerm).onFailure {
                _uiState.value = _uiState.value.copy(
                    domainTerms = _uiState.value.domainTerms
                        .map { if (it.id == originalTerm.id) originalTerm else it }
                        .sortedBy { it.term.lowercase() },
                    errorMessage = "Couldn't save your changes to '${originalTerm.term}'. " +
                        "Check your connection and try again.",
                )
            }
        }
    }

    fun deleteDomainTerm(termId: String) {
        val deletedTerm = _uiState.value.domainTerms.find { it.id == termId } ?: return
        _uiState.value = _uiState.value.copy(
            domainTerms = _uiState.value.domainTerms.filter { it.id != termId },
        )
        viewModelScope.launch {
            sessionRepository.deleteDomainTerm(sessionId, termId).onFailure {
                val currentTerms = _uiState.value.domainTerms
                _uiState.value = _uiState.value.copy(
                    domainTerms = if (currentTerms.any { it.id == termId }) {
                        currentTerms
                    } else {
                        (currentTerms + deletedTerm).sortedBy { it.term.lowercase() }
                    },
                    errorMessage = "Couldn't delete '${deletedTerm.term}'. Check your connection and try again.",
                )
            }
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
            context: Context,
            sessionId: String,
            sessionRepository: SessionRepository = FirebaseSessionRepository(),
            authRepository: AuthRepository = FirebaseAuthRepository(),
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            private val appContext = context.applicationContext

            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HostStudioViewModel(
                    sessionId = sessionId,
                    transcriptionEngineFactory = { onSegmentComplete ->
                        SpeechToTextManager(appContext, onSegmentComplete)
                    },
                    audioInputRepository = AndroidAudioInputRepository(appContext),
                    sessionRepository = sessionRepository,
                    authRepository = authRepository,
                ) as T
            }
        }
    }
}
