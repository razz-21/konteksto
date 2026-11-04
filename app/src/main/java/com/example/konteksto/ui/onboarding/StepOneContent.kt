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
import com.example.konteksto.ui.theme.KontekstoTheme

@Composable
fun StepOneContent(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "01 · What is Konteksto?",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
            ),
            color = Color(0xFF3B52D4),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Never lose the thread of a live talk again.",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 38.sp,
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Whether you are attending a live presentation, lecture, seminar, or key talk, speakers often use dense terminology and complex jargon that can leave listeners behind.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(28.dp))

        LiveTranscriptCard()
    }
}

@Preview(showBackground = true)
@Composable
private fun StepOneContentPreview() {
    KontekstoTheme {
        StepOneContent()
    }
}
