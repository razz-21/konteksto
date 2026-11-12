package com.example.konteksto.ui.host

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.R
import com.example.konteksto.domain.model.HostSession
import com.example.konteksto.domain.model.UserProfile
import com.example.konteksto.ui.components.KontekstoTopBar
import com.example.konteksto.ui.theme.KontekstoButtonDefaults
import com.example.konteksto.ui.theme.KontekstoTheme
import com.example.konteksto.ui.theme.extendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostSessionsScreen(
    modifier: Modifier = Modifier,
    sessions: List<HostSession> = emptyList(),
    isLoading: Boolean = false,
    isRefreshing: Boolean = false,
    currentProfile: UserProfile? = null,
    onRefresh: () -> Unit = {},
    onNewSessionClick: () -> Unit = {},
    onPresentQrClick: (HostSession) -> Unit = {},
    onLiveRoomClick: (HostSession) -> Unit = {},
    onProfileClick: (() -> Unit)? = null,
    onShowMessage: (String) -> Unit = {},
) {
    val listState = rememberLazyListState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Kept outside the scrolling content so it stays pinned to the top.
                KontekstoTopBar(
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
                    currentProfile = currentProfile,
                    onProfileClick = onProfileClick,
                )

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item(key = "header") {
                            HostSessionHeader(
                                sessionCount = if (isLoading) null else sessions.size,
                            )
                        }

                        when {
                            isLoading -> items(count = 2, key = { "skeleton_$it" }) {
                                HostSessionCardSkeleton()
                            }

                            sessions.isEmpty() -> item(key = "empty") {
                                // Empty State View
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.extendedColors.cardContainer,
                                        contentColor = MaterialTheme.colorScheme.onBackground,
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp, horizontal = 18.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = MaterialTheme.shapes.medium,
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_nav_host),
                                                contentDescription = null,
                                                modifier = Modifier.size(26.dp),
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = stringResource(id = R.string.host_empty_title),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground,
                                            textAlign = TextAlign.Center,
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = stringResource(id = R.string.host_empty_subtitle),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp,
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                        )

                                        Spacer(modifier = Modifier.height(20.dp))

                                        Button(
                                            onClick = onNewSessionClick,
                                            shape = KontekstoButtonDefaults.shape,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                            ),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                            modifier = Modifier.height(KontekstoButtonDefaults.Height),
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_add),
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(id = R.string.host_create_first_session),
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                            )
                                        }
                                    }
                                }
                            }

                            else -> items(
                                items = sessions,
                                key = { it.id },
                            ) { session ->
                                HostSessionCard(
                                    session = session,
                                    onPresentQrClick = onPresentQrClick,
                                    onLiveRoomClick = onLiveRoomClick,
                                    onShowMessage = onShowMessage,
                                    currentProfile = currentProfile,
                                )
                            }
                        }
                    }
                }
            }

            // Floating Action Button: + New Session
            Button(
                onClick = onNewSessionClick,
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
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.host_new_session),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HostSessionsScreenEmptyPreview() {
    KontekstoTheme(darkTheme = false) {
        HostSessionsScreen(sessions = emptyList())
    }
}

@Preview(showBackground = true)
@Composable
fun HostSessionsScreenLoadingPreview() {
    KontekstoTheme(darkTheme = false) {
        HostSessionsScreen(isLoading = true)
    }
}

@Preview(showBackground = true)
@Composable
fun HostSessionsScreenContentPreview() {
    val sampleSessions = listOf(
        HostSession(
            id = "1",
            venue = "CDO Ayala",
            time = "10:00 – 11:30 AM",
            talkCode = "4VY4N6",
            title = "Object Oriented Programming",
            speakerName = "Ernesto Razo",
            category = "Technology & digital",
            attendeesCount = 1,
            isLive = false,
        ),
    )
    KontekstoTheme(darkTheme = false) {
        HostSessionsScreen(sessions = sampleSessions)
    }
}
