package com.example.konteksto.ui.host.studio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EditContextNoteRoute(
    viewModel: HostStudioViewModel,
    noteId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EditContextNoteScreen(
        note = uiState.contextNotes.find { it.id == noteId },
        onBackClick = onBackClick,
        onSaveClick = { editedNote ->
            viewModel.updateContextNote(editedNote)
            onBackClick()
        },
        modifier = modifier,
    )
}
