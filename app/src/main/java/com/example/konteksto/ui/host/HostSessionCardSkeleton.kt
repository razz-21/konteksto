package com.example.konteksto.ui.host

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
import com.example.konteksto.ui.theme.KontekstoButtonDefaults
import com.example.konteksto.ui.theme.KontekstoTheme
import com.example.konteksto.ui.theme.extendedColors

@Composable
fun HostSessionCardSkeleton(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "host_skeleton_transition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "host_skeleton_alpha",
    )

    val skeletonColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.extendedColors.cardContainer,
        ),
    ) {
        Column {
            // Cover Image Banner Skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(135.dp)
                    .background(skeletonColor),
            ) {
                // Category Pill Skeleton
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .size(width = 96.dp, height = 22.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Session Title Skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(20.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(skeletonColor),
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Speaker Row Skeleton
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(skeletonColor),
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(width = 120.dp, height = 14.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(skeletonColor),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Metadata Chips Skeleton
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(84.dp, 96.dp, 88.dp).forEach { chipWidth ->
                        Box(
                            modifier = Modifier
                                .size(width = chipWidth, height = 26.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(skeletonColor),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Join Code Container Skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(skeletonColor),
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons Skeleton
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(KontekstoButtonDefaults.Height)
                                .clip(KontekstoButtonDefaults.shape)
                                .background(skeletonColor),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HostSessionCardSkeletonLightPreview() {
    KontekstoTheme(darkTheme = false) {
        HostSessionCardSkeleton(modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun HostSessionCardSkeletonDarkPreview() {
    KontekstoTheme(darkTheme = true) {
        HostSessionCardSkeleton(modifier = Modifier.padding(16.dp))
    }
}
