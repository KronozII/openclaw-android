package com.openclaw.android.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.openclaw.android.ui.screens.*
import com.openclaw.android.ui.theme.BgDark
import com.openclaw.android.ui.theme.OpenClawTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenClawTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BgDark) {
                    OpenClawNavGraph()
                }
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Chat : Screen("chat")
    object Permissions : Screen("permissions")
    object Sandbox : Screen("sandbox")
    object Settings : Screen("settings")
}

@Composable
fun OpenClawNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Chat.route) {

        composable(Screen.Chat.route) {
            ChatScreen(
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenPermissions = { navController.navigate(Screen.Permissions.route) },
                onOpenSandbox = { navController.navigate(Screen.Sandbox.route) },
            )
        }

        composable(Screen.Permissions.route) {
            PermissionsScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Sandbox.route) {
            SandboxScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
