package com.example.konteksto.ui.sessions.studio

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.R
import com.example.konteksto.domain.model.HostSession

@Suppress("DEPRECATION")
@Composable
fun SessionStudioTopBar(
    session: HostSession,
    onBackClick: () -> Unit,
    onPresentQrClick: (HostSession) -> Unit,
    onLeaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val talkCodeText = session.talkCode.ifBlank { "TALK–892" }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Back Button + Text Label ("Sessions")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable { onBackClick() }
                .padding(vertical = 6.dp, horizontal = 4.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = "Back to Sessions",
                tint = Color(0xFF3B52D4),
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Sessions",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
                color = Color(0xFF3B52D4),
            )
        }

        // Right side: Talk Code Badge Pill + Leave Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Talk Code Pill Button
            Surface(
                onClick = {
                    clipboardManager.setText(AnnotatedString(talkCodeText))
                    Toast.makeText(context, "Copied $talkCodeText to clipboard", Toast.LENGTH_SHORT).show()
                    onPresentQrClick(session)
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_copy),
                        contentDescription = "Copy Talk Code",
                        tint = Color(0xFF3B52D4),
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = talkCodeText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp,
                            letterSpacing = 0.5.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Leave Session Button (replaced bookmark icon)
            IconButton(
                onClick = onLeaveClick,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_leave),
                    contentDescription = "Leave Session",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
