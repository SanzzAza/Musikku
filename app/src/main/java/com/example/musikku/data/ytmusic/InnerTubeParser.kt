package com.example.musikku.data.ytmusic

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Parser respons InnerTube (API internal YouTube Music, client WEB_REMIX).
 * Dibuat toleran: field yang tidak ada dilewati, bukan bikin crash.
 */
internal object InnerTubeParser {

    // ---------- helper JSON ----------
    fun JsonElement?.obj(key: String): JsonObject? =
        (this as? JsonObject)?.get(key)?.takeIf { it.isJsonObject }?.asJsonObject

    fun JsonElement?.arr(key: String): JsonArray? =
        (this as? JsonObject)?.get(key)?.takeIf { it.isJsonArray }?.asJsonArray

    fun JsonElement?.str(key: String): String? =
        (this as? JsonObject)?.get(key)?.takeIf { it.isJsonPrimitive }?.asString

    /** Ambil path bertingkat, contoh: path(o, "a", "b", 0, "c") */
    fun path(root: JsonElement?, vararg keys: Any): JsonElement? {
        var cur: JsonElement? = root
        for (k in keys) {
            cur = when (k) {
                is String -> (cur as? JsonObject)?.get(k)
                is Int -> (cur as? JsonArray)?.let { if (k < it.size()) it[k] else null }
                else -> null
            } ?: return null
        }
        return cur
    }

    data class Run(val text: String, val browseId: String?, val pageType: String?, val videoId: String?)

    fun runs(textObj: JsonElement?): List<Run> =
        textObj.arr("runs")?.mapNotNull { r ->
            val o = r as? JsonObject ?: return@mapNotNull null
            val nav = o.obj("navigationEndpoint")
            val browse = nav.obj("browseEndpoint")
            Run(
                text = o.str("text").orEmpty(),
                browseId = browse.str("browseId"),
                pageType = path(browse, "browseEndpointContextSupportedConfigs", "browseEndpointContextMusicConfig", "pageType")?.asString,
                videoId = nav.obj("watchEndpoint").str("videoId"),
            )
        }.orEmpty()

    fun text(textObj: JsonElement?): String = runs(textObj).joinToString("") { it.text }

    private const val ARTIST = "MUSIC_PAGE_TYPE_ARTIST"
    private const val USER_CHANNEL = "MUSIC_PAGE_TYPE_USER_CHANNEL"
    private const val ALBUM = "MUSIC_PAGE_TYPE_ALBUM"
    private const val PLAYLIST = "MUSIC_PAGE_TYPE_PLAYLIST"

    private val DURATION = Regex("^(\\d{1,2})[:.](\\d{2})(?:[:.](\\d{2}))?$")

    fun parseDuration(s: String?): Int {
        val m = DURATION.matchEntire(s?.trim() ?: return 0) ?: return 0
        val (a, b, c) = m.destructured
        return if (c.isEmpty()) a.toInt() * 60 + b.toInt()
        else a.toInt() * 3600 + b.toInt() * 60 + c.toInt()
    }

    /** Ambil thumbnail terbesar lalu naikkan resolusinya (URL googleusercontent bisa di-resize). */
    fun thumbnail(o: JsonElement?): String? {
        val list = (path(o, "thumbnail", "musicThumbnailRenderer", "thumbnail", "thumbnails")
            ?: path(o, "thumbnailRenderer", "musicThumbnailRenderer", "thumbnail", "thumbnails")
            ?: path(o, "thumbnail", "croppedSquareThumbnailRenderer", "thumbnail", "thumbnails")
            ?: path(o, "thumbnail", "thumbnails")
            ?: path(o, "thumbnails")) as? JsonArray ?: return null
        val url = list.lastOrNull()?.str("url") ?: return null
        return upscale(url)
    }

    fun upscale(url: String): String = when {
        url.contains("googleusercontent.com") || url.contains("ggpht.com") ->
            url.replace(Regex("=w\\d+-h\\d+[^&]*$"), "=w544-h544-l90-rj")
                .replace(Regex("=s\\d+[^&]*$"), "=s544")
        else -> url
    }

    private fun isTypeLabel(t: String) = t in setOf("Lagu", "Song", "Video", "Episode", "Single", "EP", "Album", "Playlist", "Artis", "Artist")

    // ---------- item parser ----------

    fun parseItem(wrapper: JsonElement?): YTItem? {
        val o = wrapper as? JsonObject ?: return null
        o.obj("musicResponsiveListItemRenderer")?.let { return parseResponsive(it) }
        o.obj("musicTwoRowItemRenderer")?.let { return parseTwoRow(it) }
        o.obj("playlistPanelVideoRenderer")?.let { return parsePanelVideo(it) }
        return null
    }

    private fun songFromRuns(
        videoId: String, title: String, meta: List<Run>, duration: Int, thumb: String?,
    ): SongItem {
        val artists = meta.filter { it.pageType == ARTIST || it.pageType == USER_CHANNEL }
            .map { ArtistRef(it.browseId, it.text) }
            .ifEmpty {
                // Tanpa link: ambil teks pertama sebelum " • " sebagai nama artis
                meta.map { it.text }.filter { it.isNotBlank() && it.trim() != "•" && it.trim() != "&" && it.trim() != "," }
                    .firstOrNull { !isTypeLabel(it.trim()) && parseDuration(it) == 0 && !it.contains("pemutaran") && !it.contains("ditonton") && !it.contains("views") && !it.contains("plays") }
                    ?.let { listOf(ArtistRef(null, it.trim())) }
                    .orEmpty()
            }
        val album = meta.firstOrNull { it.pageType == ALBUM }?.let { AlbumRef(it.browseId, it.text) }
        val dur = if (duration > 0) duration else meta.firstNotNullOfOrNull { parseDuration(it.text).takeIf { d -> d > 0 } } ?: 0
        return SongItem(videoId, title, artists, album, dur, thumb)
    }

    private fun parseResponsive(r: JsonObject): YTItem? {
        val cols = r.arr("flexColumns")?.map { runs(path(it, "musicResponsiveListItemFlexColumnRenderer", "text")) }.orEmpty()
        val title = cols.getOrNull(0)?.joinToString("") { it.text }?.takeIf { it.isNotBlank() } ?: return null
        val meta = cols.drop(1).flatten()
        val thumb = thumbnail(r)

        val videoId = path(r, "playlistItemData", "videoId")?.asString
            ?: path(r, "overlay", "musicItemThumbnailOverlayRenderer", "content", "musicPlayButtonRenderer", "playNavigationEndpoint", "watchEndpoint", "videoId")?.asString
            ?: cols.getOrNull(0)?.firstOrNull()?.videoId

        val browse = r.obj("navigationEndpoint").obj("browseEndpoint")
        val browseId = browse.str("browseId")
        val pageType = path(browse, "browseEndpointContextSupportedConfigs", "browseEndpointContextMusicConfig", "pageType")?.asString

        if (browseId != null && pageType != null) {
            val subtitle = meta.joinToString("") { it.text }
            return when (pageType) {
                ARTIST, USER_CHANNEL -> ArtistItem(browseId, title, subtitle.removePrefix("Artis • ").removePrefix("Artist • "), thumb)
                ALBUM -> AlbumItem(browseId, title, subtitle, thumb, isPlaylist = false)
                PLAYLIST -> AlbumItem(browseId, title, subtitle, thumb, isPlaylist = true)
                else -> null
            }
        }
        if (videoId != null) {
            val fixed = r.arr("fixedColumns")?.firstOrNull()
                ?.let { text(path(it, "musicResponsiveListItemFixedColumnRenderer", "text")) }
            return songFromRuns(videoId, title, meta, parseDuration(fixed), thumb)
        }
        return null
    }

    private fun parseTwoRow(r: JsonObject): YTItem? {
        val title = text(r.obj("title")).takeIf { it.isNotBlank() } ?: return null
        val subRuns = runs(r.obj("subtitle"))
        val subtitle = subRuns.joinToString("") { it.text }
        val thumb = thumbnail(r)
        val nav = r.obj("navigationEndpoint")

        nav.obj("watchEndpoint").str("videoId")?.let { vid ->
            return songFromRuns(vid, title, subRuns, 0, thumb)
        }
        val browse = nav.obj("browseEndpoint") ?: return null
        val browseId = browse.str("browseId") ?: return null
        return when (path(browse, "browseEndpointContextSupportedConfigs", "browseEndpointContextMusicConfig", "pageType")?.asString) {
            ARTIST, USER_CHANNEL -> ArtistItem(browseId, title, subtitle, thumb)
            ALBUM -> AlbumItem(browseId, title, subtitle, thumb, isPlaylist = false)
            PLAYLIST -> AlbumItem(browseId, title, subtitle, thumb, isPlaylist = true)
            else -> null
        }
    }

    private fun parsePanelVideo(v: JsonObject): SongItem? {
        val id = v.str("videoId") ?: return null
        val title = text(v.obj("title"))
        return songFromRuns(id, title, runs(v.obj("longBylineText")), parseDuration(text(v.obj("lengthText"))), thumbnail(v))
    }

    // ---------- section parser ----------

    fun parseSections(contents: JsonArray?): List<Section> = contents?.mapNotNull { parseSection(it) }.orEmpty()

    fun parseSection(el: JsonElement?): Section? {
        val o = el as? JsonObject ?: return null
        o.obj("musicCarouselShelfRenderer")?.let { s ->
            val header = path(s, "header", "musicCarouselShelfBasicHeaderRenderer")
            val more = path(header, "moreContentButton", "buttonRenderer", "navigationEndpoint", "browseEndpoint", "browseId")?.asString
            val items = s.arr("contents")?.mapNotNull { parseItem(it) }.orEmpty()
            return Section(text(header.obj("title")), items, more).takeIf { items.isNotEmpty() }
        }
        o.obj("musicShelfRenderer")?.let { s ->
            val more = path(s, "bottomEndpoint", "browseEndpoint", "browseId")?.asString
            val items = s.arr("contents")?.mapNotNull { parseItem(it) }.orEmpty()
            return Section(text(s.obj("title")), items, more).takeIf { items.isNotEmpty() }
        }
        o.obj("gridRenderer")?.let { s ->
            val items = s.arr("items")?.mapNotNull { parseItem(it) }.orEmpty()
            val title = text(path(s, "header", "gridHeaderRenderer", "title"))
            return Section(title, items).takeIf { items.isNotEmpty() }
        }
        return null
    }
}
