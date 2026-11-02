package com.example.konteksto.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.konteksto.data.repository.AuthRepository
import com.example.konteksto.data.repository.FirebaseAuthRepository
import com.example.konteksto.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class ProfileViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
) : ViewModel() {

    val currentUser: AuthUser?
        get() = authRepository.currentUser

    val authStateStream: Flow<AuthUser?> = authRepository.getAuthStateStream()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun setLoading() {
        _uiState.value = ProfileUiState(isLoading = true, errorMessage = null)
    }

    fun signInWithGoogleIdToken(idToken: String) {
        if (idToken.isBlank()) {
            _uiState.value = ProfileUiState(
                isLoading = false,
                errorMessage = "Google ID token was empty.",
            )
            return
        }

        _uiState.value = ProfileUiState(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            authRepository.signInWithGoogleIdToken(idToken)
                .onSuccess {
                    _uiState.value = ProfileUiState(isLoading = false, errorMessage = null)
                }
                .onFailure { throwable ->
                    _uiState.value = ProfileUiState(
                        isLoading = false,
                        errorMessage = throwable.localizedMessage
                            ?: "Failed to authenticate with Firebase.",
                    )
                }
        }
    }

    fun onSignInCancelled() {
        _uiState.value = ProfileUiState(isLoading = false, errorMessage = null)
    }

    fun onSignInError(message: String) {
        _uiState.value = ProfileUiState(isLoading = false, errorMessage = message)
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.value = ProfileUiState(isLoading = false, errorMessage = null)
    }

    companion object {
        fun provideFactory(
            authRepository: AuthRepository = FirebaseAuthRepository(),
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(authRepository) as T
            }
        }
    }
}
