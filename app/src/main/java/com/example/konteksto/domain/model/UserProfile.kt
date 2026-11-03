package com.example.konteksto.domain.model

data class UserProfile(
    val uid: String,
    val name: String,
    val role: String,
    val email: String,
    val organization: String,
    val bio: String,
    val photoUrl: String? = null,
) {
    companion object {

        // Used when the user has no Firestore profile document yet.
        fun fromAuthUser(user: AuthUser): UserProfile {
            val fallbackName = user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                ?: "Konteksto User"

            return UserProfile(
                uid = user.uid,
                name = fallbackName,
                role = "Attendee",
                email = user.email ?: "",
                organization = "",
                bio = "",
                photoUrl = user.photoUrl,
            )
        }
    }
}