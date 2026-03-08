package com.champengine.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.champengine.android.network.ChampEngineClient
import com.champengine.android.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── Smart Router Modes ─────────────────────────────────────────────
val ROUTER_MODES = listOf(
    "auto"     to "Auto · ChampEngine picks the best model for each task",
    "fast"     to "Fast · Always use smallest/fastest model",
    "powerful" to "Powerful · Always use largest/best model",
)

// ── Model Role Descriptions (informational only) ──────────────────
val MODEL_ROLES = listOf(
    "llama3.2:3b"      to "⚡ Fast Chat & Quick Questions",
    "phi3:mini"        to "⚡ Instant Responses",
    "mistral:7b"       to "✍️  Writing & Creative Tasks",
    "gemma2:9b"        to "⚖️  Balanced General Purpose",
    "llama3.1:8b"      to "🧠 Deep Reasoning",
    "mistral-nemo:12b" to "🧠 Best Quality Responses",
    "codellama:13b"    to "💻 Code Generation",
    "deepseek-r1:7b"   to "🔬 Complex Problem Solving",
    "llava:7b"         to "👁️  Vision & Image Understanding",
)

@Composable
fun SettingsScreen(client: ChampEngineClient) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var routerMode by remember { mutableStateOf("auto") }
    var pingStatus by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    var availableModels by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingModels by remember { mutableStateOf(true) }
    var showAdvanced by remember { mutableStateOf(false) }
    var customEndpoint by remember { mutableStateOf(client.getEndpoint()) }
    var customToken by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    // Auto-load available models on screen open
    LaunchedEffect(Unit) {
        val models = withContext(Dispatchers.IO) { client.listModels() }
        availableModels = models
        isLoadingModels = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Settings", color = TextPrimary, fontSize = 20.sp)

        // ── SERVER STATUS ─────────────────────────────────────────
        Surface(color = SurfaceDark, shape = RoundedCornerShape(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("🌐", fontSize = 16.sp)
                    Text(
                        "SERVER STATUS",
                        color = ClawGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                // Live model count
                if (isLoadingModels) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(
                            color = ClawGreen,
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            "Loading models...",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                } else {
                    Text(
                        "● ${availableModels.size} models online",
                        color = if (availableModels.isNotEmpty()) ClawGreen else ClawRed,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                HorizontalDivider(color = BorderDark)

                // Model roster
                Text(
                    "ACTIVE MODELS",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                )
                MODEL_ROLES.forEach { (modelId, role) ->
                    val isOnline = availableModels.any { it.startsWith(modelId.substringBefore(":")) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            role,
                            color = if (isOnline) TextPrimary else TextMuted.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                        )
                        Text(
                            if (isOnline) "●" else "○",
                            color = if (isOnline) ClawGreen else TextMuted.copy(alpha = 0.3f),
                            fontSize = 10.sp,
                        )
                    }
                }

                // Test connection button
                OutlinedButton(
                    onClick = {
                        isTesting = true
                        pingStatus = "Testing..."
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) { client.ping() }
                            pingStatus = if (ok) "✓ Connected to ChampEngine servers"
                                         else "✗ Could not connect — check internet"
                            isTesting = false
                        }
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, ClawGreen.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            color = ClawGreen,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        if (isTesting) "Testing..." else "TEST CONNECTION",
                        color = ClawGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                if (pingStatus.isNotBlank()) {
                    Text(
                        pingStatus,
                        color = if (pingStatus.startsWith("✓")) ClawGreen else ClawRed,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }

        // ── SMART ROUTER ──────────────────────────────────────────
        Surface(color = SurfaceDark, shape = RoundedCornerShape(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("🧭", fontSize = 16.sp)
                    Text(
                        "SMART ROUTER",
                        color = ClawGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Text(
                    "ChampEngine automatically routes each conversation " +
                    "to the best model for the task. No manual selection needed.",
                    color = TextMuted,
                    fontSize = 12.sp,
                )
                HorizontalDivider(color = BorderDark)

                ROUTER_MODES.forEach { (modeId, desc) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadioButton(
                            selected = routerMode == modeId,
                            onClick = { routerMode = modeId },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = ClawGreen,
                                unselectedColor = TextMuted,
                            ),
                        )
                        Text(desc, color = TextPrimary, fontSize = 12.sp)
                    }
                }
            }
        }

        // ── ADVANCED — BYOK or custom server ─────────────────────
        Surface(color = SurfaceDark, shape = RoundedCornerShape(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "ADVANCED",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    TextButton(onClick = { showAdvanced = !showAdvanced }) {
                        Text(
                            if (showAdvanced) "HIDE" else "SHOW",
                            color = ClawGreen,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }

                if (showAdvanced) {
                    Text(
                        "Bring your own server or API key. " +
                        "Your keys are stored only on this device — never sent to ChampEngine.",
                        color = TextMuted,
                        fontSize = 12.sp,
                    )
                    HorizontalDivider(color = BorderDark)

                    OutlinedTextField(
                        value = customEndpoint,
                        onValueChange = { customEndpoint = it; saved = false },
                        label = { Text("Custom Server URL", color = TextMuted) },
                        placeholder = {
                            Text(
                                "https://api.champengine.cloud",
                                color = TextMuted.copy(alpha = 0.4f),
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ClawGreen,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = customToken,
                        onValueChange = { customToken = it; saved = false },
                        label = { Text("Auth Token", color = TextMuted) },
                        placeholder = {
                            Text(
                                "Leave blank to use ChampEngine default",
                                color = TextMuted.copy(alpha = 0.4f),
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ClawGreen,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                client.saveCustomConfig(
                                    customEndpoint,
                                    customToken.ifBlank { client.getToken() },
                                    client.getModel(),
                                )
                                saved = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ClawGreen.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                if (saved) "✓ SAVED" else "SAVE",
                                color = ClawGreen,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                client.resetToDefaults()
                                customEndpoint = client.getEndpoint()
                                customToken = ""
                                saved = false
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, TextMuted.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                "RESET",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        }

        // ── ABOUT ─────────────────────────────────────────────────
        Surface(color = SurfaceDark, shape = RoundedCornerShape(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "ABOUT",
                    color = ClawGreen,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Text("ChampEngine v1.0.0", color = TextPrimary, fontSize = 13.sp)
                Text(
                    "AI. Unchained.",
                    color = ClawGreen.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                )
                Text(
                    "Private by design. Powerful by default.",
                    color = TextMuted,
                    fontSize = 11.sp,
                )
            }
        }
    }
}
