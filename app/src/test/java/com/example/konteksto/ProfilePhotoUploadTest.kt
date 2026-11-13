package com.example.konteksto

import android.net.Uri
import com.example.konteksto.data.repository.AuthRepository
import com.example.konteksto.data.repository.FirebaseAuthRepository
import com.example.konteksto.data.repository.UserProfileRepository
import com.example.konteksto.domain.model.AuthUser
import com.example.konteksto.domain.model.UserProfile
import com.example.konteksto.ui.profile.ProfileViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private val signedInUser = AuthUser(
    uid = "user_1",
    email = "user@example.com",
    displayName = "User",
    photoUrl = null,
)

private val profileAuth = object : AuthRepository by FirebaseAuthRepository(authProvider = { error("No auth in unit test") }) {
    override val currentUser: AuthUser = signedInUser
    override fun getAuthStateStream(): Flow<AuthUser?> = flowOf(signedInUser)
}

private class FakeProfileRepository : UserProfileRepository {
    var savedProfile: UserProfile? = null

    override fun getProfileStream(uid: String): Flow<UserProfile?> = flowOf(null)

    override suspend fun saveProfile(profile: UserProfile): Result<Unit> {
        savedProfile = profile
        return Result.success(Unit)
    }

    override suspend fun uploadProfilePhoto(uid: String, uri: Uri): Result<String> =
        Result.success("https://example.com/users/$uid/avatar.jpg")
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProfilePhotoUploadTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ProfileViewModel(profileAuth, FakeProfileRepository())

    @Test
    fun uploadShowsLoadingUntilItSucceeds() = runTest(testDispatcher) {
        val upload = CompletableDeferred<Result<String>>()
        val viewModel = viewModel()

        viewModel.startPhotoUpload { upload.await() }
        assertTrue(viewModel.uiState.value.isUploadingPhoto)

        upload.complete(Result.success("https://example.com/avatar.jpg"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isUploadingPhoto)
        assertEquals("https://example.com/avatar.jpg", viewModel.uploadedPhotoUrl.value)
        assertNull(viewModel.uiState.value.photoErrorMessage)
    }

    @Test
    fun failedUploadShowsErrorAndNoPhoto() = runTest(testDispatcher) {
        val upload = CompletableDeferred<Result<String>>()
        val viewModel = viewModel()

        viewModel.startPhotoUpload { upload.await() }
        assertTrue(viewModel.uiState.value.isUploadingPhoto)

        upload.complete(Result.failure(Exception("offline")))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isUploadingPhoto)
        assertNull(viewModel.uploadedPhotoUrl.value)
        assertNotNull(viewModel.uiState.value.photoErrorMessage)
    }

    @Test
    fun aSecondUploadIsIgnoredWhileTheFirstIsRunning() = runTest(testDispatcher) {
        val firstUpload = CompletableDeferred<Result<String>>()
        var uploadsStarted = 0
        val viewModel = viewModel()

        viewModel.startPhotoUpload {
            uploadsStarted++
            firstUpload.await()
        }
        viewModel.startPhotoUpload {
            uploadsStarted++
            Result.success("https://example.com/second.jpg")
        }

        assertEquals(1, uploadsStarted)

        firstUpload.complete(Result.success("https://example.com/first.jpg"))
        advanceUntilIdle()
        assertEquals("https://example.com/first.jpg", viewModel.uploadedPhotoUrl.value)
    }

    @Test
    fun retryingAfterAFailureSucceeds() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.startPhotoUpload { Result.failure(Exception("offline")) }
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.photoErrorMessage)
        assertNull(viewModel.uploadedPhotoUrl.value)

        viewModel.startPhotoUpload { Result.success("https://example.com/avatar.jpg") }
        advanceUntilIdle()

        assertEquals("https://example.com/avatar.jpg", viewModel.uploadedPhotoUrl.value)
        assertNull(viewModel.uiState.value.photoErrorMessage)
        assertFalse(viewModel.uiState.value.isUploadingPhoto)
    }

    @Test
    fun successfulUploadSavesThePhotoToTheProfile() = runTest(testDispatcher) {
        val repository = FakeProfileRepository()
        val viewModel = ProfileViewModel(profileAuth, repository)

        viewModel.startPhotoUpload { Result.success("https://example.com/avatar.jpg") }
        advanceUntilIdle()

        // Saving the profile is what makes the photo survive an app restart.
        assertEquals("https://example.com/avatar.jpg", repository.savedProfile?.photoUrl)
        assertEquals(signedInUser.uid, repository.savedProfile?.uid)
    }
}
