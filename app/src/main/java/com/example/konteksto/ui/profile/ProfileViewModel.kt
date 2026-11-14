package com.example.konteksto.ui.profile

import android.net.Uri
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isUploadingPhoto: Boolean = false,
    val photoErrorMessage: String? = null,
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

    // The URL of the photo that just finished uploading, shown in Edit Profile.
    private val _uploadedPhotoUrl = MutableStateFlow<String?>(null)
    val uploadedPhotoUrl: StateFlow<String?> = _uploadedPhotoUrl.asStateFlow()

    private val _photoMessage = MutableSharedFlow<String>()
    val photoMessage: SharedFlow<String> = _photoMessage.asSharedFlow()

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
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
    }

    fun signInWithGoogleIdToken(idToken: String) {
        if (idToken.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Google ID token was empty.",
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            authRepository.signInWithGoogleIdToken(idToken)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.localizedMessage
                                ?: "Failed to authenticate with Firebase.",
                        )
                    }
                }
        }
    }

    fun onSignInCancelled() {
        _uiState.update { it.copy(isLoading = false, errorMessage = null) }
    }

    fun onSignInError(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    // Saves the Edit Profile fields, avatar color, and uploaded photo URL to Firestore & SharedPreferences.
    fun saveProfile(
        name: String,
        role: String,
        organization: String,
        bio: String,
        avatarColorIndex: Int = 3,
        photoUrl: String? = null,
        onComplete: (Boolean) -> Unit,
    ) {
        val user = currentUser
        if (user == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "You must be signed in to edit your profile.",
                )
            }
            onComplete(false)
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val updatedProfile = UserProfile(
                uid = user.uid,
                name = name.trim(),
                role = role.trim(),
                email = user.email ?: "",
                organization = organization.trim(),
                bio = bio.trim(),
                photoUrl = photoUrl,
                avatarColorIndex = avatarColorIndex,
                explanationLanguage = _profile.value?.explanationLanguage ?: ExplanationLanguage.Default,
            )

            userProfileRepository.saveProfile(updatedProfile)
                .onSuccess {
                    _profile.value = updatedProfile
                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                    onComplete(true)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.localizedMessage
                                ?: "Failed to save your profile.",
                        )
                    }
                    onComplete(false)
                }
        }
    }

    // Uploads the picked image immediately so the user sees progress before saving.
    fun uploadProfilePhoto(uri: Uri) {
        val user = currentUser
        if (user == null) {
            _uiState.update {
                it.copy(photoErrorMessage = "You must be signed in to upload a photo.")
            }
            return
        }

        startPhotoUpload { userProfileRepository.uploadProfilePhoto(user.uid, uri) }
    }

    // Runs one upload and owns the loading/error state, so extra taps are ignored while it runs.
    internal fun startPhotoUpload(upload: suspend () -> Result<String>) {
        if (_uiState.value.isUploadingPhoto) return

        _uploadedPhotoUrl.value = null
        _uiState.update { it.copy(isUploadingPhoto = true, photoErrorMessage = null) }
        viewModelScope.launch {
            upload()
                .onSuccess { url ->
                    _uploadedPhotoUrl.value = url
                    _uiState.update { it.copy(isUploadingPhoto = false) }
                    // Save right away so the new photo survives closing the app.
                    persistUploadedPhoto(url)
                    _photoMessage.emit("Profile photo updated")
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isUploadingPhoto = false,
                            photoErrorMessage = "Couldn't upload your photo. Check your connection and try again.",
                        )
                    }
                    _photoMessage.emit("Couldn't upload your photo. Please try again.")
                }
        }
    }

    // Writes the uploaded photo URL to the profile, which also updates local storage,
    // Firestore, and the shared cache used by Sessions and Host Sessions.
    private suspend fun persistUploadedPhoto(url: String) {
        val currentProfile = _profile.value ?: return
        val updatedProfile = currentProfile.copy(photoUrl = url)
        userProfileRepository.saveProfile(updatedProfile)
            .onSuccess { _profile.value = updatedProfile }
    }

    // Called once Edit Profile has shown the uploaded photo, so it isn't applied again later.
    fun consumeUploadedPhotoUrl() {
        _uploadedPhotoUrl.value = null
    }

    fun clearPhotoUploadError() {
        _uiState.update { it.copy(photoErrorMessage = null) }
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
        _uiState.update { it.copy(isLoading = false, errorMessage = null) }
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
