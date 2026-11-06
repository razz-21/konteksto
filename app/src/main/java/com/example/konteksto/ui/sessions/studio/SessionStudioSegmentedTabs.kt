package com.example.konteksto.ui.sessions.studio

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SessionStudioTab(val label: String) {
    LiveCompanion("Live companion"),
    ConceptExplanation("Concept explanation"),
    Terms("Terms"),
    Glossary("Glossary"),
}

/** Scrollable so all four tabs and their counts fit on narrow phones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionStudioSegmentedTabs(
    selectedTab: SessionStudioTab,
    onTabSelected: (SessionStudioTab) -> Unit,
    modifier: Modifier = Modifier,
    conceptCount: Int = 0,
    termsCount: Int = 0,
    glossaryCount: Int = 0,
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.background,
        edgePadding = 0.dp,
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedTab.ordinal, matchContentSize = true),
                width = Dp.Unspecified,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) },
    ) {
        SessionStudioTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val count = when (tab) {
                SessionStudioTab.LiveCompanion -> null
                SessionStudioTab.ConceptExplanation -> conceptCount
                SessionStudioTab.Terms -> termsCount
                SessionStudioTab.Glossary -> glossaryCount
            }

            Tab(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                selectedContentColor = MaterialTheme.colorScheme.onBackground,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Row(
                    // Custom Tab content gets no built-in padding, so match Material's 16dp tab padding.
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.5.sp,
                        ),
                    )

                    if (count != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Text(
                                text = count.toString(),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
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
