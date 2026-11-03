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
    val contextNotes: List<ContextNoteItem> = emptyList(),
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
            transcripts = listOf(
                TranscriptEntry(
                    id = "1",
                    speakerName = "Engr. Marco Valerio",
                    timestamp = "10:05 AM",
                    text = "Notice our diagram on screen. If your seminar app has to hit a server in US-West for every spoken word, your users experience 400ms lag. In live talks, that breaks the flow.",
                ),
                TranscriptEntry(
                    id = "2",
                    speakerName = "Engr. Marco Valerio",
                    timestamp = "12:43 PM",
                    text = "Notice how memory consumption scales with batch size during continuous live streaming.",
                ),
            ),
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
                    )
                }
            }
        }
    }

    fun setSelectedTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
    }

    fun toggleAudioBroadcasting() {
        _uiState.value = _uiState.value.copy(
            isAudioBroadcasting = !_uiState.value.isAudioBroadcasting,
        )
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

    fun addTranscriptEntry(speakerName: String = "", text: String) {
        if (text.isBlank()) return
        val currentSpeaker = speakerName.ifBlank {
            _uiState.value.session?.speakerName?.takeIf { it.isNotBlank() } ?: "Host"
        }
        val newEntry = TranscriptEntry(
            id = System.currentTimeMillis().toString(),
            speakerName = currentSpeaker,
            timestamp = "Just now",
            text = text.trim(),
        )
        _uiState.value = _uiState.value.copy(
            transcripts = _uiState.value.transcripts + newEntry,
        )
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
