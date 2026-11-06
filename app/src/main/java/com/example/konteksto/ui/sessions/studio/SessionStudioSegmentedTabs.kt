package com.example.konteksto.ui.sessions.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SessionStudioSegmentedTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    termsCount: Int = 49,
    glossaryCount: Int = 21,
) {
    val effectiveTermsCount = if (termsCount <= 0) 49 else termsCount
    val effectiveGlossaryCount = if (glossaryCount <= 0) 21 else glossaryCount

    val tabs = listOf(
        TabData("Live companion", count = null),
        TabData("Terms", count = effectiveTermsCount),
        TabData("Glossary", count = effectiveGlossaryCount),
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTab == index

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(index) }
                        .padding(top = 10.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.5.sp,
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color(0xFF667085),
                        )

                        if (tab.count != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEAECF0),
                            ) {
                                Text(
                                    text = tab.count.toString(),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                    ),
                                    color = Color(0xFF344054),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Indicator Line under active tab
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(2.5.dp)
                            .background(
                                color = if (isSelected) Color(0xFF2563EB) else Color.Transparent,
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = Color(0xFFEAECF0),
        )
    }
}

private data class TabData(
    val label: String,
    val count: Int?,
)
