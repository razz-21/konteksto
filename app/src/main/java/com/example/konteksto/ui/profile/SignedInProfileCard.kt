package com.example.konteksto.ui.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.konteksto.R
import com.example.konteksto.domain.model.UserProfile
import com.example.konteksto.ui.components.rememberDebouncedClick
import com.example.konteksto.ui.theme.extendedColors

/** Number of bio lines shown before the "Read More" toggle appears. */
private const val BIO_PREVIEW_LINES = 4

/** Duration of the bio expand/collapse animation. */
private const val BIO_ANIMATION_DURATION_MS = 300

@Composable
fun SignedInProfileHeaderCard(
    profile: UserProfile,
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit = {},
) {
    val initials = remember(profile.name, profile.email) { profile.initials }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.extendedColors.cardContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatar(profile = profile, initials = initials)

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name.ifBlank { "Konteksto User" },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = profile.email.ifBlank { "No email available" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                onClick = rememberDebouncedClick(onClick = onEditClick),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(40.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "Edit Profile",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun AboutYouSectionCard(
    profile: UserProfile,
    modifier: Modifier = Modifier,
) {
    val bioPlaceholder = stringResource(R.string.profile_bio_placeholder)
    val bioText = profile.bio.ifBlank { bioPlaceholder }
    var isBioExpanded by rememberSaveable(profile.bio) { mutableStateOf(false) }
    var isBioOverflowing by remember(profile.bio) { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "About you",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.extendedColors.cardContainer,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Item 1: Role Row (Display-only)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_badge),
                            contentDescription = "Role",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = "Role",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )

                    Text(
                        text = profile.role.ifBlank { "Attendee" },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                // Item 2: Organization Row (Display-only)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_business),
                            contentDescription = "Organization",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Organization",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = profile.organization.ifBlank { "Add where you work or study" },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                // Item 3: Bio Row (Display-only)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    // Top alignment keeps the icon beside the "Bio" label when the bio expands.
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_align_left),
                            contentDescription = "Bio",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bio",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // AnimatedContent clips the taller version while its height animates,
                        // so the bio slides open and closed instead of jumping.
                        AnimatedContent(
                            targetState = isBioExpanded,
                            transitionSpec = {
                                (fadeIn(tween(BIO_ANIMATION_DURATION_MS)) togetherWith
                                    fadeOut(tween(BIO_ANIMATION_DURATION_MS)))
                                    .using(
                                        SizeTransform(clip = true) { _, _ ->
                                            tween(
                                                durationMillis = BIO_ANIMATION_DURATION_MS,
                                                easing = FastOutSlowInEasing,
                                            )
                                        },
                                    )
                            },
                            contentAlignment = Alignment.TopStart,
                            label = "BioExpansion",
                        ) { expanded ->
                            Text(
                                text = bioText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (expanded) Int.MAX_VALUE else BIO_PREVIEW_LINES,
                                overflow = TextOverflow.Ellipsis,
                                onTextLayout = { layout ->
                                    if (!expanded) {
                                        isBioOverflowing = layout.hasVisualOverflow
                                    }
                                },
                            )
                        }

                        // Only long bios get the toggle, so short ones stay unchanged.
                        if (isBioOverflowing || isBioExpanded) {
                            TextButton(
                                onClick = { isBioExpanded = !isBioExpanded },
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary,
                                ),
                            ) {
                                Text(
                                    text = if (isBioExpanded) {
                                        stringResource(R.string.profile_bio_show_less)
                                    } else {
                                        stringResource(R.string.profile_bio_read_more)
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(
    profile: UserProfile,
    initials: String,
) {
    Box(modifier = Modifier.size(64.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(profile.avatarGradientColors),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (!profile.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = profile.photoUrl,
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = 2.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xFF34C759))
                .border(
                    width = 2.dp,
                    color = MaterialTheme.extendedColors.cardContainer,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_verified_shield),
                contentDescription = "Verified",
                modifier = Modifier.size(12.dp),
                tint = Color.White,
            )
        }
    }
}
