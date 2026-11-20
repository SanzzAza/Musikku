package com.vyora.music.data.importer

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/*
 * Parser halaman Spotify & Apple Music.
 *
 * Keduanya membaca JSON yang sudah ditanam di HTML halaman publik — tidak perlu
 * API key maupun login. Semua fungsi di sini murni (String -> model), sehingga
 * bisa diuji unit test dengan contoh HTML tanpa jaringan.
 *
 * Catatan: kalau Spotify/Apple mengubah struktur halaman, parser akan
 * mengembalikan null dan aplikasi menampilkan pesan error yang jelas —
 * bukan crash.
 */

private fun JsonObject.obj(key: String): JsonObject? =
    get(key)?.takeIf { it.isJsonObject }?.asJsonObject

private fun JsonObject.arr(key: String): JsonArray? =
    get(key)?.takeIf { it.isJsonArray }?.asJsonArray

private fun JsonObject.str(key: String): String {
    val element: JsonElement = get(key) ?: return ""
    return if (element.isJsonPrimitive) element.asString.orEmpty() else ""
}

private fun JsonObject.num(key: String): Long {
    val element: JsonElement = get(key) ?: return 0L
    return if (element.isJsonPrimitive && element.asJsonPrimitive.isNumber) element.asLong else 0L
}

private fun JsonArray.objects(): List<JsonObject> =
    mapNotNull { element -> element.takeIf { it.isJsonObject }?.asJsonObject }

private fun parseJson(raw: String): JsonObject? =
    runCatching { JsonParser.parseString(raw) }.getOrNull()?.takeIf { it.isJsonObject }?.asJsonObject

/**
 * Mencari array bernama [key] di mana pun di dalam JSON.
 *
 * Dipakai sebagai jaring pengaman: kalau Spotify memindahkan `trackList` ke
 * kedalaman lain, pengimporan tetap jalan (dulu lokasinya dipatok ke
 * `props.pageProps.state.data.entity` sehingga gampang patah).
 */
private fun findArrayDeep(element: JsonElement, key: String, depth: Int = 0): JsonArray? {
    if (depth > 12) return null
    return when {
        element.isJsonObject -> {
            val obj = element.asJsonObject
            obj.arr(key)?.takeIf { it.size() > 0 }
                ?: obj.entrySet().asSequence()
                    .firstNotNullOfOrNull { findArrayDeep(it.value, key, depth + 1) }
        }
        element.isJsonArray -> element.asJsonArray.asSequence()
            .firstNotNullOfOrNull { findArrayDeep(it, key, depth + 1) }
        else -> null
    }
}

/** Membaca halaman embed Spotify (`open.spotify.com/embed/playlist/...`). */
object SpotifyPageParser {

    // Atribut <script> bisa berurutan berbeda antar versi halaman, jadi
    // id="__NEXT_DATA__" dicari tanpa mematok urutan atribut.
    private val nextDataRegex = Regex(
        """<script[^>]*id="__NEXT_DATA__"[^>]*>(.+?)</script>""",
        RegexOption.DOT_MATCHES_ALL,
    )

    fun parse(html: String): ImportedPlaylist? {
        val raw = nextDataRegex.find(html)?.groupValues?.getOrNull(1) ?: return null
        val root = parseJson(raw) ?: return null

        val entity = root.obj("props")
            ?.obj("pageProps")
            ?.obj("state")
            ?.obj("data")
            ?.obj("entity")

        val trackList = entity?.arr("trackList")?.takeIf { it.size() > 0 }
            ?: findArrayDeep(root, "trackList")
            ?: return null

        val name = entity?.str("name").orEmpty()
            .ifBlank { entity?.str("title").orEmpty() }

        val tracks = trackList.objects().mapNotNull { track ->
            val title = track.str("title").trim()
            if (title.isEmpty()) {
                null
            } else {
                ImportedTrack(
                    title = title,
                    artist = track.str("subtitle").trim(),
                    durationSec = (track.num("duration") / 1000L).toInt(),
                )
            }
        }
        if (tracks.isEmpty()) return null

        return ImportedPlaylist(
            name = name.ifBlank { "Playlist Spotify" },
            tracks = tracks,
            source = ImportSource.SPOTIFY,
        )
    }
}

/** Membaca halaman playlist Apple Music (`music.apple.com/...`). */
object AppleMusicPageParser {

    private val serializedRegex = Regex(
        """<script[^>]*id="serialized-server-data"[^>]*>(.+?)</script>""",
        RegexOption.DOT_MATCHES_ALL,
    )

    fun parse(html: String): ImportedPlaylist? {
        val raw = serializedRegex.find(html)?.groupValues?.getOrNull(1) ?: return null
        val root = parseJson(raw) ?: return null

        // Struktur: { data: [ { data: { sections: [ { itemKind, items: [...] } ] } } ] }
        val pages = root.arr("data")?.objects() ?: return null

        var name = ""
        val tracks = ArrayList<ImportedTrack>()

        for (page in pages) {
            val sections = page.obj("data")?.arr("sections")?.objects() ?: continue
            for (section in sections) {
                val items = section.arr("items")?.objects() ?: continue
                val kind = section.str("itemKind")

                when {
                    // Header playlist menyimpan nama playlist.
                    kind.contains("Header", ignoreCase = true) -> {
                        val header = items.firstOrNull()
                        val title = header?.str("title").orEmpty().trim()
                        if (name.isBlank() && title.isNotBlank()) name = title
                    }

                    kind.contains("trackLockup", ignoreCase = true) -> {
                        items.forEach { item ->
                            val title = item.str("title").trim()
                            if (title.isNotEmpty()) {
                                val artist = item.str("artistName").ifBlank {
                                    item.arr("subtitleLinks")?.objects()
                                        ?.mapNotNull { it.str("title").takeIf { t -> t.isNotBlank() } }
                                        ?.joinToString(", ")
                                        .orEmpty()
                                }
                                tracks.add(
                                    ImportedTrack(
                                        title = title,
                                        artist = artist,
                                        durationSec = (item.num("duration") / 1000L).toInt(),
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        if (tracks.isEmpty()) return null
        return ImportedPlaylist(
            name = name.ifBlank { "Playlist Apple Music" },
            tracks = tracks,
            source = ImportSource.APPLE_MUSIC,
        )
    }
}
