package com.example.konteksto.ui.sessions.studio

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.host.studio.DOMAIN_TERM_CATEGORIES
import com.example.konteksto.ui.host.studio.DeleteDomainTermDialog
import com.example.konteksto.ui.host.studio.DomainTermItem
import com.example.konteksto.ui.theme.KontekstoTheme

@Composable
fun SessionStudioScreen(
    session: HostSession,
    onBackClick: () -> Unit,
    onLeaveConfirm: () -> Unit,
    onPresentQrClick: (HostSession) -> Unit,
    modifier: Modifier = Modifier,
    customTranscripts: List<CompanionTranscriptItem> = emptyList(),
    domainTerms: List<DomainTermItem> = emptyList(),
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var termsList by remember(domainTerms) { mutableStateOf(domainTerms) }
    var termToDelete by remember { mutableStateOf<DomainTermItem?>(null) }
    var termToEdit by remember { mutableStateOf<DomainTermItem?>(null) }

    if (showLeaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmDialog = false },
            title = {
                Text(
                    text = "Leave Session?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to leave this session?",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveConfirmDialog = false
                        onLeaveConfirm()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text("Leave", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLeaveConfirmDialog = false },
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    termToDelete?.let { term ->
        DeleteDomainTermDialog(
            term = term.term,
            onConfirm = {
                termsList = termsList.filterNot { it.id == term.id }
                termToDelete = null
                Toast.makeText(context, "Deleted ${term.term}", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { termToDelete = null },
        )
    }

    termToEdit?.let { term ->
        EditSessionDomainTermDialog(
            domainTerm = term,
            onConfirm = { updated ->
                termsList = termsList.map { if (it.id == updated.id) updated else it }
                termToEdit = null
                Toast.makeText(context, "Saved changes to ${updated.term}", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { termToEdit = null },
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
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Top Bar: Back button navigates directly back, Leave button opens prompt
            SessionStudioTopBar(
                session = session,
                onBackClick = onBackClick,
                onPresentQrClick = onPresentQrClick,
                onLeaveClick = { showLeaveConfirmDialog = true },
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Hero Banner Card & Speaker Info Card with session details
            SessionStudioHeaderCard(
                session = session,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Material Segmented Tabs directly below speaker section
            // Contains 3 tabs: Live Companion, Terms, Glossary
            SessionStudioSegmentedTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                termsCount = termsList.size,
                glossaryCount = 3,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                when (selectedTab) {
                    0 -> {
                        // Live Companion Tab View (Transcripts, What did they mean button, Live Concept Explanations)
                        SessionLiveCompanionView(
                            session = session,
                            isAudioBroadcasting = session.isLive,
                            customTranscripts = customTranscripts,
                            onAskWhatTheyMean = { selectedItems ->
                                val msg = "Analyzing ${selectedItems.size} selected statement(s)…"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                    1 -> {
                        // Terms Tab
                        SessionTermsView(
                            domainTerms = termsList,
                            onEditTermClick = { termToEdit = it },
                            onDeleteTermClick = { termToDelete = it },
                        )
                    }
                    2 -> {
                        // Glossary Tab
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 16.dp),
                        ) {
                            Text(
                                text = "Session Glossary Entries",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditSessionDomainTermDialog(
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
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
fun SessionStudioScreenLightPreview() {
    val sampleSession = HostSession(
        id = "1",
        venue = "Main Auditorium · Stage A",
        time = "75 min",
        talkCode = "TALK–892",
        title = "Revolutionizing Deep Learning: Advancements in Neural Architectures",
        speakerName = "Dr. Elena Santos",
        category = "Frontier",
        attendeesCount = 892,
        isLive = true,
    )
    KontekstoTheme(darkTheme = false) {
        SessionStudioScreen(
            session = sampleSession,
            onBackClick = {},
            onLeaveConfirm = {},
            onPresentQrClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SessionStudioScreenDarkPreview() {
    val sampleSession = HostSession(
        id = "1",
        venue = "Main Auditorium · Stage A",
        time = "75 min",
        talkCode = "TALK–892",
        title = "Revolutionizing Deep Learning: Advancements in Neural Architectures",
        speakerName = "Dr. Elena Santos",
        category = "Frontier",
        attendeesCount = 892,
        isLive = true,
    )
    KontekstoTheme(darkTheme = true) {
        SessionStudioScreen(
            session = sampleSession,
            onBackClick = {},
            onLeaveConfirm = {},
            onPresentQrClick = {},
        )
    }
}
