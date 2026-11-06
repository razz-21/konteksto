package com.example.konteksto.ui.sessions.attend

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.ui.theme.KontekstoTheme

@Composable
internal fun AttendSessionMainContent(
    onDismiss: () -> Unit,
    onVerifyCode: (code: String, onResult: (Result<HostSession>) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var roomCode by rememberSaveable { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showQrScanner by rememberSaveable { mutableStateOf(false) }

    var isCameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        isCameraGranted = granted
    }

    val handleVerify = {
        if (roomCode.length >= ROOM_CODE_LENGTH && !isVerifying) {
            isVerifying = true
            errorMessage = null
            keyboardController?.hide()
            onVerifyCode(roomCode) { result ->
                isVerifying = false
                result.onFailure { error ->
                    errorMessage = error.localizedMessage ?: "Invalid session room code."
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        AttendSheetHeader(
            title = "Attend a session",
            onDismiss = onDismiss,
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Enter the code shown on the host's screen",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (!showQrScanner) {
            RoomCodeEntryView(
                roomCode = roomCode,
                errorMessage = errorMessage,
                isVerifying = isVerifying,
                onRoomCodeChange = { code ->
                    roomCode = code
                    errorMessage = null
                },
                onVerify = handleVerify,
                onScanQrClick = {
                    showQrScanner = true
                    if (!isCameraGranted) {
                        cameraLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
            )
        } else {
            QrScannerView(
                isCameraGranted = isCameraGranted,
                onRequestCameraPermission = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                onSimulateScan = {
                    onVerifyCode(DEMO_ROOM_CODE) { result ->
                        result.onFailure { error ->
                            errorMessage = error.localizedMessage
                        }
                    }
                },
                onBackToCodeEntry = { showQrScanner = false },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AttendSessionMainContentPreview() {
    KontekstoTheme {
        AttendSessionMainContent(
            onDismiss = {},
            onVerifyCode = { _, _ -> },
        )
    }
}
