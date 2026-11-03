package com.example.konteksto.ui.host.studio

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.R
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostStudioSlidesView(
    contextNotes: List<ContextNoteItem>,
    onAddNoteClick: () -> Unit,
    modifier: Modifier = Modifier,
    slideDeckInfo: SlideDeckInfo? = null,
    analyzedSlides: List<AnalyzedSlideItem> = emptyList(),
    isUploadingPdf: Boolean = false,
    isAnalyzingPdf: Boolean = false,
    onUploadPdfSubmitted: (uri: Uri, fileName: String, fileSizeMb: Double, pageCount: Int, pdfBytes: ByteArray?) -> Unit = { _, _, _, _, _ -> },
    onRemoveSlideDeckClick: () -> Unit = {},
    onAnalyzeSlideDeckClick: () -> Unit = {},
    onEditSlideClick: (slideId: String) -> Unit = {},
) {
    val context = LocalContext.current
    var subTabSelected by remember { mutableIntStateOf(0) } // 0: Deck & Analysis, 1: AI Guidance, 2: Glossary

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            val contentResolver = context.contentResolver
            var fileName = "Uploaded_Presentation.pdf"
            var fileSizeMb = 2.5
            var pageCount = 1
            var pdfBytes: ByteArray? = null

            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) {
                            fileName = cursor.getString(nameIndex) ?: fileName
                        }
                        if (sizeIndex != -1) {
                            val bytes = cursor.getLong(sizeIndex)
                            fileSizeMb = (bytes / (1024.0 * 1024.0) * 10).roundToInt() / 10.0
                            if (fileSizeMb == 0.0) fileSizeMb = 0.5
                        }
                    }
                }
                contentResolver.openInputStream(uri)?.use { stream ->
                    pdfBytes = stream.readBytes()
                }
                contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    val renderer = android.graphics.pdf.PdfRenderer(pfd)
                    pageCount = renderer.pageCount
                    renderer.close()
                }
            } catch (_: Exception) { }

            onUploadPdfSubmitted(uri, fileName, fileSizeMb, pageCount, pdfBytes)
        }
    }

    if (isUploadingPdf) {
        UploadPdfProgressDialog(
            fileName = slideDeckInfo?.fileName ?: "Slide Deck.pdf",
        )
    }

    if (isAnalyzingPdf) {
        AnalyzePdfProgressDialog(
            fileName = slideDeckInfo?.fileName ?: "Slide Deck.pdf",
            onDismissRequest = {},
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Top Section: Slides & Grounding Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_image),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Slides & Grounding",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${slideDeckInfo?.pageCount ?: 0} slides indexed · ${contextNotes.size + 3} domain terms",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Status Badge: Grounded / Ready
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = if (slideDeckInfo != null) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    if (slideDeckInfo != null) Color(0xFF81C784) else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (slideDeckInfo != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline,
                                shape = CircleShape,
                            ),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (slideDeckInfo != null) "Grounded" else "No PDF",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                        ),
                        color = if (slideDeckInfo != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Responsive Scrollable Sub-Tab Row: Slide Deck | AI Guidance | Terms
        SecondaryScrollableTabRow(
            selectedTabIndex = subTabSelected,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 0.dp,
        ) {
            Tab(
                selected = subTabSelected == 0,
                onClick = { subTabSelected = 0 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_image),
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Slide Deck",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (subTabSelected == 0) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                            ),
                            maxLines = 1,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape,
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = if (slideDeckInfo != null) "1" else "0",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Tab(
                selected = subTabSelected == 1,
                onClick = { subTabSelected = 1 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_sparkle),
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Guidance",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (subTabSelected == 1) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                            ),
                            maxLines = 1,
                        )
                    }
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Tab(
                selected = subTabSelected == 2,
                onClick = { subTabSelected = 2 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tag),
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Terms",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (subTabSelected == 2) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                            ),
                            maxLines = 1,
                        )
                    }
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (subTabSelected == 0) {
            // Sub-Tab 0: Slide Deck & Analysis
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (slideDeckInfo == null) {
                    // PDF Slide Deck Empty State Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = Color(0xFFFFEBEE),
                                        shape = MaterialTheme.shapes.medium,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_pdf_document),
                                    contentDescription = null,
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(28.dp),
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "No Slide Deck Uploaded Yet",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Upload a PDF presentation deck (.pdf files only) to analyze slides, extract diagrams, and ground AI responses.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { pdfPickerLauncher.launch("application/pdf") },
                                shape = MaterialTheme.shapes.small,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_upload),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Upload PDF Slide Deck",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                            }
                        }
                    }
                } else {
                    // Slide Deck Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // PDF Icon Badge Container
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(
                                                color = Color(0xFFFFEBEE),
                                                shape = MaterialTheme.shapes.small,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_pdf_document),
                                            contentDescription = "PDF Document",
                                            tint = Color(0xFFE53935),
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = slideDeckInfo.fileName,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground,
                                            maxLines = 1,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${slideDeckInfo.fileSizeMb} MB  ·  ${slideDeckInfo.pageCount} Pages",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = slideDeckInfo.uploadTime,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Badge: Analyzed / Uploaded
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = if (slideDeckInfo.isAnalyzed) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = BorderStroke(
                                        1.dp,
                                        if (slideDeckInfo.isAnalyzed) Color(0xFF81C784) else MaterialTheme.colorScheme.outlineVariant,
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            painter = painterResource(id = if (slideDeckInfo.isAnalyzed) R.drawable.ic_check else R.drawable.ic_upload),
                                            contentDescription = null,
                                            tint = if (slideDeckInfo.isAnalyzed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (slideDeckInfo.isAnalyzed) "Analyzed" else "Uploaded",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                            ),
                                            color = if (slideDeckInfo.isAnalyzed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action Buttons Row: Remove PDF Deck | Re-analyze / Analyze
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = onRemoveSlideDeckClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp),
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_delete),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Remove PDF Deck",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                        ),
                                    )
                                }

                                Button(
                                    onClick = onAnalyzeSlideDeckClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp),
                                    shape = MaterialTheme.shapes.small,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_sparkle),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (slideDeckInfo.isAnalyzed) "Re-analyze" else "Analyze",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    // Analyzed Slides Section Header
                    Text(
                        text = "ANALYZED SLIDES (${analyzedSlides.size})",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            letterSpacing = 0.8.sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )

                    if (analyzedSlides.isEmpty()) {
                        // Empty State for Analyzed Slides
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            ),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = MaterialTheme.shapes.medium,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_sparkle),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "No Slides Analyzed Yet",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Tap 'Analyze' above to parse diagrams, key concepts, and takeaways from your uploaded PDF slide deck.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = onAnalyzeSlideDeckClick,
                                    shape = MaterialTheme.shapes.small,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_sparkle),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Analyze",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    )
                                }
                            }
                        }
                    } else {
                        // Analyzed Slides Cards List
                        analyzedSlides.forEach { slide ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                ),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Surface(
                                                shape = MaterialTheme.shapes.extraSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                            ) {
                                                Text(
                                                    text = "Slide ${slide.slideNumber}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.5.sp,
                                                    ),
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Text(
                                                text = slide.title,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                ),
                                                color = MaterialTheme.colorScheme.onBackground,
                                                maxLines = 1,
                                            )
                                        }

                                        IconButton(
                                            onClick = { onEditSlideClick(slide.id) },
                                            modifier = Modifier.size(24.dp),
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_edit),
                                                contentDescription = "Edit slide analysis",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }

                                    if (!slide.summary.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = MaterialTheme.shapes.small,
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface,
                                            ),
                                            border = BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                            ),
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                            ) {
                                                Text(
                                                    text = "SUMMARY",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        letterSpacing = 0.5.sp,
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = slide.summary,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 12.sp,
                                                        lineHeight = 16.sp,
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                            }
                                        }
                                    }

                                    if (!slide.explanation.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = MaterialTheme.shapes.small,
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                            ),
                                            border = BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                            ),
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                            ) {
                                                Text(
                                                    text = "EXPLANATION",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        letterSpacing = 0.5.sp,
                                                    ),
                                                    color = MaterialTheme.colorScheme.secondary,
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = slide.explanation,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 12.sp,
                                                        lineHeight = 16.sp,
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                            }
                                        }
                                    }

                                    if (!slide.keyTakeaway.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = MaterialTheme.shapes.small,
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                            ),
                                            border = BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            ),
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                            ) {
                                                Text(
                                                    text = "TAKEAWAY",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        letterSpacing = 0.5.sp,
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = slide.keyTakeaway,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 12.sp,
                                                        lineHeight = 16.sp,
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (subTabSelected == 1) {
            // Sub-Tab 1: AI Guidance
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "ATTACHED CONTEXT NOTES (${contextNotes.size})",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        letterSpacing = 0.8.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )

                contextNotes.forEach { note ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = MaterialTheme.shapes.extraSmall,
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = note.category,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }

                                Text(
                                    text = note.timestamp,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = note.content,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Button(
                    onClick = onAddNoteClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Context Note",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        } else {
            // Sub-Tab 2: Terms & Glossary
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "GROUNDED DOMAIN TERMS (3)",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        letterSpacing = 0.8.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )

                listOf(
                    Triple("Micro-gateways", "EDGE NETWORK", "Low-power localized routing nodes placed near edge clients to process batch frames."),
                    Triple("POP (Point of Presence)", "INFRASTRUCTURE", "Local edge data center access point reducing transit hop counts."),
                    Triple("Edge Buffer Threshold", "LATENCY SPECS", "128kb batch size cap to maintain < 15ms frame processing overhead."),
                ).forEach { (term, category, desc) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = MaterialTheme.shapes.extraSmall,
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.5.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = term,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
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
