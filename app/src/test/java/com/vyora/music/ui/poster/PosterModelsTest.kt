package com.vyora.music.ui.poster

import androidx.compose.ui.graphics.Color
import com.vyora.music.data.ytmusic.ArtistRef
import com.vyora.music.data.ytmusic.SongItem
import com.vyora.music.ui.player.CoverPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PosterModelsTest {

    private fun fakeSong(id: String, title: String, sec: Int = 217) = SongItem(
        id = id,
        title = title,
        artists = listOf(ArtistRef(null, "Artis $id")),
        album = null,
        durationSec = sec,
        thumbnail = null,
    )

    @Test
    fun aspectRatiosProduceExpectedHighResDimensions() {
        assertEquals(1080, PosterAspect.POSTER_4_5.widthPx)
        assertEquals(1350, PosterAspect.POSTER_4_5.heightPx)
        assertEquals(1080, PosterAspect.STORY_9_16.widthPx)
        assertEquals(1920, PosterAspect.STORY_9_16.heightPx)
        assertEquals(1080, PosterAspect.SQUARE_1_1.widthPx)
        assertEquals(1080, PosterAspect.SQUARE_1_1.heightPx)
    }

    @Test
    fun autoTextColorUsesDarkInkOnEditorialPaperAndIvoryOnDarkBackgrounds() {
        val palette = CoverPalette.Default
        val paperColor = PosterConfig(background = PosterBackground.EDITORIAL_PAPER).resolvePrimaryTextColor(palette)
        val glassColor = PosterConfig(background = PosterBackground.LIQUID_GLASS).resolvePrimaryTextColor(palette)
        assertEquals(Color(0xFF1A1613), paperColor)
        assertEquals(Color(0xFFF6F1EA), glassColor)
    }

    @Test
    fun swatchesAlwaysProduceFiveHarmoniousColors() {
        val paperSwatches = PosterPaletteSwatches.buildSwatches(CoverPalette.Default, isLightPaper = true)
        val darkSwatches = PosterPaletteSwatches.buildSwatches(CoverPalette.Default, isLightPaper = false)
        assertEquals(5, paperSwatches.size)
        assertEquals(5, darkSwatches.size)
        assertTrue(paperSwatches[1] == CoverPalette.Default.accent)
    }

    @Test
    fun collagePresetsGenerateExpectedCardCountsAndTimestamps() {
        val songs = (1..5).map { fakeSong("s$it", "Lagu $it") }
        val primary = songs.first()

        val single = GlassCardSlot.buildForPreset(CardCollagePreset.SINGLE, songs, primary, 0.33f, setOf("s1"))
        val trio = GlassCardSlot.buildForPreset(CardCollagePreset.TRIO, songs, primary, 0.33f, setOf("s1"))
        val five = GlassCardSlot.buildForPreset(CardCollagePreset.FIVE_COLLAGE, songs, primary, 0.33f, setOf("s1"))

        assertEquals(1, single.size)
        assertEquals(3, trio.size)
        assertEquals(5, five.size)
        assertEquals(primary.id, five[2].song.id) // kartu utama di tengah
        assertTrue(five[2].isLiked)
        val (elapsed, remain) = five[2].formattedTimestamps()
        assertTrue(elapsed.contains(":"))
        assertTrue(remain.startsWith("-"))
    }
}
