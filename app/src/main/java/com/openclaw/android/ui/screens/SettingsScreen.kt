package com.openclaw.android.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
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
import com.openclaw.android.agent.primary.OnDeviceModel
import com.openclaw.android.ui.theme.*
import java.io.File

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val downloading = remember { mutableStateMapOf<OnDeviceModel, Boolean>() }

    val modelUrls = mapOf(
        OnDeviceModel.GEMMA_2B to "https://huggingface.co/litert-community/Gemma2-2b-it/resolve/main/gemma2-2b-it-gpu-int8.bin",
        OnDeviceModel.GEMMA_2B_CPU to "https://huggingface.co/litert-community/Gemma2-2b-it/resolve/main/gemma2-2b-it-cpu-int8.bin",
        OnDeviceModel.PHI3_MINI to "https://huggingface.co/litert-community/Phi-3-mini-4k-instruct/resolve/main/phi3-mini-4k-instruct-int4.bin",
    )

    fun isDownloaded(model: OnDeviceModel) =
        File(context.filesDir, "models/${model.fileName}").let { it.exists() && it.length() > 1000 }

    fun startDownload(model: OnDeviceModel) {
        val url = modelUrls[model] ?: return
        val dest = File(context.filesDir, "models").also { it.mkdirs() }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(DownloadManager.Request(Uri.parse(url))
            .setTitle("OpenClaw: ${model.displayName}")
            .setDescription("Downloading AI model...")
            .setDestinationUri(Uri.fromFile(File(dest, model.fileName)))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true))
        downloading[model] = true
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Settings", color = TextPrimary, fontSize = 20.sp)

        Surface(color = SurfaceDark, shape = RoundedCornerShape(8.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("AI MODELS", color = ClawGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("Models stored privately on your device. ~1-2GB each.", color = TextMuted, fontSize = 12.sp)
                HorizontalDivider(color = BorderDark)
                OnDeviceModel.entries.forEach { model ->
                    val done = isDownloaded(model)
                    val inProgress = downloading[model] == true && !done
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(model.displayName, color = TextPrimary, fontSize = 14.sp)
                            Text(model.fileName, color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(Modifier.width(8.dp))
                        when {
                            done -> Text("✓ READY", color = ClawGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            inProgress -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                LinearProgressIndicator(color = ClawGreen, trackColor = BorderDark, modifier = Modifier.width(80.dp))
                                Text("CHECK NOTIFS", color = ClawWarn, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                            else -> OutlinedButton(onClick = { startDownload(model) },
                                border = androidx.compose.foundation.BorderStroke(1.dp, ClawGreen.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                                Text("DOWNLOAD", color = ClawGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                    HorizontalDivider(color = BorderDark.copy(alpha = 0.5f))
                }
            }
        }

        Surface(color = SurfaceDark, shape = RoundedCornerShape(8.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SKILL ENGINE", color = ClawGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("35+ built-in skills. Build custom skills from the Skills tab.", color = TextMuted, fontSize = 13.sp)
            }
        }

        Surface(color = SurfaceDark, shape = RoundedCornerShape(8.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SECURITY", color = ClawGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("Sentinel monitoring active. All permissions logged. Zero network by default.", color = TextMuted, fontSize = 13.sp)
            }
        }

        Surface(color = SurfaceDark, shape = RoundedCornerShape(8.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ABOUT", color = ClawGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("OpenClaw Omega v2.0.0", color = TextPrimary, fontSize = 13.sp)
                Text("The most advanced on-device AI system for Android.", color = TextMuted, fontSize = 12.sp)
                Text("100% private. No cloud. No tracking. No limits.", color = ClawGreen.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }
}
