package com.example.konteksto.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.R
import com.example.konteksto.ui.theme.KontekstoTheme
import com.example.konteksto.ui.theme.extendedColors

@Composable
fun PermissionCard(
    iconRes: Int,
    title: String,
    description: String,
    isGranted: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.extendedColors.cardContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(Color(0xFFEEF2FF)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    tint = Color(0xFF3B52D4),
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    ),
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (isGranted) {
                Surface(
                    onClick = onActionClick,
                    shape = MaterialTheme.shapes.medium,
                    color = Color(0xFFECFDF5),
                    contentColor = Color(0xFF047857),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = "Enabled",
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Enabled",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            ),
                            color = Color(0xFF047857),
                        )
                    }
                }
            } else {
                Button(
                    onClick = onActionClick,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = Color(0xFF0F172A),
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Text(
                        text = "Enable",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        ),
                        color = Color(0xFF0F172A),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionCardGrantedPreview() {
    KontekstoTheme {
        PermissionCard(
            iconRes = R.drawable.ic_mic,
            title = "Microphone Access",
            description = "Captures live speaker audio for real-time transcription.",
            isGranted = true,
            onActionClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionCardNotGrantedPreview() {
    KontekstoTheme {
        PermissionCard(
            iconRes = R.drawable.ic_camera,
            title = "Camera Access",
            description = "Scans stage projector QR codes.",
            isGranted = false,
            onActionClick = {},
        )
    }
}
