package com.example.konteksto.ui.host.studio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EditSlideRoute(
    viewModel: HostStudioViewModel,
    slideId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EditSlideScreen(
        slide = uiState.analyzedSlides.find { it.id == slideId },
        onBackClick = onBackClick,
        onSaveClick = { editedSlide ->
            viewModel.updateAnalyzedSlide(editedSlide)
            onBackClick()
        },
        modifier = modifier,
    )
}
