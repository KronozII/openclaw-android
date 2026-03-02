package com.openclaw.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openclaw.android.permission.ScopeToken
import com.openclaw.android.permission.ScopeType
import com.openclaw.android.ui.theme.*
import com.openclaw.android.ui.viewmodel.PermissionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onBack: () -> Unit,
    viewModel: PermissionViewModel = hiltViewModel(),
) {
    val tokens by viewModel.allTokens.collectAsState(initial = emptyList())
    val auditLog by viewModel.auditLog.collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "PERMISSIONS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        letterSpacing = 3.sp,
                        color = TextPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextMuted)
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.revokeAll() },
                        colors = ButtonDefaults.textButtonColors(contentColor = ClawDanger),
                    ) {
                        Text("REVOKE ALL", fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 2.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StatChip("${tokens.count { it.isValid() }}", "ACTIVE", ClawGreen)
                StatChip("${tokens.count { !it.isActive }}", "REVOKED", TextMuted)
                StatChip("${tokens.sumOf { it.usageCount }}", "TOTAL USES", ClawPurple)
            }

            HorizontalDivider(color = BorderDark)

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceDark,
                contentColor = ClawGreen,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = ClawGreen,
                    )
                }
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("GRANTS", fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 2.sp, modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("AUDIT LOG", fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 2.sp, modifier = Modifier.padding(12.dp))
                }
            }

            when (selectedTab) {
                0 -> GrantsTab(tokens, onRevoke = { viewModel.revokeToken(it) })
                1 -> AuditLogTab(auditLog)
            }
        }
    }
}

@Composable
private fun GrantsTab(tokens: List<ScopeToken>, onRevoke: (String) -> Unit) {
    if (tokens.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔒", fontSize = 40.sp)
                Spacer(Modifier.height(12.dp))
                Text("No permissions granted yet", color = TextMuted, fontSize = 14.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Group by type
            val grouped = tokens.groupBy { it.scopeType }
            grouped.forEach { (typePrefix, groupTokens) ->
                val scopeType = ScopeType.entries.firstOrNull { it.prefix == typePrefix }
                item {
                    Text(
                        "${scopeType?.icon ?: "🔑"} ${scopeType?.displayName?.uppercase() ?: typePrefix}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                items(groupTokens) { token ->
                    PermissionTokenCard(token, onRevoke)
                }
            }
        }
    }
}

@Composable
private fun PermissionTokenCard(token: ScopeToken, onRevoke: (String) -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    var expanded by remember { mutableStateOf(false) }

    Surface(
        color = if (token.isValid()) Surface2Dark else SurfaceDark.copy(alpha = 0.5f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(
            1.dp,
            if (token.isValid()) BorderDark else BorderDark.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        token.resource,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = if (token.isValid()) ClawGreen else TextMuted,
                    )
                    Text(
                        "Used ${token.usageCount}× · Granted ${dateFormat.format(Date(token.grantedAt))}",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                if (token.isValid()) {
                    TextButton(
                        onClick = { onRevoke(token.id) },
                        colors = ButtonDefaults.textButtonColors(contentColor = ClawDanger),
                    ) {
                        Text("REVOKE", fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 1.sp)
                    }
                } else {
                    Text("REVOKED", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextMuted, letterSpacing = 2.sp)
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = BorderDark, modifier = Modifier.padding(bottom = 10.dp))
                    Text("Reason: ${token.reason}", fontSize = 12.sp, color = TextMuted)
                    if (token.expiresAt != null) {
                        Text("Expires: ${dateFormat.format(Date(token.expiresAt))}", fontSize = 12.sp, color = ClawWarn)
                    } else {
                        Text("Permanent until revoked", fontSize = 12.sp, color = TextMuted)
                    }
                    Text("Token ID: ${token.id.take(16)}...", fontSize = 10.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun AuditLogTab(entries: List<com.openclaw.android.storage.models.AuditLogEntry>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(entries) { entry ->
            val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
            Surface(
                color = when (entry.threatLevel) {
                    "CRITICAL" -> ClawDanger.copy(alpha = 0.1f)
                    "HIGH" -> ClawWarn.copy(alpha = 0.08f)
                    else -> Surface2Dark
                },
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(
                    1.dp,
                    when (entry.threatLevel) {
                        "CRITICAL" -> ClawDanger.copy(alpha = 0.3f)
                        "HIGH" -> ClawWarn.copy(alpha = 0.3f)
                        else -> BorderDark
                    }
                ),
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        dateFormat.format(Date(entry.timestamp)),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = TextMuted,
                        modifier = Modifier.width(60.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                entry.actionType,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = TextPrimary,
                            )
                            Text(
                                entry.outcome,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = when (entry.outcome) {
                                    "ALLOWED" -> ClawGreen
                                    "BLOCKED" -> ClawWarn
                                    "THREAT_DETECTED" -> ClawDanger
                                    else -> TextMuted
                                },
                            )
                        }
                        Text(entry.resource, fontSize = 11.sp, color = TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontFamily = FontFamily.Monospace, fontSize = 18.sp, color = color)
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 8.sp, letterSpacing = 2.sp, color = TextMuted)
    }
}
