package com.example.konteksto.ui.sessions.studio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.R
import com.example.konteksto.data.repository.GlossaryTermItem
import com.example.konteksto.ui.theme.extendedColors

@Composable
fun SessionGlossaryView(
    glossaryTerms: List<GlossaryTermItem>,
    onRemoveTerm: (GlossaryTermItem) -> Unit,
    modifier: Modifier = Modifier,
    onShowMessage: (String) -> Unit = {},
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val sortedTerms = remember(glossaryTerms) { glossaryTerms.sortedByDescending { it.timestamp } }

    val filteredTerms = remember(searchQuery, sortedTerms) {
        if (searchQuery.isBlank()) sortedTerms
        else sortedTerms.filter {
            it.term.contains(searchQuery, ignoreCase = true) ||
                it.definition.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Search bar for session glossary
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "Search session glossary...",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            singleLine = true,
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            },
        )

        if (filteredTerms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "No glossary terms match '$searchQuery'" else "No saved terms yet.\nBookmark a live concept explanation to add it here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filteredTerms, key = { it.id }) { term ->
                    SessionGlossaryCard(
                        term = term,
                        onToggleBookmark = { item ->
                            onRemoveTerm(item)
                            onShowMessage("Removed ${item.term} from My Glossary")
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun SessionGlossaryCard(
    term: GlossaryTermItem,
    onToggleBookmark: (GlossaryTermItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isBookmarked by remember(term.isBookmarked) { mutableStateOf(term.isBookmarked) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.extendedColors.cardContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    // Category Chip
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = term.category.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Source Badge
                    Text(
                        text = if (term.source == "slides") "Slide Deck" else "Live Transcript",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Bookmark Icon Button
                IconButton(
                    onClick = {
                        isBookmarked = !isBookmarked
                        onToggleBookmark(term.copy(isBookmarked = isBookmarked))
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bookmark),
                        contentDescription = "Bookmark Term",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Term Name
            Text(
                text = term.term,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.5.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )

            // Definition Text
            Text(
                text = term.definition,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
