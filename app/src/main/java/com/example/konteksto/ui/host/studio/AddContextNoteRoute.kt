package com.example.konteksto.ui.host.studio

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AddContextNoteRoute(
    viewModel: HostStudioViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AddContextNoteScreen(
        onBackClick = onBackClick,
        onSaveClick = { title, content ->
            viewModel.addContextNote(title = title, content = content)
            onBackClick()
        },
        modifier = modifier,
    )
}
