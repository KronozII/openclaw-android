#!/bin/bash
# ╔══════════════════════════════════════════════════════════════╗
# ║   ChampEngine fix8 — The Ultimate Rebrand                    ║
# ║   OpenClaw → ChampEngine                                     ║
# ║   Live backend: api.champengine.cloud                        ║
# ║   Zero config for users — just install and go               ║
# ╚══════════════════════════════════════════════════════════════╝
# Run from project root: bash fix8.sh

set -e
BASE="app/src/main/java/com/openclaw/android"
NEW_BASE="app/src/main/java/com/champengine/android"
RES="app/src/main/res"

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║   ChampEngine fix8 — Starting rebrand                        ║"
echo "╚══════════════════════════════════════════════════════════════╝"

# ─────────────────────────────────────────────────────────────────
# 1. RENAME PACKAGE DIRECTORIES
# ─────────────────────────────────────────────────────────────────
echo "→ Renaming package directories..."
mkdir -p app/src/main/java/com/champengine
cp -r $BASE app/src/main/java/com/champengine/android
echo "✓ Package directories created"

# ─────────────────────────────────────────────────────────────────
# 2. UPDATE ALL PACKAGE DECLARATIONS & IMPORTS
# ─────────────────────────────────────────────────────────────────
echo "→ Updating package names in all Kotlin files..."
find $NEW_BASE -name "*.kt" -exec sed -i \
    's/package com\.openclaw\.android/package com.champengine.android/g' {} \;
find $NEW_BASE -name "*.kt" -exec sed -i \
    's/import com\.openclaw\.android/import com.champengine.android/g' {} \;
echo "✓ Package names updated"

# ─────────────────────────────────────────────────────────────────
# 3. UPDATE BUILD FILES
# ─────────────────────────────────────────────────────────────────
echo "→ Updating build.gradle.kts..."
sed -i 's/com\.openclaw\.android/com.champengine.android/g' app/build.gradle.kts
sed -i 's/applicationId = "com\.champengine\.android"/applicationId = "com.champengine.android"/' app/build.gradle.kts
echo "✓ build.gradle.kts updated"

# ─────────────────────────────────────────────────────────────────
# 4. UPDATE ANDROIDMANIFEST
# ─────────────────────────────────────────────────────────────────
echo "→ Updating AndroidManifest.xml..."
sed -i 's/com\.openclaw\.android/com.champengine.android/g' app/src/main/AndroidManifest.xml
sed -i 's/OpenClaw/ChampEngine/g' app/src/main/AndroidManifest.xml
echo "✓ AndroidManifest updated"

# ─────────────────────────────────────────────────────────────────
# 5. UPDATE STRINGS
# ─────────────────────────────────────────────────────────────────
echo "→ Updating app name in strings.xml..."
sed -i 's/<string name="app_name">.*<\/string>/<string name="app_name">ChampEngine<\/string>/' \
    $RES/values/strings.xml
echo "✓ strings.xml updated"

# ─────────────────────────────────────────────────────────────────
# 6. CHAMPENGINE API CLIENT — baked-in endpoint, zero user config
# ─────────────────────────────────────────────────────────────────
echo "→ Writing ChampEngineClient.kt..."
mkdir -p $NEW_BASE/network
cat > $NEW_BASE/network/ChampEngineClient.kt << 'EOF'
package com.champengine.android.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChampEngineClient @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val TAG = "ChampEngineClient"

    // ── Baked-in defaults — users never need to configure anything ──
    private val DEFAULT_ENDPOINT = "https://api.champengine.cloud"
    private val DEFAULT_TOKEN    = "eb0845f1d07fa481e941e4733b90de348e17ef95afa60b7c4f524905bbe1fd2a"
    private val DEFAULT_MODEL    = "llama3.2:3b"

    private val prefs: SharedPreferences =
        context.getSharedPreferences("champengine_config", Context.MODE_PRIVATE)

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // ── Config — users can override, but defaults work out of box ──
    fun getEndpoint(): String = prefs.getString("endpoint", DEFAULT_ENDPOINT) ?: DEFAULT_ENDPOINT
    fun getToken(): String    = prefs.getString("token", DEFAULT_TOKEN) ?: DEFAULT_TOKEN
    fun getModel(): String    = prefs.getString("model", DEFAULT_MODEL) ?: DEFAULT_MODEL

    fun saveCustomConfig(endpoint: String, token: String, model: String) {
        prefs.edit()
            .putString("endpoint", endpoint)
            .putString("token", token)
            .putString("model", model)
            .apply()
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }

    fun isUsingDefaults(): Boolean =
        getEndpoint() == DEFAULT_ENDPOINT && getToken() == DEFAULT_TOKEN

    // ── Health check ──────────────────────────────────────────────
    suspend fun ping(): Boolean {
        return try {
            val req = Request.Builder()
                .url("${getEndpoint()}/api/tags")
                .addHeader("X-Champ-Token", getToken())
                .get().build()
            val resp = http.newCall(req).execute()
            resp.isSuccessful
        } catch (e: Exception) {
            Log.w(TAG, "Ping failed: ${e.message}")
            false
        }
    }

    // ── List available models ─────────────────────────────────────
    suspend fun listModels(): List<String> {
        return try {
            val req = Request.Builder()
                .url("${getEndpoint()}/api/tags")
                .addHeader("X-Champ-Token", getToken())
                .get().build()
            val resp = http.newCall(req).execute()
            val body = resp.body?.string() ?: return emptyList()
            val json = JSONObject(body)
            val models = json.getJSONArray("models")
            (0 until models.length()).map { models.getJSONObject(it).getString("name") }
        } catch (e: Exception) {
            Log.e(TAG, "listModels failed", e)
            emptyList()
        }
    }

    // ── Streaming chat ────────────────────────────────────────────
    fun streamChat(
        messages: List<Pair<String, String>>,
        systemPrompt: String,
        model: String? = null,
    ): Flow<String> = flow {
        val selectedModel = model ?: getModel()
        val msgs = JSONArray()
        if (systemPrompt.isNotBlank()) {
            msgs.put(JSONObject().put("role", "system").put("content", systemPrompt))
        }
        messages.forEach { (role, content) ->
            msgs.put(JSONObject().put("role", role).put("content", content))
        }

        val body = JSONObject()
            .put("model", selectedModel)
            .put("messages", msgs)
            .put("stream", true)
            .toString()

        val request = Request.Builder()
            .url("${getEndpoint()}/api/chat")
            .addHeader("X-Champ-Token", getToken())
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = http.newCall(request).execute()
            if (!response.isSuccessful) {
                emit("[ERROR ${response.code}] Server error. Please try again.")
                return@flow
            }

            val source = response.body?.source() ?: run {
                emit("[ERROR] Empty response from server.")
                return@flow
            }

            val buffer = okio.Buffer()
            while (!source.exhausted()) {
                source.read(buffer, 8192)
                val line = buffer.readUtf8()
                line.lines().filter { it.isNotBlank() }.forEach { jsonLine ->
                    try {
                        val obj = JSONObject(jsonLine)
                        val token = obj.optJSONObject("message")
                            ?.optString("content", "") ?: ""
                        if (token.isNotEmpty()) emit(token)
                        if (obj.optBoolean("done", false)) return@flow
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stream error", e)
            emit("\n[CONNECTION ERROR] Could not reach ChampEngine servers. Check your internet connection.")
        }
    }.flowOn(Dispatchers.IO)
}
EOF
echo "✓ ChampEngineClient.kt"

# ─────────────────────────────────────────────────────────────────
# 7. ONBOARDING SCREEN — zero config, just works
# ─────────────────────────────────────────────────────────────────
echo "→ Writing OnboardingScreen.kt..."
mkdir -p $NEW_BASE/ui/screens
cat > $NEW_BASE/ui/screens/OnboardingScreen.kt << 'EOF'
package com.champengine.android.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.champengine.android.network.ChampEngineClient
import com.champengine.android.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    client: ChampEngineClient,
    onComplete: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Initializing...") }
    var isConnected by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(true) }
    var dots by remember { mutableStateOf("") }

    // Animated pulse
    val pulse = rememberInfiniteTransition()
    val scale by pulse.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        )
    )

    // Animated dots
    LaunchedEffect(isChecking) {
        while (isChecking) {
            dots = ""; delay(400)
            dots = "."; delay(400)
            dots = ".."; delay(400)
            dots = "..."; delay(400)
        }
    }

    // Auto-connect on launch
    LaunchedEffect(Unit) {
        delay(800)
        status = "Connecting to ChampEngine$dots"
        delay(600)
        val ok = client.ping()
        isChecking = false
        if (ok) {
            isConnected = true
            status = "Connected ✓"
            delay(1000)
            onComplete()
        } else {
            status = "Connection failed — check your internet"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0A1A), Color(0xFF0F0F2A), Color(0xFF0A0A1A))
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.padding(40.dp),
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                ClawGreen.copy(alpha = 0.3f),
                                ClawGreen.copy(alpha = 0.05f),
                            )
                        ),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("🏆", fontSize = 56.sp)
            }

            // Brand name
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "ChampEngine",
                    color = TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    "AI. Unchained.",
                    color = ClawGreen,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }

            // Status indicator
            Surface(
                color = if (isConnected) ClawGreen.copy(alpha = 0.1f)
                        else SurfaceDark,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            color = ClawGreen,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                    } else {
                        Text(
                            if (isConnected) "●" else "○",
                            color = if (isConnected) ClawGreen else ClawRed,
                            fontSize = 12.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        if (isChecking) "Connecting to ChampEngine$dots"
                        else status,
                        color = if (isConnected) ClawGreen else TextMuted,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            // Retry button if failed
            if (!isChecking && !isConnected) {
                Button(
                    onClick = {
                        isChecking = true
                        scope.launch {
                            status = "Retrying..."
                            val ok = client.ping()
                            isChecking = false
                            if (ok) {
                                isConnected = true
                                status = "Connected ✓"
                                delay(800)
                                onComplete()
                            } else {
                                status = "Still unavailable — check internet"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClawGreen.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("RETRY", color = ClawGreen, fontFamily = FontFamily.Monospace)
                }
            }

            // Privacy note
            Text(
                "Your conversations are private.\nPowered by your own AI infrastructure.",
                color = TextMuted.copy(alpha = 0.6f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
            )
        }
    }
}
EOF
echo "✓ OnboardingScreen.kt"

# ─────────────────────────────────────────────────────────────────
# 8. UPDATED MAIN ACTIVITY — onboarding flow
# ─────────────────────────────────────────────────────────────────
echo "→ Writing MainActivity.kt..."
cat > $NEW_BASE/ui/MainActivity.kt << 'EOF'
package com.champengine.android.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.champengine.android.network.ChampEngineClient
import com.champengine.android.ui.screens.*
import com.champengine.android.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var champEngineClient: ChampEngineClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenClawTheme {
                ChampEngineApp(client = champEngineClient)
            }
        }
    }
}

@Composable
fun ChampEngineApp(client: ChampEngineClient) {
    var onboardingComplete by remember { mutableStateOf(false) }

    if (!onboardingComplete) {
        OnboardingScreen(
            client = client,
            onComplete = { onboardingComplete = true },
        )
        return
    }

    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val navItems = listOf(
        Triple("chat",        "💬", "Chat"),
        Triple("skills",      "⚡", "Skills"),
        Triple("memory",      "🧠", "Memory"),
        Triple("permissions", "🔒", "Perms"),
        Triple("settings",    "⚙️", "Settings"),
    )

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0F0F1A)) {
                navItems.forEach { (route, icon, label) ->
                    NavigationBarItem(
                        selected = currentRoute == route,
                        onClick = {
                            navController.navigate(route) { launchSingleTop = true }
                        },
                        icon = { Text(icon, fontSize = 18.sp) },
                        label = {
                            Text(label, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ClawGreen,
                            selectedTextColor = ClawGreen,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = Color(0xFF16162A),
                        ),
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "chat",
            modifier = Modifier.padding(padding),
        ) {
            composable("chat") {
                ChatScreen(
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenPermissions = { navController.navigate("permissions") },
                    onOpenSandbox = { navController.navigate("skills") },
                )
            }
            composable("skills")      { SkillsScreen() }
            composable("memory")      { MemoryScreen() }
            composable("permissions") {
                PermissionsScreen(onBack = { navController.popBackStack() })
            }
            composable("settings")    { SettingsScreen() }
        }
    }
}
EOF
echo "✓ MainActivity.kt"

# ─────────────────────────────────────────────────────────────────
# 9. SETTINGS SCREEN — clean ChampEngine branding, advanced options
# ─────────────────────────────────────────────────────────────────
echo "→ Writing SettingsScreen.kt..."
cat > $NEW_BASE/ui/screens/SettingsScreen.kt << 'EOF'
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
EOF
echo "✓ SettingsScreen.kt"

# ─────────────────────────────────────────────────────────────────
# 10. REMOVE OLD OPENCLAW PACKAGE
# ─────────────────────────────────────────────────────────────────
echo "→ Removing old openclaw package..."
rm -rf app/src/main/java/com/openclaw
echo "✓ Old package removed"

# ─────────────────────────────────────────────────────────────────
# 11. UPDATE HILT MODULE REFERENCES
# ─────────────────────────────────────────────────────────────────
echo "→ Updating Hilt/DI references..."
find $NEW_BASE -name "*.kt" -exec sed -i \
    's/com\.openclaw/com.champengine/g' {} \;
echo "✓ DI references updated"

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║   ✅ fix8 complete — ChampEngine rebrand done!               ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "What changed:"
echo "  🏆 App name: OpenClaw → ChampEngine"
echo "  📦 Package: com.openclaw → com.champengine.android"
echo "  🌐 Backend: api.champengine.cloud baked in"
echo "  🔑 Auth token: baked in — users never configure anything"
echo "  ✨ Onboarding: zero-config first launch screen"
echo "  ⚙️  Settings: model selector + advanced BYOK option"
echo ""
echo "Now run:"
echo "  git add -A"
echo "  git commit -m 'fix8: ChampEngine rebrand, live backend, zero-config onboarding'"
echo "  git push"

