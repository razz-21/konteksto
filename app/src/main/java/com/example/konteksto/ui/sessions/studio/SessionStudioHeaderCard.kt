package com.example.konteksto.ui.sessions.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.konteksto.R
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.theme.extendedColors
import com.example.konteksto.ui.components.LiveOnAirBadge

private const val DEFAULT_HERO_IMAGE_URL =
    "https://images.unsplash.com/photo-1540575467063-178a50c2df87?q=80&w=1000"

@Composable
fun SessionStudioHeaderCard(
    session: HostSession,
    modifier: Modifier = Modifier,
) {
    // Details the host left blank are left out rather than filled in.
    val speakerName = session.speakerName.trim()
    val speakerInitials = remember(speakerName) {
        speakerName
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
    }

    val titleText = session.title.trim()
    val categoryText = session.category?.trim().orEmpty()
    val venueText = session.venue.trim()

    val timeLabel = remember(session.time, session.startTime, session.endTime) {
        if (session.time.isNotBlank()) {
            session.time
        } else if (!session.startTime.isNullOrBlank() && !session.endTime.isNullOrBlank()) {
            "${session.startTime} – ${session.endTime}"
        } else {
            ""
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.extendedColors.cardContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Section: Banner Image Card with Category Tag Chip & Overlay Title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            ) {
                // Background Banner Image
                AsyncImage(
                    model = session.bannerImageUrl?.takeIf { it.isNotBlank() } ?: DEFAULT_HERO_IMAGE_URL,
                    contentDescription = titleText,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                // Dark Gradient Shadow Overlay at the bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Black.copy(alpha = 0.88f),
                                ),
                            ),
                        ),
                )

                // Top-Left Category Tag Chip Pill
                if (categoryText.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                    ) {
                        Text(
                            text = categoryText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }

                if (session.isLive) {
                    LiveOnAirBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                    )
                }

                // Bottom Session Title
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 25.sp,
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp),
                )
            }

            // Bottom Section: Speaker Details & Venue / Time Pills
            if (speakerName.isNotEmpty() || venueText.isNotEmpty() || timeLabel.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Speaker Row
                    if (speakerName.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            // Speaker Avatar Initials Circle
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = speakerInitials,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = speakerName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "Speaker",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    // Venue & Time Pill Badges Row
                    if (venueText.isNotEmpty() || timeLabel.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            // Venue Pill
                            if (venueText.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_location),
                                            contentDescription = "Venue",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = venueText,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.5.sp,
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            // Time Pill
                            if (timeLabel.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_clock),
                                            contentDescription = "Time",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = timeLabel,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.5.sp,
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun SessionStudioHeaderCardShortVenuePreview() {
    val sampleSession = HostSession(
        id = "1",
        venue = "Room 301",
        time = "10:00 AM",
        talkCode = "RUMNXJ",
        title = "Cybersecurity Basics",
        speakerName = "Samantha Gayle Oliveros",
        category = "Technology & Digital",
        attendeesCount = 12,
        isLive = true,
    )
    com.example.konteksto.ui.theme.KontekstoTheme(darkTheme = true) {
        SessionStudioHeaderCard(session = sampleSession)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun SessionStudioHeaderCardLongVenuePreview() {
    val sampleSession = HostSession(
        id = "2",
        venue = "LAB 2 WAC Building 3rd Floor",
        time = "10:00 AM",
        talkCode = "RUMNXJ",
        title = "Cybersecurity Basics",
        speakerName = "Samantha Gayle Oliveros",
        category = "Technology & Digital",
        attendeesCount = 12,
        isLive = true,
    )
    com.example.konteksto.ui.theme.KontekstoTheme(darkTheme = true) {
        SessionStudioHeaderCard(session = sampleSession)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun SessionStudioHeaderCardMultiLineVenuePreview() {
    val sampleSession = HostSession(
        id = "3",
        venue = "Rodelsa Hall Liceo de Cagayan University, Sector 4 Main Auditorium",
        time = "10:00 AM – 11:30 AM",
        talkCode = "K38QZD",
        title = "Introduction to Ethical Hacking",
        speakerName = "Ernesto Razo",
        category = "Technology & Digital",
        attendeesCount = 50,
        isLive = true,
    )
    com.example.konteksto.ui.theme.KontekstoTheme(darkTheme = true) {
        SessionStudioHeaderCard(session = sampleSession)
    }
}
