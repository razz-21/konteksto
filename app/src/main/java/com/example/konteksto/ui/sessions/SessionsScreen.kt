package com.example.konteksto.ui.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.konteksto.R
import com.example.konteksto.data.repository.FirebaseSessionRepository
import com.example.konteksto.data.repository.SessionRepository
import com.example.konteksto.ui.components.KontekstoTopBar
import com.example.konteksto.ui.components.showKontekstoSnackbar
import com.example.konteksto.ui.theme.KontekstoTheme

@Composable
fun SessionsScreen(
    onNavigateToAttendeeRoom: (sessionId: String) -> Unit,
    modifier: Modifier = Modifier,
    sessionRepository: SessionRepository = remember { FirebaseSessionRepository() },
    viewModel: SessionsViewModel = viewModel(
        factory = SessionsViewModel.provideFactory(sessionRepository),
    ),
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredSessions by viewModel.filteredSessions.collectAsStateWithLifecycle(initialValue = emptyList())
    val joinedSessionIds by viewModel.joinedSessionIds.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val showAttendSheet by viewModel.showAttendSheet.collectAsStateWithLifecycle()
    val sessionToConfirm by viewModel.selectedSessionToConfirm.collectAsStateWithLifecycle()

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
                // Header Row using exact same KontekstoTopBar component and layout as HostSessionsScreen
                KontekstoTopBar(
                    onAttendClick = { viewModel.openAttendSheet() },
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                SessionsSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sessions List / Skeleton Loading / Empty State
                if (isLoading) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(count = 2) {
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
                                    text = "Tap \"Attend\" above to join a session with a code or QR code",
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
                        contentPadding = PaddingValues(bottom = 12.dp),
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
                            )
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
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
