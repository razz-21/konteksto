package com.example.konteksto.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.konteksto.data.repository.AuthRepository
import com.example.konteksto.data.repository.FirebaseAuthRepository
import com.example.konteksto.data.repository.FirebaseUserProfileRepository
import com.example.konteksto.data.repository.UserProfileRepository
import com.example.konteksto.domain.model.AuthUser
import com.example.konteksto.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class ProfileViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(), // added
    private val userProfileRepository: UserProfileRepository = FirebaseUserProfileRepository(), // added
) : ViewModel() {

    val currentUser: AuthUser?
        get() = authRepository.currentUser

    val authStateStream: Flow<AuthUser?> = authRepository.getAuthStateStream()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _profile = MutableStateFlow<UserProfile?>(null) // added
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow() // added

    // Listen to the signed-in user, then listen to that user's Firestore document. // added
    init { // added
        viewModelScope.launch { // added
            authStateStream.collectLatest { user -> // added
                if (user == null) { // added
                    _profile.value = null // added
                } else { // added
                    userProfileRepository.getProfileStream(user.uid).collect { savedProfile -> // added
                        _profile.value = mergeProfile(user, savedProfile) // added
                    }
                }
            }
        }
    }

    // Firestore is the main source, with Firebase Auth as a fallback. // added
    private fun mergeProfile(user: AuthUser, savedProfile: UserProfile?): UserProfile { // added
        val authProfile = UserProfile.fromAuthUser(user)
        if (savedProfile == null) return authProfile

        return savedProfile.copy(
            name = savedProfile.name.ifBlank { authProfile.name },
            email = authProfile.email.ifBlank { savedProfile.email },
            photoUrl = savedProfile.photoUrl ?: authProfile.photoUrl,
        )
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

    // Saves the Edit Profile fields to Firestore. // added
    fun saveProfile( // added
        name: String,
        role: String,
        organization: String,
        bio: String,
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

    fun signOut() {
        authRepository.signOut()
        _uiState.value = ProfileUiState(isLoading = false, errorMessage = null)
    }

    companion object {
        fun provideFactory(
            authRepository: AuthRepository = FirebaseAuthRepository(),
            userProfileRepository: UserProfileRepository = FirebaseUserProfileRepository(), // added
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(authRepository, userProfileRepository) as T // changed
            }
        }
    }
}