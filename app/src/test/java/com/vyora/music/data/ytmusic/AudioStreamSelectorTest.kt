package com.vyora.music.data.ytmusic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioStreamSelectorTest {

    private fun candidate(
        url: String = "https://example.com/a",
        bitrate: Int = 128,
        mime: String? = "audio/webm",
        progressive: Boolean = true,
    ) = AudioCandidate(url, bitrate, mime, progressive)

    @Test
    fun `tidak ada kandidat menghasilkan null`() {
        assertNull(AudioStreamSelector.pick(emptyList()))
    }

    @Test
    fun `URL kosong atau bukan http dibuang`() {
        val list = listOf(
            candidate(url = ""),
            candidate(url = "file:///tmp/a.webm"),
            candidate(url = "  "),
        )
        assertNull(AudioStreamSelector.pick(list))
    }

    @Test
    fun `HIGH memilih bitrate tertinggi`() {
        val list = listOf(
            candidate(url = "https://a", bitrate = 64),
            candidate(url = "https://b", bitrate = 256),
            candidate(url = "https://c", bitrate = 128),
        )
        assertEquals("https://b", AudioStreamSelector.pick(list, AudioQuality.HIGH)?.url)
    }

    @Test
    fun `kandidat non-progressive tetap dipertimbangkan`() {
        // Bug lama: hanya stream PROGRESSIVE_HTTP yang dipakai, sehingga kandidat
        // dengan bitrate lebih tinggi dari jalur lain terbuang.
        val list = listOf(
            candidate(url = "https://progressive", bitrate = 128, progressive = true),
            candidate(url = "https://adaptive", bitrate = 256, progressive = false),
        )
        assertEquals(256, AudioStreamSelector.pick(list)?.bitrateKbps ?: -1)
    }

    @Test
    fun `bitrate tidak diketahui tetap bisa dipakai`() {
        val list = listOf(candidate(url = "https://a", bitrate = 0))
        assertEquals("https://a", AudioStreamSelector.pick(list)?.url)
    }

    @Test
    fun `LOW memilih bitrate paling kecil yang diketahui`() {
        val list = listOf(
            candidate(url = "https://a", bitrate = 256),
            candidate(url = "https://b", bitrate = 48),
            candidate(url = "https://c", bitrate = 128),
        )
        assertEquals("https://b", AudioStreamSelector.pick(list, AudioQuality.LOW)?.url)
    }

    @Test
    fun `NORMAL sedekat mungkin 160 kbps`() {
        val list = listOf(
            candidate(url = "https://a", bitrate = 48),
            candidate(url = "https://b", bitrate = 160),
            candidate(url = "https://c", bitrate = 256),
        )
        assertEquals("https://b", AudioStreamSelector.pick(list, AudioQuality.NORMAL)?.url)
    }

    @Test
    fun `codec opus dipilih saat bitrate sama`() {
        val list = listOf(
            candidate(url = "https://mp4", bitrate = 128, mime = "audio/mp4"),
            candidate(url = "https://opus", bitrate = 128, mime = "audio/webm; codecs=\"opus\""),
        )
        assertEquals("https://opus", AudioStreamSelector.pick(list)?.url)
    }
}
