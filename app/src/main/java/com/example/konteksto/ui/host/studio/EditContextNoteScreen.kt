package com.example.konteksto.ui.host.studio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.konteksto.ui.theme.KontekstoTheme

@Composable
fun EditContextNoteScreen(
    note: ContextNoteItem?,
    onBackClick: () -> Unit,
    onSaveClick: (ContextNoteItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val noteId = note?.id
    var title by rememberSaveable(noteId) { mutableStateOf(note?.title.orEmpty()) }
    var content by rememberSaveable(noteId) { mutableStateOf(note?.content.orEmpty()) }
    var titleError by rememberSaveable { mutableStateOf<String?>(null) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    val hasChanges = note != null && (
        title.trim() != note.title.trim() ||
            content.trim() != note.content.trim()
        )

    val onBackRequested = {
        if (hasChanges) showDiscardDialog = true else onBackClick()
    }

    BackHandler(enabled = hasChanges) { showDiscardDialog = true }

    if (showDiscardDialog) {
        DiscardChangesDialog(
            onConfirm = {
                showDiscardDialog = false
                onBackClick()
            },
            onDismiss = { showDiscardDialog = false },
        )
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EditFormTopBar(
                title = "Edit Context Note",
                subtitle = note?.category,
                saveEnabled = hasChanges,
                onBackClick = onBackRequested,
                onSaveClick = {
                    if (note != null) {
                        if (title.isBlank()) {
                            titleError = "Title is required"
                        } else {
                            onSaveClick(note.withEdits(title, content))
                        }
                    }
                },
            )

            if (note == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "This note is no longer available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                EditFormField(
                    label = "Title / Concept *",
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = null
                    },
                    errorMessage = titleError,
                    singleLine = true,
                )
                EditFormField(
                    label = "Details / Summary",
                    value = content,
                    onValueChange = { content = it },
                )
            }
        }
    }
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
