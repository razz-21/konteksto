package com.example.konteksto.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.R
import com.example.konteksto.ui.theme.KontekstoTheme

/**
 * Marks a session the host has started, laid over its banner image. Pass [label] to shorten the
 * text and [showWaveform] to add the audio waveform after it.
 */
@Composable
fun LiveOnAirBadge(
    modifier: Modifier = Modifier,
    label: String = stringResource(id = R.string.host_live_on_air),
    showWaveform: Boolean = false,
) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.error,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier = Modifier
                .size(6.dp)
                .background(MaterialTheme.colorScheme.onError, CircleShape),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
            ),
            color = MaterialTheme.colorScheme.onError,
        )
        if (showWaveform) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_audio_waveform),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onError,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LiveOnAirBadgeLightPreview() {
    KontekstoTheme(darkTheme = false) {
        LiveOnAirBadge()
    }
}

@Preview(showBackground = true)
@Composable
private fun LiveOnAirBadgeDarkPreview() {
    KontekstoTheme(darkTheme = true) {
        LiveOnAirBadge()
    }
}

@Preview(showBackground = true)
@Composable
private fun LiveOnAirBadgeWaveformPreview() {
    KontekstoTheme(darkTheme = false) {
        LiveOnAirBadge(
            label = stringResource(id = R.string.session_live),
            showWaveform = true,
        )
    }
}
