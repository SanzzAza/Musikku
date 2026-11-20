package com.vyora.music.ui.theme

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vyora.music.R
import com.vyora.music.data.local.AppFontStyle
import com.vyora.music.data.local.ThemeMode
import com.vyora.music.data.local.UiPreferences
import com.vyora.music.data.local.UiStyle
import com.vyora.music.ui.player.CoverPalette
import dev.chrisbanes.haze.HazeState

// Warna aksen merek khas VYORA (Royal Violet berkilau).
val Accent = Color(0xFF9D6BFF)
val AccentSecondary = Color(0xFFC9A0FF)

// Token warna (WarmInk, WarmSurface, ...) ada di Tokens.kt dan mengikuti tema aktif.

val LocalUiPrefs = staticCompositionLocalOf { UiPreferences() }
val LocalCoverPalette = staticCompositionLocalOf { CoverPalette.Default }
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }
val LocalBottomOverlayPadding = staticCompositionLocalOf<Dp> { 104.dp }

val PlusJakartaFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold),
)

val PlayfairFamily = FontFamily(
    Font(R.font.playfair_display, FontWeight.Normal),
    Font(R.font.playfair_display, FontWeight.Medium),
    Font(R.font.playfair_display, FontWeight.SemiBold),
    Font(R.font.playfair_display, FontWeight.Bold),
    Font(R.font.playfair_display_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.playfair_display_italic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.playfair_display_italic, FontWeight.Bold, FontStyle.Italic),
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono, FontWeight.Bold),
)

private fun buildTypography(fontStyle: AppFontStyle): Typography {
    val headingFamily = when (fontStyle) {
        AppFontStyle.MODERN -> PlusJakartaFamily
        AppFontStyle.EDITORIAL -> PlayfairFamily
        AppFontStyle.CLASSIC_SERIF -> PlayfairFamily
        AppFontStyle.MONO_CLEAN -> JetBrainsMonoFamily
    }
    val bodyFamily = when (fontStyle) {
        AppFontStyle.CLASSIC_SERIF -> PlayfairFamily
        AppFontStyle.MONO_CLEAN -> JetBrainsMonoFamily
        else -> PlusJakartaFamily
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
private fun animatedWarm(target: WarmPalette): WarmPalette {
    val spec = tween<Color>(Motion.SLOW)
    return WarmPalette(
        background = animateColorAsState(target.background, spec, label = "w-bg").value,
        backgroundAlt = animateColorAsState(target.backgroundAlt, spec, label = "w-bg-alt").value,
        surface = animateColorAsState(target.surface, spec, label = "w-surface").value,
        surfaceElevated = animateColorAsState(target.surfaceElevated, spec, label = "w-surface-el").value,
        surfaceContainer = animateColorAsState(target.surfaceContainer, spec, label = "w-container").value,
        surfaceContainerHigh = animateColorAsState(target.surfaceContainerHigh, spec, label = "w-container-hi").value,
        ink = animateColorAsState(target.ink, spec, label = "w-ink").value,
        muted = animateColorAsState(target.muted, spec, label = "w-muted").value,
        faint = animateColorAsState(target.faint, spec, label = "w-faint").value,
        hairline = animateColorAsState(target.hairline, spec, label = "w-hairline").value,
        hairlineSoft = animateColorAsState(target.hairlineSoft, spec, label = "w-hairline-soft").value,
        glassTint = animateColorAsState(target.glassTint, spec, label = "w-glass").value,
        glassFallback = animateColorAsState(target.glassFallback, spec, label = "w-glass-fb").value,
        isLight = target.isLight,
    )
}

/**
 * Ikon status bar & navigation bar harus ikut tema: putih di latar gelap,
 * gelap di latar terang. Sebelumnya selalu dipaksa terang (putih).
 */
@Composable
private fun SystemBarsForLight(isLight: Boolean) {
    val context = LocalContext.current
    LaunchedEffect(isLight) {
        var ctx: Context? = context
        while (ctx is ContextWrapper && ctx !is ComponentActivity) ctx = ctx.baseContext
        val activity = ctx as? ComponentActivity ?: return@LaunchedEffect
        val transparent = android.graphics.Color.TRANSPARENT
        val style = if (isLight) {
            SystemBarStyle.light(transparent, transparent)
        } else {
            SystemBarStyle.dark(transparent)
        }
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}

/** Menentukan palet sesuai pilihan pengguna (Otomatis / Terang / Gelap / Hitam pekat). */
fun resolvePalette(mode: ThemeMode, systemInDark: Boolean): WarmPalette = when (mode) {
    ThemeMode.LIGHT -> WarmPalette.Light
    ThemeMode.DARK -> WarmPalette.Dark
    ThemeMode.BLACK -> WarmPalette.PureBlack
    ThemeMode.AUTO -> if (systemInDark) WarmPalette.Dark else WarmPalette.Light
}

@Composable
fun VyoraTheme(
    prefs: UiPreferences = UiPreferences(),
    palette: CoverPalette = CoverPalette.Default,
    content: @Composable () -> Unit,
) {
    val target = resolvePalette(prefs.themeMode, isSystemInDarkTheme())
    val warm = animatedWarm(target)

    // Palet aktif disebar ke seluruh layar lewat WarmTheme (lihat Tokens.kt).
    // SideEffect dipakai supaya penulisan state terjadi setelah komposisi stabil.
    SideEffect { WarmTheme.apply(warm) }

    val isLight = warm.isLight
    SystemBarsForLight(isLight)
    val activePalette = if (prefs.dynamicCoverTheme) palette else CoverPalette.Default
    val animatedPrimary by animateColorAsState(activePalette.accent, tween(600), label = "primary")
    val animatedSecondary by animateColorAsState(activePalette.secondaryAccent, tween(600), label = "secondary")
    val animatedAmbient by animateColorAsState(activePalette.ambient, tween(650), label = "ambient")
    val animatedBg by animateColorAsState(
        if (prefs.style == UiStyle.LIQUID_GLASS) warm.background else warm.backgroundAlt,
        tween(450),
        label = "bg",
    )
    val onAccent = if (isLight) Color.White else Color(0xFF140826)

    val colorScheme = if (isLight) {
        lightColorScheme(
            primary = animatedPrimary,
            onPrimary = onAccent,
            primaryContainer = animatedPrimary.copy(alpha = 0.16f),
            onPrimaryContainer = warm.ink,
            secondary = animatedSecondary,
            onSecondary = onAccent,
            secondaryContainer = warm.surfaceElevated,
            onSecondaryContainer = warm.ink,
            background = animatedBg,
            onBackground = warm.ink,
            surface = warm.surface,
            onSurface = warm.ink,
            surfaceVariant = warm.surfaceElevated,
            onSurfaceVariant = warm.muted,
            surfaceContainer = warm.surfaceContainer,
            surfaceContainerHigh = warm.surfaceContainerHigh,
            outline = warm.hairline,
            outlineVariant = warm.hairlineSoft,
        )
    } else {
        darkColorScheme(
            primary = animatedPrimary,
            onPrimary = onAccent,
            primaryContainer = animatedPrimary.copy(alpha = 0.22f),
            onPrimaryContainer = warm.ink,
            secondary = animatedSecondary,
            onSecondary = onAccent,
            secondaryContainer = warm.surfaceElevated,
            onSecondaryContainer = warm.ink,
            background = animatedBg,
            onBackground = warm.ink,
            surface = warm.surface,
            onSurface = warm.ink,
            surfaceVariant = warm.surfaceElevated,
            onSurfaceVariant = warm.muted,
            surfaceContainer = warm.surfaceContainer,
            surfaceContainerHigh = warm.surfaceContainerHigh,
            outline = warm.hairline,
            outlineVariant = warm.hairlineSoft,
        )
    }

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
