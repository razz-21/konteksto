package com.example.konteksto.ui.host.studio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.R
import com.example.konteksto.domain.model.AudioInputDevice
import com.example.konteksto.domain.model.AudioInputType
import com.example.konteksto.ui.theme.KontekstoTheme

/** Shows which microphone the live transcript is recording from. */
@Composable
fun TranscriptInputDevicePill(
    device: AudioInputDevice,
    modifier: Modifier = Modifier,
) {
    val label = when (device.type) {
        AudioInputType.BuiltInMic -> stringResource(R.string.host_studio_input_builtin_mic)
        AudioInputType.WiredHeadset -> stringResource(R.string.host_studio_input_wired_headset)
        AudioInputType.Usb -> device.productName.ifBlank { stringResource(R.string.host_studio_input_usb) }
        AudioInputType.Bluetooth -> device.productName.ifBlank { stringResource(R.string.host_studio_input_bluetooth) }
    }
    val description = stringResource(R.string.host_studio_input_device_description, label)

    Surface(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_mic),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.5.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TranscriptInputDevicePillPreview() {
    KontekstoTheme {
        TranscriptInputDevicePill(device = AudioInputDevice(AudioInputType.Usb, productName = "Rode NT-USB Mini"))
    }
}
