package com.example.musikku.data.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Uji parser halaman Spotify & Apple Music memakai contoh HTML yang
 * strukturnya disalin dari halaman asli (dipendekkan).
 *
 * Kalau Spotify/Apple mengubah format halaman, test ini yang gagal lebih dulu —
 * bukan pengguna yang menemukan fitur impor rusak.
 */
class PlaylistImportTest {

    private val spotifyHtml = """
        <!DOCTYPE html><html><head><title>Spotify Embed</title></head><body>
        <script id="__NEXT_DATA__" type="application/json">{"props":{"pageProps":{"state":{"data":{"entity":{"type":"playlist","name":"Latihan Fokus","title":"Latihan Fokus","trackList":[{"title":"Shot for Me","subtitle":"Drake","duration":224720,"uri":"spotify:track:1"},{"title":"Show Da World","subtitle":"Trill Family, Boosie Badazz","duration":262902,"uri":"spotify:track:2"},{"title":"   ","subtitle":"tanpa judul","duration":1000}]}}}}}}</script>
        </body></html>
    """.trimIndent()

    private val appleHtml = """
        <!DOCTYPE html><html><head><title>Apple Music</title></head><body>
        <script type="application/json" id="serialized-server-data">{"data":[{"data":{"sections":[{"itemKind":"containerDetailHeaderLockup","items":[{"title":"Throwbacks - Hip Hop"}]},{"itemKind":"trackLockup","items":[{"title":"Shot for Me","artistName":"Drake","duration":224720},{"title":"Tanpa Artis","subtitleLinks":[{"title":"Anonim"},{"title":"Teman"}],"duration":180000}]},{"itemKind":"spacer","items":[]}]}}]}</script>
        </body></html>
    """.trimIndent()

    // --- Spotify ---

    @Test
    fun `spotify membaca nama playlist dan lagu`() {
        val result = SpotifyPageParser.parse(spotifyHtml)
        assertNotNull(result)
        result!!
        assertEquals("Latihan Fokus", result.name)
        assertEquals(ImportSource.SPOTIFY, result.source)
        // Baris tanpa judul dibuang.
        assertEquals(2, result.tracks.size)
        assertEquals("Shot for Me", result.tracks[0].title)
        assertEquals("Drake", result.tracks[0].artist)
        assertEquals(224, result.tracks[0].durationSec)
        assertEquals(262, result.tracks[1].durationSec)
    }

    @Test
    fun `spotify mengembalikan null kalau data tanam tidak ada`() {
        assertNull(SpotifyPageParser.parse("<html><body>Halaman tidak dikenal</body></html>"))
    }

    @Test
    fun `spotify mengembalikan null kalau trackList kosong`() {
        val html = """<script id="__NEXT_DATA__" type="application/json">{"props":{"pageProps":{"state":{"data":{"entity":{"name":"Kosong","trackList":[]}}}}}}</script>"""
        assertNull(SpotifyPageParser.parse(html))
    }

    // --- Apple Music ---

    @Test
    fun `apple music membaca header dan daftar lagu`() {
        val result = AppleMusicPageParser.parse(appleHtml)
        assertNotNull(result)
        result!!
        assertEquals("Throwbacks - Hip Hop", result.name)
        assertEquals(ImportSource.APPLE_MUSIC, result.source)
        assertEquals(2, result.tracks.size)
        assertEquals("Drake", result.tracks[0].artist)
        assertEquals(224, result.tracks[0].durationSec)
        // Kalau artistName tidak ada, dipakai subtitleLinks.
        assertEquals("Anonim, Teman", result.tracks[1].artist)
    }

    @Test
    fun `apple music mengembalikan null tanpa data tanam`() {
        assertNull(AppleMusicPageParser.parse("<html><body>404</body></html>"))
    }

    // --- Deteksi link ---

    @Test
    fun `deteksi link spotify playlist dengan parameter share`() {
        val target = PlaylistLinkParser.detect(
            "Cek playlist ini https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=abc123"
        )
        assertTrue(target is LinkTarget.Spotify)
        target as LinkTarget.Spotify
        assertEquals("37i9dQZF1DXcBWIGoYBM5M", target.id)
        assertEquals(false, target.isAlbum)
    }

    @Test
    fun `deteksi link album spotify`() {
        val target = PlaylistLinkParser.detect("https://open.spotify.com/album/1ATL5GLyefJaxhQzSPVrLX")
        assertTrue(target is LinkTarget.Spotify)
        assertEquals(true, (target as LinkTarget.Spotify).isAlbum)
    }

    @Test
    fun `deteksi link apple music`() {
        val target = PlaylistLinkParser.detect(
            "https://music.apple.com/us/playlist/throwbacks/pl.u-DdAN85qtqA5AGd"
        )
        assertTrue(target is LinkTarget.AppleMusic)
    }

    @Test
    fun `deteksi link youtube music mengambil id daftar`() {
        val target = PlaylistLinkParser.detect("https://music.youtube.com/playlist?list=PLabc123_-X&si=1")
        assertTrue(target is LinkTarget.YouTubeMusic)
        assertEquals("PLabc123_-X", (target as LinkTarget.YouTubeMusic).listId)
    }

    @Test
    fun `link tidak dikenal ditolak`() {
        assertNull(PlaylistLinkParser.detect("https://example.com/playlist/123"))
        assertNull(PlaylistLinkParser.detect("bukan link sama sekali"))
        assertNull(PlaylistLinkParser.detect("https://open.spotify.com/track/abc"))
    }
}
