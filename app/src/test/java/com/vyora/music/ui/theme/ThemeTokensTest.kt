package com.vyora.music.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.vyora.music.data.local.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Uji lapisan token tema.
 *
 * Yang dijaga di sini bukan "warna terlihat cantik", tapi aturan yang bisa
 * diukur: pemetaan mode tema dan kontras teks terhadap latar. Kalau ada yang
 * memasang warna teks terang di latar terang, test ini gagal sebelum dirilis.
 */
class ThemeTokensTest {

    @Test
    fun `mode otomatis mengikuti sistem`() {
        assertTrue(resolvePalette(ThemeMode.AUTO, systemInDark = false).isLight)
        assertEquals(false, resolvePalette(ThemeMode.AUTO, systemInDark = true).isLight)
    }

    @Test
    fun `mode eksplisit mengabaikan sistem`() {
        assertTrue(resolvePalette(ThemeMode.LIGHT, systemInDark = true).isLight)
        assertEquals(false, resolvePalette(ThemeMode.DARK, systemInDark = false).isLight)
        assertEquals(false, resolvePalette(ThemeMode.BLACK, systemInDark = false).isLight)
    }

    @Test
    fun `hitam pekat memakai latar murni hitam untuk OLED`() {
        val palette = resolvePalette(ThemeMode.BLACK, systemInDark = true)
        assertEquals(Color.Black, palette.background)
        assertTrue(palette.ink.luminance() > palette.background.luminance())
    }

    @Test
    fun `teks selalu kontras terhadap latar di semua mode`() {
        ThemeMode.values().forEach { mode ->
            listOf(true, false).forEach { dark ->
                val palette = resolvePalette(mode, dark)
                val inkLum = palette.ink.luminance()
                val bgLum = palette.background.luminance()
                val gap = kotlin.math.abs(inkLum - bgLum)
                assertTrue(
                    "Kontras teks latar terlalu rendah pada $mode (dark=$dark): $gap",
                    gap > 0.45f,
                )
            }
        }
    }

    @Test
    fun `teks sekunder lebih redup dari teks utama`() {
        listOf(WarmPalette.Dark, WarmPalette.Light, WarmPalette.PureBlack).forEach { palette ->
            val inkLum = palette.ink.luminance()
            val mutedLum = palette.muted.luminance()
            val faintLum = palette.faint.luminance()
            val bgLum = palette.background.luminance()
            // Muted & faint harus berada di antara latar dan teks utama.
            val inkToBg = if (inkLum > bgLum) 1f else -1f
            assertTrue(
                "Muted harus lebih dekat ke latar daripada ink",
                (mutedLum - inkLum) * inkToBg < 0f,
            )
            assertTrue(
                "Faint harus lebih redup dari muted",
                (faintLum - mutedLum) * inkToBg < 0f,
            )
        }
    }
}
