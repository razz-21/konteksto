package com.example.konteksto.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.konteksto.domain.model.AvatarGradients
import com.example.konteksto.ui.theme.KontekstoTheme

@Composable
fun UserAvatar(
    initials: String,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    photoUrl: String? = null,
    size: Dp = 36.dp,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(gradientColors))
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = contentDescription ?: "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UserAvatarPreview() {
    KontekstoTheme {
        UserAvatar(
            initials = "RE",
            gradientColors = AvatarGradients[3],
        )
    }
}
