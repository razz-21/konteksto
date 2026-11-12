package com.example.konteksto.ui.host

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.example.konteksto.ui.components.showKontekstoSnackbar
import com.example.konteksto.ui.theme.KontekstoButtonDefaults
import com.example.konteksto.ui.theme.KontekstoTheme
import java.util.Locale
import kotlinx.coroutines.launch

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
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scrollState = rememberScrollState()
    val snackbarScope = rememberCoroutineScope()

    var customBannerUri by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedBannerId by rememberSaveable {
        mutableStateOf(ConferenceThemeBanner.DefaultCategories.first().id)
    }

    var categoryTrack by rememberSaveable {
        mutableStateOf(ConferenceThemeBanner.DefaultCategories.first().categoryName)
    }

    val selectedBanner = remember(selectedBannerId, customBannerUri, categoryTrack) {
        if (selectedBannerId == "custom_upload" && customBannerUri != null) {
            ConferenceThemeBanner(
                id = "custom_upload",
                categoryName = categoryTrack.ifBlank { "Custom Cover" },
                imageUrl = customBannerUri!!,
            )
        } else {
            ConferenceThemeBanner.DefaultCategories.find { it.id == selectedBannerId }
                ?: ConferenceThemeBanner.DefaultCategories.first()
        }
    }

    val coverImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            customBannerUri = uri.toString()
            selectedBannerId = "custom_upload"
        }
    }

    var sessionTitle by rememberSaveable { mutableStateOf("") }
    var talkCode by rememberSaveable { mutableStateOf(generateAlphanumericCode()) }
    var speakerName by rememberSaveable(initialSpeakerName) {
        mutableStateOf(initialSpeakerName.ifBlank { "Zhar" })
    }
    var roomStage by rememberSaveable { mutableStateOf("") }
    var startTime by rememberSaveable { mutableStateOf("10:00 AM") }
    var endTime by rememberSaveable { mutableStateOf("") }

    var titleError by rememberSaveable { mutableStateOf<String?>(null) }
    var codeError by rememberSaveable { mutableStateOf<String?>(null) }
    var speakerError by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryError by rememberSaveable { mutableStateOf<String?>(null) }
    var roomError by rememberSaveable { mutableStateOf<String?>(null) }
    var startTimeError by rememberSaveable { mutableStateOf<String?>(null) }

    var showStartTimePicker by rememberSaveable { mutableStateOf(false) }
    var showEndTimePicker by rememberSaveable { mutableStateOf(false) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedTextColor = MaterialTheme.colorScheme.onBackground,
        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
        disabledContainerColor = MaterialTheme.colorScheme.surface,
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
            snackbarScope.launch { snackbarHostState.showKontekstoSnackbar("Please fill in all required fields") }
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
        ) {
            // Top Bar
            CreateSessionTopBar(
                onBackClick = onBackClick,
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Section 1: Cover
            SectionHeader(title = "Cover")

            Spacer(modifier = Modifier.height(8.dp))

            CoverPhotoSelectorCard(
                selectedBanner = selectedBanner,
                sessionTitle = sessionTitle,
                banners = ConferenceThemeBanner.DefaultCategories,
                onBannerSelect = { newBanner ->
                    selectedBannerId = newBanner.id
                    categoryTrack = newBanner.categoryName
                    categoryError = null
                },
                onUploadClick = {
                    coverImagePickerLauncher.launch("image/*")
                },
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Details
            SectionHeader(title = "Details")

            Spacer(modifier = Modifier.height(8.dp))

            // Field 1: Session Title
            OutlinedTextField(
                value = sessionTitle,
                onValueChange = {
                    sessionTitle = it
                    if (it.isNotBlank()) titleError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Session title") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_text),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                placeholder = {
                    Text(
                        text = "e.g. Object Oriented Programming",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                isError = titleError != null,
                supportingText = titleError?.let { err ->
                    { Text(text = err, color = MaterialTheme.colorScheme.error) }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Field 2: Speaker
            OutlinedTextField(
                value = speakerName,
                onValueChange = {
                    speakerName = it
                    if (it.isNotBlank()) speakerError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Speaker") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_speaker),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                placeholder = {
                    Text(
                        text = "e.g. Zhar",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                isError = speakerError != null,
                supportingText = speakerError?.let { err ->
                    { Text(text = err, color = MaterialTheme.colorScheme.error) }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Field 3: Category or track
            OutlinedTextField(
                value = categoryTrack,
                onValueChange = {
                    categoryTrack = it
                    if (it.isNotBlank()) categoryError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Category or track") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tag),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                placeholder = {
                    Text(
                        text = "e.g. Business & Leadership",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                isError = categoryError != null,
                supportingText = categoryError?.let { err ->
                    { Text(text = err, color = MaterialTheme.colorScheme.error) }
                } ?: {
                    Text(
                        text = "Fills in from the cover theme. Type your own if it's different.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: When & where
            SectionHeader(title = "When & where")

            Spacer(modifier = Modifier.height(8.dp))

            // Field 1: Room or stage
            OutlinedTextField(
                value = roomStage,
                onValueChange = {
                    roomStage = it
                    if (it.isNotBlank()) roomError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Room or stage") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_location),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                placeholder = {
                    Text(
                        text = "e.g. CDO Ayala, Hall B",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                isError = roomError != null,
                supportingText = roomError?.let { err ->
                    { Text(text = err, color = MaterialTheme.colorScheme.error) }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Fields 2 & 3: Starts & Ends Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Starts Field
                Box(
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "Starts") },
                        placeholder = {
                            Text(
                                text = "10:00 AM",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        },
                        isError = startTimeError != null,
                        trailingIcon = {
                            IconButton(onClick = { showStartTimePicker = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_clock),
                                    contentDescription = "Pick Start Time",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showStartTimePicker = true },
                    )
                }

                // Ends Field
                Box(
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "Ends") },
                        placeholder = {
                            Text(
                                text = "Add time",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        },
                        supportingText = {
                            Text(
                                text = "Optional",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showEndTimePicker = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_clock),
                                    contentDescription = "Pick End Time",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showEndTimePicker = true },
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 4: Joining
            SectionHeader(title = "Joining")

            Spacer(modifier = Modifier.height(8.dp))

            // Field: Join code with Regenerate button
            OutlinedTextField(
                value = talkCode,
                onValueChange = { input ->
                    if (input.length <= 8) {
                        talkCode = input.uppercase(Locale.getDefault())
                        if (input.trim().length >= 6) codeError = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Join code") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_key),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            talkCode = generateAlphanumericCode()
                            codeError = null
                        },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_refresh),
                            contentDescription = "Regenerate code",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                isError = codeError != null,
                supportingText = codeError?.let { err ->
                    { Text(text = err, color = MaterialTheme.colorScheme.error) }
                } ?: {
                    Text(
                        text = "6 letters or numbers. Attendees type this or scan your QR.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii,
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Bottom Action Buttons: Back & + Create session
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
                        text = "Back",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Button(
                    onClick = { validateAndSubmit() },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(KontekstoButtonDefaults.Height),
                    shape = KontekstoButtonDefaults.shape,
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
                        text = "Create session",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
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
