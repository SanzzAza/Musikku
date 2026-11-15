package com.example.musikku.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musikku.data.local.AppFontStyle
import com.example.musikku.data.local.UiPreferences
import com.example.musikku.data.local.UiStyle
import com.example.musikku.ui.player.CoverPalette
import dev.chrisbanes.haze.HazeState

// Palet Obsidian Hangat & Terakota khas Liquid Glass / Editorial
val Accent = Color(0xFFE06D47)
val AccentSecondary = Color(0xFFFFB85C)
val ObsidianBackground = Color(0xFF0E0D0C)
val EditorialBackground = Color(0xFF131110)
val WarmSurface = Color(0xFF1B1816)
val WarmSurfaceElevated = Color(0xFF25211E)
val WarmInk = Color(0xFFF5F1EC)
val WarmMuted = Color(0xFFB3ABA2)
val WarmFaint = Color(0xFF837B73)
val WarmHairline = Color(0x22F5F1EC)
val GlassTintDark = Color(0x701B1816)
val GlassFallbackDark = Color(0xD9191614)

val LocalUiPrefs = staticCompositionLocalOf { UiPreferences() }
val LocalCoverPalette = staticCompositionLocalOf { CoverPalette.Default }
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }
val LocalBottomOverlayPadding = staticCompositionLocalOf<Dp> { 104.dp }

private fun buildTypography(fontStyle: AppFontStyle): Typography {
    val headingFamily = when (fontStyle) {
        AppFontStyle.MODERN -> FontFamily.SansSerif
        AppFontStyle.EDITORIAL -> FontFamily.Serif
        AppFontStyle.CLASSIC_SERIF -> FontFamily.Serif
        AppFontStyle.MONO_CLEAN -> FontFamily.Monospace
    }
    val bodyFamily = when (fontStyle) {
        AppFontStyle.CLASSIC_SERIF -> FontFamily.Serif
        AppFontStyle.MONO_CLEAN -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }
    return Typography(
        displaySmall = TextStyle(
            fontFamily = headingFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 42.sp,
            letterSpacing = (-1).sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = headingFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            letterSpacing = (-0.8).sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = headingFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.5).sp,
        ),
        titleLarge = TextStyle(
            fontFamily = headingFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 21.sp,
            lineHeight = 27.sp,
            letterSpacing = (-0.3).sp,
        ),
        titleMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 15.5.sp,
            lineHeight = 21.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 19.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp,
            lineHeight = 18.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.5.sp,
            lineHeight = 15.sp,
            letterSpacing = 0.4.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.5.sp,
            lineHeight = 14.sp,
            letterSpacing = 1.4.sp,
        ),
    )
}

@Composable
fun MusikkuTheme(
    prefs: UiPreferences = UiPreferences(),
    palette: CoverPalette = CoverPalette.Default,
    content: @Composable () -> Unit,
) {
    val activePalette = if (prefs.dynamicCoverTheme) palette else CoverPalette.Default
    val animatedPrimary by animateColorAsState(activePalette.accent, tween(600), label = "primary")
    val animatedSecondary by animateColorAsState(activePalette.secondaryAccent, tween(600), label = "secondary")
    val animatedAmbient by animateColorAsState(activePalette.ambient, tween(650), label = "ambient")
    val animatedBg by animateColorAsState(
        if (prefs.style == UiStyle.LIQUID_GLASS) ObsidianBackground else EditorialBackground,
        tween(450),
        label = "bg",
    )

    val colorScheme = darkColorScheme(
        primary = animatedPrimary,
        onPrimary = Color(0xFF1E0B04),
        primaryContainer = animatedPrimary.copy(alpha = 0.22f),
        onPrimaryContainer = WarmInk,
        secondary = animatedSecondary,
        onSecondary = Color(0xFF1E0B04),
        secondaryContainer = WarmSurfaceElevated,
        onSecondaryContainer = WarmInk,
        background = animatedBg,
        onBackground = WarmInk,
        surface = WarmSurface,
        onSurface = WarmInk,
        surfaceVariant = WarmSurfaceElevated,
        onSurfaceVariant = WarmMuted,
        surfaceContainer = Color(0xFF161311),
        surfaceContainerHigh = Color(0xFF211D1A),
        outline = WarmHairline,
        outlineVariant = Color(0x16F5F1EC),
    )

    val smoothedPalette = CoverPalette(
        ambient = animatedAmbient,
        accent = animatedPrimary,
        secondaryAccent = animatedSecondary,
    )

    CompositionLocalProvider(
        LocalUiPrefs provides prefs,
        LocalCoverPalette provides smoothedPalette,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildTypography(prefs.fontStyle),
            content = content,
        )
    }
}
