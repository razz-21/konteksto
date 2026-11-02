package com.example.konteksto.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@Composable
fun LoginRoute(
    onSignInSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.provideFactory()),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUser by viewModel.authStateStream.collectAsStateWithLifecycle(
        initialValue = viewModel.currentUser,
    )
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState, currentUser) {
        if (uiState is LoginUiState.Success || currentUser != null) {
            onSignInSuccess()
        }
    }

    LoginScreen(
        modifier = modifier,
        isLoading = uiState is LoginUiState.Loading,
        errorMessage = (uiState as? LoginUiState.Error)?.message,
        onGoogleSignInClick = {
            viewModel.setLoading()
            coroutineScope.launch {
                when (val result = GoogleAuthHelper.requestGoogleIdToken(context)) {
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
        },
    )
}
