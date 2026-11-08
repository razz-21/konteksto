package com.example.konteksto.ui.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.R
import com.example.konteksto.domain.model.ExplanationLanguage
import com.example.konteksto.ui.theme.extendedColors

@Composable
fun PreferencesSectionCard(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    explanationLanguage: ExplanationLanguage,
    onExplanationLanguageChange: (ExplanationLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Preferences",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.extendedColors.cardContainer,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Item 1: Dark theme Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_dark_mode),
                            contentDescription = "Dark theme",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dark theme",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Easier on the eyes in dim venues",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onDarkModeChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF3B52D4),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            uncheckedBorderColor = Color.Transparent,
                        ),
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                // Item 2: Explanation Language Column
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_language),
                                contentDescription = "Language",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.profile_explanation_language_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = stringResource(R.string.profile_explanation_language_subtitle),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    ExplanationLanguageSelector(
                        selected = explanationLanguage,
                        onSelect = onExplanationLanguageChange,
                    )
                }
            }
        }
    }
}

internal val ExplanationLanguage.labelRes: Int
    @StringRes get() = when (this) {
        ExplanationLanguage.ENGLISH -> R.string.explanation_language_english
        ExplanationLanguage.TAGALOG -> R.string.explanation_language_tagalog
        ExplanationLanguage.TAGLISH -> R.string.explanation_language_taglish
    }

@Composable
private fun ExplanationLanguageSelector(
    selected: ExplanationLanguage,
    onSelect: (ExplanationLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = ExplanationLanguage.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, language ->
            SegmentedButton(
                selected = language == selected,
                onClick = { onSelect(language) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                label = {
                    Text(
                        text = stringResource(language.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}
