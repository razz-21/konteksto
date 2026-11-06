package com.example.konteksto.domain.usecase

import com.example.konteksto.data.repository.AuthRepository
import com.example.konteksto.data.repository.UserProfileRepository
import com.example.konteksto.domain.model.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

// Emits the signed-in user's profile, or null when signed out.
// Falls back to the Firebase Auth profile if the Firestore profile can't be read.
class ObserveCurrentProfileUseCase(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
) {
    val currentProfile: UserProfile?
        get() = authRepository.currentUser?.let { UserProfile.fromAuthUser(it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<UserProfile?> = authRepository.getAuthStateStream()
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(null)
            } else {
                userProfileRepository.getProfileStream(user.uid)
                    .map { savedProfile -> UserProfile.merge(user, savedProfile) }
                    .catch { emit(UserProfile.fromAuthUser(user)) }
            }
        }
}
