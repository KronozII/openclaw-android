package com.champengine.android.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// OpenClaw brand colors
val ClawGreen = Color(0xFF00FFB3)
val ClawGreenDim = Color(0xFF00C88A)
val ClawPurple = Color(0xFF7B5CFF)
val ClawRed = Color(0xFFFF5C8A)
val ClawWarn = Color(0xFFFFAA00)
val ClawDanger = Color(0xFFFF3C5A)

val BgDark = Color(0xFF09090F)
val SurfaceDark = Color(0xFF0F0F1A)
val Surface2Dark = Color(0xFF16162A)
val BorderDark = Color(0xFF2A2A4A)
val TextPrimary = Color(0xFFE8E8F0)
val TextMuted = Color(0xFF7878A0)

private val DarkColorScheme = darkColorScheme(
    primary = ClawGreen,
    onPrimary = Color.Black,
    secondary = ClawPurple,
    onSecondary = Color.White,
    error = ClawDanger,
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = Surface2Dark,
    onSurfaceVariant = TextMuted,
    outline = BorderDark,
)

@Composable
fun OpenClawTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = OpenClawTypography,
        content = content,
    )
}

val OpenClawTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        color = TextPrimary,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        color = TextPrimary,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp,
        color = TextPrimary,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        color = TextPrimary,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        color = TextPrimary,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        color = TextMuted,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        letterSpacing = 2.sp,
        color = TextMuted,
    ),
)
