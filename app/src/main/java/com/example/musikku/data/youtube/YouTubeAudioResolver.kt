package com.example.musikku.data.youtube

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Mencari lagu yang sama di YouTube Music lalu mengambil URL stream audio FULL-nya.
 * Memakai NewPipe Extractor (library yang juga dipakai NewPipe / ViMusic / InnerTune).
 *
 * Semua fungsi di sini BLOCKING, jadi harus dipanggil dari background thread
 * (di aplikasi ini dipanggil dari thread loader ExoPlayer).
 */
object YouTubeAudioResolver {

    private const val TAG = "YouTubeAudio"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"
    private const val STREAM_TTL_MS = 60 * 60 * 1000L // URL stream YouTube berlaku ± 6 jam

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /** trackId Deezer -> URL video YouTube (permanen selama app hidup). */
    private val videoCache = ConcurrentHashMap<Long, String>()

    /** trackId Deezer -> (URL stream audio, waktu diambil). */
    private val streamCache = ConcurrentHashMap<Long, Pair<String, Long>>()

    @Volatile private var initialized = false

    private fun ensureInit() {
        if (initialized) return
        synchronized(this) {
            if (!initialized) {
                NewPipe.init(OkHttpDownloader(client), Localization("id", "ID"), ContentCountry("ID"))
                initialized = true
            }
        }
    }

    /**
     * @return URL audio full, atau null kalau tidak ketemu.
     */
    fun resolve(trackId: Long, title: String, artist: String, durationSec: Int): String? {
        streamCache[trackId]?.let { (url, at) ->
            if (System.currentTimeMillis() - at < STREAM_TTL_MS) return url
        }
        return try {
            ensureInit()
            val videoUrl = videoCache[trackId]
                ?: findVideo(title, artist, durationSec)?.also { videoCache[trackId] = it }
                ?: return null
            val streamUrl = bestAudioUrl(videoUrl) ?: return null
            streamCache[trackId] = streamUrl to System.currentTimeMillis()
            streamUrl
        } catch (e: Exception) {
            Log.w(TAG, "Gagal resolve '$artist - $title'", e)
            null
        }
    }

    /** Cari di kategori "Songs" YouTube Music dulu (audio resmi), kalau kosong cari di video biasa. */
    private fun findVideo(title: String, artist: String, durationSec: Int): String? {
        val query = "$artist $title"
        val filters = listOf(
            YoutubeSearchQueryHandlerFactory.MUSIC_SONGS,
            YoutubeSearchQueryHandlerFactory.VIDEOS,
        )
        for (filter in filters) {
            val extractor = ServiceList.YouTube.getSearchExtractor(query, listOf(filter), "")
            extractor.fetchPage()
            val candidates = extractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .take(10)
            if (candidates.isEmpty()) continue

            val best = candidates.minBy { score(it, title, artist, durationSec) }
            Log.d(TAG, "Match '$query' -> '${best.name}' by ${best.uploaderName} (${best.duration}s)")
            return best.url
        }
        return null
    }

    /** Semakin kecil semakin cocok: durasi mirip, judul & nama artis sama. */
    private fun score(item: StreamInfoItem, title: String, artist: String, durationSec: Int): Long {
        var score = 0L
        score += if (durationSec > 0 && item.duration > 0) abs(item.duration - durationSec) else 30
        if (!normalize(item.name).contains(normalize(title).take(20))) score += 25
        val artistKey = normalize(artist).split(" ").firstOrNull().orEmpty()
        if (artistKey.isNotEmpty() && !normalize(item.uploaderName.orEmpty()).contains(artistKey)) score += 20
        val name = item.name.lowercase(Locale.ROOT)
        if (listOf("cover", "karaoke", "remix", "live", "8d").any { it in name } &&
            listOf("cover", "karaoke", "remix", "live", "8d").none { it in title.lowercase(Locale.ROOT) }
        ) score += 40
        return score
    }

    private fun normalize(s: String): String =
        s.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N} ]"), " ").replace(Regex("\\s+"), " ").trim()

    /** Ambil stream audio dengan bitrate tertinggi (biasanya Opus 160kbps / M4A 128kbps). */
    private fun bestAudioUrl(videoUrl: String): String? {
        val info = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)
        return info.audioStreams
            .filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.isUrl }
            .maxByOrNull { it.averageBitrate }
            ?.content
    }

    /** Implementasi Downloader untuk NewPipe memakai OkHttp. */
    private class OkHttpDownloader(private val client: OkHttpClient) : Downloader() {
        override fun execute(request: Request): Response {
            val body = request.dataToSend()?.toRequestBody()
            val builder = okhttp3.Request.Builder()
                .url(request.url())
                .method(request.httpMethod(), body)
                .header("User-Agent", USER_AGENT)
            request.headers().forEach { (name, values) ->
                builder.removeHeader(name)
                values.forEach { builder.addHeader(name, it) }
            }
            client.newCall(builder.build()).execute().use { resp ->
                return Response(
                    resp.code,
                    resp.message,
                    resp.headers.toMultimap(),
                    resp.body?.string(),
                    resp.request.url.toString()
                )
            }
        }
    }
}
