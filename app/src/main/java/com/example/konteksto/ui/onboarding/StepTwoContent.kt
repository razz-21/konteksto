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
fun StepTwoContent(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "02 · Core Features",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
            ),
            color = Color(0xFF3B52D4),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Real-time clarity tailored to how you learn.",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 38.sp,
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Konteksto listens alongside you and builds a personalized companion feed as the session unfolds.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(28.dp))

        FeatureItemRow(
            iconRes = R.drawable.ic_live_broadcast,
            title = "Live Jargon Detection",
            description = "Automatically flags specialized terms in the live transcript and generates bite-sized concept cards.",
        )

        Spacer(modifier = Modifier.height(20.dp))

        FeatureItemRow(
            iconRes = R.drawable.ic_language,
            title = "Tagalog & English Support",
            description = "Supports live transcription in both Tagalog and English so you can follow along without missing key concepts.",
        )

        Spacer(modifier = Modifier.height(20.dp))

        FeatureItemRow(
            iconRes = R.drawable.ic_nav_glossary,
            title = "Personal Concept Glossary",
            description = "Save and filter key terms from every talk you attend for post-conference review.",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StepTwoContentPreview() {
    KontekstoTheme {
        StepTwoContent()
    }
}
