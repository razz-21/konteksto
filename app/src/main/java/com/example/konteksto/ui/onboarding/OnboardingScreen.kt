package com.example.konteksto.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.konteksto.R
import com.example.konteksto.ui.theme.KontekstoTheme
import com.example.konteksto.ui.theme.extendedColors

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentStep by rememberSaveable { mutableIntStateOf(1) }

    var isMicGranted by remember {
        mutableStateOf(isPermissionGranted(context, Manifest.permission.RECORD_AUDIO))
    }
    var isCameraGranted by remember {
        mutableStateOf(isPermissionGranted(context, Manifest.permission.CAMERA))
    }
    var isConceptSyncEnabled by rememberSaveable { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isMicGranted = isPermissionGranted(context, Manifest.permission.RECORD_AUDIO)
                isCameraGranted = isPermissionGranted(context, Manifest.permission.CAMERA)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        isMicGranted = granted
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        isCameraGranted = granted
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        isConceptSyncEnabled = granted
    }

    BackHandler {
        if (currentStep > 1) {
            currentStep--
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            // Top Progress Bar (4 segments)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (step in 1..4) {
                    val isActive = step <= currentStep
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) {
                                    Color(0xFF3B52D4)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                },
                            ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Header Row: App logo + Konteksto, Step Counter, Skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo_icon),
                        contentDescription = "Konteksto Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(MaterialTheme.shapes.small),
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Konteksto",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        ),
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "0$currentStep / 04",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (currentStep < 4) {
                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                            ),
                            color = Color(0xFF3B52D4),
                            modifier = Modifier.clickable {
                                currentStep = 4
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Scrollable Step Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                when (currentStep) {
                    1 -> StepOneContent()
                    2 -> StepTwoContent()
                    3 -> StepThreeContent()
                    4 -> StepFourContent(
                        isMicGranted = isMicGranted,
                        isCameraGranted = isCameraGranted,
                        isConceptSyncEnabled = isConceptSyncEnabled,
                        onRequestMic = {
                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onRequestCamera = {
                            cameraLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onToggleConceptSync = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isConceptSyncEnabled) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                isConceptSyncEnabled = !isConceptSyncEnabled
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier
                            .width(100.dp)
                            .height(52.dp),
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Back",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                    }
                }

                Button(
                    onClick = {
                        if (currentStep < 4) {
                            currentStep++
                        } else {
                            onOnboardingComplete()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B52D4),
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        text = if (currentStep == 4) "Enable All & Get Started" else "Continue",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_forward),
                        contentDescription = "Next",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StepOneContent() {
    Column {
        Text(
            text = "01 · What is Konteksto?",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
            ),
            color = Color(0xFF3B52D4),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Never lose the thread of a live talk again.",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 38.sp,
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Whether you are attending a medical symposium, business summit, academic lecture, or industry seminar, speakers often use dense terminology that can leave listeners behind.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.extendedColors.cardContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "What the speaker says live:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "“When Quantitative Tightening constricts liquidity, capital allocation shifts defensively...”",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Instant Konteksto Analogy",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color(0xFF3B52D4),
                    )

                    Text(
                        text = "Taglish · English",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Parang kapag binawasan ang umiikot na pera sa meerkado, mas nagiging maingat ang mga negosyo kung saan sila gagastos.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Science  ·  Business  ·  Healthcare  ·  Academia  ·  Policy",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StepTwoContent() {
    Column {
        Text(
            text = "02 · Core Features",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
            ),
            color = Color(0xFF3B52D4),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Real-time clarity tailored to how you learn.",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 38.sp,
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Konteksto listens alongside you and builds a personalized companion feed as the session unfolds.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(28.dp))

        FeatureItemRow(
            iconRes = R.drawable.ic_live_broadcast,
            title = "Live Jargon Detection",
            description = "Automatically flags specialized terms in the live transcript and generates bite-sized concept cards.",
        )

        Spacer(modifier = Modifier.height(20.dp))

        FeatureItemRow(
            iconRes = R.drawable.ic_language,
            title = "Multi-Language Everyday Analogies",
            description = "Switch explanations effortlessly between English and Taglish so complex ideas click immediately.",
        )

        Spacer(modifier = Modifier.height(20.dp))

        FeatureItemRow(
            iconRes = R.drawable.ic_nav_glossary,
            title = "Personal Concept Glossary",
            description = "Save and filter key terms from every talk you attend for post-conference review.",
        )
    }
}

@Composable
private fun StepThreeContent() {
    Column {
        Text(
            text = "03 · Slide-Grounded & Host Console",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
            ),
            color = Color(0xFF3B52D4),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Anchored in the speaker’s actual presentation.",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 38.sp,
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "For both attendees and speakers, Konteksto connects spoken words directly to visual slides and diagrams.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(28.dp))

        FeatureItemRow(
            iconRes = R.drawable.ic_download,
            title = "Slide PDF Analysis",
            description = "Hosts upload their slide deck before the talk. Konteksto extracts diagrams, charts, and key takeaways to ground all answers.",
        )

        Spacer(modifier = Modifier.height(20.dp))

        FeatureItemRow(
            iconRes = R.drawable.ic_qr_code,
            title = "Instant QR & Code Room Sync",
            description = "Join any auditorium or lecture hall in one scan using the projector QR code or room code.",
        )
    }
}

@Composable
private fun StepFourContent(
    isMicGranted: Boolean,
    isCameraGranted: Boolean,
    isConceptSyncEnabled: Boolean,
    onRequestMic: () -> Unit,
    onRequestCamera: () -> Unit,
    onToggleConceptSync: () -> Unit,
) {
    Column {
        Text(
            text = "04 · Device Permissions",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
            ),
            color = Color(0xFF3B52D4),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Enable permissions for Konteksto to work.",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 38.sp,
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Grant access below so Konteksto can capture live seminar audio, scan projector QR codes, and deliver real-time concept cards.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        PermissionCard(
            iconRes = R.drawable.ic_mic,
            title = "Microphone Access",
            description = "Captures live speaker audio for real-time transcription and jargon detection.",
            isGranted = isMicGranted,
            onActionClick = onRequestMic,
        )

        Spacer(modifier = Modifier.height(14.dp))

        PermissionCard(
            iconRes = R.drawable.ic_camera,
            title = "Camera Access",
            description = "Scans stage projector QR codes to join live conference rooms.",
            isGranted = isCameraGranted,
            onActionClick = onRequestCamera,
        )

        Spacer(modifier = Modifier.height(14.dp))

        PermissionCard(
            iconRes = R.drawable.ic_bell,
            title = "Live Concept Sync",
            description = "Receives real-time slide takeaways and terminology cards during sessions.",
            isGranted = isConceptSyncEnabled,
            onActionClick = onToggleConceptSync,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_verified_shield),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF34C759),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Audio is processed exclusively for live session comprehension.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FeatureItemRow(
    iconRes: Int,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(MaterialTheme.shapes.small)
                .background(Color(0xFFEEF2FF)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = Color(0xFF3B52D4),
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PermissionCard(
    iconRes: Int,
    title: String,
    description: String,
    isGranted: Boolean,
    onActionClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.extendedColors.cardContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(Color(0xFFEEF2FF)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    tint = Color(0xFF3B52D4),
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    ),
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (isGranted) {
                Surface(
                    onClick = onActionClick,
                    shape = MaterialTheme.shapes.medium,
                    color = Color(0xFFECFDF5),
                    contentColor = Color(0xFF047857),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = "Enabled",
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Enabled",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            ),
                            color = Color(0xFF047857),
                        )
                    }
                }
            } else {
                Button(
                    onClick = onActionClick,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = Color(0xFF0F172A),
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Text(
                        text = "Enable",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        ),
                        color = Color(0xFF0F172A),
                    )
                }
            }
        }
    }
}

private fun isPermissionGranted(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    KontekstoTheme {
        OnboardingScreen(onOnboardingComplete = {})
    }
}
