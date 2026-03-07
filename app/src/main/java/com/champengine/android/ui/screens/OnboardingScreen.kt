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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

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
        var errorMsg = "unknown"
        val ok = try {
            withContext(Dispatchers.IO) { client.ping() }
        } catch (e: java.net.UnknownHostException) {
            errorMsg = "DNS_FAIL: " + (e.message ?: "null")
            false
        } catch (e: java.net.ConnectException) {
            errorMsg = "CONNECT_FAIL: " + (e.message ?: "null")
            false
        } catch (e: javax.net.ssl.SSLException) {
            errorMsg = "SSL_FAIL: " + (e.message ?: "null")
            false
        } catch (e: java.io.IOException) {
            errorMsg = "IO_FAIL: " + (e.message ?: "null")
            false
        } catch (e: Exception) {
            errorMsg = e.javaClass.name + ": " + (e.message ?: "null") + " cause=" + (e.cause?.message ?: "null")
            false
        }
        isChecking = false
        if (ok) {
            isConnected = true
            status = "Connected ✓"
            delay(1000)
            onComplete()
        } else {
            status = "Failed: ${client.lastPingError} | ${client.getEndpoint()}"
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
                            val ok = withContext(Dispatchers.IO) { client.ping() }
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
