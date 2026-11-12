package com.example.konteksto.ui.profile

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.konteksto.R
import com.example.konteksto.domain.model.AvatarGradients
import com.example.konteksto.domain.model.UserProfile
import com.example.konteksto.ui.theme.KontekstoButtonDefaults

@Composable
fun EditProfileScreen(
    profile: UserProfile,
    onBackClick: () -> Unit,
    onSaveClick: (name: String, role: String, organization: String, bio: String, avatarColorIndex: Int, photoUrl: String?) -> Unit,
    modifier: Modifier = Modifier,
    isSaving: Boolean = false,
    errorMessage: String? = null,
) {
    var name by rememberSaveable(profile.uid) { mutableStateOf(profile.name) }
    var role by rememberSaveable(profile.uid) { mutableStateOf(profile.role) }
    var organization by rememberSaveable(profile.uid) { mutableStateOf(profile.organization) }
    var bio by rememberSaveable(profile.uid) { mutableStateOf(profile.bio) }
    var photoUrl by rememberSaveable(profile.uid, profile.photoUrl) { mutableStateOf(profile.photoUrl) }
    var selectedGradientIndex by rememberSaveable(profile.uid, profile.avatarColorIndex) {
        mutableStateOf(profile.avatarColorIndex)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            photoUrl = uri.toString()
        }
    }

    val initials = remember(name, profile.email) {
        val source = name.takeIf { it.isNotBlank() }
            ?: profile.email.takeIf { it.isNotBlank() }
            ?: "K"

        source
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "K" }
    }

    BackHandler { onBackClick() }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            // Top Bar: Back button & Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBackClick,
                    enabled = !isSaving,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "Back",
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Edit profile",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Centered Avatar & Photo Action Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Large Circular Avatar Preview
                Box(modifier = Modifier.size(96.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    AvatarGradients.getOrElse(selectedGradientIndex) {
                                        AvatarGradients[0]
                                    },
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Text(
                                text = initials,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                            )
                        }
                    }

                    // Verified shield badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34C759))
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.background,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_verified_shield),
                            contentDescription = "Verified",
                            modifier = Modifier.size(15.dp),
                            tint = Color.White,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Upload & Remove Photo Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        enabled = !isSaving,
                        shape = KontekstoButtonDefaults.shape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(KontekstoButtonDefaults.Height),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_camera),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Upload Photo",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    if (!photoUrl.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = { photoUrl = null },
                            enabled = !isSaving,
                            shape = KontekstoButtonDefaults.shape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(KontekstoButtonDefaults.Height),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_delete),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Remove Photo",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                ),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Avatar color",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Gradient Chips Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarGradients.forEachIndexed { index, colors ->
                        val isSelected = selectedGradientIndex == index

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            shape = CircleShape,
                                        )
                                    } else {
                                        Modifier
                                    },
                                )
                                .padding(if (isSelected) 3.dp else 0.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(colors))
                                .clickable { selectedGradientIndex = index },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check),
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section Header: "Your info"
            Text(
                text = "Your info",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            val textFieldShape = RoundedCornerShape(12.dp)

            val disabledTextFieldColors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )

            val activeTextFieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Full Name (Editable)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full name") },
                placeholder = { Text("Enter your full name") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_badge),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                singleLine = true,
                enabled = !isSaving,
                shape = textFieldShape,
                colors = activeTextFieldColors,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Email (Disabled + Lock icon + Helper text)
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = profile.email,
                    onValueChange = {},
                    label = { Text("Email") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_email),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_lock),
                            contentDescription = "Locked",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    },
                    singleLine = true,
                    enabled = false,
                    shape = textFieldShape,
                    colors = disabledTextFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Your sign-in email can't be changed here.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Role or title (Editable)
            OutlinedTextField(
                value = role,
                onValueChange = { role = it },
                label = { Text("Role or title") },
                placeholder = { Text("Attendee") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_badge),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                singleLine = true,
                enabled = !isSaving,
                shape = textFieldShape,
                colors = activeTextFieldColors,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Organization (Editable)
            OutlinedTextField(
                value = organization,
                onValueChange = { organization = it },
                label = { Text("Organization") },
                placeholder = { Text("Where you work or study") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_business),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                singleLine = true,
                enabled = !isSaving,
                shape = textFieldShape,
                colors = activeTextFieldColors,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Bio (Editable multi-line)
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                placeholder = { Text("Tell speakers a little about yourself") },
                leadingIcon = {
                    Box(
                        modifier = Modifier.padding(top = 12.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_align_left),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                minLines = 3,
                maxLines = 6,
                enabled = !isSaving,
                shape = textFieldShape,
                colors = activeTextFieldColors,
                modifier = Modifier.fillMaxWidth(),
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = textFieldShape,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Bottom Action Buttons: Cancel & Save Changes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onBackClick,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(KontekstoButtonDefaults.Height),
                    shape = KontekstoButtonDefaults.shape,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        ),
                    )
                }

                Button(
                    onClick = {
                        onSaveClick(
                            name,
                            role,
                            organization,
                            bio,
                            selectedGradientIndex,
                            photoUrl,
                        )
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(KontekstoButtonDefaults.Height),
                    shape = KontekstoButtonDefaults.shape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save changes",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
