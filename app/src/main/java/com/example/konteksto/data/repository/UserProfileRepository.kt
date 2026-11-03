package com.example.konteksto.data.repository

import com.example.konteksto.domain.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface UserProfileRepository {
    fun getProfileStream(uid: String): Flow<UserProfile?>
    suspend fun saveProfile(profile: UserProfile): Result<Unit>
}

class FirebaseUserProfileRepository(
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() },
) : UserProfileRepository {

    // Returns null if Firebase/Firestore is not configured instead of crashing.
    private val firestoreOrNull: FirebaseFirestore?
        get() = try {
            firestoreProvider()
        } catch (_: Exception) {
            null
        }

    override fun getProfileStream(uid: String): Flow<UserProfile?> = callbackFlow {
        val firestore = firestoreOrNull
        if (firestore == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }

                val data = snapshot?.data
                if (data == null) {
                    trySend(null)
                } else {
                    trySend(
                        UserProfile(
                            uid = uid,
                            name = data["name"] as? String ?: "",
                            role = data["role"] as? String ?: "",
                            email = data["email"] as? String ?: "",
                            organization = data["organization"] as? String ?: "",
                            bio = data["bio"] as? String ?: "",
                            photoUrl = data["photoUrl"] as? String,
                        ),
                    )
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun saveProfile(profile: UserProfile): Result<Unit> = runCatching {
        val firestore = firestoreOrNull
            ?: throw IllegalStateException("Firestore is not available.")

        val data = mapOf(
            "name" to profile.name,
            "role" to profile.role,
            "email" to profile.email,
            "organization" to profile.organization,
            "bio" to profile.bio,
            "photoUrl" to profile.photoUrl,
        )

        firestore.collection("users")
            .document(profile.uid)
            .set(data)
            .await()
    }
}