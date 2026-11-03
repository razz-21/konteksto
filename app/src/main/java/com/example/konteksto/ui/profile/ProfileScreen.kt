package com.example.konteksto.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.konteksto.data.repository.AuthRepository
import com.example.konteksto.data.repository.FirebaseAuthRepository
import com.example.konteksto.data.repository.FirebaseUserProfileRepository
import com.example.konteksto.data.repository.UserProfileRepository
import com.example.konteksto.domain.model.UserProfile
import com.example.konteksto.ui.login.GoogleAuthHelper
import com.example.konteksto.ui.login.GoogleSignInResult
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    authRepository: AuthRepository = remember { FirebaseAuthRepository() },
    userProfileRepository: UserProfileRepository = remember { FirebaseUserProfileRepository() },
    viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.provideFactory(authRepository, userProfileRepository),
    ),
    isDarkMode: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {},
    onSignOutClick: () -> Unit = {},
) {
    val currentUser by viewModel.authStateStream
        .collectAsStateWithLifecycle(initialValue = viewModel.currentUser)
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showEditProfile by rememberSaveable { mutableStateOf(false) }

    val triggerGoogleLogin: () -> Unit = {
        viewModel.setLoading()
        coroutineScope.launch {
            when (
                val result = GoogleAuthHelper.requestGoogleIdToken(
                    context = context,
                    autoSelectEnabled = false,
                )
            ) {
                is GoogleSignInResult.Success -> {
                    viewModel.signInWithGoogleIdToken(result.idToken)
                }
                is GoogleSignInResult.Cancelled -> {
                    viewModel.onSignInCancelled()
                }
                is GoogleSignInResult.Error -> {
                    viewModel.onSignInError(result.message)
                }
            }
        }
    }

    // Shows Firebase Auth data first, then Firestore data once it arrives.
    val visibleProfile = currentUser?.let { user ->
        profile ?: UserProfile.fromAuthUser(user)
    }

    // Show Edit Profile in place of the Profile & Settings content.
    if (showEditProfile && visibleProfile != null) {
        BackHandler { showEditProfile = false }

        EditProfileScreen(
            profile = visibleProfile,
            isSaving = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            onBackClick = { showEditProfile = false },
            onSaveClick = { name, role, organization, bio, avatarColorIndex ->
                viewModel.saveProfile(name, role, organization, bio, avatarColorIndex) { wasSaved ->
                    if (wasSaved) {
                        showEditProfile = false
                    }
                }
            },
        )
        return
    }

    ProfileScreenContent(
        modifier = modifier,
        currentUser = currentUser,
        profile = visibleProfile,
        isDarkMode = isDarkMode,
        onDarkModeChange = onDarkModeChange,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onEditProfileClick = { showEditProfile = true },
        onGoogleSignInClick = triggerGoogleLogin,
        onConfirmSignOut = {
            viewModel.signOut()
            onSignOutClick()
            coroutineScope.launch {
                GoogleAuthHelper.clearCredentialState(context)
            }
        },
    )
}
