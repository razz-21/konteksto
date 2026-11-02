package com.example.konteksto

import com.example.konteksto.data.repository.AuthRepository
import com.example.konteksto.domain.model.AuthUser
import com.example.konteksto.ui.login.LoginUiState
import com.example.konteksto.ui.login.LoginViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginScreenTest {
    @Test
    fun testLoginScreenPackageExists() {
        val clazz = Class.forName("com.example.konteksto.ui.login.LoginScreenKt")
        assertNotNull(clazz)
    }

    @Test
    fun testLoginViewModelInitialStateWhenSignedIn() {
        val fakeUser = AuthUser(
            uid = "uid-123",
            email = "speaker@konteksto.app",
            displayName = "Dr. Sarah Chen",
        )
        val fakeRepo = FakeAuthRepository(initialUser = fakeUser)
        val viewModel = LoginViewModel(authRepository = fakeRepo)

        val state = viewModel.uiState.value
        assertTrue(state is LoginUiState.Success)
        assertEquals(fakeUser, (state as LoginUiState.Success).user)
    }

    @Test
    fun testLoginViewModelBlankGoogleTokenShowsError() {
        val fakeRepo = FakeAuthRepository()
        val viewModel = LoginViewModel(authRepository = fakeRepo)

        viewModel.signInWithGoogleIdToken("")
        val state = viewModel.uiState.value
        assertTrue(state is LoginUiState.Error)
    }
}

private class FakeAuthRepository(
    initialUser: AuthUser? = null,
) : AuthRepository {
    private val userFlow = MutableStateFlow(initialUser)

    override val currentUser: AuthUser?
        get() = userFlow.value

    override fun getAuthStateStream(): Flow<AuthUser?> = userFlow

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthUser> {
        val user = AuthUser(uid = "google-$idToken", email = "user@gmail.com")
        userFlow.value = user
        return Result.success(user)
    }

    override suspend fun signInWithEmailAndPassword(email: String, password: String): Result<AuthUser> {
        val user = AuthUser(uid = "email-uid", email = email)
        userFlow.value = user
        return Result.success(user)
    }

    override suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<AuthUser> {
        val user = AuthUser(uid = "signup-uid", email = email)
        userFlow.value = user
        return Result.success(user)
    }

    override suspend fun signInAnonymously(): Result<AuthUser> {
        val user = AuthUser(uid = "anon-uid", isAnonymous = true)
        userFlow.value = user
        return Result.success(user)
    }

    override fun signOut() {
        userFlow.value = null
    }
}
