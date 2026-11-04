package com.example.konteksto.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.R
import com.example.konteksto.ui.theme.KontekstoTheme

@Composable
fun StepFourContent(
    isMicGranted: Boolean,
    isCameraGranted: Boolean,
    isConceptSyncEnabled: Boolean,
    onRequestMic: () -> Unit,
    onRequestCamera: () -> Unit,
    onToggleConceptSync: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "04 · Device Permissions",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
            ),
            color = Color(0xFF3B52D4),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Enable permissions for Konteksto to work.",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 38.sp,
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Grant access below so Konteksto can capture live seminar audio, scan projector QR codes, and deliver real-time concept cards.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        PermissionCard(
            iconRes = R.drawable.ic_mic,
            title = "Microphone Access",
            description = "Captures live speaker audio for real-time transcription and jargon detection.",
            isGranted = isMicGranted,
            onActionClick = onRequestMic,
        )

        Spacer(modifier = Modifier.height(14.dp))

        PermissionCard(
            iconRes = R.drawable.ic_camera,
            title = "Camera Access",
            description = "Scans stage projector QR codes to join live conference rooms.",
            isGranted = isCameraGranted,
            onActionClick = onRequestCamera,
        )

        Spacer(modifier = Modifier.height(14.dp))

        PermissionCard(
            iconRes = R.drawable.ic_bell,
            title = "Live Concept Sync",
            description = "Receives real-time slide takeaways and terminology cards during sessions.",
            isGranted = isConceptSyncEnabled,
            onActionClick = onToggleConceptSync,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_verified_shield),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF34C759),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Audio is processed exclusively for live session comprehension.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StepFourContentPreview() {
    KontekstoTheme {
        StepFourContent(
            isMicGranted = true,
            isCameraGranted = false,
            isConceptSyncEnabled = true,
            onRequestMic = {},
            onRequestCamera = {},
            onToggleConceptSync = {},
        )
    }
}
