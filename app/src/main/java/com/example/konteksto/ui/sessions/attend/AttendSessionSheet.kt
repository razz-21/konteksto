package com.example.konteksto.ui.sessions.attend

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.konteksto.domain.model.HostSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendSessionSheet(
    onDismiss: () -> Unit,
    onVerifyCode: (code: String, onResult: (Result<HostSession>) -> Unit) -> Unit,
    onConfirmJoin: (session: HostSession) -> Unit,
    sessionToConfirm: HostSession? = null,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = modifier,
    ) {
        if (sessionToConfirm != null) {
            SessionConfirmationContent(
                session = sessionToConfirm,
                onDismiss = onDismiss,
                onBackToCode = { onVerifyCode("") { } },
                onConfirmJoin = { onConfirmJoin(sessionToConfirm) },
            )
        } else {
            AttendSessionMainContent(
                onDismiss = onDismiss,
                onVerifyCode = onVerifyCode,
            )
        }
    }
}
