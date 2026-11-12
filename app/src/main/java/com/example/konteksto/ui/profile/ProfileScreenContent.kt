package com.example.konteksto.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.R
import com.example.konteksto.domain.model.AuthUser
import com.example.konteksto.domain.model.ExplanationLanguage
import com.example.konteksto.domain.model.UserProfile
import com.example.konteksto.ui.login.GoogleSignInButton
import com.example.konteksto.ui.theme.KontekstoButtonDefaults
import com.example.konteksto.ui.theme.KontekstoTheme
import com.example.konteksto.ui.theme.extendedColors

@Composable
fun ProfileScreenContent(
    currentUser: AuthUser?,
    modifier: Modifier = Modifier,
    profile: UserProfile? = null,
    isDarkMode: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {},
    explanationLanguage: ExplanationLanguage = ExplanationLanguage.Default,
    onExplanationLanguageChange: (ExplanationLanguage) -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onEditProfileClick: () -> Unit = {},
    onGoogleSignInClick: () -> Unit = {},
    onConfirmSignOut: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text(
                    text = "Sign Out",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out of your account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        onConfirmSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    shape = KontekstoButtonDefaults.shape,
                    modifier = Modifier.height(KontekstoButtonDefaults.Height),
                ) {
                    Text(
                        text = "Sign Out",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignOutDialog = false },
                    shape = KontekstoButtonDefaults.shape,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                }
            },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Text(
                text = "Profile & settings",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Your attendee identity and app preferences",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Use Firestore profile, then fall back to Firebase Auth.
            val shownProfile = profile ?: currentUser?.let { UserProfile.fromAuthUser(it) }

            if (shownProfile != null) {
                SignedInProfileHeaderCard(
                    profile = shownProfile,
                    onEditClick = onEditProfileClick,
                )

                Spacer(modifier = Modifier.height(20.dp))

                AboutYouSectionCard(
                    profile = shownProfile,
                )
            } else {
                SignedOutProfileCard()
            }

            Spacer(modifier = Modifier.height(20.dp))

            PreferencesSectionCard(
                isDarkMode = isDarkMode,
                onDarkModeChange = onDarkModeChange,
                explanationLanguage = explanationLanguage,
                onExplanationLanguageChange = onExplanationLanguageChange,
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (currentUser != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable(enabled = !isLoading) { showSignOutDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.extendedColors.cardContainer,
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_logout),
                            contentDescription = "Sign out",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFFDC2626),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sign out",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            ),
                            color = Color(0xFFDC2626),
                        )
                    }
                }
            } else {
                GoogleSignInButton(
                    onClick = onGoogleSignInClick,
                    isLoading = isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenSignedInPreview() {
    KontekstoTheme(darkTheme = false) {
        ProfileScreenContent(
            currentUser = AuthUser(
                uid = "firebase_uid_123456789",
                email = "razz.ernesto@gmail.com",
                displayName = "Razz Ernesto",
                isAnonymous = false,
            ),
            profile = UserProfile(
                uid = "firebase_uid_123456789",
                name = "Razz Ernesto",
                role = "AI Systems & Cloud Engineer",
                email = "razz.ernesto@gmail.com",
                organization = "Google Developer Group / Tech Summit PH",
                bio = "Building real-time intelligent interfaces and on-device AI tools.",
            ),
            isDarkMode = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenSignedOutPreview() {
    KontekstoTheme(darkTheme = false) {
        ProfileScreenContent(
            currentUser = null,
        )
    }
}
