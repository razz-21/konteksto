package com.example.konteksto.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.domain.model.AuthUser
import com.example.konteksto.domain.model.UserProfile
import com.example.konteksto.ui.login.GoogleSignInButton
import com.example.konteksto.ui.theme.KontekstoTheme

@Composable
fun ProfileScreenContent(
    currentUser: AuthUser?,
    modifier: Modifier = Modifier,
    profile: UserProfile? = null,
    isDarkMode: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {},
    isTaglish: Boolean = false,
    onLanguageChange: (Boolean) -> Unit = {},
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
                    shape = MaterialTheme.shapes.small,
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
                    shape = MaterialTheme.shapes.small,
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Profile & Settings",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Manage your attendee identity and app preferences",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Use Firestore profile, then fall back to Firebase Auth.
            val shownProfile = profile ?: currentUser?.let { UserProfile.fromAuthUser(it) }

            if (shownProfile != null) {
                SignedInProfileCard(
                    profile = shownProfile,
                    onEditClick = onEditProfileClick,
                )
            } else {
                SignedOutProfileCard()
            }

            Spacer(modifier = Modifier.height(16.dp))

            ThemeAppearanceCard(
                isDarkMode = isDarkMode,
                onDarkModeChange = onDarkModeChange,
            )

            Spacer(modifier = Modifier.height(16.dp))

            ExplanationLanguageCard(
                isTaglish = isTaglish,
                onLanguageChange = onLanguageChange,
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

            Spacer(modifier = Modifier.height(24.dp))

            if (currentUser != null) {
                Button(
                    onClick = { showSignOutDialog = true },
                    enabled = !isLoading,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                ) {
                    Text(
                        text = "Sign Out",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        ),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                GoogleSignInButton(
                    onClick = onGoogleSignInClick,
                    isLoading = isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
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