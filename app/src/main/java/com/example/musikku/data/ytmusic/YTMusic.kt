package com.example.musikku.data.ytmusic

import com.example.musikku.data.ytmusic.InnerTubeParser.arr
import com.example.musikku.data.ytmusic.InnerTubeParser.obj
import com.example.musikku.data.ytmusic.InnerTubeParser.path
import com.example.musikku.data.ytmusic.InnerTubeParser.text
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Klien YouTube Music (InnerTube, client WEB_REMIX) — sama seperti yang dipakai
 * music.youtube.com di browser. Tidak butuh login / API key.
 */
class YTMusic(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
    private val hl: String = "id",
    private val gl: String = "ID",
) {
    private val json = "application/json".toMediaType()

    private fun context() = JsonObject().apply {
        add("client", JsonObject().apply {
            addProperty("clientName", "WEB_REMIX")
            addProperty("clientVersion", CLIENT_VERSION)
            addProperty("hl", hl)
            addProperty("gl", gl)
        })
    }

    private suspend fun post(endpoint: String, body: JsonObject): JsonObject = withContext(Dispatchers.IO) {
        body.add("context", context())
        val request = Request.Builder()
            .url("https://music.youtube.com/youtubei/v1/$endpoint?prettyPrint=false")
            .post(body.toString().toRequestBody(json))
            .header("User-Agent", USER_AGENT)
            .header("Origin", "https://music.youtube.com")
            .header("Referer", "https://music.youtube.com/")
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", CLIENT_VERSION)
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("YouTube Music error ${resp.code}")
            JsonParser.parseString(resp.body?.string().orEmpty()).asJsonObject
        }
    }

    private suspend fun browse(browseId: String, extra: JsonObject.() -> Unit = {}): JsonObject =
        post("browse", JsonObject().apply { addProperty("browseId", browseId); extra() })

    private fun singleColumnSections(d: JsonObject): JsonArray? =
        path(d, "contents", "singleColumnBrowseResultsRenderer", "tabs", 0, "tabRenderer", "content", "sectionListRenderer", "contents") as? JsonArray

    // ---------------- Beranda ----------------

    /** Beranda = Home YT Music + Tangga lagu Indonesia + Rilisan baru. */
    suspend fun home(): List<Section> = coroutineScope {
        val home = async { runCatching { InnerTubeParser.parseSections(singleColumnSections(browse("FEmusic_home"))) }.getOrDefault(emptyList()) }
        val charts = async {
            runCatching {
                InnerTubeParser.parseSections(singleColumnSections(browse("FEmusic_charts") {
                    add("formData", JsonObject().apply { add("selectedValues", JsonArray().apply { add(gl) }) })
                }))
            }.getOrDefault(emptyList())
        }
        val releases = async { runCatching { InnerTubeParser.parseSections(singleColumnSections(browse("FEmusic_new_releases"))) }.getOrDefault(emptyList()) }

        val chartSections = charts.await()

        // Ambil isi playlist "Trending 20 Indonesia" supaya beranda langsung ada lagu Indonesia yang lagi hits
        val trending = chartSections.flatMap { it.items }.filterIsInstance<AlbumItem>().firstOrNull { it.isPlaylist }
            ?.let { pl -> runCatching { collection(pl.id) }.getOrNull() }
            ?.songs?.takeIf { it.isNotEmpty() }
            ?.let { Section("Trending di Indonesia", it.take(20), moreBrowseId = null) }

        val result = listOfNotNull(trending) + home.await() + chartSections + releases.await()
        if (result.isEmpty()) throw IOException("Gagal memuat beranda")
        result.filter { it.title.isNotBlank() }
    }

    // ---------------- Pencarian ----------------

    suspend fun search(query: String, filter: SearchFilter): List<YTItem> {
        val d = post("search", JsonObject().apply {
            addProperty("query", query)
            addProperty("params", filter.params)
        })
        val contents = path(d, "contents", "tabbedSearchResultsRenderer", "tabs", 0, "tabRenderer", "content", "sectionListRenderer", "contents") as? JsonArray
        return InnerTubeParser.parseSections(contents).flatMap { it.items }.filter {
            when (filter) {
                SearchFilter.SONGS -> it is SongItem
                SearchFilter.ARTISTS -> it is ArtistItem
                SearchFilter.ALBUMS -> it is AlbumItem && !it.isPlaylist
                SearchFilter.PLAYLISTS -> it is AlbumItem && it.isPlaylist
            }
        }
    }

    /** Saran kata kunci saat mengetik. */
    suspend fun suggestions(query: String): List<String> {
        val d = post("music/get_search_suggestions", JsonObject().apply { addProperty("input", query) })
        val out = mutableListOf<String>()
        d.arr("contents")?.forEach { section ->
            path(section, "searchSuggestionsSectionRenderer", "contents")?.asJsonArray?.forEach { s ->
                path(s, "searchSuggestionRenderer", "suggestion")?.let { out += text(it) }
            }
        }
        return out.filter { it.isNotBlank() }.distinct().take(8)
    }

    // ---------------- Artis ----------------

    suspend fun artist(browseId: String): ArtistPage {
        val d = browse(browseId)
        val header = d.obj("header")?.let { h ->
            // as? JsonObject: field pertama header tidak selalu object (kalau YouTube menambah
            // field baru, cast paksa asJsonObject bikin crash)
            h.obj("musicImmersiveHeaderRenderer") ?: h.obj("musicVisualHeaderRenderer")
                ?: h.entrySet().firstOrNull()?.value as? JsonObject
        }
        val name = text(header.obj("title"))
        val subs = text(path(header, "subscriptionButton", "subscribeButtonRenderer", "subscriberCountText"))
            .takeIf { it.isNotBlank() }
        val description = text(header.obj("description")).takeIf { it.isNotBlank() }
        val thumb = InnerTubeParser.thumbnail(header)
            ?: (path(header, "foregroundThumbnail", "musicThumbnailRenderer", "thumbnail", "thumbnails") as? JsonArray)
                ?.lastOrNull()?.let { InnerTubeParser.upscale(it.asJsonObject.get("url").asString) }

        val sections = InnerTubeParser.parseSections(singleColumnSections(d))
            // Buang video musik & live dari halaman artis supaya fokus ke musik
            .map { s ->
                // Lagu tanpa info artis → isi dengan artis halaman ini
                s.copy(items = s.items.map { item ->
                    if (item is SongItem && item.artists.isEmpty()) item.copy(artists = listOf(ArtistRef(browseId, name))) else item
                })
            }
        return ArtistPage(browseId, name, thumb?.let { bigArtistImage(it) }, subs, description, sections)
    }

    private fun bigArtistImage(url: String): String =
        url.replace(Regex("=w\\d+-h\\d+[^&]*$"), "=w1080-h1080-p-l90-rj")

    // ---------------- Album / Playlist ----------------

    suspend fun collection(browseId: String): CollectionPage {
        val d = browse(browseId)
        val two = path(d, "contents", "twoColumnBrowseResultsRenderer")
        val headerWrap = path(two, "tabs", 0, "tabRenderer", "content", "sectionListRenderer", "contents", 0) as? JsonObject
        val header = headerWrap?.entrySet()?.firstOrNull()?.value as? JsonObject

        val title = text(header.obj("title"))
        val strap = InnerTubeParser.runs(header.obj("straplineTextOne"))
        val author = strap.firstOrNull()?.let { ArtistRef(it.browseId, it.text) }
        val subtitle = text(header.obj("subtitle"))
        val second = text(header.obj("secondSubtitle"))
        val cover = InnerTubeParser.thumbnail(header)
            ?: InnerTubeParser.thumbnail(path(d, "background"))

        val shelf = path(two, "secondaryContents", "sectionListRenderer", "contents", 0) as? JsonObject
        val shelfBody = shelf?.obj("musicShelfRenderer") ?: shelf?.obj("musicPlaylistShelfRenderer")
        val songs = shelfBody.arr("contents")?.mapNotNull { InnerTubeParser.parseItem(it) as? SongItem }.orEmpty()
            .map { s ->
                s.copy(
                    thumbnail = s.thumbnail ?: cover,
                    artists = s.artists.ifEmpty { listOfNotNull(author) },
                    album = s.album ?: if (browseId.startsWith("MPRE")) AlbumRef(browseId, title) else null,
                )
            }
        return CollectionPage(browseId, title, subtitle, second, cover, author, songs)
    }

    // ---------------- Radio / Up next ----------------

    /** Antrian otomatis ala YT Music ("radio") berdasarkan satu lagu. */
    suspend fun radio(videoId: String): List<SongItem> {
        val d = post("next", JsonObject().apply {
            addProperty("videoId", videoId)
            addProperty("playlistId", "RDAMVM$videoId")
            addProperty("isAudioOnly", true)
        })
        val contents = path(
            d, "contents", "singleColumnMusicWatchNextResultsRenderer", "tabbedRenderer",
            "watchNextTabbedResultsRenderer", "tabs", 0, "tabRenderer", "content",
            "musicQueueRenderer", "content", "playlistPanelRenderer", "contents"
        ) as? JsonArray
        return contents?.mapNotNull { InnerTubeParser.parseItem(it) as? SongItem }.orEmpty()
    }

    companion object {
        const val CLIENT_VERSION = "1.20250915.03.00"
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"
    }
}
