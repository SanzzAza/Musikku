package com.example.musikku.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Accent = Color(0xFF1ED760)
val Background = Color(0xFF0E0E10)
val Surface = Color(0xFF18181B)
val SurfaceVariant = Color(0xFF27272A)

private val ColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.Black,
    secondary = Color(0xFFB3B3B3),
    background = Background,
    onBackground = Color.White,
    surface = Surface,
    onSurface = Color.White,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = Color(0xFFA1A1AA),
    surfaceContainer = Color(0xFF121214),
    secondaryContainer = SurfaceVariant,
    onSecondaryContainer = Color.White,
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 28.sp),
)

@Composable
fun MusikkuTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ColorScheme, typography = AppTypography, content = content)
}
