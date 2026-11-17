package com.example.musikku.data.importer

/** Sumber playlist yang bisa diimpor. */
enum class ImportSource(val label: String) {
    SPOTIFY("Spotify"),
    APPLE_MUSIC("Apple Music"),
    YOUTUBE_MUSIC("YouTube Music"),
}

/** Satu lagu hasil pembacaan halaman sumber, sebelum dicarikan di YouTube Music. */
data class ImportedTrack(
    val title: String,
    val artist: String,
    val durationSec: Int,
)

/** Playlist hasil pembacaan halaman sumber. */
data class ImportedPlaylist(
    val name: String,
    val tracks: List<ImportedTrack>,
    val source: ImportSource,
)

/** Hasil deteksi sebuah link. */
sealed interface LinkTarget {
    data class Spotify(val id: String, val isAlbum: Boolean) : LinkTarget
    data class AppleMusic(val url: String) : LinkTarget
    data class YouTubeMusic(val listId: String) : LinkTarget
}

/**
 * Mengenali link playlist dari teks yang ditempel pengguna.
 *
 * Teks share dari aplikasi lain biasanya berbentuk
 * "Cek playlist ini https://open.spotify.com/playlist/xxx?si=abc", jadi URL
 * diambil dulu dari dalam teks.
 */
object PlaylistLinkParser {

    private val urlRegex = Regex("""https?://[^\s"'<>]+""")
    private val listIdRegex = Regex("""[?&]list=([A-Za-z0-9_-]+)""")

    fun extractUrl(text: String): String? = urlRegex.find(text.trim())?.value

    fun detect(rawText: String): LinkTarget? {
        val url = extractUrl(rawText) ?: return null
        val lower = url.lowercase()

        return when {
            lower.contains("open.spotify.com") -> {
                val segments = url.substringAfter("open.spotify.com/", "").split('/', '?')
                val kind = segments.getOrNull(0)?.lowercase()
                val id = segments.getOrNull(1)?.takeIf { it.isNotBlank() }
                if (id == null || (kind != "playlist" && kind != "album")) {
                    null
                } else {
                    LinkTarget.Spotify(id, kind == "album")
                }
            }

            lower.contains("music.apple.com") -> LinkTarget.AppleMusic(url)

            lower.contains("music.youtube.com") || lower.contains("youtube.com") -> {
                val listId = listIdRegex.find(url)?.groupValues?.getOrNull(1)
                if (listId.isNullOrBlank()) null else LinkTarget.YouTubeMusic(listId)
            }

            else -> null
        }
    }
}
