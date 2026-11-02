package com.example.konteksto.ui.host

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.konteksto.ui.theme.KontekstoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSessionTimePickerDialog(
    title: String,
    initialHour: Int = 10,
    initialMinute: Int = 0,
    onDismissRequest: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false,
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                },
            ) {
                Text(
                    text = stringResource(id = android.R.string.ok),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = stringResource(id = android.R.string.cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            TimePicker(state = timePickerState)
        },
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}

@Preview
@Composable
fun CreateSessionTimePickerDialogPreview() {
    KontekstoTheme {
        CreateSessionTimePickerDialog(
            title = "Select Start Time",
            onDismissRequest = {},
            onConfirm = { _, _ -> },
        )
    }
}
