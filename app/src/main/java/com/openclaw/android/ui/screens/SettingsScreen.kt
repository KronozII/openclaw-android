package com.openclaw.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.openclaw.android.agent.primary.OnDeviceModel
import com.openclaw.android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = { Text("SETTINGS", fontFamily = FontFamily.Monospace, fontSize = 13.sp, letterSpacing = 3.sp, color = TextPrimary) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = TextMuted) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SettingsSectionHeader("ON-DEVICE MODEL")
            }
            item {
                OnDeviceModel.entries.forEach { model ->
                    Surface(
                        color = Surface2Dark,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(model.displayName, color = TextPrimary, fontSize = 14.sp)
                                Text(
                                    "${model.ramRequiredMb}MB RAM · ${model.fileName}",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                            OutlinedButton(
                                onClick = { /* trigger model download */ },
                                border = androidx.compose.foundation.BorderStroke(1.dp, ClawGreen.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text("DOWNLOAD", fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = 2.sp, color = ClawGreen)
                            }
                        }
                    }
                }
            }

            item { SettingsSectionHeader("SECURITY") }
            item {
                SettingsToggle(
                    title = "Sentinel Monitoring",
                    subtitle = "Autonomous security auditing (recommended: ON)",
                    checked = true,
                    accent = ClawGreen,
                )
            }
            item {
                SettingsToggle(
                    title = "Injection Scan",
                    subtitle = "Scan all inputs for prompt injection patterns",
                    checked = true,
                    accent = ClawGreen,
                )
            }
            item {
                SettingsToggle(
                    title = "Exfiltration Guard",
                    subtitle = "Monitor outgoing payload size",
                    checked = true,
                    accent = ClawGreen,
                )
            }

            item { SettingsSectionHeader("NETWORK") }
            item {
                SettingsToggle(
                    title = "Default: Offline Mode",
                    subtitle = "Block all network unless explicitly permitted",
                    checked = true,
                    accent = ClawWarn,
                )
            }

            item { SettingsSectionHeader("ABOUT") }
            item {
                Surface(color = Surface2Dark, shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("OpenClaw Android", color = TextPrimary, fontSize = 14.sp)
                            Surface(color = ClawGreen.copy(alpha = 0.15f), shape = RoundedCornerShape(2.dp)) {
                                Text("v1.0.0", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = ClawGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                            }
                        }
                        Text("No API calls · On-device inference · Sovereign permissions · Sentinel security", color = TextMuted, fontSize = 11.sp, lineHeight = 17.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        title,
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp,
        letterSpacing = 3.sp,
        color = TextMuted,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsToggle(title: String, subtitle: String, checked: Boolean, accent: Color) {
    var state by remember { mutableStateOf(checked) }
    Surface(color = Surface2Dark, shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 14.sp)
                Text(subtitle, color = TextMuted, fontSize = 11.sp)
            }
            Switch(
                checked = state,
                onCheckedChange = { state = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = accent,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = BorderDark,
                ),
            )
        }
    }
}
