package com.example.konteksto.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.konteksto.data.repository.AuthRepository
import com.example.konteksto.data.repository.FirebaseAuthRepository
import com.example.konteksto.data.repository.FirebaseUserProfileRepository
import com.example.konteksto.data.repository.UserProfileRepository
import com.example.konteksto.domain.model.AuthUser
import com.example.konteksto.domain.model.ExplanationLanguage
import com.example.konteksto.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class ProfileViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val userProfileRepository: UserProfileRepository = FirebaseUserProfileRepository(),
) : ViewModel() {

    val currentUser: AuthUser?
        get() = authRepository.currentUser

    val authStateStream: Flow<AuthUser?> = authRepository.getAuthStateStream()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    // Listen to the signed-in user, then listen to that user's Firestore document.
    init {
        viewModelScope.launch {
            authStateStream.collectLatest { user ->
                if (user == null) {
                    _profile.value = null
                } else {
                    userProfileRepository.getProfileStream(user.uid).collect { savedProfile ->
                        _profile.value = UserProfile.merge(user, savedProfile)
                    }
                }
            }
        }
    }

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

    // Saves the Edit Profile fields and avatar color to Firestore.
    fun saveProfile(
        name: String,
        role: String,
        organization: String,
        bio: String,
        avatarColorIndex: Int = 3,
        onComplete: (Boolean) -> Unit,
    ) {
        val user = currentUser
        if (user == null) {
            _uiState.value = ProfileUiState(
                isLoading = false,
                errorMessage = "You must be signed in to edit your profile.",
            )
            onComplete(false)
            return
        }

        _uiState.value = ProfileUiState(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val updatedProfile = UserProfile(
                uid = user.uid,
                name = name.trim(),
                role = role.trim(),
                email = user.email ?: "",
                organization = organization.trim(),
                bio = bio.trim(),
                photoUrl = user.photoUrl,
                avatarColorIndex = avatarColorIndex,
                explanationLanguage = _profile.value?.explanationLanguage ?: ExplanationLanguage.Default,
            )

            userProfileRepository.saveProfile(updatedProfile)
                .onSuccess {
                    _profile.value = updatedProfile
                    _uiState.value = ProfileUiState(isLoading = false, errorMessage = null)
                    onComplete(true)
                }
                .onFailure { throwable ->
                    _uiState.value = ProfileUiState(
                        isLoading = false,
                        errorMessage = throwable.localizedMessage
                            ?: "Failed to save your profile.",
                    )
                    onComplete(false)
                }
        }
    }

    // Saves the explanation language right away so the next explanation uses it.
    fun updateLanguage(language: ExplanationLanguage, onComplete: (Boolean) -> Unit) {
        if (currentUser == null) return
        val previousProfile = _profile.value ?: return
        if (previousProfile.explanationLanguage == language) return

        val updatedProfile = previousProfile.copy(explanationLanguage = language)
        _profile.update { updatedProfile }

        viewModelScope.launch {
            userProfileRepository.saveProfile(updatedProfile)
                .onSuccess { onComplete(true) }
                .onFailure {
                    _profile.update { previousProfile }
                    onComplete(false)
                }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.value = ProfileUiState(isLoading = false, errorMessage = null)
    }

    companion object {
        fun provideFactory(
            authRepository: AuthRepository = FirebaseAuthRepository(),
            userProfileRepository: UserProfileRepository = FirebaseUserProfileRepository(),
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(authRepository, userProfileRepository) as T
            }
        }
    }
}
