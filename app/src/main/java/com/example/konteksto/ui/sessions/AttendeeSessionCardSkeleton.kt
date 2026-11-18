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
import androidx.compose.foundation.shape.RoundedCornerShape
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.extendedColors.cardContainer,
        ),
    ) {
        Column {
            // Banner Skeleton with the category pill and two title lines over it
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .background(skeletonColor),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .size(width = 120.dp, height = 24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(20.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(20.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Metadata Chips Skeleton
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(130.dp, 80.dp).forEach { chipWidth ->
                        Box(
                            modifier = Modifier
                                .size(width = chipWidth, height = 24.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(skeletonColor),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(90.dp, 90.dp).forEach { chipWidth ->
                        Box(
                            modifier = Modifier
                                .size(width = chipWidth, height = 24.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(skeletonColor),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Speaker + Enter Room Row Skeleton
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(skeletonColor),
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 80.dp, height = 14.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(skeletonColor),
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 56.dp, height = 12.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(skeletonColor),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(width = 132.dp, height = KontekstoButtonDefaults.Height)
                            .clip(KontekstoButtonDefaults.shape)
                            .background(skeletonColor),
                    )
                }
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
