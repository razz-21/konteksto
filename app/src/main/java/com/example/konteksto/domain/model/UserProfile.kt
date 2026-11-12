package com.example.konteksto.domain.model

import androidx.compose.ui.graphics.Color

val AvatarGradients = listOf(
    listOf(Color(0xFF5A75FF), Color(0xFF3A55E0)), // 0: Blue
    listOf(Color(0xFFB866EE), Color(0xFF8D37D7)), // 1: Purple
    listOf(Color(0xFF4DB6AC), Color(0xFF00897B)), // 2: Teal
    listOf(Color(0xFFF97316), Color(0xFFDC2626)), // 3: Orange/Red
    listOf(Color(0xFFE94A75), Color(0xFFF89A52)), // 4: Coral/Pink
)

data class UserProfile(
    val uid: String,
    val name: String,
    val role: String,
    val email: String,
    val organization: String,
    val bio: String,
    val photoUrl: String? = null,
    val avatarColorIndex: Int = 3,
    val explanationLanguage: ExplanationLanguage = ExplanationLanguage.Default,
) {
    val avatarGradientColors: List<Color>
        get() = AvatarGradients.getOrElse(avatarColorIndex) { AvatarGradients[0] }

    val initials: String
        get() {
            val source = name.takeIf { it.isNotBlank() }
                ?: email.takeIf { it.isNotBlank() }
                ?: "K"

            return source
                .trim()
                .split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercase() }
                .ifEmpty { "K" }
        }

    companion object {

        // Firestore is the main source, with Firebase Auth as a fallback.
        fun merge(user: AuthUser, savedProfile: UserProfile?): UserProfile {
            val authProfile = fromAuthUser(user)
            if (savedProfile == null) return authProfile

            return savedProfile.copy(
                name = savedProfile.name.ifBlank { authProfile.name },
                email = authProfile.email.ifBlank { savedProfile.email },
                // A saved profile owns its photo field, so removing the photo stays removed
                // instead of falling back to the Google account photo.
                photoUrl = savedProfile.photoUrl,
                avatarColorIndex = savedProfile.avatarColorIndex,
                explanationLanguage = savedProfile.explanationLanguage,
            )
        }

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
                avatarColorIndex = 3,
                explanationLanguage = ExplanationLanguage.Default,
            )
        }
    }
}
