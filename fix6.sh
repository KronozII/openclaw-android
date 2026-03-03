#!/bin/bash
BASE="app/src/main/java/com/openclaw/android"

rm -f $BASE/ui/screens/SettingsScreen.kt

cat > $BASE/ui/screens/SettingsScreen.kt << 'SETTINGSEOF'
package com.openclaw.android.ui.screens

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
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
import com.openclaw.android.ui.theme.*
import java.io.File

data class ModelInfo(
    val id: String,
    val displayName: String,
    val fileName: String,
    val kaggleUrl: String,
    val sizeMb: Int,
)

val MODELS = listOf(
    ModelInfo(
        id = "gemma2_gpu",
        displayName = "Gemma 2 2B GPU (recommended)",
        fileName = "gemma2-2b-it-gpu-int8.bin",
        kaggleUrl = "https://www.kaggle.com/models/google/gemma-2/tfLite/gemma2-2b-it-gpu-int8",
        sizeMb = 2400,
    ),
    ModelInfo(
        id = "gemma2_cpu",
        displayName = "Gemma 2 2B CPU",
        fileName = "gemma2-2b-it-cpu-int8.bin",
        kaggleUrl = "https://www.kaggle.com/models/google/gemma-2/tfLite/gemma2-2b-it-cpu-int8",
        sizeMb = 2400,
    ),
    ModelInfo(
        id = "phi3_mini",
        displayName = "Phi-3 Mini (lower RAM)",
        fileName = "phi-3-mini-4k-instruct.bin",
        kaggleUrl = "https://www.kaggle.com/models/microsoft/phi-3/tfLite/phi-3-mini-4k-instruct",
        sizeMb = 1800,
    ),
)

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val downloadIds = remember { mutableStateMapOf<String, Long>() }
    val downloadComplete = remember { mutableStateMapOf<String, Boolean>() }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val safeCtx = ctx ?: return
                val safeIntent = intent ?: return
                val id = safeIntent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id == -1L) return

                val modelEntry = downloadIds.entries.find { it.value == id } ?: return
                val model = MODELS.find { it.id == modelEntry.key } ?: return

                try {
                    val dm = safeCtx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val cursor = dm.query(DownloadManager.Query().setFilterById(id))
                    if (cursor.moveToFirst()) {
                        val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (cursor.getInt(statusCol) == DownloadManager.STATUS_SUCCESSFUL) {
                            val srcFile = File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                model.fileName,
                            )
                            val destDir = File(safeCtx.filesDir, "models").also { it.mkdirs() }
                            val destFile = File(destDir, model.fileName)
                            if (srcFile.exists()) {
                                srcFile.copyTo(destFile, overwrite = true)
                                srcFile.delete()
                            }
                            downloadComplete[model.id] = true
                        }
                    }
                    cursor.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            try { context.unregisterReceiver(receiver) } catch (e: Exception) { }
        }
    }

    fun isReady(model: ModelInfo): Boolean {
        return File(context.filesDir, "models/${model.fileName}")
            .let { it.exists() && it.length() > 1_000_000 }
    }

    fun startDownload(model: ModelInfo) {
        try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(model.kaggleUrl)).apply {
                setTitle("OpenClaw: ${model.displayName}")
                setDescription("~${model.sizeMb}MB — downloading...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, model.fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            val id = dm.enqueue(request)
            downloadIds[model.id] = id
        } catch (e: Exception) {
            // Fallback: open Kaggle in browser
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(model.kaggleUrl)))
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
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

        Surface(color = SurfaceDark, shape = RoundedCornerShape(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("AI MODELS", color = ClawGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text(
                    "Tap DOWNLOAD to open Kaggle (free Google account required). Model auto-moves to the correct folder when done.",
                    color = TextMuted,
                    fontSize = 12.sp,
                )
                HorizontalDivider(color = BorderDark)

                MODELS.forEach { model ->
                    val ready = isReady(model) || downloadComplete[model.id] == true
                    val inProgress = downloadIds.containsKey(model.id) && !ready

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(model.displayName, color = TextPrimary, fontSize = 13.sp)
                            Text(
                                "~${model.sizeMb}MB",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        when {
                            ready -> Text(
                                "✓ READY",
                                color = ClawGreen,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                            inProgress -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                LinearProgressIndicator(
                                    color = ClawGreen,
                                    trackColor = BorderDark,
                                    modifier = Modifier.width(80.dp),
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "DOWNLOADING",
                                    color = ClawWarn,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                            else -> OutlinedButton(
                                onClick = { startDownload(model) },
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, ClawGreen.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    "DOWNLOAD",
                                    color = ClawGreen,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = BorderDark.copy(alpha = 0.4f))
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
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ABOUT", color = ClawGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("OpenClaw Omega v2.0.0", color = TextPrimary, fontSize = 13.sp)
                Text("100% private. No cloud. No tracking. No limits.", color = ClawGreen.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }
}
SETTINGSEOF
echo "✓ SettingsScreen.kt ($(wc -l < $BASE/ui/screens/SettingsScreen.kt) lines)"
echo ""
echo "Run: git add -A && git commit -m 'fix settings crash Android 14 receiver flag' && git push"

