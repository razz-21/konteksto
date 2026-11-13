package com.example.konteksto.ui.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.konteksto.ui.components.SessionLookup
import com.example.konteksto.ui.components.SessionUnavailableScreen

@Composable
fun PresentQrRoute(
    viewModel: PresentQrViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDownloadQrClick: (() -> Unit)? = null,
) {
    val sessionLookup by viewModel.sessionLookup.collectAsStateWithLifecycle()

    when (val lookup = sessionLookup) {
        is SessionLookup.Found -> PresentQrScreen(
            session = lookup.session,
            onBackClick = onBackClick,
            modifier = modifier,
            onDownloadQrClick = onDownloadQrClick,
        )

        else -> SessionUnavailableScreen(
            lookup = lookup,
            onBackClick = onBackClick,
            onRetryClick = viewModel::retrySessionLookup,
            modifier = modifier,
        )
    }
}
