package com.example.konteksto.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.R
import com.example.konteksto.ui.theme.KontekstoTheme

@Composable
fun StepThreeContent(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "03 · Slide-Grounded & Host Console",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
            ),
            color = Color(0xFF3B52D4),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Anchored in the speaker’s actual presentation.",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 38.sp,
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "For both attendees and speakers, Konteksto connects spoken words directly to visual slides and diagrams.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(28.dp))

        FeatureItemRow(
            iconRes = R.drawable.ic_download,
            title = "Slide PDF Analysis",
            description = "Hosts upload their slide deck before the talk. Konteksto extracts diagrams, charts, and key takeaways to ground all answers.",
        )

        Spacer(modifier = Modifier.height(20.dp))

        FeatureItemRow(
            iconRes = R.drawable.ic_qr_code,
            title = "Instant QR & Code Room Sync",
            description = "Join any auditorium or lecture hall in one scan using the projector QR code or room code.",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StepThreeContentPreview() {
    KontekstoTheme {
        StepThreeContent()
    }
}
