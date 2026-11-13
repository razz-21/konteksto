package com.example.konteksto.ui.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.konteksto.R
import com.example.konteksto.domain.model.ConferenceThemeBanner
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.domain.model.UserProfile
import com.example.konteksto.ui.components.LiveOnAirBadge
import com.example.konteksto.ui.components.SessionBadge
import com.example.konteksto.ui.theme.KontekstoButtonDefaults
import com.example.konteksto.ui.theme.KontekstoTheme
import com.example.konteksto.ui.theme.extendedColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttendeeSessionCard(
    session: HostSession,
    isAttending: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    currentProfile: UserProfile? = null,
    onShowMessage: (String) -> Unit = {},
) {
    val clipboardManager = LocalClipboardManager.current

    // Use the signed-in user's profile avatar when this is their own session.
    val ownProfile = currentProfile?.takeIf { profile ->
        session.hostUid.isNullOrBlank() ||
        session.hostUid == profile.uid ||
        session.speakerName.isBlank() ||
        session.speakerName.trim().equals(profile.name.trim(), ignoreCase = true)
    }
    val speakerName = ownProfile?.name ?: session.speakerName
    val speakerInitials = remember(speakerName, ownProfile) {
        ownProfile?.initials ?: speakerName
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "K" }
    }

    val bannerUrl = remember(session.bannerImageUrl, session.category) {
        session.bannerImageUrl?.ifBlank { null }
            ?: ConferenceThemeBanner.DefaultCategories.find {
                it.categoryName.equals(session.category, ignoreCase = true)
            }?.imageUrl
            ?: ConferenceThemeBanner.DefaultCategories.first().imageUrl
    }

    val categoryLabel = remember(session.category) {
        session.category?.ifBlank { null } ?: "Technology & digital"
    }

    // Matches the Host Sessions card so both lists look the same.
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onActionClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.extendedColors.cardContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Cover Image Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(135.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                AsyncImage(
                    model = bannerUrl,
                    contentDescription = session.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                // Category overlay pill on top left
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = categoryLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Live On Air badge overlay on top right if session is live
                if (session.isLive) {
                    LiveOnAirBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                    )
                }
            }

            // Card Details Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                // Session Title
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Speaker Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(
                                brush = if (ownProfile != null) {
                                    Brush.linearGradient(ownProfile.avatarGradientColors)
                                } else {
                                    SolidColor(MaterialTheme.colorScheme.primaryContainer)
                                },
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!ownProfile?.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ownProfile.photoUrl,
                                contentDescription = "Profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                            )
                        } else {
                            Text(
                                text = speakerInitials,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                ),
                                color = if (ownProfile != null) {
                                    Color.White
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = speakerName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Metadata Chips: Venue, Time, Attendees
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (session.venue.isNotBlank()) {
                        SessionBadge(
                            iconRes = R.drawable.ic_location,
                            text = session.venue,
                        )
                        if (session.time.isNotBlank()) {
                            SessionBadge(
                                iconRes = R.drawable.ic_clock,
                                text = session.time,
                            )
                        }
                        SessionBadge(
                            iconRes = R.drawable.ic_people,
                            text = "${session.attendeesCount} ${if (session.attendeesCount == 1) "attendee" else "attendees"}",
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Join Code Container
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_key),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = "Join code",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = session.talkCode,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        letterSpacing = 1.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(session.talkCode))
                                    onShowMessage("Join code copied!")
                                },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_copy),
                                    contentDescription = "Copy join code",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Enter Room Button
                    Button(
                        onClick = onActionClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(KontekstoButtonDefaults.Height),
                        shape = KontekstoButtonDefaults.shape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            text = "Enter room",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            ),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_forward),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AttendeeSessionCardLightPreview() {
    val sampleSession = HostSession(
        id = "1",
        venue = "CDO Ayala",
        time = "10:00 – 11:30 AM",
        talkCode = "4VY4N6",
        title = "Object Oriented Programming",
        speakerName = "Ernesto Razo",
        attendeesCount = 1,
        isLive = true,
        category = "Technology & digital",
    )
    KontekstoTheme(darkTheme = false) {
        AttendeeSessionCard(
            session = sampleSession,
            isAttending = false,
            onActionClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AttendeeSessionCardDarkPreview() {
    val sampleSession = HostSession(
        id = "1",
        venue = "CDO Ayala",
        time = "10:00 – 11:30 AM",
        talkCode = "4VY4N6",
        title = "Object Oriented Programming",
        speakerName = "Ernesto Razo",
        attendeesCount = 1,
        isLive = true,
        category = "Technology & digital",
    )
    KontekstoTheme(darkTheme = true) {
        AttendeeSessionCard(
            session = sampleSession,
            isAttending = false,
            onActionClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AttendeeSessionCardLongVenuePreview() {
    val sampleSession = HostSession(
        id = "2",
        venue = "Rodelsa Hall Liceo de Cagayan University",
        time = "10:00 AM – 11:30 PM",
        talkCode = "K38QZD",
        title = "Introduction to Ethical Hacking",
        speakerName = "ERNESTO RAZO",
        attendeesCount = 1,
        isLive = false,
        category = "Technology & Digital",
    )
    KontekstoTheme(darkTheme = true) {
        AttendeeSessionCard(
            session = sampleSession,
            isAttending = false,
            onActionClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
