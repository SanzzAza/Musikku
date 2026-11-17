package com.example.musikku.data.importer

import com.example.musikku.data.local.PlaylistStore
import com.example.musikku.data.ytmusic.SearchFilter
import com.example.musikku.data.ytmusic.SongItem
import com.example.musikku.data.ytmusic.YTMusic
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
        .build()

    suspend fun import(
        rawText: String,
        onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> },
    ): Result {
        val target = PlaylistLinkParser.detect(rawText)
            ?: return Result(
                error = "Link tidak dikenali. Tempel link playlist Spotify, Apple Music, atau YouTube Music."
            )

        return try {
            when (target) {
                is LinkTarget.YouTubeMusic -> importFromYouTube(target.listId)
                is LinkTarget.Spotify -> {
                    val url = if (target.isAlbum) {
                        "https://open.spotify.com/embed/album/${target.id}"
                    } else {
                        "https://open.spotify.com/embed/playlist/${target.id}"
                    }
                    val html = fetch(url, MOBILE_USER_AGENT)
                    val parsed = SpotifyPageParser.parse(html)
                        ?: return Result(error = "Daftar lagu Spotify tidak bisa dibaca. Pastikan playlist-nya publik.")
                    matchAndSave(parsed, onProgress)
                }
                is LinkTarget.AppleMusic -> {
                    val html = fetch(target.url, DESKTOP_USER_AGENT)
                    val parsed = AppleMusicPageParser.parse(html)
                        ?: return Result(error = "Daftar lagu Apple Music tidak bisa dibaca. Pastikan playlist-nya publik.")
                    matchAndSave(parsed, onProgress)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result(error = e.message ?: "Gagal mengimpor playlist.")
        }
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

    private fun fetch(url: String, userAgent: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Sumber menolak permintaan (kode ${response.code}).")
            }
            return response.body?.string().orEmpty()
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
