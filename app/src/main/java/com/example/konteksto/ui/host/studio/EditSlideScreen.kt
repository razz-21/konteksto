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
fun EditSlideScreen(
    slide: AnalyzedSlideItem?,
    onBackClick: () -> Unit,
    onSaveClick: (AnalyzedSlideItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val slideId = slide?.id
    var title by rememberSaveable(slideId) { mutableStateOf(slide?.title.orEmpty()) }
    var summary by rememberSaveable(slideId) { mutableStateOf(slide?.summary.orEmpty()) }
    var explanation by rememberSaveable(slideId) { mutableStateOf(slide?.explanation.orEmpty()) }
    var keyTakeaway by rememberSaveable(slideId) { mutableStateOf(slide?.keyTakeaway.orEmpty()) }
    var titleError by rememberSaveable { mutableStateOf<String?>(null) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    val hasChanges = slide != null && (
        title.trim() != slide.title.trim() ||
            summary.trim() != slide.summary.orEmpty().trim() ||
            explanation.trim() != slide.explanation.orEmpty().trim() ||
            keyTakeaway.trim() != slide.keyTakeaway.orEmpty().trim()
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
                title = "Edit Slide",
                subtitle = slide?.let { "Slide ${it.slideNumber}" },
                saveEnabled = hasChanges,
                onBackClick = onBackRequested,
                onSaveClick = {
                    if (slide != null) {
                        if (title.isBlank()) {
                            titleError = "Title is required"
                        } else {
                            onSaveClick(slide.withEdits(title, summary, explanation, keyTakeaway))
                        }
                    }
                },
            )

            if (slide == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "This slide is no longer available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                EditFormField(
                    label = "Title *",
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = null
                    },
                    errorMessage = titleError,
                    singleLine = true,
                )
                EditFormField(
                    label = "Summary",
                    value = summary,
                    onValueChange = { summary = it },
                )
                EditFormField(
                    label = "Explanation",
                    value = explanation,
                    onValueChange = { explanation = it },
                )
                EditFormField(
                    label = "Takeaway",
                    value = keyTakeaway,
                    onValueChange = { keyTakeaway = it },
                )
            }
        }
    }
}

private val previewSlide = AnalyzedSlideItem(
    id = "s_1",
    slideNumber = 1,
    title = "Edge Buffer Thresholds",
    summary = "Covers the latency targets for local frame processing.",
    explanation = "Batching frames at the edge keeps processing overhead below the target.",
    keyTakeaway = "Keep the edge buffer small to protect latency.",
)

@Preview(showBackground = true)
@Composable
fun EditSlideScreenLightPreview() {
    KontekstoTheme(darkTheme = false) {
        EditSlideScreen(slide = previewSlide, onBackClick = {}, onSaveClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun EditSlideScreenDarkPreview() {
    KontekstoTheme(darkTheme = true) {
        EditSlideScreen(slide = previewSlide, onBackClick = {}, onSaveClick = {})
    }
}
