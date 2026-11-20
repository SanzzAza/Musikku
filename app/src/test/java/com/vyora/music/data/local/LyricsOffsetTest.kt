package com.vyora.music.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsOffsetTest {

    @Test
    fun `clamp membatasi ke plus minus lima detik`() {
        assertEquals(LyricsOffset.MIN_MS, LyricsOffset.clamp(-99_000L))
        assertEquals(LyricsOffset.MAX_MS, LyricsOffset.clamp(99_000L))
        assertEquals(1_250L, LyricsOffset.clamp(1_250L))
    }

    @Test
    fun `step memajukan seperempat detik`() {
        assertEquals(250L, LyricsOffset.step(0L, LyricsOffset.STEP_MS))
        assertEquals(-250L, LyricsOffset.step(0L, -LyricsOffset.STEP_MS))
        assertEquals(500L, LyricsOffset.step(250L, LyricsOffset.STEP_MS))
    }

    @Test
    fun `step tidak melewati batas`() {
        assertEquals(LyricsOffset.MAX_MS, LyricsOffset.step(LyricsOffset.MAX_MS, LyricsOffset.STEP_MS))
        assertEquals(LyricsOffset.MIN_MS, LyricsOffset.step(LyricsOffset.MIN_MS, -LyricsOffset.STEP_MS))
    }

    @Test
    fun `nilai nol berarti lirik sinkron`() {
        assertEquals("Sinkron", LyricsOffset.format(0L))
    }

    @Test
    fun `format menampilkan tanda positif dan negatif`() {
        assertEquals(true, LyricsOffset.format(1_500L).startsWith("+"))
        assertEquals(true, LyricsOffset.format(-1_500L).startsWith("-"))
    }
}
