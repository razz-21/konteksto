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
) {
    val avatarGradientColors: List<Color>
        get() = AvatarGradients.getOrElse(avatarColorIndex) { AvatarGradients[0] }

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
                avatarColorIndex = 3,
            )
        }
    }
}
