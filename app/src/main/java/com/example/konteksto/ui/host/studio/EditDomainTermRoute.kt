package com.example.konteksto.ui.host.studio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EditDomainTermRoute(
    viewModel: HostStudioViewModel,
    termId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EditDomainTermScreen(
        domainTerm = uiState.domainTerms.find { it.id == termId },
        isNameTaken = { name -> uiState.domainTerms.hasOtherTermNamed(termId, name) },
        onBackClick = onBackClick,
        onSaveClick = { editedTerm ->
            viewModel.updateDomainTerm(editedTerm)
            onBackClick()
        },
        modifier = modifier,
    )
}
