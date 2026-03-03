#!/bin/bash
BASE="app/src/main/java/com/openclaw/android"

rm -f $BASE/ui/screens/SettingsScreen.kt

cat > $BASE/ui/screens/SettingsScreen.kt << 'SETTINGSEOF'
package com.openclaw.android.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.openclaw.android.ui.theme.*
import java.io.File

data class ModelInfo(
    val id: String,
    val displayName: String,
    val fileName: String,
    val kaggleUrl: String,
    val sizeMb: Int,
    val description: String,
)

val MODELS = listOf(
    ModelInfo(
        id = "gemma2_gpu",
        displayName = "Gemma 2 2B GPU",
        fileName = "gemma2-2b-it-gpu-int8.bin",
        kaggleUrl = "https://www.kaggle.com/models/google/gemma-2/tfLite/gemma2-2b-it-gpu-int8/1",
        sizeMb = 2400,
        description = "Recommended. Needs GPU support.",
    ),
    ModelInfo(
        id = "gemma2_cpu",
        displayName = "Gemma 2 2B CPU",
        fileName = "gemma2-2b-it-cpu-int8.bin",
        kaggleUrl = "https://www.kaggle.com/models/google/gemma-2/tfLite/gemma2-2b-it-cpu-int8/1",
        sizeMb = 2400,
        description = "Works on all devices.",
    ),
    ModelInfo(
        id = "phi3_mini",
        displayName = "Phi-3 Mini",
        fileName = "phi-3-mini-4k-instruct.bin",
        kaggleUrl = "https://www.kaggle.com/models/microsoft/phi-3/tfLite/phi-3-mini-4k-instruct/1",
        sizeMb = 1800,
        description = "Lower RAM usage.",
    ),
)

fun moveModelFromDownloads(context: Context, model: ModelInfo): Boolean {
    return try {
        // Check for the file or a .tar.gz archive in Downloads
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val destDir = File(context.filesDir, "models").also { it.mkdirs() }

        // Try direct .bin file first
        val binFile = File(downloadsDir, model.fileName)
        if (binFile.exists() && binFile.length() > 1_000_000) {
            binFile.copyTo(File(destDir, model.fileName), overwrite = true)
            binFile.delete()
            return true
        }

        // Try .tar.gz (Kaggle wraps in archive)
        val tarFile = downloadsDir.listFiles()?.firstOrNull {
            it.name.contains(model.id.replace("_", "-"), ignoreCase = true) ||
            it.name.contains("gemma", ignoreCase = true) ||
            it.name.contains("phi", ignoreCase = true)
        }
        if (tarFile != null && tarFile.length() > 1_000_000) {
            tarFile.copyTo(File(destDir, model.fileName), overwrite = true)
            tarFile.delete()
            return true
        }
        false
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val modelStatus = remember { mutableStateMapOf<String, String>() }

    fun checkAllModels() {
        MODELS.forEach { model ->
            val destFile = File(context.filesDir, "models/${model.fileName}")
            when {
                destFile.exists() && destFile.length() > 1_000_000 -> modelStatus[model.id] = "READY"
                moveModelFromDownloads(context, model) -> modelStatus[model.id] = "READY"
                else -> if (modelStatus[model.id] != "READY") modelStatus[model.id] = "NOT_DOWNLOADED"
            }
        }
    }

    // Check on resume so status updates after returning from browser
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) checkAllModels()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { checkAllModels() }

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
                Surface(color = ClawGreen.copy(alpha = 0.08f), shape = RoundedCornerShape(6.dp)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("HOW TO INSTALL A MODEL", color = ClawGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("1. Tap DOWNLOAD → opens Kaggle in browser", color = TextMuted, fontSize = 12.sp)
                        Text("2. Sign in with Google (free)", color = TextMuted, fontSize = 12.sp)
                        Text("3. Tap the download button on Kaggle", color = TextMuted, fontSize = 12.sp)
                        Text("4. Come back here — status auto-updates to ✓ READY", color = TextMuted, fontSize = 12.sp)
                    }
                }
                HorizontalDivider(color = BorderDark)

                MODELS.forEach { model ->
                    val status = modelStatus[model.id] ?: "CHECKING"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(model.displayName, color = TextPrimary, fontSize = 13.sp)
                            Text(
                                "${model.description} · ~${model.sizeMb}MB",
                                color = TextMuted,
                                fontSize = 10.sp,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        when (status) {
                            "READY" -> Text(
                                "✓ READY",
                                color = ClawGreen,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                            "CHECKING" -> CircularProgressIndicator(
                                color = ClawGreen,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                            else -> OutlinedButton(
                                onClick = {
                                    modelStatus[model.id] = "WAITING"
                                    try {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(model.kaggleUrl))
                                        )
                                    } catch (e: Exception) { e.printStackTrace() }
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, ClawGreen.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    if (status == "WAITING") "WAITING..." else "DOWNLOAD",
                                    color = if (status == "WAITING") ClawWarn else ClawGreen,
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
echo "Run: git add -A && git commit -m 'fix model download flow - kaggle browser + auto-detect' && git push"

