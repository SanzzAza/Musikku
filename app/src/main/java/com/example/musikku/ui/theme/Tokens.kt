package com.example.musikku.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Token desain Musikku.
 *
 * Sebelumnya warna ditulis sebagai `val` tetap (hanya mode gelap), sehingga
 * mode terang dan hitam pekat mustahil dibuat. Sekarang semua warna berasal
 * dari satu [WarmPalette] yang aktif.
 *
 * Kenapa memakai holder `mutableStateOf` di luar composition (bukan
 * `CompositionLocal`)? Karena warna-warna ini dipakai di 400+ tempat, termasuk
 * di dalam `remember { }`, `Modifier.composed`, lambda `onClick`, dan fungsi
 * biasa — semua konteks yang tidak boleh membaca `@Composable` getter.
 * Membaca snapshot state tetap dilacak Compose, jadi UI otomatis
 * menggambar ulang saat tema berganti.
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
        /** Mode gelap bawaan: obsidian hangat + terakota. */
        val Dark = WarmPalette(
            background = Color(0xFF0E0D0C),
            backgroundAlt = Color(0xFF131110),
            surface = Color(0xFF1B1816),
            surfaceElevated = Color(0xFF25211E),
            surfaceContainer = Color(0xFF161311),
            surfaceContainerHigh = Color(0xFF211D1A),
            ink = Color(0xFFF5F1EC),
            muted = Color(0xFFB3ABA2),
            faint = Color(0xFF837B73),
            hairline = Color(0x22F5F1EC),
            hairlineSoft = Color(0x16F5F1EC),
            glassTint = Color(0x701B1816),
            glassFallback = Color(0xD9191614),
        )

        /** Mode hitam pekat: latar benar-benar #000 untuk layar OLED. */
        val PureBlack = WarmPalette(
            background = Color(0xFF000000),
            backgroundAlt = Color(0xFF040404),
            surface = Color(0xFF0B0B0C),
            surfaceElevated = Color(0xFF151517),
            surfaceContainer = Color(0xFF08080A),
            surfaceContainerHigh = Color(0xFF121214),
            ink = Color(0xFFF7F6F4),
            muted = Color(0xFFA8A49E),
            faint = Color(0xFF78746E),
            hairline = Color(0x26FFFFFF),
            hairlineSoft = Color(0x14FFFFFF),
            glassTint = Color(0x70000000),
            glassFallback = Color(0xE0050505),
        )

        /** Mode terang: kertas hangat, bukan putih dingin. */
        val Light = WarmPalette(
            background = Color(0xFFFAF7F2),
            backgroundAlt = Color(0xFFF4EFE8),
            surface = Color(0xFFFFFFFF),
            surfaceElevated = Color(0xFFF2ECE4),
            surfaceContainer = Color(0xFFF6F1EB),
            surfaceContainerHigh = Color(0xFFEAE3DA),
            ink = Color(0xFF181310),
            muted = Color(0xFF5D554D),
            faint = Color(0xFF8B8378),
            hairline = Color(0x1F181310),
            hairlineSoft = Color(0x12181310),
            glassTint = Color(0x70FFFFFF),
            glassFallback = Color(0xE6F6F1EA),
            isLight = true,
        )
    }
}

/** Penyimpan palet aktif. Ditulis oleh `MusikkuTheme`, dibaca semua layar. */
object WarmTheme {
    internal val state = mutableStateOf(WarmPalette.Dark)
    val palette: WarmPalette get() = state.value
    val isLight: Boolean get() = state.value.isLight

    fun apply(next: WarmPalette) {
        if (state.value != next) state.value = next
    }
}

// --- Nama token lama dipertahankan supaya seluruh layar tidak perlu diubah ---

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

/**
 * Warna lapisan tipis di atas permukaan (kartu, chip, tombol sekunder).
 * Sebelumnya selalu putih transparan — di mode terang itu jadi tak terlihat,
 * jadi sekarang otomatis jadi hitam tipis.
 */
fun warmWash(alpha: Float): Color =
    if (WarmTheme.isLight) Color.Black.copy(alpha = alpha * 0.5f) else Color.White.copy(alpha = alpha)

/** Skala jarak (dp) supaya padding tidak lagi angka acak. */
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

/** Durasi animasi standar (ms) supaya gerak terasa satu keluarga. */
object Motion {
    const val FAST = 160
    const val NORMAL = 240
    const val SLOW = 420
}

/** Lebar konten maksimum agar tablet & layar lebar tidak melar. */
val ContentMaxWidth = 720.dp
