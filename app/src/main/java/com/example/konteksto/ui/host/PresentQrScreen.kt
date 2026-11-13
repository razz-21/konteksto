package com.example.konteksto.ui.host

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.konteksto.R
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.components.showKontekstoSnackbar
import com.example.konteksto.ui.theme.KontekstoButtonDefaults
import com.example.konteksto.ui.theme.KontekstoTheme
import com.example.konteksto.ui.theme.extendedColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PresentQrScreen(
    session: HostSession,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDownloadQrClick: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scrollState = rememberScrollState()
    val snackbarScope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { message ->
        snackbarScope.launch { snackbarHostState.showKontekstoSnackbar(message) }
    }
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val qrDownloadedMessage = stringResource(id = R.string.qr_downloaded_toast)
    val qrDownloadFailedMessage = stringResource(id = R.string.qr_download_failed)
    val context = LocalContext.current
    var isSavingQr by remember { mutableStateOf(false) }

    val qrApiUrl = buildQrApiUrl(session.talkCode)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Column {
                        Text(
                            text = stringResource(id = R.string.present_qr_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = stringResource(id = R.string.present_qr_subtitle),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Card 1: QR Code & Join Code Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.extendedColors.cardContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // QR Code Image Container
                        Box(
                            modifier = Modifier
                                .size(210.dp)
                                .background(Color.White)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            AsyncImage(
                                model = qrApiUrl,
                                contentDescription = "Session QR Code",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(id = R.string.present_qr_scan_hint),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Join Code Box
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Join code",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = session.talkCode,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp,
                                            letterSpacing = 2.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(session.talkCode))
                                        showMessage("Join code copied!")
                                    },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_copy),
                                        contentDescription = "Copy join code",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Card 2: Session Details Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.extendedColors.cardContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text(
                            text = session.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                lineHeight = 24.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (session.speakerName.isNotBlank()) {
                                PresentQrBadge(
                                    iconRes = R.drawable.ic_speaker,
                                    text = session.speakerName,
                                )
                            }

                            if (session.time.isNotBlank()) {
                                PresentQrBadge(
                                    iconRes = R.drawable.ic_clock,
                                    text = session.time,
                                )
                            }

                            if (session.venue.isNotBlank()) {
                                PresentQrBadge(
                                    iconRes = R.drawable.ic_location,
                                    text = session.venue,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Action Buttons: Back & Download QR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(KontekstoButtonDefaults.Height),
                    shape = KontekstoButtonDefaults.shape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Text(
                        text = stringResource(id = R.string.action_back),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Button(
                    onClick = {
                        if (isSavingQr) return@Button
                        if (onDownloadQrClick != null) {
                            onDownloadQrClick.invoke()
                        } else {
                            isSavingQr = true
                            snackbarScope.launch {
                                val saved = saveQrCodeToGallery(
                                    context = context,
                                    talkCode = session.talkCode,
                                    qrUrl = qrApiUrl,
                                )
                                isSavingQr = false
                                if (saved) {
                                    showMessage(qrDownloadedMessage)
                                } else {
                                    showMessage(qrDownloadFailedMessage)
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(KontekstoButtonDefaults.Height),
                    enabled = !isSavingQr,
                    shape = KontekstoButtonDefaults.shape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_download),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.action_download_qr),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PresentQrBadge(
    iconRes: Int,
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun buildQrApiUrl(talkCode: String): String {
    return "https://api.qrserver.com/v1/create-qr-code/?size=450x450&data=$talkCode"
}

internal fun qrFilenameForTalkCode(talkCode: String): String {
    val safeTalkCode = talkCode.filter { it.isLetterOrDigit() }.ifBlank { "code" }
    return "konteksto_qr_$safeTalkCode.png"
}

internal fun saveBitmapToMediaStore(
    context: Context,
    bitmap: Bitmap,
    talkCode: String,
): Boolean {
    val softwareBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
        bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
    } else {
        bitmap
    }

    val filename = qrFilenameForTalkCode(talkCode)

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Konteksto")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        ?: return false

    val saved = try {
        resolver.openOutputStream(uri)?.use { stream ->
            softwareBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        } ?: false
    } catch (_: Exception) {
        false
    }

    return if (saved) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
        true
    } else {
        resolver.delete(uri, null, null)
        false
    }
}

internal suspend fun saveQrCodeToGallery(
    context: Context,
    talkCode: String,
    qrUrl: String,
): Boolean = withContext(Dispatchers.IO) {
    try {
        val rawBitmap: Bitmap? = try {
            val loader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(qrUrl)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                (result.drawable as? BitmapDrawable)?.bitmap ?: result.drawable.toBitmap()
            } else {
                null
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }

        val bitmap = rawBitmap ?: try {
            val connection = (URL(qrUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            connection.inputStream.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        } ?: return@withContext false

        saveBitmapToMediaStore(context, bitmap, talkCode)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        false
    }
}

@Preview(showBackground = true)
@Composable
fun PresentQrScreenLightPreview() {
    val sampleSession = HostSession(
        id = "1",
        venue = "WAC Building 203, West Academic Sector",
        time = "10:00 AM – 11:00 PM",
        talkCode = "7EJCD2",
        title = "Literacy in AI Era",
        speakerName = "Zhar",
        attendeesCount = 1,
        isLive = false,
    )
    KontekstoTheme(darkTheme = false) {
        PresentQrScreen(
            session = sampleSession,
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PresentQrScreenDarkPreview() {
    val sampleSession = HostSession(
        id = "1",
        venue = "WAC Building 203, West Academic Sector",
        time = "10:00 AM – 11:00 PM",
        talkCode = "7EJCD2",
        title = "Literacy in AI Era",
        speakerName = "Zhar",
        attendeesCount = 1,
        isLive = true,
    )
    KontekstoTheme(darkTheme = true) {
        PresentQrScreen(
            session = sampleSession,
            onBackClick = {},
        )
    }
}
