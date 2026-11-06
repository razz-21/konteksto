package com.example.konteksto.data.repository

import android.content.Context
import android.util.Log
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
    private val context: Context? = null,
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() },
) : UserProfileRepository {

    // Returns null if Firebase/Firestore is not configured instead of crashing.
    private val firestoreOrNull: FirebaseFirestore?
        get() = try {
            firestoreProvider()
        } catch (_: Exception) {
            null
        }

    private fun getLocalProfile(uid: String): UserProfile? {
        val prefs = context?.getSharedPreferences("konteksto_user_profile_$uid", Context.MODE_PRIVATE)
            ?: return null
        val name = prefs.getString("name", null) ?: return null
        return UserProfile(
            uid = uid,
            name = name,
            role = prefs.getString("role", "Attendee") ?: "Attendee",
            email = prefs.getString("email", "") ?: "",
            organization = prefs.getString("organization", "") ?: "",
            bio = prefs.getString("bio", "") ?: "",
            photoUrl = prefs.getString("photoUrl", null),
            avatarColorIndex = prefs.getInt("avatarColorIndex", 3),
            explanationLanguage = prefs.getString("explanationLanguage", "english") ?: "english",
        )
    }

    private fun saveLocalProfile(profile: UserProfile) {
        val prefs = context?.getSharedPreferences("konteksto_user_profile_${profile.uid}", Context.MODE_PRIVATE)
            ?: return
        prefs.edit()
            .putString("name", profile.name)
            .putString("role", profile.role)
            .putString("email", profile.email)
            .putString("organization", profile.organization)
            .putString("bio", profile.bio)
            .putString("photoUrl", profile.photoUrl)
            .putInt("avatarColorIndex", profile.avatarColorIndex)
            .putString("explanationLanguage", profile.explanationLanguage)
            .apply()
    }

    override fun getProfileStream(uid: String): Flow<UserProfile?> = callbackFlow {
        val cached = getLocalProfile(uid)
        if (cached != null) {
            trySend(cached)
        }

        val firestore = firestoreOrNull
        if (firestore == null) {
            if (cached == null) {
                trySend(null)
            }
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(getLocalProfile(uid))
                    return@addSnapshotListener
                }

                val data = snapshot?.data
                if (data == null) {
                    trySend(getLocalProfile(uid))
                } else {
                    val remoteProfile = UserProfile(
                        uid = uid,
                        name = data["name"] as? String ?: "",
                        role = data["role"] as? String ?: "",
                        email = data["email"] as? String ?: "",
                        organization = data["organization"] as? String ?: "",
                        bio = data["bio"] as? String ?: "",
                        photoUrl = data["photoUrl"] as? String,
                        avatarColorIndex = (data["avatarColorIndex"] as? Number)?.toInt() ?: 3,
                        explanationLanguage = data["explanationLanguage"] as? String ?: "english",
                    )
                    saveLocalProfile(remoteProfile)
                    trySend(remoteProfile)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun saveProfile(profile: UserProfile): Result<Unit> = runCatching {
        saveLocalProfile(profile)

        val firestore = firestoreOrNull
        if (firestore != null) {
            val data = mapOf(
                "name" to profile.name,
                "role" to profile.role,
                "email" to profile.email,
                "organization" to profile.organization,
                "bio" to profile.bio,
                "photoUrl" to profile.photoUrl,
                "avatarColorIndex" to profile.avatarColorIndex,
                "explanationLanguage" to profile.explanationLanguage,
            )

            try {
                firestore.collection("users")
                    .document(profile.uid)
                    .set(data)
                    .await()
            } catch (e: Exception) {
                Log.e("FirebaseUserProfileRepo", "Firestore save profile failed: ${e.message}")
            }
        }
    }
}
