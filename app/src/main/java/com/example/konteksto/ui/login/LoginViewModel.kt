package com.example.konteksto.ui.login

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

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Success(val user: AuthUser) : LoginUiState
    data class Error(val message: String) : LoginUiState
}

class LoginViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
) : ViewModel() {

    val currentUser: AuthUser?
        get() = authRepository.currentUser

    val authStateStream: Flow<AuthUser?> = authRepository.getAuthStateStream()

    private val _uiState = MutableStateFlow<LoginUiState>(
        authRepository.currentUser?.let { LoginUiState.Success(it) } ?: LoginUiState.Idle,
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun setLoading() {
        _uiState.value = LoginUiState.Loading
    }

    fun signInWithGoogleIdToken(idToken: String) {
        if (idToken.isBlank()) {
            _uiState.value = LoginUiState.Error("Google ID token was empty.")
            return
        }

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            authRepository.signInWithGoogleIdToken(idToken)
                .onSuccess { user ->
                    _uiState.value = LoginUiState.Success(user)
                }
                .onFailure { throwable ->
                    _uiState.value = LoginUiState.Error(
                        throwable.localizedMessage ?: "Failed to authenticate with Firebase.",
                    )
                }
        }
    }

    fun signInWithEmailAndPassword(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Please enter both email and password.")
            return
        }

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            authRepository.signInWithEmailAndPassword(email, password)
                .onSuccess { user ->
                    _uiState.value = LoginUiState.Success(user)
                }
                .onFailure { throwable ->
                    _uiState.value = LoginUiState.Error(
                        throwable.localizedMessage ?: "Email sign-in failed.",
                    )
                }
        }
    }

    fun signInAnonymously() {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            authRepository.signInAnonymously()
                .onSuccess { user ->
                    _uiState.value = LoginUiState.Success(user)
                }
                .onFailure { throwable ->
                    _uiState.value = LoginUiState.Error(
                        throwable.localizedMessage ?: "Anonymous sign-in failed.",
                    )
                }
        }
    }

    fun onSignInCancelled() {
        _uiState.value = LoginUiState.Idle
    }

    fun onSignInError(message: String) {
        _uiState.value = LoginUiState.Error(message)
    }

    fun clearError() {
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }

    companion object {
        fun provideFactory(
            authRepository: AuthRepository = FirebaseAuthRepository(),
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LoginViewModel(authRepository) as T
            }
        }
    }
}
