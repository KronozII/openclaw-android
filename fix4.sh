#!/bin/bash
BASE="app/src/main/java/com/openclaw/android"

rm -f $BASE/ui/screens/SettingsScreen.kt

cat > $BASE/ui/screens/SettingsScreen.kt << 'SETTINGSEOF'
package com.openclaw.android.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
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

    fun isDownloaded(model: OnDeviceModel): Boolean {
        val internalFile = File(context.filesDir, "models/${model.fileName}")
        return internalFile.exists() && internalFile.length() > 1_000_000
    }

    fun startDownload(model: OnDeviceModel) {
        try {
            val url = modelUrls[model] ?: return
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle("OpenClaw: ${model.displayName}")
                setDescription("Downloading AI model to Downloads folder...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, model.fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            dm.enqueue(request)
            downloading[model] = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Settings", color = TextPrimary, fontSize = 20.sp)

        // Model downloads
        Surface(color = SurfaceDark, shape = RoundedCornerShape(8.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("AI MODELS", color = ClawGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text(
                    "Models download to your Downloads folder. After downloading, move the file to:\nAndroid/data/com.openclaw.android/files/models/",
                    color = TextMuted,
                    fontSize = 12.sp,
                )
                HorizontalDivider(color = BorderDark)

                OnDeviceModel.entries.forEach { model ->
                    val done = isDownloaded(model)
                    val inProgress = downloading[model] == true && !done

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(model.displayName, color = TextPrimary, fontSize = 14.sp)
                            Text(model.fileName, color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(Modifier.width(8.dp))
                        when {
                            done -> Text("✓ READY", color = ClawGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            inProgress -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                LinearProgressIndicator(
                                    color = ClawGreen,
                                    trackColor = BorderDark,
                                    modifier = Modifier.width(80.dp),
                                )
                                Spacer(Modifier.height(2.dp))
                                Text("CHECK NOTIFS", color = ClawWarn, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                            else -> OutlinedButton(
                                onClick = { startDownload(model) },
                                border = androidx.compose.foundation.BorderStroke(1.dp, ClawGreen.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text("DOWNLOAD", color = ClawGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                    HorizontalDivider(color = BorderDark.copy(alpha = 0.5f))
                }
            }
        }

        // Skill engine info
        Surface(color = SurfaceDark, shape = RoundedCornerShape(8.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SKILL ENGINE", color = ClawGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("35+ built-in skills. Build custom skills from the Skills tab.", color = TextMuted, fontSize = 13.sp)
            }
        }

        // About
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
SETTINGSEOF
echo "✓ SettingsScreen.kt ($(wc -l < $BASE/ui/screens/SettingsScreen.kt) lines)"
echo ""
echo "Run: git add -A && git commit -m 'fix download crash - use public Downloads dir' && git push"

