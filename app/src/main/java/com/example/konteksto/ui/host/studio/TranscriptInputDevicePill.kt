package com.example.konteksto.ui.host.studio

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
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

    TranscriptStatusPill(
        iconRes = R.drawable.ic_mic,
        label = label,
        description = stringResource(R.string.host_studio_input_device_description, label),
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun TranscriptInputDevicePillPreview() {
    KontekstoTheme {
        TranscriptInputDevicePill(device = AudioInputDevice(AudioInputType.Usb, productName = "Rode NT-USB Mini"))
    }
}
