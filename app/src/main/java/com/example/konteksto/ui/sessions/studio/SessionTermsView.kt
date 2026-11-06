package com.example.konteksto.ui.sessions.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.R
import com.example.konteksto.ui.host.studio.DomainTermItem
import com.example.konteksto.ui.theme.extendedColors

@Composable
fun SessionTermsView(
    domainTerms: List<DomainTermItem>,
    modifier: Modifier = Modifier,
    showEditDeleteActions: Boolean = false,
    onEditTermClick: (DomainTermItem) -> Unit = {},
    onDeleteTermClick: (DomainTermItem) -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Section Header: GROUNDED DOMAIN TERMS (count)
        Text(
            text = "GROUNDED DOMAIN TERMS (${domainTerms.size})",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                letterSpacing = 0.8.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (domainTerms.isEmpty()) {
            // Empty State Card when no terms exist for this session
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.extendedColors.cardContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.medium,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tag),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "No Grounded Terms Yet",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "No grounded domain terms found for this session yet. Terms extracted from presentation slides or session materials will appear here.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            // Display scrollable LazyColumn list of Domain Terms
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(domainTerms, key = { it.id }) { domainTerm ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.extendedColors.cardContainer,
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                        ) {
                            // Top Row: Category Pill Badge + Optional Edit & Delete Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = BorderStroke(
                                        0.5.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    ),
                                ) {
                                    Text(
                                        text = domainTerm.category,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            letterSpacing = 0.5.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                if (showEditDeleteActions) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        IconButton(
                                            onClick = { onEditTermClick(domainTerm) },
                                            modifier = Modifier.size(28.dp),
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_edit),
                                                contentDescription = "Edit term",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteTermClick(domainTerm) },
                                            modifier = Modifier.size(28.dp),
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_delete),
                                                contentDescription = "Delete term",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Term Name
                            Text(
                                text = domainTerm.term,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Term Description
                            Text(
                                text = domainTerm.description,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
