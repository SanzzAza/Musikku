package com.vyora.music.data.importer

import com.vyora.music.data.local.PlaylistStore
import com.vyora.music.data.ytmusic.SearchFilter
import com.vyora.music.data.ytmusic.SongItem
import com.vyora.music.data.ytmusic.YTMusic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Mengimpor playlist dari link Spotify / Apple Music / YouTube Music.
 *
 * Alur: kenali link -> ambil halaman publik -> baca daftar lagu ->
 * carikan tiap lagu di YouTube Music -> simpan sebagai playlist lokal.
 */
class PlaylistImporter(
    private val ytMusic: YTMusic,
    private val playlists: PlaylistStore,
) {

    data class Result(
        val playlistId: String? = null,
        val name: String = "",
        val matched: Int = 0,
        val total: Int = 0,
        val error: String? = null,
    ) {
        val success: Boolean get() = playlistId != null
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun import(
        rawText: String,
        onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> },
    ): Result {
        var target = PlaylistLinkParser.detect(rawText)
            ?: return Result(
                error = "Link tidak dikenali. Tempel link playlist Spotify, Apple Music, atau YouTube Music."
            )

        // Tautan pendek (spotify.link / apple.co) diikuti dulu, maksimal 2 lompatan.
        var hops = 0
        while (target is LinkTarget.ShortLink && hops < 2) {
            val resolved = runCatching { resolveRedirect(target.url) }.getOrNull()
                ?: return Result(error = "Tautan pendek tidak bisa dibuka. Coba tempel link lengkapnya.")
            target = PlaylistLinkParser.detect(resolved)
                ?: return Result(error = "Tautan pendek mengarah ke halaman yang tidak dikenali.")
            hops++
        }

        return try {
            when (target) {
                is LinkTarget.YouTubeMusic -> importFromYouTube(target.listId)
                is LinkTarget.Spotify -> importFromSpotify(target, onProgress)
                is LinkTarget.AppleMusic -> importFromApple(target.url, onProgress)
                is LinkTarget.ShortLink -> Result(error = "Tautan pendek terlalu banyak dialihkan.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result(error = e.message ?: "Gagal mengimpor playlist.")
        }
    }

    private suspend fun importFromSpotify(
        target: LinkTarget.Spotify,
        onProgress: suspend (Int, Int) -> Unit,
    ): Result = withContext(Dispatchers.IO) {
        val kind = if (target.isAlbum) "album" else "playlist"
        val url = "https://open.spotify.com/embed/$kind/${target.id}"
        var problem: String? = null

        // Coba dua user-agent: sebagian respons Spotify berbeda tergantung UA.
        for (userAgent in listOf(MOBILE_USER_AGENT, DESKTOP_USER_AGENT)) {
            val html = try {
                fetch(url, userAgent)
            } catch (e: IOException) {
                problem = e.message
                continue
            }
            val parsed = SpotifyPageParser.parse(html)
            if (parsed != null) return@withContext matchAndSave(parsed, onProgress)
            problem = "Daftar lagu Spotify tidak terbaca (halaman berubah). Coba lagi nanti."
        }

        Result(error = problem ?: "Gagal membaca playlist Spotify.")
    }

    private suspend fun importFromApple(
        url: String,
        onProgress: suspend (Int, Int) -> Unit,
    ): Result = withContext(Dispatchers.IO) {
        val html = fetch(url, DESKTOP_USER_AGENT)
        val parsed = AppleMusicPageParser.parse(html)
            ?: return@withContext Result(
                error = "Daftar lagu Apple Music tidak terbaca. Pastikan playlist bersifat publik."
            )
        matchAndSave(parsed, onProgress)
    }

    private suspend fun importFromYouTube(listId: String): Result = withContext(Dispatchers.IO) {
        val browseId = if (listId.startsWith("VL")) listId else "VL$listId"
        val page = ytMusic.collection(browseId)
        if (page.songs.isEmpty()) {
            return@withContext Result(error = "Playlist YouTube Music ini kosong atau tidak bersifat publik.")
        }
        val playlist = playlists.create(
            page.title.ifBlank { "Playlist YouTube Music" },
            page.songs,
        )
        Result(
            playlistId = playlist.id,
            name = playlist.name,
            matched = page.songs.size,
            total = page.songs.size,
        )
    }

    private suspend fun matchAndSave(
        imported: ImportedPlaylist,
        onProgress: suspend (Int, Int) -> Unit,
    ): Result = withContext(Dispatchers.IO) {
        val tracks = imported.tracks.take(MAX_TRACKS)
        val songs = ArrayList<SongItem>(tracks.size)

        tracks.forEachIndexed { index, track ->
            withContext(Dispatchers.Main) { onProgress(index + 1, tracks.size) }

            val query = listOf(track.title, track.artist)
                .filter { it.isNotBlank() }
                .joinToString(" ")

            val found = runCatching {
                ytMusic.search(query, SearchFilter.SONGS)
            }.getOrDefault(emptyList())
                .filterIsInstance<SongItem>()
                .firstOrNull()

            if (found != null && songs.none { it.id == found.id }) songs.add(found)

            // Jeda kecil supaya tidak membanjiri YouTube Music dengan puluhan permintaan.
            delay(REQUEST_SPACING_MS)
        }

        if (songs.isEmpty()) {
            return@withContext Result(
                total = tracks.size,
                error = "Tidak ada lagu yang cocok di YouTube Music.",
            )
        }

        val playlist = playlists.create(imported.name, songs)
        Result(
            playlistId = playlist.id,
            name = playlist.name,
            matched = songs.size,
            total = tracks.size,
        )
    }

    /** Mengikuti tautan pendek dan mengembalikan URL akhir. */
    private fun resolveRedirect(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", DESKTOP_USER_AGENT)
            .header("Accept-Language", "id-ID,id;q=0.9,en;q=0.8")
            .build()
        client.newCall(request).execute().use { response ->
            return response.request.url.toString()
        }
    }

    private fun fetch(url: String, userAgent: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
            .build()
        client.newCall(request).execute().use { response ->
            return when {
                response.isSuccessful -> response.body?.string().orEmpty()
                response.code == 404 -> throw IOException(
                    "Playlist tidak ditemukan. Pastikan tautannya benar dan playlist bersifat publik."
                )
                response.code == 401 || response.code == 403 -> throw IOException(
                    "Playlist ini privat. Ubah menjadi publik lalu coba lagi."
                )
                response.code == 429 -> throw IOException(
                    "Sumber sedang membatasi permintaan. Coba lagi beberapa saat."
                )
                else -> throw IOException("Sumber menolak permintaan (kode ${response.code}).")
            }
        }
    }

    private companion object {
        const val MAX_TRACKS = 150
        const val REQUEST_SPACING_MS = 120L
        const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/126.0.0.0 Mobile Safari/537.36"
        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/126.0.0.0 Safari/537.36"
    }
}
