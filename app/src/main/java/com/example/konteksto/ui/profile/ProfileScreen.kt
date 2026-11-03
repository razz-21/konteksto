package com.example.konteksto.ui.profile

import androidx.activity.compose.BackHandler // added
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf // added
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable // added
import androidx.compose.runtime.setValue // added
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.konteksto.data.repository.AuthRepository
import com.example.konteksto.data.repository.FirebaseAuthRepository
import com.example.konteksto.data.repository.FirebaseUserProfileRepository // added
import com.example.konteksto.data.repository.UserProfileRepository // added
import com.example.konteksto.domain.model.UserProfile // added
import com.example.konteksto.ui.login.GoogleAuthHelper
import com.example.konteksto.ui.login.GoogleSignInResult
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    authRepository: AuthRepository = remember { FirebaseAuthRepository() },
    userProfileRepository: UserProfileRepository = remember { FirebaseUserProfileRepository() }, // added
    viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.provideFactory(authRepository, userProfileRepository), // changed
    ),
    isDarkMode: Boolean = false, // added
    onDarkModeChange: (Boolean) -> Unit = {}, // added
    onSignOutClick: () -> Unit = {},
) {
    val currentUser by viewModel.authStateStream
        .collectAsStateWithLifecycle(initialValue = viewModel.currentUser)
    val profile by viewModel.profile.collectAsStateWithLifecycle() // added
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showEditProfile by rememberSaveable { mutableStateOf(false) } // added

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

    // Shows Firebase Auth data first, then Firestore data once it arrives. // added
    val visibleProfile = currentUser?.let { user -> // added
        profile ?: UserProfile.fromAuthUser(user) // added
    }

    // Show Edit Profile in place of the Profile & Settings content. // added
    if (showEditProfile && visibleProfile != null) { // added
        BackHandler { showEditProfile = false } // added

        EditProfileScreen(
            profile = visibleProfile,
            isSaving = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            onBackClick = { showEditProfile = false },
            onSaveClick = { name, role, organization, bio ->
                viewModel.saveProfile(name, role, organization, bio) { wasSaved ->
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
        profile = visibleProfile, // added
        isDarkMode = isDarkMode, // added
        onDarkModeChange = onDarkModeChange, // added
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onEditProfileClick = { showEditProfile = true }, // added
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