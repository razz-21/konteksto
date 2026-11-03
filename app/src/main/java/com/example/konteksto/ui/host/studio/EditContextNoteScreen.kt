package com.example.konteksto.ui.host.studio

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.konteksto.ui.theme.KontekstoTheme

@Composable
fun EditContextNoteScreen(
    note: ContextNoteItem?,
    onBackClick: () -> Unit,
    onSaveClick: (ContextNoteItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    ContextNoteFormScreen(
        screenTitle = "Edit Context Note",
        screenSubtitle = note?.category,
        formKey = note?.id,
        initialTitle = note?.title.orEmpty(),
        initialContent = note?.content.orEmpty(),
        isAvailable = note != null,
        onBackClick = onBackClick,
        onSaveClick = { title, content ->
            note?.let { onSaveClick(it.withEdits(title, content)) }
        },
        modifier = modifier,
    )
}

private val previewNote = ContextNoteItem(
    id = "note_1",
    category = "BACKGROUND NOTE",
    title = "POP",
    content = "Means Point of Presence in this talk, not the protocol.",
    timestamp = "10:00 AM",
)

@Preview(showBackground = true)
@Composable
fun EditContextNoteScreenLightPreview() {
    KontekstoTheme(darkTheme = false) {
        EditContextNoteScreen(note = previewNote, onBackClick = {}, onSaveClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun EditContextNoteScreenDarkPreview() {
    KontekstoTheme(darkTheme = true) {
        EditContextNoteScreen(note = previewNote, onBackClick = {}, onSaveClick = {})
    }
}
