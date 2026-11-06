package com.example.konteksto.ui.host

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.konteksto.data.repository.AuthRepository
import com.example.konteksto.data.repository.FirebaseAuthRepository
import com.example.konteksto.data.repository.FirebaseSessionRepository
import com.example.konteksto.data.repository.FirebaseUserProfileRepository
import com.example.konteksto.data.repository.SessionRepository
import com.example.konteksto.data.repository.UserProfileRepository
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.domain.model.UserProfile
import com.example.konteksto.domain.usecase.ObserveCurrentProfileUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HostViewModel(
    private val sessionRepository: SessionRepository = FirebaseSessionRepository(),
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    userProfileRepository: UserProfileRepository = FirebaseUserProfileRepository(),
) : ViewModel() {

    private val observeCurrentProfile = ObserveCurrentProfileUseCase(authRepository, userProfileRepository)

    val currentProfile: StateFlow<UserProfile?> = observeCurrentProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = observeCurrentProfile.currentProfile,
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val hostSessions: StateFlow<List<HostSession>> = authRepository.getAuthStateStream()
        .flatMapLatest { user ->
            val hostUid = user?.uid ?: ""
            sessionRepository.getHostSessionsStream(hostUid)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun refreshSessions() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(600)
            _isRefreshing.value = false
        }
    }

    fun createSession(
        title: String,
        speakerName: String,
        category: String,
        venue: String,
        startTime: String,
        endTime: String,
        bannerImageUrl: String,
        talkCode: String = "",
        onSuccess: (sessionId: String) -> Unit = {},
        onError: (message: String) -> Unit = {},
    ) {
        if (title.isBlank()) {
            onError("Please enter a session title.")
            return
        }

        val currentUser = authRepository.currentUser
        val hostUid = currentUser?.uid ?: ""
        val speaker = speakerName.ifBlank {
            currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "Host"
        }

        viewModelScope.launch {
            sessionRepository.createSession(
                title = title,
                speakerName = speaker,
                category = category,
                venue = venue,
                startTime = startTime,
                endTime = endTime,
                bannerImageUrl = bannerImageUrl,
                talkCode = talkCode,
                hostUid = hostUid,
            ).onSuccess { sessionId ->
                onSuccess(sessionId)
            }.onFailure { throwable ->
                onError(throwable.localizedMessage ?: "Failed to create session.")
            }
        }
    }

    fun toggleSessionLiveStatus(
        sessionId: String,
        isLive: Boolean,
        onSuccess: () -> Unit = {},
        onError: (message: String) -> Unit = {},
    ) {
        viewModelScope.launch {
            sessionRepository.updateSessionLiveStatus(sessionId, isLive)
                .onSuccess { onSuccess() }
                .onFailure { throwable ->
                    onError(throwable.localizedMessage ?: "Failed to update session live status.")
                }
        }
    }

    companion object {
        fun provideFactory(
            sessionRepository: SessionRepository = FirebaseSessionRepository(),
            authRepository: AuthRepository = FirebaseAuthRepository(),
            userProfileRepository: UserProfileRepository = FirebaseUserProfileRepository(),
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HostViewModel(sessionRepository, authRepository, userProfileRepository) as T
            }
        }
    }
}
