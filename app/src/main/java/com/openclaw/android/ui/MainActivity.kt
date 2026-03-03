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
            composable("skills") { SkillsScreen() }
            composable("memory") { MemoryScreen() }
            composable("permissions") { PermissionsScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}
