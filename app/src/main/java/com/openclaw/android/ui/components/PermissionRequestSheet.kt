package com.openclaw.android.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openclaw.android.agent.sentinel.ThreatAlert
import com.openclaw.android.agent.sentinel.ThreatLevel
import com.openclaw.android.permission.PermissionDecision
import com.openclaw.android.permission.PermissionRequest
import com.openclaw.android.permission.ScopeType
import com.openclaw.android.ui.theme.*

/**
 * PermissionRequestSheet — shown when agent needs a privileged resource.
 *
 * Design principle: Full transparency. The user sees:
 * - WHAT is being requested (resource + scope type icon)
 * - WHY (agent's stated reason)
 * - WHAT THE AGENT WAS DOING (context)
 * - Three clear choices: Always / Once / Never
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionRequestSheet(
    request: PermissionRequest,
    onDecision: (PermissionDecision) -> Unit,
) {
    // Full-screen scrim + bottom sheet — cannot be dismissed by tapping outside
    // User MUST make an explicit choice
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Surface2Dark,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {

                // Header — scope type and icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        color = permissionColor(request.scopeType).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            scopeIcon(request.scopeType),
                            modifier = Modifier.padding(10.dp),
                            fontSize = 24.sp,
                        )
                    }
                    Column {
                        Text(
                            "PERMISSION REQUEST",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            letterSpacing = 3.sp,
                            color = TextMuted,
                        )
                        Text(
                            request.scopeType.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // What is being requested
                PermissionDetailRow("Resource", request.resource, highlighted = true)
                PermissionDetailRow("Why", request.reason)
                if (request.agentContext.isNotBlank()) {
                    PermissionDetailRow("Context", request.agentContext)
                }

                Spacer(Modifier.height(4.dp))

                // Fine print — Sentinel note
                Surface(
                    color = ClawGreen.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, ClawGreen.copy(alpha = 0.15f)),
                ) {
                    Text(
                        "🛡 All permissions are monitored by the Sentinel. You can revoke this anytime in Permissions.",
                        modifier = Modifier.padding(10.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = TextMuted,
                        lineHeight = 15.sp,
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Decision buttons — three clear options
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onDecision(PermissionDecision.AllowPermanent(request.requestId)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = permissionColor(request.scopeType),
                            contentColor = Color.Black,
                        ),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            "ALWAYS ALLOW",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            letterSpacing = 2.sp,
                        )
                    }
                    OutlinedButton(
                        onClick = { onDecision(PermissionDecision.AllowOnce(request.requestId)) },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, permissionColor(request.scopeType).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            "ALLOW ONCE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            letterSpacing = 2.sp,
                            color = permissionColor(request.scopeType),
                        )
                    }
                    TextButton(
                        onClick = { onDecision(PermissionDecision.Deny(request.requestId)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "DENY",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            letterSpacing = 2.sp,
                            color = ClawDanger,
                        )
                    }
                    TextButton(
                        onClick = { onDecision(PermissionDecision.DenyAndBlock(request.requestId)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "DENY & NEVER ASK AGAIN",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            color = TextMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionDetailRow(label: String, value: String, highlighted: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            label.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 2.sp,
            color = TextMuted,
            modifier = Modifier.width(64.dp),
        )
        Text(
            value,
            fontSize = 13.sp,
            color = if (highlighted) ClawGreen else TextPrimary,
            fontFamily = if (highlighted) FontFamily.Monospace else FontFamily.SansSerif,
        )
    }
}

/**
 * ThreatAlertBanner — shown at top of screen, blocks interaction until reviewed
 */
@Composable
fun ThreatAlertBanner(
    alerts: List<ThreatAlert>,
    onReview: (ThreatAlert) -> Unit,
) {
    val topAlert = alerts.first()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = when (topAlert.level) {
            ThreatLevel.CRITICAL -> ClawDanger
            ThreatLevel.HIGH -> ClawWarn
            else -> Surface2Dark
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("🛡️", fontSize = 18.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "SENTINEL ALERT",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 3.sp,
                    color = Color.Black.copy(alpha = 0.7f),
                )
                Text(
                    topAlert.description,
                    fontSize = 13.sp,
                    color = Color.Black,
                    fontFamily = FontFamily.Monospace,
                )
            }
            TextButton(
                onClick = { onReview(topAlert) },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Black),
            ) {
                Text("REVIEW", fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 2.sp)
            }
        }
    }
}

private fun permissionColor(scopeType: ScopeType) = when (scopeType) {
    ScopeType.NETWORK, ScopeType.SANDBOX_NET -> ClawGreen
    ScopeType.CAMERA, ScopeType.MICROPHONE -> ClawPurple
    ScopeType.CONTACTS, ScopeType.LOCATION -> ClawWarn
    ScopeType.SOCIAL_POST -> ClawRed
    ScopeType.BACKGROUND -> ClawDanger
    else -> ClawGreen
}

private fun scopeIcon(scopeType: ScopeType) = scopeType.icon
