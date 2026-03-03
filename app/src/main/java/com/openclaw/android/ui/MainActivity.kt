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
import com.openclaw.android.ui.screens.*
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

    Scaffold(bottomBar = {
        NavigationBar(containerColor = Color(0xFF0F0F1A)) {
            navItems.forEach { (route, icon, label) ->
                NavigationBarItem(
                    selected = currentRoute == route,
                    onClick = { navController.navigate(route) { launchSingleTop = true } },
                    icon = { Text(icon, fontSize = 18.sp) },
                    label = { Text(label, fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ClawGreen, selectedTextColor = ClawGreen,
                        unselectedIconColor = TextMuted, unselectedTextColor = TextMuted,
                        indicatorColor = Color(0xFF16162A),
                    ),
                )
            }
        }
    }) { padding ->
        NavHost(navController = navController, startDestination = "chat", modifier = Modifier.padding(padding)) {
            composable("chat") { ChatScreen() }
            composable("skills") { SkillsScreen() }
            composable("memory") { MemoryScreen() }
            composable("permissions") { PermissionsScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}
