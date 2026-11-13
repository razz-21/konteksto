package com.example.konteksto.data.repository

import android.content.Context
import android.util.Log
import com.example.konteksto.domain.model.ExplanationLanguage
import com.example.konteksto.domain.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
            explanationLanguage = ExplanationLanguage.fromStorageValue(prefs.getString("explanationLanguage", null)),
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
            .putString("explanationLanguage", profile.explanationLanguage.storageValue)
            .apply()
    }

    // Every repository instance shares this cache, so a profile saved on one screen
    // is immediately visible on Sessions, Host Sessions, and Profile & Settings.
    private fun cacheProfile(profile: UserProfile) {
        savedProfiles.update { profiles -> profiles + (profile.uid to profile) }
    }

    override fun getProfileStream(uid: String): Flow<UserProfile?> = callbackFlow {
        val cachedInMemory = savedProfiles.value[uid]
        val localProfile = cachedInMemory ?: getLocalProfile(uid)
        if (localProfile != null) {
            if (cachedInMemory == null) {
                cacheProfile(localProfile)
            }
            trySend(localProfile)
        }

        // Emit the saved profile as soon as it changes, even if Firestore has stale data.
        var lastEmittedProfile = localProfile
        val cacheJob = launch {
            savedProfiles.collect { profiles ->
                val profile = profiles[uid] ?: return@collect
                if (profile != lastEmittedProfile) {
                    lastEmittedProfile = profile
                    trySend(profile)
                }
            }
        }

        val firestore = firestoreOrNull
        if (firestore == null) {
            if (localProfile == null) {
                trySend(null)
            }
            awaitClose { cacheJob.cancel() }
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (savedProfiles.value[uid] == null && getLocalProfile(uid) == null) {
                        trySend(null)
                    }
                    return@addSnapshotListener
                }

                val data = snapshot?.data
                if (data == null) {
                    if (savedProfiles.value[uid] == null && getLocalProfile(uid) == null) {
                        trySend(null)
                    }
                } else if (savedProfiles.value[uid] == null && getLocalProfile(uid) == null) {
                    // Only use Firestore when this device has no saved profile yet.
                    // Otherwise a stale remote document could overwrite a newer local save
                    // (for example, after removing a photo or changing the avatar color).
                    val remoteProfile = UserProfile(
                        uid = uid,
                        name = data["name"] as? String ?: "",
                        role = data["role"] as? String ?: "",
                        email = data["email"] as? String ?: "",
                        organization = data["organization"] as? String ?: "",
                        bio = data["bio"] as? String ?: "",
                        photoUrl = data["photoUrl"] as? String,
                        avatarColorIndex = (data["avatarColorIndex"] as? Number)?.toInt() ?: 3,
                        explanationLanguage = ExplanationLanguage.fromStorageValue(data["explanationLanguage"] as? String),
                    )
                    saveLocalProfile(remoteProfile)
                    cacheProfile(remoteProfile)
                }
            }

        awaitClose {
            cacheJob.cancel()
            listener.remove()
        }
    }

    override suspend fun saveProfile(profile: UserProfile): Result<Unit> = runCatching {
        var finalProfile = profile
        val photoUrl = profile.photoUrl

        if (context != null && !photoUrl.isNullOrBlank() && photoUrl.startsWith("content://")) {
            try {
                val uri = android.net.Uri.parse(photoUrl)
                val photosDir = java.io.File(context.filesDir, "profile_photos")
                if (!photosDir.exists()) photosDir.mkdirs()
                val destinationFile = java.io.File(photosDir, "profile_${profile.uid}.jpg")
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    java.io.FileOutputStream(destinationFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                val localUri = android.net.Uri.fromFile(destinationFile).toString()
                finalProfile = profile.copy(photoUrl = localUri)
            } catch (e: Exception) {
                Log.e("FirebaseUserProfileRepo", "Failed to convert content URI to local file: ${e.message}")
            }
        } else if (context != null && photoUrl.isNullOrBlank()) {
            try {
                val destinationFile = java.io.File(java.io.File(context.filesDir, "profile_photos"), "profile_${profile.uid}.jpg")
                if (destinationFile.exists()) {
                    destinationFile.delete()
                }
            } catch (_: Exception) {}
        }

        saveLocalProfile(finalProfile)
        cacheProfile(finalProfile)

        val firestore = firestoreOrNull
        if (firestore != null) {
            val data = mapOf(
                "name" to finalProfile.name,
                "role" to finalProfile.role,
                "email" to finalProfile.email,
                "organization" to finalProfile.organization,
                "bio" to finalProfile.bio,
                "photoUrl" to finalProfile.photoUrl,
                "avatarColorIndex" to finalProfile.avatarColorIndex,
                "explanationLanguage" to finalProfile.explanationLanguage.storageValue,
            )

            try {
                firestore.collection("users")
                    .document(finalProfile.uid)
                    .set(data)
                    .await()
            } catch (e: Exception) {
                Log.e("FirebaseUserProfileRepo", "Firestore save profile failed: ${e.message}")
            }
        }
    }

    private companion object {
        // Shared by all repository instances so every screen observes the same profile.
        val savedProfiles = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    }
}
