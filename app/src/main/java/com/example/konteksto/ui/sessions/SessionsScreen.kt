package com.example.konteksto.ui.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.konteksto.R
import com.example.konteksto.data.repository.FirebaseSessionRepository
import com.example.konteksto.data.repository.FirebaseUserProfileRepository
import com.example.konteksto.data.repository.SessionRepository
import com.example.konteksto.data.repository.UserProfileRepository
import com.example.konteksto.ui.components.KontekstoTopBar
import com.example.konteksto.ui.components.showKontekstoSnackbar
import com.example.konteksto.ui.sessions.attend.AttendSessionSheet
import com.example.konteksto.ui.theme.KontekstoButtonDefaults
import com.example.konteksto.ui.theme.KontekstoTheme

@Composable
fun SessionsScreen(
    onNavigateToAttendeeRoom: (sessionId: String) -> Unit,
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = {},
    sessionRepository: SessionRepository = remember { FirebaseSessionRepository() },
    userProfileRepository: UserProfileRepository = LocalContext.current.let { context ->
        remember(context) { FirebaseUserProfileRepository(context.applicationContext) }
    },
    viewModel: SessionsViewModel = viewModel(
        factory = SessionsViewModel.provideFactory(
            sessionRepository = sessionRepository,
            userProfileRepository = userProfileRepository,
        ),
    ),
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredSessions by viewModel.filteredSessions.collectAsStateWithLifecycle(initialValue = emptyList())
    val joinedSessionIds by viewModel.joinedSessionIds.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val showAttendSheet by viewModel.showAttendSheet.collectAsStateWithLifecycle()
    val sessionToConfirm by viewModel.selectedSessionToConfirm.collectAsStateWithLifecycle()
    val currentProfile by viewModel.currentProfile.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showKontekstoSnackbar(message)
        }
    }

    if (showAttendSheet) {
        AttendSessionSheet(
            onDismiss = { viewModel.closeAttendSheet() },
            onVerifyCode = { code, onResult ->
                viewModel.verifyCodeAndAttend(code, onResult)
            },
            onConfirmJoin = { session ->
                viewModel.confirmJoinSession(session) { isLive ->
                    if (isLive) {
                        onNavigateToAttendeeRoom(session.id)
                    }
                }
            },
            sessionToConfirm = sessionToConfirm,
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                // Top Header Row
                KontekstoTopBar(
                    currentProfile = currentProfile,
                    onProfileClick = onProfileClick,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                SessionsSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title Row with Session Count Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Sessions",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )

                    if (!isLoading) {
                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Text(
                                text = filteredSessions.size.toString(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sessions List / Skeleton Loading / Empty State
                if (isLoading) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(count = 2, key = { "skeleton_$it" }) {
                            AttendeeSessionCardSkeleton()
                        }
                    }
                } else if (filteredSessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_nav_sessions),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = if (searchQuery.isBlank()) {
                                    "No attended sessions yet"
                                } else {
                                    "No sessions match \"$searchQuery\""
                                },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )

                            if (searchQuery.isBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap \"Attend session\" below to join with a code or QR code",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(
                            items = filteredSessions,
                            key = { it.id },
                        ) { session ->
                            val isAttending = joinedSessionIds.contains(session.id)

                            AttendeeSessionCard(
                                session = session,
                                isAttending = isAttending,
                                onActionClick = {
                                    onNavigateToAttendeeRoom(session.id)
                                },
                                currentProfile = currentProfile,
                            )
                        }
                    }
                }
            }

            // Floating Action Button ("Attend session"), styled like the Host Sessions "New Session" button.
            Button(
                onClick = { viewModel.openAttendSheet() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 20.dp)
                    .height(KontekstoButtonDefaults.Height),
                shape = KontekstoButtonDefaults.shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 6.dp,
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_qr_code),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Attend session",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SessionsScreenLightPreview() {
    KontekstoTheme(darkTheme = false) {
        SessionsScreen(onNavigateToAttendeeRoom = {})
    }
}

@Preview(showBackground = true)
@Composable
fun SessionsScreenDarkPreview() {
    KontekstoTheme(darkTheme = true) {
        SessionsScreen(onNavigateToAttendeeRoom = {})
    }
}
