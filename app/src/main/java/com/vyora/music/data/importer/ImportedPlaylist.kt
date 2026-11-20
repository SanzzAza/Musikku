package com.vyora.music.data.importer

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

    /** Tautan pendek (spotify.link / apple.co) yang harus diikuti dulu. */
    data class ShortLink(val url: String) : LinkTarget
}

/**
 * Mengenali link playlist dari teks yang ditempel pengguna.
 *
 * Format yang diterima:
 * - `open.spotify.com/playlist/<id>` — termasuk variasi `/intl-id/`, `/embed/`,
 *   dan `/user/<nama>/playlist/<id>` yang sering muncul dari tombol Bagikan.
 * - `spotify:playlist:<id>` (URI aplikasi Spotify)
 * - `spotify.link/...` (tautan pendek, diikuti dulu oleh pengimpor)
 * - `music.apple.com/<negara>/playlist/...` dan `apple.co/...`
 * - `music.youtube.com/playlist?list=...`
 * - Domain tanpa `https://` dan teks share lengkap
 *   ("Cek playlist ini https://…").
 */
object PlaylistLinkParser {

    private val spotifyUriRegex = Regex("""spotify:(?:playlist|album):[A-Za-z0-9]+""", RegexOption.IGNORE_CASE)
    private val httpUrlRegex = Regex("""https?://[^\s"'<>]+""", RegexOption.IGNORE_CASE)
    private val bareHostRegex = Regex(
        """(?:open\.spotify\.com|spotify\.link|music\.apple\.com|itunes\.apple\.com|apple\.co|music\.youtube\.com|youtube\.com)/[^\s"'<>]*""",
        RegexOption.IGNORE_CASE,
    )
    private val listIdRegex = Regex("""[?&]list=([A-Za-z0-9_-]+)""")

    fun extractUrl(text: String): String? {
        val trimmed = text.trim()
        spotifyUriRegex.find(trimmed)?.let { return it.value }
        httpUrlRegex.find(trimmed)?.let { return it.value }
        bareHostRegex.find(trimmed)?.let { return "https://" + it.value }
        return null
    }

    fun detect(rawText: String): LinkTarget? {
        val url = extractUrl(rawText) ?: return null
        val lower = url.lowercase()

        // URI langsung dari aplikasi Spotify.
        if (lower.startsWith("spotify:")) {
            val parts = url.split(':')
            val kind = parts.getOrNull(1)?.lowercase()
            val id = parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: return null
            return when (kind) {
                "playlist" -> LinkTarget.Spotify(id, isAlbum = false)
                "album" -> LinkTarget.Spotify(id, isAlbum = true)
                else -> null
            }
        }

        if (lower.contains("open.spotify.com")) {
            val path = url.substringAfter("://", url).substringAfter('/', "")
            val segments = path.trim('/').split('/', '?').map { it.trim() }
            // Ambil segmen "playlist"/"album" lalu segmen berikutnya sebagai ID.
            // Dengan begitu "/intl-id/playlist/x", "/embed/playlist/x", dan
            // "/user/budi/playlist/x" semuanya terbaca.
            val kindIndex = segments.indexOfFirst {
                it.equals("playlist", true) || it.equals("album", true)
            }
            if (kindIndex < 0) return null
            val id = segments.getOrNull(kindIndex + 1)?.substringBefore('?')?.trim()
            if (id.isNullOrBlank()) return null
            return LinkTarget.Spotify(id, isAlbum = segments[kindIndex].equals("album", true))
        }

        // Tautan pendek. Pemeriksaan host dibuat ketat: "music.apple.com"
        // juga mengandung teks "apple.co", jadi tidak boleh dicek dengan contains biasa.
        if (lower.contains("spotify.link/") || lower.contains("spotify.app.link/")) {
            return LinkTarget.ShortLink(url)
        }
        if (lower.contains("music.apple.com") || lower.contains("itunes.apple.com")) {
            return LinkTarget.AppleMusic(url)
        }
        if (lower.contains("//apple.co/") || lower.contains("//www.apple.co/")) {
            return LinkTarget.ShortLink(url)
        }
        if (lower.contains("youtube.com")) {
            val listId = listIdRegex.find(url)?.groupValues?.getOrNull(1)
            if (listId.isNullOrBlank()) return null
            return LinkTarget.YouTubeMusic(listId)
        }
        return null
    }
}
