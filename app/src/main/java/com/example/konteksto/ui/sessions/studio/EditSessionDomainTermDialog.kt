package com.example.konteksto.ui.sessions.studio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.ui.host.studio.DOMAIN_TERM_CATEGORIES
import com.example.konteksto.ui.host.studio.DomainTermItem
import com.example.konteksto.ui.theme.KontekstoButtonDefaults

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditSessionDomainTermDialog(
    domainTerm: DomainTermItem,
    onConfirm: (DomainTermItem) -> Unit,
    onDismiss: () -> Unit,
) {
    var term by remember(domainTerm) { mutableStateOf(domainTerm.term) }
    var category by remember(domainTerm) { mutableStateOf(domainTerm.category) }
    var description by remember(domainTerm) { mutableStateOf(domainTerm.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Domain Term",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DOMAIN_TERM_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                        )
                    }
                }
                OutlinedTextField(
                    value = term,
                    onValueChange = { term = it },
                    label = { Text("Term Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(domainTerm.copy(term = term.trim(), category = category, description = description.trim()))
                },
                enabled = term.isNotBlank() && description.isNotBlank(),
                shape = KontekstoButtonDefaults.shape,
                modifier = Modifier.height(KontekstoButtonDefaults.Height),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.height(KontekstoButtonDefaults.Height),
                shape = KontekstoButtonDefaults.shape,
            ) {
                Text("Cancel")
            }
        },
    )
}
