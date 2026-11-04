package com.example.konteksto.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.R
import com.example.konteksto.ui.theme.KontekstoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

data class TranscriptScript(
    val category: String,
    val speakerQuote: String,
    val detectedTerm: String,
    val explanation: String,
)

private val sampleScripts = listOf(
    TranscriptScript(
        category = "Finance & Economics",
        speakerQuote = "“When Quantitative Tightening constricts liquidity, capital allocation shifts defensively...”",
        detectedTerm = "Quantitative Tightening",
        explanation = "When central banks reduce money circulation, businesses become more cautious about capital allocation.",
    ),
    TranscriptScript(
        category = "Tech & Distributed Systems",
        speakerQuote = "“By deploying Edge AI directly to local gateway nodes, we reduce latency to sub-milliseconds...”",
        detectedTerm = "Edge AI",
        explanation = "Directly processing data on local devices instead of sending everything back and forth to distant cloud servers.",
    ),
    TranscriptScript(
        category = "Healthcare & Biotech",
        speakerQuote = "“Targeted immunotherapy enhances T-cell responsiveness against malignant cell proliferation...”",
        detectedTerm = "Immunotherapy",
        explanation = "Treatments that train your body’s natural immune system to recognize and fight harmful target cells.",
    ),
)

@Composable
fun LiveTranscriptCard(
    modifier: Modifier = Modifier,
    scripts: List<TranscriptScript> = sampleScripts,
) {
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(scripts) {
        while (isActive) {
            delay(3600L)
            currentIndex = (currentIndex + 1) % scripts.size
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Live Header Badge + Domain Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444)),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE TRANSCRIPT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        ),
                        color = Color(0xFFEF4444),
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = Color(0xFFEEF2FF),
                ) {
                    Text(
                        text = scripts[currentIndex].category,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                        ),
                        color = Color(0xFF3B52D4),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Animated Transcript & Concept Explanation
            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = {
                    (fadeIn(tween(350)) + slideInVertically(tween(350)) { height -> height / 3 })
                        .togetherWith(fadeOut(tween(300)) + slideOutVertically(tween(300)) { height -> -height / 3 })
                },
                label = "TranscriptScriptAnimation",
            ) { targetIndex ->
                val currentScript = scripts[targetIndex]
                Column {
                    Text(
                        text = "Speaker live stream:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = currentScript.speakerQuote,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_live_broadcast),
                                contentDescription = null,
                                tint = Color(0xFF3B52D4),
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Detected: ${currentScript.detectedTerm}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Color(0xFF3B52D4),
                            )
                        }

                        Text(
                            text = "Konteksto Concept Card",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = currentScript.explanation,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated Step Indicators (Dots)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                scripts.indices.forEach { index ->
                    val isSelected = index == currentIndex
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 18.dp else 6.dp,
                        animationSpec = tween(300),
                        label = "IndicatorWidth",
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFF3B52D4) else Color.LightGray.copy(alpha = 0.5f),
                            ),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LiveTranscriptCardPreview() {
    KontekstoTheme {
        LiveTranscriptCard()
    }
}
