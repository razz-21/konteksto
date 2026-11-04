package com.example.konteksto.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.konteksto.data.repository.AuthRepository
import com.example.konteksto.data.repository.FirebaseAuthRepository
import com.example.konteksto.data.repository.SessionRepository
import com.example.konteksto.domain.model.HostSession
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SessionsViewModel(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
) : ViewModel() {

    private val userUid: String
        get() = authRepository.currentUser?.uid ?: "guest_user"

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _allSessions = MutableStateFlow<List<HostSession>>(emptyList())
    val allSessions: StateFlow<List<HostSession>> = _allSessions.asStateFlow()

    private val _joinedSessionIds = MutableStateFlow<Set<String>>(emptySet())
    val joinedSessionIds: StateFlow<Set<String>> = _joinedSessionIds.asStateFlow()

    val filteredSessions = combine(_allSessions, _joinedSessionIds, _searchQuery) { sessions, joinedIds, query ->
        val attendedSessions = sessions.filter { it.id in joinedIds }
        if (query.isBlank()) {
            attendedSessions
        } else {
            val q = query.trim().lowercase()
            attendedSessions.filter { session ->
                session.title.lowercase().contains(q) ||
                    session.speakerName.lowercase().contains(q) ||
                    session.talkCode.lowercase().contains(q) ||
                    session.venue.lowercase().contains(q)
            }
        }
    }

    private val _selectedSessionToConfirm = MutableStateFlow<HostSession?>(null)
    val selectedSessionToConfirm: StateFlow<HostSession?> = _selectedSessionToConfirm.asStateFlow()

    private val _showAttendSheet = MutableStateFlow(false)
    val showAttendSheet: StateFlow<Boolean> = _showAttendSheet.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            sessionRepository.getAllSessionsStream().collect { sessions ->
                _allSessions.value = sessions
            }
        }

        viewModelScope.launch {
            sessionRepository.getJoinedSessionIdsStream(userUid).collect { ids ->
                _joinedSessionIds.value = ids
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun openAttendSheet() {
        _showAttendSheet.value = true
    }

    fun closeAttendSheet() {
        _showAttendSheet.value = false
        _selectedSessionToConfirm.value = null
    }

    fun verifyCodeAndAttend(code: String, onResult: (Result<HostSession>) -> Unit) {
        viewModelScope.launch {
            sessionRepository.findSessionByCode(code)
                .onSuccess { session ->
                    _selectedSessionToConfirm.value = session
                    onResult(Result.success(session))
                }
                .onFailure { error ->
                    onResult(Result.failure(error))
                }
        }
    }

    fun confirmJoinSession(session: HostSession, onComplete: (isLive: Boolean) -> Unit) {
        viewModelScope.launch {
            sessionRepository.joinSession(userUid, session.id)
                .onSuccess { joinedSession ->
                    _selectedSessionToConfirm.value = null
                    _showAttendSheet.value = false
                    val isLive = joinedSession.isLive || joinedSession.status.equals("live", ignoreCase = true)
                    if (!isLive) {
                        _snackbarMessage.emit("You're attending ${joinedSession.title}")
                    }
                    onComplete(isLive)
                }
                .onFailure { error ->
                    _snackbarMessage.emit("Failed to join session: ${error.message}")
                }
        }
    }

    companion object {
        fun provideFactory(
            sessionRepository: SessionRepository,
            authRepository: AuthRepository = FirebaseAuthRepository(),
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SessionsViewModel(sessionRepository, authRepository) as T
            }
        }
    }
}
