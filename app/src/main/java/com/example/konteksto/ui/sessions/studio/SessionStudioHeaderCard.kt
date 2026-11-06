package com.example.konteksto.ui.sessions.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

private const val DEFAULT_HERO_IMAGE_URL =
    "https://images.unsplash.com/photo-1540575467063-178a50c2df87?q=80&w=1000"

@Composable
fun SessionStudioHeaderCard(
    session: HostSession,
    modifier: Modifier = Modifier,
) {
    // Calculate speaker initials
    val speakerInitials = remember(session.speakerName) {
        val name = session.speakerName.ifBlank { "Ernesto Razo" }
        name.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "ER" }
    }

    val speakerName = session.speakerName.ifBlank { "Ernesto Razo" }
    val titleText = session.title.ifBlank { "Object Oriented Programming" }
    val categoryText = session.category?.takeIf { it.isNotBlank() } ?: "Technology & digital"
    val venueText = session.venue.ifBlank { "CDO Ayala" }

    val timeLabel = remember(session.time, session.startTime, session.endTime) {
        if (session.time.isNotBlank()) {
            session.time
        } else if (!session.startTime.isNullOrBlank() && !session.endTime.isNullOrBlank()) {
            "${session.startTime} – ${session.endTime}"
        } else {
            "10:00 – 11:30 AM"
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFEAECF0),
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Speaker Avatar Initials Circle
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCE2FF)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = speakerInitials,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            ),
                            color = Color(0xFF152270),
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
                            color = Color(0xFF667085),
                        )
                    }
                }

                // Venue & Time Pill Badges Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Venue Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF2F4F7),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_location),
                                contentDescription = "Venue",
                                tint = Color(0xFF475467),
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = venueText,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                ),
                                color = Color(0xFF344054),
                            )
                        }
                    }

                    // Time Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF2F4F7),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_clock),
                                contentDescription = "Time",
                                tint = Color(0xFF475467),
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = timeLabel,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                ),
                                color = Color(0xFF344054),
                            )
                        }
                    }
                }
            }
        }
    }
}
