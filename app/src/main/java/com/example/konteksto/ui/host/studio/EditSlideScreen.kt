package com.example.konteksto.ui.host.studio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.R
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
        DiscardSlideChangesDialog(
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
            // Top Bar: Back, Title and Save
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBackRequested) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Edit Slide",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (slide != null) {
                        Text(
                            text = "Slide ${slide.slideNumber}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (slide != null) {
                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                titleError = "Title is required"
                            } else {
                                onSaveClick(slide.withEdits(title, summary, explanation, keyTakeaway))
                            }
                        },
                        enabled = hasChanges,
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }

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
                EditSlideField(
                    label = "Title *",
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = null
                    },
                    errorMessage = titleError,
                    singleLine = true,
                )
                EditSlideField(
                    label = "Summary",
                    value = summary,
                    onValueChange = { summary = it },
                )
                EditSlideField(
                    label = "Explanation",
                    value = explanation,
                    onValueChange = { explanation = it },
                )
                EditSlideField(
                    label = "Takeaway",
                    value = keyTakeaway,
                    onValueChange = { keyTakeaway = it },
                )
            }
        }
    }
}

@Composable
private fun EditSlideField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    errorMessage: String? = null,
    singleLine: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = label) },
        isError = errorMessage != null,
        supportingText = errorMessage?.let { message -> { Text(text = message) } },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        shape = MaterialTheme.shapes.small,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            errorBorderColor = MaterialTheme.colorScheme.error,
            errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
        ),
    )
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
