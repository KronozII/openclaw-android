#!/bin/bash
BASE="app/src/main/java/com/openclaw/android"

rm -f $BASE/network/AllowListInterceptor.kt
rm -f $BASE/ui/MainActivity.kt
echo "Deleted old files"

# ── AllowListInterceptor - using CORRECT original method signatures ────────────
cat > $BASE/network/AllowListInterceptor.kt << 'ALLOWEOF'
package com.openclaw.android.network

import android.util.Log
import com.openclaw.android.permission.PermissionVault
import com.openclaw.android.permission.ScopeType
import com.openclaw.android.storage.db.AuditLogDao
import com.openclaw.android.storage.models.AuditLogEntry
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AllowListInterceptor @Inject constructor(
    private val vault: PermissionVault,
    private val auditLogDao: AuditLogDao,
) : Interceptor {

    private val TAG = "AllowListInterceptor"
    private val permanentAllowList = setOf("localhost", "127.0.0.1")

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host
        val url = request.url.toString()

        if (host in permanentAllowList) return chain.proceed(request)

        val isGranted = runBlocking {
            vault.isGranted(ScopeType.NETWORK, host) ||
            vault.isGranted(ScopeType.NETWORK, "*")
        }

        return if (isGranted) {
            logAccess(host, url, allowed = true)
            chain.proceed(request)
        } else {
            Log.w(TAG, "BLOCKED: $host")
            logAccess(host, url, allowed = false)
            okhttp3.Response.Builder()
                .request(request)
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .code(403)
                .message("Blocked by OpenClaw")
                .body("{}".toResponseBody())
                .build()
        }
    }

    private fun logAccess(host: String, url: String, allowed: Boolean) {
        runBlocking {
            auditLogDao.insert(
                AuditLogEntry(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    agentId = "primary",
                    actionType = "NETWORK_REQUEST",
                    resource = host,
                    scopeTokenId = null,
                    outcome = if (allowed) "ALLOWED" else "BLOCKED",
                    details = url.take(200),
                )
            )
        }
    }
}
ALLOWEOF
echo "✓ AllowListInterceptor.kt ($(wc -l < $BASE/network/AllowListInterceptor.kt) lines)"

# ── MainActivity - pass onBack to PermissionsScreen ──────────────────────────
cat > $BASE/ui/MainActivity.kt << 'MAINEOF'
package com.openclaw.android.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.openclaw.android.ui.theme.ClawGreen
import com.openclaw.android.ui.theme.OpenClawTheme
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
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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
            composable("permissions") {
                PermissionsScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable("settings") { SettingsScreen() }
        }
    }
}
MAINEOF
echo "✓ MainActivity.kt ($(wc -l < $BASE/ui/MainActivity.kt) lines)"

echo ""
echo "Verifying class counts..."
grep -c "class AllowListInterceptor" $BASE/network/AllowListInterceptor.kt
grep -c "class MainActivity" $BASE/ui/MainActivity.kt
echo "Both should be 1"

echo ""
echo "Run: git add -A && git commit -m 'fix interceptor signatures and permissions nav' && git push"

