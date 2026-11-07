package com.example.konteksto.ui.host

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.konteksto.data.repository.SessionRepository
import com.example.konteksto.ui.components.SessionLookup
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PresentQrViewModel(
    private val sessionId: String,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _sessionLookup = MutableStateFlow<SessionLookup>(SessionLookup.Loading)
    val sessionLookup: StateFlow<SessionLookup> = _sessionLookup.asStateFlow()

    private var observeJob: Job? = null

    init {
        observeSession()
    }

    fun retrySessionLookup() {
        _sessionLookup.update { SessionLookup.Loading }
        observeSession()
    }

    private fun observeSession() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            sessionRepository.getSessionStream(sessionId)
                .catch {
                    // A QR code already on screen stays there; only a first load that fails is shown as one.
                    _sessionLookup.update { lookup ->
                        if (lookup is SessionLookup.Found) lookup else SessionLookup.Failed
                    }
                }
                .collect { session ->
                    _sessionLookup.update {
                        if (session != null) SessionLookup.Found(session) else SessionLookup.NotFound
                    }
                }
        }
    }

    companion object {
        fun provideFactory(
            sessionId: String,
            sessionRepository: SessionRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PresentQrViewModel(sessionId, sessionRepository) as T
            }
        }
    }
}
