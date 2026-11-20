package com.example.konteksto.ui.host.studio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.konteksto.R
import com.example.konteksto.ui.theme.KontekstoTheme

/** Tells the host, above the transcript, that Gemini failed and the phone's recognizer is transcribing now. */
@Composable
fun TranscriptionFallbackNotice(
    message: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_error_outline),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TranscriptionFallbackNoticePreview() {
    KontekstoTheme {
        TranscriptionFallbackNotice(message = stringResource(R.string.host_studio_transcription_fallback))
    }
}
