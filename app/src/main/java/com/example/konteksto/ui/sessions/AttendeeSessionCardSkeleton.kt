package com.example.konteksto.ui.sessions

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.konteksto.ui.theme.KontekstoTheme
import com.example.konteksto.ui.theme.extendedColors

@Composable
fun AttendeeSessionCardSkeleton(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_transition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton_alpha",
    )

    val skeletonColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.extendedColors.cardContainer,
        ),
    ) {
        Column {
            // Banner Image Skeleton Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(skeletonColor),
            ) {
                // Category Tag Pill Skeleton
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .size(width = 90.dp, height = 22.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                )

                // Talk Code Badge Pill Skeleton
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(width = 65.dp, height = 22.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                )
            }

            // Card Content Skeleton
            Column(modifier = Modifier.padding(18.dp)) {
                // Header Row (Venue & Time)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 120.dp, height = 14.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(skeletonColor),
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 60.dp, height = 14.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(skeletonColor),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Session Title Skeleton (2 lines)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(18.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(skeletonColor),
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(18.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(skeletonColor),
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Speaker Row Skeleton
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(skeletonColor),
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(width = 130.dp, height = 14.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(skeletonColor),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Row Skeleton
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 100.dp, height = 14.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(skeletonColor),
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 80.dp, height = 14.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(skeletonColor),
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Button Skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(skeletonColor),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AttendeeSessionCardSkeletonLightPreview() {
    KontekstoTheme(darkTheme = false) {
        AttendeeSessionCardSkeleton(modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun AttendeeSessionCardSkeletonDarkPreview() {
    KontekstoTheme(darkTheme = true) {
        AttendeeSessionCardSkeleton(modifier = Modifier.padding(16.dp))
    }
}
