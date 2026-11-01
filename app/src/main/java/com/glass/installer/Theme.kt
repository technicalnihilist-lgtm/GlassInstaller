package com.glass.installer

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Design tokens. One place for every colour and text style used in the app. */
object Pal {
    val bg = Color(0xFF06070E)
    val ink = Color(0xFFF4F6FF)
    val mint = Color(0xFF34D399)
    val mintDeep = Color(0xFF059669)
    val amber = Color(0xFFFBBF24)
    val rose = Color(0xFFFB7185)
    val roseDeep = Color(0xFFBE123C)
    val violet = Color(0xFF7C5CFF)
    val cyan = Color(0xFF22D3EE)
    val pink = Color(0xFFFF4FA3)
    val blue = Color(0xFF3B82F6)
    val indigo = Color(0xFF6366F1)

    fun risk(r: Int) = when (r) {
        2 -> rose
        1 -> amber
        else -> mint
    }

    fun scoreColor(s: Int) = when {
        s >= 75 -> mint
        s >= 45 -> amber
        else -> rose
    }
}

/** Shared type scale — keeps every screen on the same rhythm. */
object Type {
    val hero = TextStyle(fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)
    val h1 = TextStyle(fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
    val h2 = TextStyle(fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp)
    val h3 = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal)
    val small = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal)
    val micro = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
    val mono = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, fontFamily = FontFamily.Monospace)
    val number = TextStyle(fontSize = 26.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
}

/** Android version name for an API level, or a neutral fallback. */
fun an(n: Int): String = if (n == 0) "pata nahi" else NAMES[n] ?: "API $n"

private val NAMES = mapOf(
    36 to "Android 16", 35 to "Android 15", 34 to "Android 14", 33 to "Android 13",
    32 to "Android 12L", 31 to "Android 12", 30 to "Android 11", 29 to "Android 10",
    28 to "Android 9", 27 to "Android 8.1", 26 to "Android 8.0", 24 to "Android 7.0",
    23 to "Android 6", 21 to "Android 5.0"
)

private val scheme = darkColorScheme(
    primary = Pal.violet,
    onPrimary = Color.White,
    secondary = Pal.cyan,
    background = Pal.bg,
    onBackground = Pal.ink,
    surface = Color(0xFF0C1020),
    onSurface = Pal.ink,
    error = Pal.rose
)

@Composable
fun GlassTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(bodyLarge = Type.body, bodyMedium = Type.body, bodySmall = Type.small),
        content = content
    )
}
