package com.vyora.music.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsParserAndRomanizerTest {

    @Test
    fun `parseTtml extracts word-synced karaoke spans and timestamps`() {
        val sampleTtml = """
            <tt xmlns="http://www.w3.org/ns/ttml">
              <body>
                <div>
                  <p begin="00:12.500" end="00:15.200">
                    <span begin="00:12.500" end="00:13.100">Kau </span>
                    <span begin="00:13.100" end="00:14.000">dan </span>
                    <span begin="00:14.000" end="00:15.200">aku</span>
                  </p>
                </div>
              </body>
            </tt>
        """.trimIndent()

        val lines = LyricsRepository.parseTtml(sampleTtml)
        assertEquals(1, lines.size)
        val first = lines.first()
        assertEquals(12_500L, first.timeMs)
        assertEquals(15_200L, first.endTimeMs)
        assertEquals("Kau dan aku", first.text)
        assertEquals(3, first.words.size)
        assertEquals("Kau ", first.words[0].text)
        assertEquals(13_100L, first.words[1].startMs)
    }

    @Test
    fun `parseLrc parses both classic LRC and enhanced word-synced LRC`() {
        val sampleLrc = """
            [00:05.00]Baris biasa pertama
            [00:10.00]<00:10.00>Menyanyi <00:10.80>bersama <00:11.60>VYORA
        """.trimIndent()

        val lines = LyricsRepository.parseLrc(sampleLrc)
        assertEquals(2, lines.size)
        assertEquals("Baris biasa pertama", lines[0].text)
        assertTrue(lines[0].words.isEmpty())

        assertEquals("Menyanyi bersama VYORA", lines[1].text)
        assertEquals(3, lines[1].words.size)
        assertEquals(10_800L, lines[1].words[1].startMs)
    }

    @Test
    fun `LyricsRomanizer romanizes Hangul and Kana while skipping Latin`() {
        assertNull(LyricsRomanizer.romanizeLine("Cinta Luar Biasa"))

        val hangulRomaji = LyricsRomanizer.romanizeLine("사랑해")
        assertNotNull(hangulRomaji)
        assertEquals("saranghae", hangulRomaji)

        val kanaRomaji = LyricsRomanizer.romanizeLine("ありがとう")
        assertNotNull(kanaRomaji)
        assertEquals("arigatou", kanaRomaji)
    }
}
