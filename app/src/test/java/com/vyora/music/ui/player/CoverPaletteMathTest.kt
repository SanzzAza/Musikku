package com.vyora.music.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverPaletteMathTest {

    @Test
    fun grayscaleCoverFallsBackToSignatureVyoraVioletAccent() {
        val palette = CoverPaletteMath.buildPalette(
            avgRgb = Triple(128, 128, 128),
            vibrantRgb = Triple(140, 140, 140),
        )
        assertEquals(CoverPalette.Default.accent, palette.accent)
        assertEquals(CoverPalette.Default.secondaryAccent, palette.secondaryAccent)
    }

    @Test
    fun vibrantCoverProducesLegibleDarkAmbientAndBrightAccent() {
        val palette = CoverPaletteMath.buildPalette(
            avgRgb = Triple(220, 40, 60),
            vibrantRgb = Triple(240, 50, 70),
        )
        val (_, _, ambientL) = CoverPaletteMath.rgbToHsl(
            (palette.ambient.red * 255).toInt(),
            (palette.ambient.green * 255).toInt(),
            (palette.ambient.blue * 255).toInt(),
        )
        val (_, accentS, accentL) = CoverPaletteMath.rgbToHsl(
            (palette.accent.red * 255).toInt(),
            (palette.accent.green * 255).toInt(),
            (palette.accent.blue * 255).toInt(),
        )
        assertTrue("Ambient should stay dark for legible white ink: $ambientL", ambientL in 0.14f..0.32f)
        assertTrue("Accent should be bright enough on obsidian: $accentL", accentL in 0.52f..0.72f)
        assertTrue("Accent should retain rich saturation: $accentS", accentS >= 0.40f)
    }
}
