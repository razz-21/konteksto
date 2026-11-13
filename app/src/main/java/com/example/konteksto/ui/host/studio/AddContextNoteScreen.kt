package com.example.konteksto.ui.host.studio

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.konteksto.ui.theme.KontekstoTheme

@Composable
fun AddContextNoteScreen(
    onBackClick: () -> Unit,
    onSaveClick: (title: String, content: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ContextNoteFormScreen(
        screenTitle = "Add Context Note",
        screenSubtitle = null,
        formKey = null,
        initialTitle = "",
        initialContent = "",
        onBackClick = onBackClick,
        onSaveClick = onSaveClick,
        modifier = modifier,
        useVerticalButtonLayout = true,
    )
}

@Preview(showBackground = true)
@Composable
fun AddContextNoteScreenLightPreview() {
    KontekstoTheme(darkTheme = false) {
        AddContextNoteScreen(onBackClick = {}, onSaveClick = { _, _ -> })
    }
}

@Preview(showBackground = true)
@Composable
fun AddContextNoteScreenDarkPreview() {
    KontekstoTheme(darkTheme = true) {
        AddContextNoteScreen(onBackClick = {}, onSaveClick = { _, _ -> })
    }
}
