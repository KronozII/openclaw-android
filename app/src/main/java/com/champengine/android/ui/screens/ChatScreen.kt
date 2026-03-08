package com.champengine.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.champengine.android.agent.primary.AgentState
import com.champengine.android.agent.sentinel.ThreatAlert
import com.champengine.android.agent.sentinel.ThreatLevel
import com.champengine.android.ui.components.PermissionRequestSheet
import com.champengine.android.ui.components.ThreatAlertBanner
import com.champengine.android.ui.theme.*
import com.champengine.android.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onOpenSettings: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenSandbox: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val agentState by viewModel.agentState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    val activeAlerts by viewModel.activeAlerts.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val topRequest = pendingRequests.firstOrNull()

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    when (agentState) {
                                        is AgentState.Ready,
                                        is AgentState.Generating -> ClawGreen
                                        is AgentState.Error      -> ClawRed
                                        else                     -> TextMuted
                                    },
                                    shape = RoundedCornerShape(4.dp),
                                )
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "CHAMPENGINE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            letterSpacing = 3.sp,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                        if (agentState is AgentState.Frozen) {
                            Surface(
                                color = ClawDanger.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(2.dp),
                            ) {
                                Text(
                                    "FROZEN",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = ClawDanger,
                                    letterSpacing = 2.sp,
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (activeAlerts.isNotEmpty()) {
                        IconButton(onClick = { }) {
                            Badge(containerColor = ClawDanger) {
                                Text("${activeAlerts.size}", fontSize = 10.sp)
                            }
                            Icon(Icons.Default.Security, "Alerts", tint = ClawDanger)
                        }
                    }
                    IconButton(onClick = onOpenSandbox) {
                        Icon(Icons.Default.Code, "Sandbox", tint = ClawPurple)
                    }
                    IconButton(onClick = onOpenPermissions) {
                        Icon(Icons.Default.Shield, "Permissions", tint = ClawGreen)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = TextMuted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    scrolledContainerColor = SurfaceDark,
                ),
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(SurfaceDark)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                AgentStatusBar(agentState)
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                when (agentState) {
                                    is AgentState.Frozen     -> "Agent paused — review security alert"
                                    is AgentState.Connecting -> "Connecting to ChampEngine..."
                                    is AgentState.Error      -> "Connection error — check Settings"
                                    else                     -> "Ask anything..."
                                },
                                color = TextMuted,
                                fontSize = 14.sp,
                            )
                        },
                        enabled = agentState is AgentState.Ready,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ClawGreen,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = ClawGreen,
                            disabledBorderColor = BorderDark,
                        ),
                        shape = RoundedCornerShape(4.dp),
                    )
                    FloatingActionButton(
                        onClick = {
                            if (inputText.isNotBlank() && agentState is AgentState.Ready) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier.size(52.dp),
                        containerColor = ClawGreen,
                        contentColor = Color.Black,
                        shape = RoundedCornerShape(4.dp),
                        elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    ) {
                        if (agentState is AgentState.Generating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Default.Send, "Send")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            AnimatedVisibility(
                visible = activeAlerts.isNotEmpty(),
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                ThreatAlertBanner(
                    alerts = activeAlerts,
                    onReview = { alert -> viewModel.reviewThreat(alert) },
                )
            }

            if (messages.isEmpty()) {
                EmptyStateView()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(messages) { message ->
                        MessageBubble(message = message)
                    }
                    if (agentState is AgentState.Generating) {
                        item { LoadingIndicator("Generating...") }
                    }
                    if (agentState is AgentState.Connecting) {
                        item { LoadingIndicator("Connecting to ChampEngine...") }
                    }
                }
            }

            if (topRequest != null) {
                PermissionRequestSheet(
                    request = topRequest,
                    onDecision = { decision ->
                        viewModel.resolvePermission(topRequest.requestId, decision)
                    },
                )
            }
        }
    }
}

// ── Agent Status Bar ──────────────────────────────────────────────
@Composable
fun AgentStatusBar(state: AgentState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val (statusText, statusColor) = when (state) {
            is AgentState.Idle       -> "INITIALISING" to TextMuted
            is AgentState.Connecting -> "CONNECTING..." to ClawWarn
            is AgentState.Ready      -> "LIVE · ${state.model.uppercase()}" to ClawGreen
            is AgentState.Generating -> "GENERATING..." to ClawGreen
            is AgentState.Frozen     -> "⚠ FROZEN BY SENTINEL" to ClawDanger
            is AgentState.Error      -> "ERROR · CHECK SETTINGS" to ClawDanger
        }
        Text(
            statusText,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 2.sp,
            color = statusColor,
        )
        if (state is AgentState.Ready || state is AgentState.Generating) {
            Text("·", color = TextMuted, fontSize = 9.sp)
            Text(
                "PRIVATE · NO DATA STORED",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 2.sp,
                color = TextMuted,
            )
        }
    }
}

// ── Message Bubble ────────────────────────────────────────────────
@Composable
fun MessageBubble(message: com.champengine.android.storage.models.ChatMessageEntity) {
    val isUser = message.role == "user"
    val isSentinel = message.role == "sentinel"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (isSentinel) {
            Surface(
                color = ClawDanger.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, ClawDanger.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("🛡️", fontSize = 16.sp)
                    Column {
                        Text(
                            "SENTINEL",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            letterSpacing = 2.sp,
                            color = ClawDanger,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(message.content, color = TextPrimary,
                             fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }
        } else {
            Surface(
                color = if (isUser) ClawGreen.copy(alpha = 0.12f) else Surface2Dark,
                shape = RoundedCornerShape(
                    topStart = if (isUser) 12.dp else 4.dp,
                    topEnd = if (isUser) 4.dp else 12.dp,
                    bottomStart = 12.dp,
                    bottomEnd = 12.dp,
                ),
                border = if (isUser)
                    BorderStroke(1.dp, ClawGreen.copy(alpha = 0.2f))
                else null,
                modifier = Modifier.widthIn(max = 320.dp),
            ) {
                Text(
                    message.content,
                    modifier = Modifier.padding(12.dp),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                )
            }
        }
    }
}

// ── Empty State ───────────────────────────────────────────────────
@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("⚡", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "CHAMPENGINE",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            letterSpacing = 4.sp,
            color = TextMuted,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "AI. Unchained.",
            color = TextPrimary,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Private by design. Powerful by default.",
            color = TextMuted,
            fontSize = 13.sp,
        )
    }
}

// ── Loading Indicator ─────────────────────────────────────────────
@Composable
fun LoadingIndicator(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(8.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = ClawGreen,
            strokeWidth = 2.dp,
        )
        Text(text, color = TextMuted, fontSize = 13.sp,
             fontFamily = FontFamily.Monospace)
    }
}
