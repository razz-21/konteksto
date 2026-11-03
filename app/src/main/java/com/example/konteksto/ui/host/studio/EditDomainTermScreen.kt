package com.example.konteksto.ui.host.studio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditDomainTermScreen(
    domainTerm: DomainTermItem?,
    isNameTaken: (String) -> Boolean,
    onBackClick: () -> Unit,
    onSaveClick: (DomainTermItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val termId = domainTerm?.id
    var term by rememberSaveable(termId) { mutableStateOf(domainTerm?.term.orEmpty()) }
    var category by rememberSaveable(termId) { mutableStateOf(domainTerm?.category.orEmpty()) }
    var description by rememberSaveable(termId) { mutableStateOf(domainTerm?.description.orEmpty()) }
    var termError by rememberSaveable { mutableStateOf<String?>(null) }
    var descriptionError by rememberSaveable { mutableStateOf<String?>(null) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    val hasChanges = domainTerm != null && (
        term.trim() != domainTerm.term.trim() ||
            category != domainTerm.category ||
            description.trim() != domainTerm.description.trim()
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
                title = "Edit Domain Term",
                subtitle = null,
                saveEnabled = hasChanges,
                onBackClick = onBackRequested,
                onSaveClick = {
                    termError = when {
                        term.isBlank() -> "Term is required"
                        isNameTaken(term) -> "This term is already in your list"
                        else -> null
                    }
                    descriptionError = if (description.isBlank()) "Description is required" else null
                    if (domainTerm != null && termError == null && descriptionError == null) {
                        onSaveClick(domainTerm.withEdits(term, category, description))
                    }
                },
            )

            if (domainTerm == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "This term is no longer available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                EditFormField(
                    label = "Term *",
                    value = term,
                    onValueChange = {
                        term = it
                        termError = null
                    },
                    errorMessage = termError,
                    singleLine = true,
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Keeps a category outside the standard list selectable until the host changes it.
                    (DOMAIN_TERM_CATEGORIES + domainTerm.category).distinct().forEach { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option },
                            label = { Text(text = option) },
                        )
                    }
                }

                EditFormField(
                    label = "Description *",
                    value = description,
                    onValueChange = {
                        description = it
                        descriptionError = null
                    },
                    errorMessage = descriptionError,
                )
            }
        }
    }
}

private val previewTerm = DomainTermItem(
    id = "term_1",
    term = "POP (Point of Presence)",
    category = "ABBREVIATION",
    description = "A local edge data center that reduces the distance requests have to travel.",
)

@Preview(showBackground = true)
@Composable
fun EditDomainTermScreenLightPreview() {
    KontekstoTheme(darkTheme = false) {
        EditDomainTermScreen(domainTerm = previewTerm, isNameTaken = { false }, onBackClick = {}, onSaveClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun EditDomainTermScreenDarkPreview() {
    KontekstoTheme(darkTheme = true) {
        EditDomainTermScreen(domainTerm = previewTerm, isNameTaken = { false }, onBackClick = {}, onSaveClick = {})
    }
}
