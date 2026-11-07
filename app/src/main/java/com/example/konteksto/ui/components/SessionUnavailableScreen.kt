package com.example.konteksto.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.konteksto.R
import com.example.konteksto.ui.theme.KontekstoTheme

/** Stands in for a session screen while its session is loading, missing, or failed to load. */
@Composable
fun SessionUnavailableScreen(
    lookup: SessionLookup,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.action_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(22.dp),
                )
            }

            when (lookup) {
                SessionLookup.Loading, is SessionLookup.Found -> {
                    val loadingDescription = stringResource(R.string.session_loading_description)
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .semantics { contentDescription = loadingDescription },
                    )
                }

                SessionLookup.NotFound -> SessionUnavailableMessage(
                    title = stringResource(R.string.session_not_found_title),
                    message = stringResource(R.string.session_not_found_message),
                    actionLabel = stringResource(R.string.session_not_found_action),
                    onActionClick = onBackClick,
                    modifier = Modifier.align(Alignment.Center),
                )

                SessionLookup.Failed -> SessionUnavailableMessage(
                    title = stringResource(R.string.session_load_failed_title),
                    message = stringResource(R.string.session_load_failed_message),
                    actionLabel = stringResource(R.string.transcript_retry),
                    onActionClick = onRetryClick,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun SessionUnavailableMessage(
    title: String,
    message: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onActionClick,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(text = actionLabel)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionUnavailableScreenNotFoundPreview() {
    KontekstoTheme(darkTheme = false) {
        SessionUnavailableScreen(lookup = SessionLookup.NotFound, onBackClick = {}, onRetryClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionUnavailableScreenFailedDarkPreview() {
    KontekstoTheme(darkTheme = true) {
        SessionUnavailableScreen(lookup = SessionLookup.Failed, onBackClick = {}, onRetryClick = {})
    }
}
