package com.example.konteksto.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.konteksto.data.repository.AuthRepository
import com.example.konteksto.data.repository.FirebaseAuthRepository
import com.example.konteksto.ui.login.GoogleAuthHelper
import com.example.konteksto.ui.login.GoogleSignInResult
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    authRepository: AuthRepository = remember { FirebaseAuthRepository() },
    viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.provideFactory(authRepository),
    ),
    onSignOutClick: () -> Unit = {},
) {
    val currentUser by viewModel.authStateStream
        .collectAsStateWithLifecycle(initialValue = viewModel.currentUser)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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

    ProfileScreenContent(
        modifier = modifier,
        currentUser = currentUser,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
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
