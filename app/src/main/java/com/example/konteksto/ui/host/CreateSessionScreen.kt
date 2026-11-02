package com.example.konteksto.ui.host

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.R
import com.example.konteksto.domain.model.ConferenceThemeBanner
import com.example.konteksto.ui.theme.KontekstoTheme
import java.util.Locale

@Composable
fun CreateSessionScreen(
    onBackClick: () -> Unit,
    onCreateSessionSubmit: (
        title: String,
        talkCode: String,
        speaker: String,
        category: String,
        room: String,
        startTime: String,
        endTime: String,
        banner: ConferenceThemeBanner,
    ) -> Unit,
    modifier: Modifier = Modifier,
    initialSpeakerName: String = "",
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var selectedBanner by remember {
        mutableStateOf(ConferenceThemeBanner.DefaultCategories.first())
    }
    var sessionTitle by remember { mutableStateOf("") }
    var talkCode by remember { mutableStateOf(generateAlphanumericCode()) }
    var speakerName by remember(initialSpeakerName) { mutableStateOf(initialSpeakerName) }
    var categoryTrack by remember { mutableStateOf(selectedBanner.categoryName) }
    var roomStage by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("10:00 AM") }
    var endTime by remember { mutableStateOf("") }

    var titleError by remember { mutableStateOf<String?>(null) }
    var codeError by remember { mutableStateOf<String?>(null) }
    var speakerError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var roomError by remember { mutableStateOf<String?>(null) }
    var startTimeError by remember { mutableStateOf<String?>(null) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        focusedTextColor = MaterialTheme.colorScheme.onBackground,
        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        disabledTextColor = MaterialTheme.colorScheme.onBackground,
        errorBorderColor = MaterialTheme.colorScheme.error,
        errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
    )

    fun validateAndSubmit() {
        titleError = if (sessionTitle.isBlank()) "Session title is required" else null
        codeError = when {
            talkCode.isBlank() -> "Code is required"
            talkCode.trim().length < 6 -> "Code must be 6 characters"
            else -> null
        }
        speakerError = if (speakerName.isBlank()) "Speaker name is required" else null
        categoryError = if (categoryTrack.isBlank()) "Category / Track is required" else null
        roomError = if (roomStage.isBlank()) "Room / Stage is required" else null
        startTimeError = if (startTime.isBlank()) "Start time is required" else null

        if (titleError != null || codeError != null || speakerError != null || categoryError != null || roomError != null || startTimeError != null) {
            Toast.makeText(context, "Please fill in all required fields (*)", Toast.LENGTH_SHORT).show()
            return
        }

        onCreateSessionSubmit(
            sessionTitle,
            talkCode.trim().uppercase(Locale.getDefault()),
            speakerName,
            categoryTrack,
            roomStage,
            startTime,
            endTime,
            selectedBanner,
        )
    }

    if (showStartTimePicker) {
        CreateSessionTimePickerDialog(
            title = stringResource(id = R.string.time_picker_start_title),
            initialHour = 10,
            initialMinute = 0,
            onDismissRequest = { showStartTimePicker = false },
            onConfirm = { hour, minute ->
                startTime = formatTime(hour, minute)
                startTimeError = null
                showStartTimePicker = false
            },
        )
    }

    if (showEndTimePicker) {
        CreateSessionTimePickerDialog(
            title = stringResource(id = R.string.time_picker_end_title),
            initialHour = 11,
            initialMinute = 30,
            onDismissRequest = { showEndTimePicker = false },
            onConfirm = { hour, minute ->
                endTime = formatTime(hour, minute)
                showEndTimePicker = false
            },
        )
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            // Top Bar
            CreateSessionTopBar(
                onBackClick = onBackClick,
                onCreateClick = { validateAndSubmit() },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Cover Photo Selector Card (main preview + 12 categories)
            CoverPhotoSelectorCard(
                selectedBanner = selectedBanner,
                sessionTitle = sessionTitle,
                banners = ConferenceThemeBanner.DefaultCategories,
                onBannerSelect = { newBanner ->
                    selectedBanner = newBanner
                    categoryTrack = newBanner.categoryName
                    categoryError = null
                },
                onUploadClick = { /* Handle custom image upload */ },
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Field 1: Session Title (*)
            FieldLabel(
                iconRes = R.drawable.ic_text,
                title = stringResource(id = R.string.field_session_title),
                isRequired = true,
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = sessionTitle,
                onValueChange = {
                    sessionTitle = it
                    if (it.isNotBlank()) titleError = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.field_session_title_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                isError = titleError != null,
                supportingText = titleError?.let { err ->
                    { Text(text = err, color = MaterialTheme.colorScheme.error) }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                colors = textFieldColors,
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Field 2: Code (*) (6-character alphanumeric auto-filled)
            FieldLabel(
                iconRes = R.drawable.ic_qr_code,
                title = stringResource(id = R.string.field_code),
                isRequired = true,
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = talkCode,
                onValueChange = { input ->
                    if (input.length <= 8) {
                        talkCode = input.uppercase(Locale.getDefault())
                        if (input.trim().length >= 6) codeError = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.field_code_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                isError = codeError != null,
                supportingText = codeError?.let { err ->
                    { Text(text = err, color = MaterialTheme.colorScheme.error) }
                } ?: {
                    Text(
                        text = stringResource(id = R.string.field_code_hint),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii,
                ),
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                colors = textFieldColors,
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Field 3: Speaker Name (*)
            FieldLabel(
                iconRes = R.drawable.ic_speaker,
                title = stringResource(id = R.string.field_speaker_name),
                isRequired = true,
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = speakerName,
                onValueChange = {
                    speakerName = it
                    if (it.isNotBlank()) speakerError = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.field_speaker_name_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                isError = speakerError != null,
                supportingText = speakerError?.let { err ->
                    { Text(text = err, color = MaterialTheme.colorScheme.error) }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                colors = textFieldColors,
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Field 4: Category / Track (*)
            FieldLabel(
                iconRes = R.drawable.ic_tag,
                title = stringResource(id = R.string.field_category_track),
                isRequired = true,
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = categoryTrack,
                onValueChange = {
                    categoryTrack = it
                    if (it.isNotBlank()) categoryError = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.field_category_track_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                isError = categoryError != null,
                supportingText = categoryError?.let { err ->
                    { Text(text = err, color = MaterialTheme.colorScheme.error) }
                } ?: {
                    Text(
                        text = stringResource(id = R.string.field_category_track_hint),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                colors = textFieldColors,
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Field 5: Room / Stage (*)
            FieldLabel(
                iconRes = R.drawable.ic_location,
                title = stringResource(id = R.string.field_room_stage),
                isRequired = true,
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = roomStage,
                onValueChange = {
                    roomStage = it
                    if (it.isNotBlank()) roomError = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.field_room_stage_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                isError = roomError != null,
                supportingText = roomError?.let { err ->
                    { Text(text = err, color = MaterialTheme.colorScheme.error) }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                colors = textFieldColors,
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Field 6: Start Time (*)
            FieldLabel(
                iconRes = R.drawable.ic_clock,
                title = stringResource(id = R.string.field_start_time),
                isRequired = true,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.field_start_time_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    },
                    isError = startTimeError != null,
                    supportingText = startTimeError?.let { err ->
                        { Text(text = err, color = MaterialTheme.colorScheme.error) }
                    },
                    trailingIcon = {
                        IconButton(onClick = { showStartTimePicker = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_clock),
                                contentDescription = "Pick Start Time",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    colors = textFieldColors,
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showStartTimePicker = true },
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Field 7: End Time (Optional - isRequired = false)
            FieldLabel(
                iconRes = R.drawable.ic_clock,
                title = stringResource(id = R.string.field_end_time),
                isRequired = false,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = endTime,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.field_end_time_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { showEndTimePicker = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_clock),
                                contentDescription = "Pick End Time",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    colors = textFieldColors,
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showEndTimePicker = true },
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Bottom Action Buttons: Cancel & + Create Session
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onBackClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Text(
                        text = stringResource(id = R.string.action_cancel),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                Button(
                    onClick = { validateAndSubmit() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.action_create_session),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun generateAlphanumericCode(length: Int = 6): String {
    val allowedChars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}

private fun formatTime(hour: Int, minute: Int): String {
    val isPm = hour >= 12
    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, if (isPm) "PM" else "AM")
}

@Composable
private fun FieldLabel(
    iconRes: Int,
    title: String,
    modifier: Modifier = Modifier,
    isRequired: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (isRequired) {
            Text(
                text = " *",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                ),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateSessionScreenLightPreview() {
    KontekstoTheme(darkTheme = false) {
        CreateSessionScreen(
            onBackClick = {},
            onCreateSessionSubmit = { _, _, _, _, _, _, _, _ -> },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CreateSessionScreenDarkPreview() {
    KontekstoTheme(darkTheme = true) {
        CreateSessionScreen(
            onBackClick = {},
            onCreateSessionSubmit = { _, _, _, _, _, _, _, _ -> },
        )
    }
}
