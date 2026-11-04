package com.example.konteksto.ui.sessions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.konteksto.R
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.theme.KontekstoTheme

@Composable
fun AttendeeSessionCard(
    session: HostSession,
    isAttending: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val speakerInitials = remember(session.speakerName) {
        session.speakerName
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "K" }
    }

    val tagText = remember(session.category, session.venue) {
        session.category?.takeIf { it.isNotBlank() } ?: session.venue
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            // Banner Image Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            ) {
                AsyncImage(
                    model = session.bannerImageUrl ?: "https://images.unsplash.com/photo-1540575467063-178a50c2df87?q=80&w=1000",
                    contentDescription = "Session Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Category / Venue Pill Badge in Vibrant Blue
                if (tagText.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        shape = MaterialTheme.shapes.extraSmall,
                        color = Color(0xFF3B52D4),
                    ) {
                        Text(
                            text = tagText.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            ),
                            color = Color.White,
                        )
                    }
                }

                // Talk Code Badge in Vibrant Blue
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = Color(0xFF3B52D4),
                ) {
                    Text(
                        text = session.talkCode,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        ),
                        color = Color.White,
                    )
                }
            }

            // Card Content
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = session.venue,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = session.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                    ),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Speaker Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B52D4)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = speakerInitials,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = session.speakerName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Audiences count & Status Live on air Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Audiences / Joined count
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_people),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF3B52D4),
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = session.attendeesCount.toString(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = if (session.attendeesCount == 1) "audience" else "audiences",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Live Status Indicator
                    if (session.isLive || session.status.equals("live", ignoreCase = true)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(
                                        color = Color(0xFFDC2626),
                                        shape = CircleShape,
                                    ),
                            )

                            Spacer(modifier = Modifier.width(5.dp))

                            Text(
                                text = "LIVE ON AIR",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Color(0xFFDC2626),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Enter Room Action Button
                Button(
                    onClick = onActionClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B52D4),
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_live_broadcast),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enter Room",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_forward),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
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
        venue = "Tech Summit Hall B",
        time = "10:00 AM",
        talkCode = "TALK-415",
        title = "Edge AI & Distributed Cloud Architecture",
        speakerName = "Engr. Marco Valerio",
        attendeesCount = 120,
        isLive = true,
        category = "Technology & Digital",
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
        venue = "Tech Summit Hall B",
        time = "10:00 AM",
        talkCode = "TALK-415",
        title = "Edge AI & Distributed Cloud Architecture",
        speakerName = "Engr. Marco Valerio",
        attendeesCount = 120,
        isLive = true,
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
