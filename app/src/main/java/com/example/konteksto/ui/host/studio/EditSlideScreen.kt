package com.example.konteksto.ui.host.studio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.R
import com.example.konteksto.ui.components.rememberDebouncedClick
import com.example.konteksto.ui.theme.KontekstoButtonDefaults

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
        Column(modifier = Modifier.fillMaxSize()) {
            // Kept outside the scrolling form so it stays pinned to the top.
            EditFormTopBar(
                title = "Edit Slide",
                subtitle = slide?.let { "Slide ${it.slideNumber}" },
                onBackClick = onBackRequested,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
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
                        label = "Title",
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bottom Action Buttons: Back & Save
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = rememberDebouncedClick(onClick = onBackRequested),
                            modifier = Modifier
                                .weight(1f)
                                .height(KontekstoButtonDefaults.Height),
                            shape = KontekstoButtonDefaults.shape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Text(
                                text = "Back",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Button(
                            onClick = rememberDebouncedClick {
                                if (title.isBlank()) {
                                    titleError = "Title is required"
                                } else {
                                    onSaveClick(slide.withEdits(title, summary, explanation, keyTakeaway))
                                }
                            },
                            enabled = hasChanges,
                            modifier = Modifier
                                .weight(1.5f)
                                .height(KontekstoButtonDefaults.Height),
                            shape = KontekstoButtonDefaults.shape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Save",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
