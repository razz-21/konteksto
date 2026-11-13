package com.example.konteksto.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.konteksto.BuildConfig
import com.example.konteksto.domain.model.ExplanationLanguage
import com.example.konteksto.domain.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CancellationException
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
    /** Uploads the image to Cloud Storage and returns its download URL. */
    suspend fun uploadProfilePhoto(uid: String, uri: Uri): Result<String>
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
        saveLocalProfile(profile)
        cacheProfile(profile)

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
                "explanationLanguage" to profile.explanationLanguage.storageValue,
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

    override suspend fun uploadProfilePhoto(uid: String, uri: Uri): Result<String> {
        if (uid.isBlank()) {
            return Result.failure(IllegalArgumentException("You must be signed in to upload a photo."))
        }

        return try {
            val metadata = StorageMetadata.Builder()
                .setContentType(context?.contentResolver?.getType(uri) ?: "image/jpeg")
                .build()
            val storageRef = profilePhotoStorageRef(uid)
            storageRef.putFile(uri, metadata).await()
            Result.success(storageRef.downloadUrl.await().toString())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("FirebaseUserProfileRepo", "Profile photo upload failed: ${e.message}")
            Result.failure(e)
        }
    }

    private fun profilePhotoStorageRef(uid: String): StorageReference {
        val bucketUrl = BuildConfig.FIREBASE_STORAGE_BUCKET
        val storage = if (bucketUrl.isNotBlank()) {
            FirebaseStorage.getInstance(bucketUrl)
        } else {
            FirebaseStorage.getInstance()
        }
        return storage.reference.child("users/$uid/profile/avatar.jpg")
    }

    private companion object {
        // Shared by all repository instances so every screen observes the same profile.
        val savedProfiles = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    }
}
