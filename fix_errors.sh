#!/bin/bash
BASE="app/src/main/java/com/openclaw/android"

# ── FIX 1: DreamEngine ────────────────────────────────────────────────────────
cat > $BASE/agent/dream/DreamEngine.kt << 'DREAMENEOF'
package com.openclaw.android.agent.dream

import android.content.Context
import android.util.Log
import com.openclaw.android.memory.LongTermMemory
import com.openclaw.android.skill.SkillEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DreamEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val memory: LongTermMemory,
    private val skillEngine: SkillEngine,
) {
    private val TAG = "DreamEngine"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var dreaming = false

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive

    private val _lastInsight = MutableStateFlow("")
    val lastInsight: StateFlow<String> = _lastInsight

    fun startDreaming() {
        dreaming = true
        _isActive.value = true
        scope.launch {
            while (dreaming) {
                try {
                    dream()
                    delay(30_000)
                } catch (e: Exception) {
                    Log.e(TAG, "Dream error", e)
                    delay(60_000)
                }
            }
        }
        Log.i(TAG, "Dream Engine started")
    }

    fun stopDreaming() {
        dreaming = false
        _isActive.value = false
    }

    private fun dream() {
        val memories = memory.getRecentMemories(20)
        if (memories.isEmpty()) return
        val topics = memories
            .flatMap { it.content.split(" ") }
            .filter { it.length > 4 }
            .groupBy { it.lowercase() }
            .entries
            .sortedByDescending { it.value.size }
            .take(5)
            .map { it.key }
        if (topics.isNotEmpty()) {
            val insight = "Recurring themes: ${topics.joinToString(", ")}."
            _lastInsight.value = insight
            Log.i(TAG, "Dream insight: $insight")
        }
    }

    fun getPreloadContext(): String {
        return if (_lastInsight.value.isNotBlank()) "BACKGROUND ANALYSIS: ${_lastInsight.value}" else ""
    }
}
DREAMENEOF
echo "✓ DreamEngine.kt"

# ── FIX 2: LongTermMemory ─────────────────────────────────────────────────────
cat > $BASE/memory/LongTermMemory.kt << 'MEMEOF'
package com.openclaw.android.memory

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class Memory(
    val id: String,
    val content: String,
    val summary: String,
    val timestamp: Long,
    val category: String = "general",
    val importance: Float = 0.5f,
)

@Singleton
class LongTermMemory @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val TAG = "LongTermMemory"
    private val memoryFile = File(context.filesDir, "memory/memories.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _memories = MutableStateFlow<List<Memory>>(emptyList())
    val memories: StateFlow<List<Memory>> = _memories

    init { loadMemories() }

    private fun loadMemories() {
        try {
            memoryFile.parentFile?.mkdirs()
            if (!memoryFile.exists()) return
            val json = JSONArray(memoryFile.readText())
            val loaded = mutableListOf<Memory>()
            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)
                loaded.add(Memory(
                    id = obj.getString("id"),
                    content = obj.getString("content"),
                    summary = obj.getString("summary"),
                    timestamp = obj.getLong("timestamp"),
                    category = obj.optString("category", "general"),
                    importance = obj.optDouble("importance", 0.5).toFloat(),
                ))
            }
            _memories.value = loaded
            Log.i(TAG, "Loaded ${loaded.size} memories")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading memories", e)
        }
    }

    suspend fun remember(content: String, summary: String, category: String = "general", importance: Float = 0.5f) {
        withContext(Dispatchers.IO) {
            val memory = Memory(
                id = java.util.UUID.randomUUID().toString(),
                content = content,
                summary = summary,
                timestamp = System.currentTimeMillis(),
                category = category,
                importance = importance,
            )
            val current = _memories.value.toMutableList()
            current.add(0, memory)
            if (current.size > 1000) current.removeAt(current.size - 1)
            _memories.value = current
            saveMemories(current)
        }
    }

    fun recall(query: String, limit: Int = 5): List<Memory> {
        val queryWords = query.lowercase().split(" ").toSet()
        return _memories.value.sortedByDescending { memory ->
            val words = (memory.content + " " + memory.summary).lowercase().split(" ").toSet()
            queryWords.intersect(words).size.toFloat() * memory.importance
        }.take(limit)
    }

    fun getRecentMemories(limit: Int = 10) = _memories.value.take(limit)

    fun buildMemoryContext(query: String): String {
        val relevant = recall(query, 5)
        if (relevant.isEmpty()) return ""
        return "RELEVANT MEMORIES:\n" + relevant.joinToString("\n") { "- ${it.summary}" }
    }

    fun deleteMemory(id: String) {
        val current = _memories.value.toMutableList()
        current.removeAll { it.id == id }
        _memories.value = current
        scope.launch { saveMemories(current) }
    }

    fun clearAll() {
        _memories.value = emptyList()
        memoryFile.delete()
    }

    private fun saveMemories(memories: List<Memory>) {
        try {
            val json = JSONArray()
            memories.forEach { m ->
                json.put(JSONObject().apply {
                    put("id", m.id)
                    put("content", m.content)
                    put("summary", m.summary)
                    put("timestamp", m.timestamp)
                    put("category", m.category)
                    put("importance", m.importance)
                })
            }
            memoryFile.parentFile?.mkdirs()
            memoryFile.writeText(json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error saving memories", e)
        }
    }
}
MEMEOF
echo "✓ LongTermMemory.kt"

# ── FIX 3: AllowListInterceptor ───────────────────────────────────────────────
cat > $BASE/network/AllowListInterceptor.kt << 'ALLOWEOF'
package com.openclaw.android.network

import android.util.Log
import com.openclaw.android.permission.PermissionVault
import com.openclaw.android.permission.ScopeType
import com.openclaw.android.storage.db.AuditLogDao
import com.openclaw.android.storage.models.AuditLogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AllowListInterceptor @Inject constructor(
    private val vault: PermissionVault,
    private val auditLogDao: AuditLogDao,
) : Interceptor {

    private val TAG = "AllowListInterceptor"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val permanentAllowList = setOf("localhost", "127.0.0.1")

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val host = request.url.host

        if (host in permanentAllowList) return chain.proceed(request)

        val isGranted = runBlocking {
            vault.hasActiveScope(ScopeType.NETWORK, host)
        }

        return if (isGranted) {
            logAccess(host, url, allowed = true)
            val response = chain.proceed(request)
            inspectResponseForExfiltration(request, response)
        } else {
            Log.w(TAG, "BLOCKED unauthorized request to: $host")
            logAccess(host, url, allowed = false)
            scope.launch {
                vault.requireScope(
                    scopeType = ScopeType.NETWORK,
                    resource = host,
                    reason = "Network request intercepted",
                    agentContext = "AllowListInterceptor",
                )
            }
            okhttp3.Response.Builder()
                .request(request)
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .code(403)
                .message("Blocked by OpenClaw AllowList")
                .body("Blocked".toResponseBody())
                .build()
        }
    }

    private fun inspectResponseForExfiltration(request: okhttp3.Request, response: Response): Response {
        val contentLength = response.body?.contentLength() ?: 0
        if (contentLength > 50 * 1024) {
            Log.w(TAG, "LARGE response from ${request.url.host}: $contentLength bytes")
            scope.launch {
                auditLogDao.insert(AuditLogEntry(
                    agentId = "AllowListInterceptor",
                    actionType = "EXFILTRATION_RISK",
                    resource = request.url.host,
                    outcome = "FLAGGED",
                    threatLevel = "HIGH",
                    details = "Large outbound payload: $contentLength bytes",
                ))
            }
        }
        return response
    }

    private fun logAccess(host: String, url: String, allowed: Boolean) {
        scope.launch {
            auditLogDao.insert(AuditLogEntry(
                agentId = "AllowListInterceptor",
                actionType = "NETWORK_REQUEST",
                resource = host,
                outcome = if (allowed) "ALLOWED" else "BLOCKED",
                threatLevel = "NONE",
                details = url,
            ))
        }
    }
}
ALLOWEOF
echo "✓ AllowListInterceptor.kt"

# ── FIX 4: MainActivity - wire ChatScreen nav callbacks ──────────────────────
cat > $BASE/ui/MainActivity.kt << 'MAINEOF'
package com.openclaw.android.ui

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
import com.openclaw.android.ui.screens.ChatScreen
import com.openclaw.android.ui.screens.MemoryScreen
import com.openclaw.android.ui.screens.PermissionsScreen
import com.openclaw.android.ui.screens.SettingsScreen
import com.openclaw.android.ui.screens.SkillsScreen
import com.openclaw.android.ui.theme.OpenClawTheme
import com.openclaw.android.ui.theme.ClawGreen
import com.openclaw.android.ui.theme.TextMuted
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { OpenClawTheme { OpenClawApp() } }
    }
}

@Composable
fun OpenClawApp() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val navItems = listOf(
        Triple("chat", "💬", "Chat"),
        Triple("skills", "⚡", "Skills"),
        Triple("memory", "🧠", "Memory"),
        Triple("permissions", "🔒", "Perms"),
        Triple("settings", "⚙️", "Settings"),
    )

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0F0F1A)) {
                navItems.forEach { (route, icon, label) ->
                    NavigationBarItem(
                        selected = currentRoute == route,
                        onClick = { navController.navigate(route) { launchSingleTop = true } },
                        icon = { Text(icon, fontSize = 18.sp) },
                        label = { Text(label, fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
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
            composable("skills") { SkillsScreen() }
            composable("memory") { MemoryScreen() }
            composable("permissions") { PermissionsScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}
MAINEOF
echo "✓ MainActivity.kt"

echo ""
echo "All fixes applied. Now run:"
echo "  git add -A"
echo "  git commit -m 'fix compilation errors'"
echo "  git push"
