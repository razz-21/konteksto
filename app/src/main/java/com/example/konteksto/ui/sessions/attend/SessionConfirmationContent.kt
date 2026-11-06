package com.example.konteksto.ui.sessions.attend

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.theme.KontekstoTheme

@Composable
internal fun SessionConfirmationContent(
    session: HostSession,
    onDismiss: () -> Unit,
    onBackToCode: () -> Unit,
    onConfirmJoin: () -> Unit,
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        AttendSheetHeader(
            title = "Session found",
            onDismiss = onDismiss,
            onBack = onBackToCode,
        )

        Spacer(modifier = Modifier.height(16.dp))

        SessionPreviewCard(session = session)

        Spacer(modifier = Modifier.height(14.dp))

        SessionDetailRow(
            label = "Speaker",
            value = session.speakerName,
            leadingContainerColor = Color(0xFF3B52D4),
        ) {
            Text(
                text = speakerInitials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        SessionDetailRow(
            label = "Room",
            value = session.venue,
            leadingContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_location),
                contentDescription = "Room Location",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onConfirmJoin,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B52D4),
                contentColor = Color.White,
            ),
        ) {
            Text(
                text = "Join session",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_forward),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Enter a different code",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                ),
                color = Color(0xFF3B52D4),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onBackToCode() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun SessionConfirmationContentPreview() {
    val sampleSession = HostSession(
        id = "1",
        venue = "CDO Ayala",
        time = "10:00 – 11:30 AM",
        talkCode = "4VY4N6",
        title = "Object Oriented Programming",
        speakerName = "Ernesto Razo",
        attendeesCount = 1,
        isLive = true,
    )
    KontekstoTheme {
        SessionConfirmationContent(
            session = sampleSession,
            onDismiss = {},
            onBackToCode = {},
            onConfirmJoin = {},
        )
    }
}
