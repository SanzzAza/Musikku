package com.vyora.music.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Token desain VYORA.
 *
 * Semua permukaan dan warna teks mengikuti satu [WarmPalette] yang aktif
 * (Gelap Obsidian-Violet, Hitam Pekat OLED, atau Terang Mutiara).
 */
data class WarmPalette(
    val background: Color,
    val backgroundAlt: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val hairline: Color,
    val hairlineSoft: Color,
    val glassTint: Color,
    val glassFallback: Color,
    val isLight: Boolean = false,
) {
    companion object {
        /** Mode gelap bawaan: obsidian malam bernuansa royal violet khas VYORA. */
        val Dark = WarmPalette(
            background = Color(0xFF0D0B14),
            backgroundAlt = Color(0xFF120F1C),
            surface = Color(0xFF181424),
            surfaceElevated = Color(0xFF221D33),
            surfaceContainer = Color(0xFF14111F),
            surfaceContainerHigh = Color(0xFF1E192E),
            ink = Color(0xFFF5F3FA),
            muted = Color(0xFFB3ADC4),
            faint = Color(0xFF7F7894),
            hairline = Color(0x22F5F3FA),
            hairlineSoft = Color(0x16F5F3FA),
            glassTint = Color(0x70181424),
            glassFallback = Color(0xD9161221),
        )

        /** Mode hitam pekat: latar benar-benar #000 untuk layar OLED. */
        val PureBlack = WarmPalette(
            background = Color(0xFF000000),
            backgroundAlt = Color(0xFF050408),
            surface = Color(0xFF0C0A12),
            surfaceElevated = Color(0xFF161320),
            surfaceContainer = Color(0xFF09080E),
            surfaceContainerHigh = Color(0xFF13101B),
            ink = Color(0xFFF7F6FA),
            muted = Color(0xFFA9A4B8),
            faint = Color(0xFF78738A),
            hairline = Color(0x26FFFFFF),
            hairlineSoft = Color(0x14FFFFFF),
            glassTint = Color(0x70000000),
            glassFallback = Color(0xE0050408),
        )

        /** Mode terang: kertas mutiara lembut dengan sedikit sentuhan lavender. */
        val Light = WarmPalette(
            background = Color(0xFFFAF8FE),
            backgroundAlt = Color(0xFFF3EFFA),
            surface = Color(0xFFFFFFFF),
            surfaceElevated = Color(0xFFEEE8F8),
            surfaceContainer = Color(0xFFF5F1FC),
            surfaceContainerHigh = Color(0xFFE6DFF4),
            ink = Color(0xFF151022),
            muted = Color(0xFF5A526E),
            faint = Color(0xFF877F9C),
            hairline = Color(0x1F151022),
            hairlineSoft = Color(0x12151022),
            glassTint = Color(0x70FFFFFF),
            glassFallback = Color(0xE6F5F1FC),
            isLight = true,
        )
    }
}

/** Penyimpan palet aktif. Ditulis oleh `VyoraTheme`, dibaca semua layar. */
object WarmTheme {
    internal val state = mutableStateOf(WarmPalette.Dark)
    val palette: WarmPalette get() = state.value
    val isLight: Boolean get() = state.value.isLight

    fun apply(next: WarmPalette) {
        if (state.value != next) state.value = next
    }
}

// --- Nama token dipertahankan supaya seluruh layar konsisten ---

val ObsidianBackground: Color get() = WarmTheme.palette.background
val EditorialBackground: Color get() = WarmTheme.palette.backgroundAlt
val WarmSurface: Color get() = WarmTheme.palette.surface
val WarmSurfaceElevated: Color get() = WarmTheme.palette.surfaceElevated
val WarmInk: Color get() = WarmTheme.palette.ink
val WarmMuted: Color get() = WarmTheme.palette.muted
val WarmFaint: Color get() = WarmTheme.palette.faint
val WarmHairline: Color get() = WarmTheme.palette.hairline
val WarmHairlineSoft: Color get() = WarmTheme.palette.hairlineSoft
val GlassTintDark: Color get() = WarmTheme.palette.glassTint
val GlassFallbackDark: Color get() = WarmTheme.palette.glassFallback

fun warmWash(alpha: Float): Color =
    if (WarmTheme.isLight) Color.Black.copy(alpha = alpha * 0.5f) else Color.White.copy(alpha = alpha)

/** Skala jarak (dp). */
object Space {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val huge = 32.dp
}

/** Skala sudut. */
object Radius {
    val sm = 10.dp
    val md = 14.dp
    val lg = 18.dp
    val xl = 24.dp
    val pill = 999.dp
}

/** Durasi animasi standar (ms). */
object Motion {
    const val FAST = 160
    const val NORMAL = 240
    const val SLOW = 420
}

/** Lebar konten maksimum agar tablet & layar lebar tidak melar. */
val ContentMaxWidth = 720.dp

@Composable
fun adaptiveSidePadding(base: Dp = 16.dp): Dp {
    val widthDp = LocalConfiguration.current.screenWidthDp
    if (widthDp < 600) return base
    val extra = (widthDp - ContentMaxWidth.value) / 2f
    return base + extra.coerceAtLeast(0f).dp
}
