package com.vyora.music.data.lyrics

import android.util.Base64
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/** Satu kata dengan waktu mulai & selesai (ms) untuk lirik karaoke per-kata ala Apple Music / Auralys. */
data class LyricWord(
    val text: String,
    val startMs: Long,
    val endMs: Long,
)

/** Satu baris lirik dengan waktu mulai (ms) dan daftar kata opsional untuk highlight per-kata. */
data class LyricLine(
    val timeMs: Long,
    val text: String,
    val endTimeMs: Long = 0L,
    val words: List<LyricWord> = emptyList(),
)

sealed interface Lyrics {
    /** Lirik yang tersinkron dengan waktu lagu (mendukung per-baris & karaoke per-kata). */
    data class Synced(
        val lines: List<LyricLine>,
        val source: String = "LRCLIB",
        val hasWordSync: Boolean = lines.any { it.words.isNotEmpty() },
    ) : Lyrics

    /** Lirik biasa tanpa timing. */
    data class Plain(
        val text: String,
        val source: String = "LRCLIB",
    ) : Lyrics

    data object Instrumental : Lyrics
    data object NotFound : Lyrics
}

data class LrcLibResult(
    val id: Long = 0,
    val trackName: String? = null,
    val artistName: String? = null,
    val duration: Double = 0.0,
    val instrumental: Boolean = false,
    @SerializedName("plainLyrics") val plainLyrics: String? = null,
    @SerializedName("syncedLyrics") val syncedLyrics: String? = null,
)

/** LRCLIB — database lirik gratis & terbuka: https://lrclib.net */
interface LrcLibApi {
    @GET("api/get")
    suspend fun get(
        @Query("artist_name") artist: String,
        @Query("track_name") track: String,
        @Query("album_name") album: String?,
        @Query("duration") duration: Int?,
    ): LrcLibResult

    @GET("api/search")
    suspend fun search(
        @Query("track_name") track: String,
        @Query("artist_name") artist: String,
    ): List<LrcLibResult>
}

/**
 * Repositori Lirik Multi-Sumber VYORA (100% Guest, Tanpa Login):
 * 1. Disk Offline Cache (`LyricsCacheStore`) — instan 0ms & jalan saat offline
 * 2. BetterLyrics (`lyrics-api.boidu.dev`) — lirik TTML karaoke per-kata (word-by-word)
 * 3. LRCLIB (`lrclib.net`) — lirik LRC tersinkron (termasuk enhanced LRC `<mm:ss.xx>`) & plain
 * 4. KuGou (`kugou.com`) — fallback katalog Asia / Indonesia / Jepang / Korea / Barat
 */
class LyricsRepository(
    private val diskCache: LyricsCacheStore? = null,
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(16, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "VYORA/1.1 (pemutar musik pribadi)")
                    .build()
            )
        }
        .build()

    private val api: LrcLibApi = Retrofit.Builder()
        .baseUrl("https://lrclib.net/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(LrcLibApi::class.java)

    private val cache = ConcurrentHashMap<String, Lyrics>()

    suspend fun getLyrics(
        trackId: String,
        title: String,
        artist: String,
        album: String?,
        durationSec: Int,
    ): Lyrics {
        cache[trackId]?.let { return it }
        return withContext(Dispatchers.IO) {
            diskCache?.get(trackId)?.let { cached ->
                cache[trackId] = cached
                return@withContext cached
            }

            val fetched = fetchMultiSource(title, artist, album, durationSec)
            if (fetched != Lyrics.NotFound) {
                cache[trackId] = fetched
                diskCache?.put(trackId, fetched)
            }
            fetched
        }
    }

    private suspend fun fetchMultiSource(
        title: String,
        artist: String,
        album: String?,
        durationSec: Int,
    ): Lyrics {
        val cleanTitle = cleanTrackTitle(title)

        // 1. Coba BetterLyrics dulu untuk mendapatkan Karaoke Per-Kata (TTML)
        fetchBetterLyrics(cleanTitle.ifBlank { title }, artist, album, durationSec)?.let { return it }

        // 2. Coba LRCLIB (exact lalu search)
        val lrcLib = runCatching { fetchLrcLib(title, cleanTitle, artist, album, durationSec) }.getOrNull()
        if (lrcLib is Lyrics.Synced || lrcLib is Lyrics.Instrumental) return lrcLib

        // 3. Coba KuGou untuk lagu yang belum ada synced-nya di LRCLIB
        fetchKuGouLyrics(cleanTitle.ifBlank { title }, artist, durationSec)?.let { return it }

        // 4. Bila LRCLIB punya lirik Plain (tanpa waktu), pakai itu sebelum menyerah
        if (lrcLib is Lyrics.Plain) return lrcLib

        return Lyrics.NotFound
    }

    private fun fetchBetterLyrics(
        title: String,
        artist: String,
        album: String?,
        durationSec: Int,
    ): Lyrics.Synced? = runCatching {
        val urlBuilder = "https://lyrics-api.boidu.dev/getLyrics".toHttpUrl().newBuilder()
            .addQueryParameter("s", title)
            .addQueryParameter("a", artist)
        if (!album.isNullOrBlank()) urlBuilder.addQueryParameter("al", album)
        if (durationSec > 0) urlBuilder.addQueryParameter("d", durationSec.toString())

        val req = Request.Builder().url(urlBuilder.build()).build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            val ttml = runCatching {
                val json = JsonParser.parseString(body)
                if (json.isJsonObject) json.asJsonObject.get("ttml")?.takeIf { !it.isJsonNull }?.asString else body
            }.getOrNull() ?: return null

            val lines = parseTtml(ttml)
            if (lines.isEmpty()) return null
            val hasWords = lines.any { it.words.size > 1 }
            Lyrics.Synced(
                lines = lines,
                source = if (hasWords) "BetterLyrics • Karaoke Kata" else "BetterLyrics",
                hasWordSync = hasWords,
            )
        }
    }.getOrNull()

    private suspend fun fetchLrcLib(
        rawTitle: String,
        cleanTitle: String,
        artist: String,
        album: String?,
        durationSec: Int,
    ): Lyrics? {
        val exact = try {
            api.get(artist, rawTitle, album, durationSec.takeIf { it > 0 })
        } catch (e: HttpException) {
            if (e.code() == 404) null else throw e
        }
        exact?.toLyrics()?.let { return it }

        val results = api.search(cleanTitle.ifBlank { rawTitle }, artist)
        val artistKey = artist.lowercase(Locale.ROOT)
        val best = results
            .filter { !it.syncedLyrics.isNullOrBlank() || !it.plainLyrics.isNullOrBlank() || it.instrumental }
            .minByOrNull { r ->
                var score = if (durationSec > 0) abs(r.duration - durationSec) else 0.0
                if (r.artistName?.lowercase(Locale.ROOT)?.contains(artistKey) != true) score += 60
                if (r.syncedLyrics.isNullOrBlank()) score += 5
                score
            }
        if (best != null && (durationSec <= 0 || abs(best.duration - durationSec) <= 15)) {
            best.toLyrics()?.let { return it }
        }
        return null
    }

    private fun fetchKuGouLyrics(title: String, artist: String, durationSec: Int): Lyrics.Synced? = runCatching {
        val keyword = listOf(artist, title).filter { it.isNotBlank() }.joinToString(" - ")
        val searchUrl = "https://mobileservice.kugou.com/api/v3/search/lyric".toHttpUrl().newBuilder()
            .addQueryParameter("version", "9108")
            .addQueryParameter("plat", "0")
            .addQueryParameter("pagesize", "5")
            .addQueryParameter("showtype", "1")
            .addQueryParameter("keyword", keyword)
            .apply {
                if (durationSec > 0) addQueryParameter("duration", (durationSec * 1000).toString())
            }
            .build()

        val candidate = httpClient.newCall(Request.Builder().url(searchUrl).build()).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val root = JsonParser.parseString(resp.body?.string() ?: return null)
            val infoArr = root.takeIf { it.isJsonObject }
                ?.asJsonObject?.getAsJsonObject("data")
                ?.getAsJsonArray("info") ?: return null
            if (infoArr.size() == 0) return null
            val first = infoArr[0].asJsonObject
            val id = first.get("id")?.asString ?: return null
            val accessKey = first.get("accesskey")?.asString ?: return null
            id to accessKey
        }

        val dlUrl = "https://krcs.kugou.com/download".toHttpUrl().newBuilder()
            .addQueryParameter("ver", "1")
            .addQueryParameter("client", "pc")
            .addQueryParameter("id", candidate.first)
            .addQueryParameter("accesskey", candidate.second)
            .addQueryParameter("fmt", "lrc")
            .addQueryParameter("charset", "utf8")
            .build()

        httpClient.newCall(Request.Builder().url(dlUrl).build()).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val root = JsonParser.parseString(resp.body?.string() ?: return null)
            val b64 = root.takeIf { it.isJsonObject }?.asJsonObject?.get("content")?.asString ?: return null
            val decoded = String(Base64.decode(b64, Base64.DEFAULT), Charsets.UTF_8)
            val lines = parseLrc(decoded)
            if (lines.isEmpty()) return null
            Lyrics.Synced(lines = lines, source = "KuGou")
        }
    }.getOrNull()

    private fun LrcLibResult.toLyrics(): Lyrics? = when {
        !syncedLyrics.isNullOrBlank() -> parseLrc(syncedLyrics).takeIf { it.isNotEmpty() }?.let {
            val hasWords = it.any { line -> line.words.size > 1 }
            Lyrics.Synced(it, source = if (hasWords) "LRCLIB • Karaoke Kata" else "LRCLIB", hasWordSync = hasWords)
        } ?: plainLyrics?.let { Lyrics.Plain(it, "LRCLIB") }
        !plainLyrics.isNullOrBlank() -> Lyrics.Plain(plainLyrics, "LRCLIB")
        instrumental -> Lyrics.Instrumental
        else -> null
    }

    companion object {
        private val TIME_TAG = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?]")
        private val WORD_TAG = Regex("<(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?>([^<]*)")
        private val P_TAG_REGEX = Regex("<p\\b([^>]*)>(.*?)</p>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        private val SPAN_TAG_REGEX = Regex("<span\\b([^>]*)>(.*?)</span>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        private val ATTR_BEGIN = Regex("\\bbegin\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
        private val ATTR_END = Regex("\\bend\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
        private val XML_TAG_STRIP = Regex("<[^>]+>")

        internal fun cleanTrackTitle(title: String): String =
            title.replace(Regex("\\s*[(\\[].*?[)\\]]"), "")
                .replace(Regex("\\s+-\\s+.*$"), "")
                .trim()

        private fun parseClockMs(minStr: String, secStr: String, fracStr: String): Long {
            val min = minStr.toLongOrNull() ?: 0L
            val sec = secStr.toLongOrNull() ?: 0L
            val ms = when (fracStr.length) {
                0 -> 0L
                1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                else -> fracStr.take(3).toLongOrNull() ?: 0L
            }
            return min * 60_000L + sec * 1000L + ms
        }

        /**
         * Parse waktu TTML: mendukung "01:23.456", "00:01:23.45", atau "83.45s" / "1250ms".
         */
        fun parseTtmlTimeMs(raw: String?): Long? {
            val s = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            if (s.endsWith("ms", ignoreCase = true)) {
                return s.dropLast(2).trim().toDoubleOrNull()?.toLong()
            }
            if (s.endsWith("s", ignoreCase = true)) {
                return s.dropLast(1).trim().toDoubleOrNull()?.let { (it * 1000.0).toLong() }
            }
            val parts = s.split(":")
            return when (parts.size) {
                3 -> {
                    val h = parts[0].toLongOrNull() ?: return null
                    val m = parts[1].toLongOrNull() ?: return null
                    val secMillis = (parts[2].toDoubleOrNull() ?: return null) * 1000.0
                    h * 3_600_000L + m * 60_000L + secMillis.toLong()
                }
                2 -> {
                    val m = parts[0].toLongOrNull() ?: return null
                    val secMillis = (parts[1].toDoubleOrNull() ?: return null) * 1000.0
                    m * 60_000L + secMillis.toLong()
                }
                1 -> s.toDoubleOrNull()?.let { (it * 1000.0).toLong() }
                else -> null
            }
        }

        private fun unescapeXml(s: String): String = s
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")

        /**
         * Murni Kotlin (mudah diuji di JVM tanpa Android runtime):
         * Mem-parse lirik TTML `<p begin="..." end="..."><span begin="..." end="...">kata</span></p>`
         * menjadi daftar [LyricLine] lengkap dengan timing karaoke per-kata [LyricWord].
         */
        fun parseTtml(ttml: String): List<LyricLine> {
            val out = ArrayList<LyricLine>()
            for (pMatch in P_TAG_REGEX.findAll(ttml)) {
                val pAttrs = pMatch.groupValues[1]
                val inner = pMatch.groupValues[2]
                val pBegin = parseTtmlTimeMs(ATTR_BEGIN.find(pAttrs)?.groupValues?.get(1))
                val pEnd = parseTtmlTimeMs(ATTR_END.find(pAttrs)?.groupValues?.get(1)) ?: 0L

                val words = ArrayList<LyricWord>()
                var cursor = 0
                for (spanMatch in SPAN_TAG_REGEX.findAll(inner)) {
                    val sAttrs = spanMatch.groupValues[1]
                    // Lewati span metadata seperti ttm:role="x-bg" / x-translation bila tidak punya begin
                    val wBegin = parseTtmlTimeMs(ATTR_BEGIN.find(sAttrs)?.groupValues?.get(1))
                    val wEnd = parseTtmlTimeMs(ATTR_END.find(sAttrs)?.groupValues?.get(1))
                    if (wBegin != null) {
                        val rawWord = unescapeXml(XML_TAG_STRIP.replace(spanMatch.groupValues[2], ""))
                        if (rawWord.isNotEmpty()) {
                            val nextSpanStart = inner.indexOf("<span", spanMatch.range.last + 1, ignoreCase = true)
                            val needsSpace = !rawWord.last().isWhitespace() &&
                                nextSpanStart > spanMatch.range.last &&
                                inner.substring(spanMatch.range.last + 1, nextSpanStart).any { it.isWhitespace() }
                            words.add(
                                LyricWord(
                                    text = if (needsSpace) "$rawWord " else rawWord,
                                    startMs = wBegin,
                                    endMs = wEnd ?: (wBegin + 320L),
                                )
                            )
                        }
                    }
                    cursor = spanMatch.range.last + 1
                }

                val fullText = if (words.isNotEmpty()) {
                    words.joinToString("") { it.text }.replace(Regex("\\s+"), " ").trim()
                } else {
                    unescapeXml(XML_TAG_STRIP.replace(inner, "")).replace(Regex("\\s+"), " ").trim()
                }

                val lineStart = pBegin ?: words.firstOrNull()?.startMs ?: continue
                if (fullText.isNotEmpty() || words.isNotEmpty()) {
                    out.add(
                        LyricLine(
                            timeMs = lineStart,
                            text = fullText,
                            endTimeMs = pEnd.takeIf { it > lineStart } ?: (words.lastOrNull()?.endMs ?: 0L),
                            words = words,
                        )
                    )
                }
            }
            return out.sortedBy { it.timeMs }
        }

        /** Parse format LRC & Enhanced LRC: "[01:23.45] <01:23.45>Aku <01:23.90>pulang" */
        fun parseLrc(lrc: String): List<LyricLine> = lrc.lineSequence().flatMap { line ->
            val tags = TIME_TAG.findAll(line).toList()
            if (tags.isEmpty()) return@flatMap emptySequence()
            val rawBody = line.substring(tags.last().range.last + 1).trim()

            // Cek apakah baris memiliki Enhanced LRC word tags (<mm:ss.xx>kata)
            val wordMatches = WORD_TAG.findAll(rawBody).toList()
            val parsedWords = if (wordMatches.size >= 2) {
                wordMatches.mapIndexedNotNull { idx, wm ->
                    val start = parseClockMs(wm.groupValues[1], wm.groupValues[2], wm.groupValues[3])
                    val nextStart = wordMatches.getOrNull(idx + 1)?.let {
                        parseClockMs(it.groupValues[1], it.groupValues[2], it.groupValues[3])
                    } ?: (start + 400L)
                    val wText = wm.groupValues[4]
                    if (wText.isBlank()) null else LyricWord(wText, start, nextStart.coerceAtLeast(start + 80L))
                }
            } else {
                emptyList()
            }

            val cleanText = if (wordMatches.isNotEmpty()) {
                rawBody.replace(Regex("<\\d{1,2}:\\d{2}(?:[.:]\\d{1,3})?>"), "").replace(Regex("\\s+"), " ").trim()
            } else {
                rawBody
            }

            tags.asSequence().map { m ->
                val lineMs = parseClockMs(m.groupValues[1], m.groupValues[2], m.groupValues[3])
                LyricLine(
                    timeMs = lineMs,
                    text = cleanText,
                    endTimeMs = parsedWords.lastOrNull()?.endMs ?: 0L,
                    words = if (tags.size == 1) parsedWords else emptyList(),
                )
            }
        }.sortedBy { it.timeMs }.toList()
    }
}
