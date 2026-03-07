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
import kotlinx.coroutines.launch

val CHAMP_MODELS = listOf(
    "llama3.2:3b"      to "Llama 3.2 3B   · Fast · Best for most tasks",
    "llama3.1:8b"      to "Llama 3.1 8B   · Powerful · Deep reasoning",
    "mistral:7b"       to "Mistral 7B     · Sharp · Great for writing",
    "gemma2:9b"        to "Gemma 2 9B     · Google · Balanced",
    "phi3:mini"        to "Phi-3 Mini     · Tiny · Instant responses",
    "mistral-nemo:12b" to "Mistral Nemo   · Best quality available",
)

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { ChampEngineClient(context) }

    var selectedModel by remember { mutableStateOf(client.getModel()) }
    var pingStatus by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    var customEndpoint by remember { mutableStateOf(client.getEndpoint()) }
    var customToken by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Settings", color = TextPrimary, fontSize = 20.sp)

        // ── MODEL SELECTOR ────────────────────────────────────────
        Surface(color = SurfaceDark, shape = RoundedCornerShape(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("🤖", fontSize = 16.sp)
                    Text(
                        "AI MODEL",
                        color = ClawGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    if (client.isUsingDefaults()) {
                        Text(
                            "● LIVE",
                            color = ClawGreen,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
                Text(
                    "All models run on ChampEngine's private servers. " +
                    "Nothing is stored. Everything is private.",
                    color = TextMuted,
                    fontSize = 12.sp,
                )
                HorizontalDivider(color = BorderDark)

                CHAMP_MODELS.forEach { (modelId, desc) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadioButton(
                            selected = selectedModel == modelId,
                            onClick = {
                                selectedModel = modelId
                                client.saveCustomConfig(
                                    client.getEndpoint(),
                                    client.getToken(),
                                    modelId,
                                )
                                saved = true
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = ClawGreen,
                                unselectedColor = TextMuted,
                            ),
                        )
                        Column {
                            Text(
                                modelId,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                            Text(desc, color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }

                // Test connection
                OutlinedButton(
                    onClick = {
                        isTesting = true
                        pingStatus = "Testing..."
                        scope.launch {
                            val ok = client.ping()
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
                                    selectedModel,
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
                                selectedModel = client.getModel()
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
