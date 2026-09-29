package com.example.musikku.data.lyrics

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/** Satu baris lirik dengan waktu mulai (ms). */
data class LyricLine(val timeMs: Long, val text: String)

sealed interface Lyrics {
    /** Lirik yang tersinkron dengan waktu lagu (bisa auto-scroll & highlight). */
    data class Synced(val lines: List<LyricLine>) : Lyrics
    /** Lirik biasa tanpa timing. */
    data class Plain(val text: String) : Lyrics
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

class LyricsRepository {

    private val api: LrcLibApi = Retrofit.Builder()
        .baseUrl("https://lrclib.net/")
        .client(
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", "Musikku/1.0 (https://github.com/SanzzAza/Musikku)")
                            .build()
                    )
                }
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(LrcLibApi::class.java)

    private val cache = ConcurrentHashMap<String, Lyrics>()

    suspend fun getLyrics(trackId: String, title: String, artist: String, album: String?, durationSec: Int): Lyrics =
        cache[trackId] ?: withContext(Dispatchers.IO) {
            fetch(title, artist, album, durationSec).also { cache[trackId] = it }
        }

    private suspend fun fetch(title: String, artist: String, album: String?, durationSec: Int): Lyrics {
        // 1. Pencarian tepat (judul + artis + album + durasi)
        val exact = try {
            api.get(artist, title, album, durationSec.takeIf { it > 0 })
        } catch (e: HttpException) {
            if (e.code() == 404) null else throw e
        }
        exact?.toLyrics()?.let { return it }

        // 2. Pencarian longgar pakai judul yang dibersihkan: "Lagu (feat. X)" -> "Lagu"
        val cleanTitle = title.replace(Regex("\\s*[(\\[].*?[)\\]]"), "").replace(Regex("\\s+-\\s+.*$"), "").trim()
        val results = api.search(cleanTitle.ifBlank { title }, artist)
        val artistKey = artist.lowercase(Locale.ROOT)
        val best = results
            .filter { !it.syncedLyrics.isNullOrBlank() || !it.plainLyrics.isNullOrBlank() || it.instrumental }
            .minByOrNull { r ->
                var score = if (durationSec > 0) abs(r.duration - durationSec) else 0.0
                if (r.artistName?.lowercase(Locale.ROOT)?.contains(artistKey) != true) score += 60
                if (r.syncedLyrics.isNullOrBlank()) score += 5
                score
            }
        // Jangan pakai hasil yang durasinya beda jauh (kemungkinan lagu lain)
        if (best != null && (durationSec <= 0 || abs(best.duration - durationSec) <= 15)) {
            best.toLyrics()?.let { return it }
        }
        return Lyrics.NotFound
    }

    private fun LrcLibResult.toLyrics(): Lyrics? = when {
        !syncedLyrics.isNullOrBlank() -> parseLrc(syncedLyrics).takeIf { it.isNotEmpty() }?.let { Lyrics.Synced(it) }
            ?: plainLyrics?.let { Lyrics.Plain(it) }
        !plainLyrics.isNullOrBlank() -> Lyrics.Plain(plainLyrics)
        instrumental -> Lyrics.Instrumental
        else -> null
    }

    companion object {
        private val TIME_TAG = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?]")

        /** Parse format LRC: "[01:23.45] teks lirik" */
        fun parseLrc(lrc: String): List<LyricLine> = lrc.lineSequence().flatMap { line ->
            val tags = TIME_TAG.findAll(line).toList()
            if (tags.isEmpty()) return@flatMap emptySequence()
            val text = line.substring(tags.last().range.last + 1).trim()
            tags.asSequence().map { m ->
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val ms = when (frac.length) {
                    0 -> 0L
                    1 -> frac.toLong() * 100
                    2 -> frac.toLong() * 10
                    else -> frac.take(3).toLong()
                }
                LyricLine(min * 60_000 + sec * 1000 + ms, text)
            }
        }.sortedBy { it.timeMs }.toList()
    }
}
