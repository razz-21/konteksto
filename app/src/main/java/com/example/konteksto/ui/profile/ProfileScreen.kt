package com.example.konteksto.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.konteksto.R
import com.example.konteksto.data.repository.AuthRepository
import com.example.konteksto.data.repository.FirebaseAuthRepository
import com.example.konteksto.data.repository.FirebaseUserProfileRepository
import com.example.konteksto.data.repository.UserProfileRepository
import com.example.konteksto.domain.model.ExplanationLanguage
import com.example.konteksto.domain.model.UserProfile
import com.example.konteksto.ui.login.GoogleAuthHelper
import com.example.konteksto.ui.login.GoogleSignInResult
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    authRepository: AuthRepository = remember { FirebaseAuthRepository() },
    userProfileRepository: UserProfileRepository = remember {
        FirebaseUserProfileRepository()
    },
    isDarkMode: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {},
    onSignOutClick: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val actualRepository = remember(context, userProfileRepository) {
        if (userProfileRepository is FirebaseUserProfileRepository) {
            FirebaseUserProfileRepository(context.applicationContext)
        } else {
            userProfileRepository
        }
    }

    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.provideFactory(authRepository, actualRepository),
    )

    val currentUser by viewModel.authStateStream
        .collectAsStateWithLifecycle(initialValue = viewModel.currentUser)
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uploadedPhotoUrl by viewModel.uploadedPhotoUrl.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.photoMessage.collect { message -> onShowMessage(message) }
    }

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
    val isEditingProfile = showEditProfile && visibleProfile != null

    BackHandler(enabled = isEditingProfile) { showEditProfile = false }

    AnimatedContent(
        targetState = isEditingProfile,
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            // Mirror the NavHost transitions: opening slides towards Start,
            // going back slides towards End.
            val direction = if (targetState) {
                AnimatedContentTransitionScope.SlideDirection.Start
            } else {
                AnimatedContentTransitionScope.SlideDirection.End
            }
            slideIntoContainer(
                towards = direction,
                animationSpec = tween(400),
            ) togetherWith slideOutOfContainer(
                towards = direction,
                animationSpec = tween(400),
            )
        },
        label = "profile_edit_profile_transition",
    ) { editingProfile ->
        val profileToEdit = visibleProfile
        if (editingProfile && profileToEdit != null) {
            EditProfileScreen(
                profile = profileToEdit,
                isSaving = uiState.isLoading,
                errorMessage = uiState.errorMessage,
                isUploadingPhoto = uiState.isUploadingPhoto,
                photoErrorMessage = uiState.photoErrorMessage,
                uploadedPhotoUrl = uploadedPhotoUrl,
                onUploadPhoto = viewModel::uploadProfilePhoto,
                onUploadedPhotoShown = viewModel::consumeUploadedPhotoUrl,
                onBackClick = {
                    viewModel.clearPhotoUploadError()
                    showEditProfile = false
                },
                onSaveClick = { name, role, organization, bio, avatarColorIndex, photoUrl ->
                    viewModel.saveProfile(name, role, organization, bio, avatarColorIndex, photoUrl) { wasSaved ->
                        if (wasSaved) {
                            showEditProfile = false
                        }
                    }
                },
            )
        } else {
            ProfileScreenContent(
                currentUser = currentUser,
                profile = visibleProfile,
                isDarkMode = isDarkMode,
                onDarkModeChange = onDarkModeChange,
                explanationLanguage = visibleProfile?.explanationLanguage ?: ExplanationLanguage.Default,
                onExplanationLanguageChange = { language ->
                    viewModel.updateLanguage(language) { wasSaved ->
                        onShowMessage(
                            if (wasSaved) {
                                context.getString(
                                    R.string.profile_explanation_language_saved,
                                    context.getString(language.labelRes),
                                )
                            } else {
                                context.getString(R.string.profile_explanation_language_save_failed)
                            },
                        )
                    }
                },
                isLoading = uiState.isLoading,
                errorMessage = uiState.errorMessage,
                onEditProfileClick = {
                    viewModel.clearPhotoUploadError()
                    showEditProfile = true
                },
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
    }
}
