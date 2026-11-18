package com.example.konteksto.ui.host.studio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.konteksto.R
import com.example.konteksto.ui.theme.KontekstoTheme

/** Shows whether Gemini or the phone's own speech recognizer is writing the live transcript. */
@Composable
fun TranscriptionEnginePill(
    engine: TranscriptionEngineType,
    modifier: Modifier = Modifier,
) {
    val (iconRes, label) = when (engine) {
        TranscriptionEngineType.Gemini -> R.drawable.ic_sparkle to stringResource(R.string.host_studio_engine_gemini)
        TranscriptionEngineType.PhoneRecognizer -> R.drawable.ic_smartphone to stringResource(R.string.host_studio_engine_phone)
    }

    TranscriptStatusPill(
        iconRes = iconRes,
        label = label,
        description = stringResource(R.string.host_studio_engine_description, label),
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun TranscriptionEnginePillPreview() {
    KontekstoTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TranscriptionEnginePill(engine = TranscriptionEngineType.Gemini)
            TranscriptionEnginePill(engine = TranscriptionEngineType.PhoneRecognizer)
        }
    }
}
