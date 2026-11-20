package com.vyora.music.data.ytmusic

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Mengambil URL stream audio FULL dan video klip (MV) dari videoId YouTube (Music)
 * memakai NewPipe Extractor (100% mode Guest, tanpa login akun).
 * BLOCKING — panggil dari background thread.
 */
object StreamResolver {

    private const val TTL_MS = 60 * 60 * 1000L // URL YouTube berlaku ± 6 jam, kita refresh tiap 1 jam
    private const val MAX_CACHE = 120
    private const val MAX_ATTEMPTS = 2
    private const val RETRY_DELAY_MS = 300L

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val cache = ConcurrentHashMap<String, Pair<String, Long>>()

    @Volatile private var initialized = false

    private fun ensureInit() {
        if (initialized) return
        synchronized(this) {
            if (!initialized) {
                NewPipe.init(OkHttpDownloader(httpClient), Localization("id", "ID"), ContentCountry("ID"))
                initialized = true
            }
        }
    }

    /** Buang URL lama agar percobaan ulang mengambil URL stream baru. */
    fun invalidate(videoId: String) {
        cache.keys.filter { it.startsWith("$videoId|") }.forEach { cache.remove(it) }
    }

    private fun cacheKey(videoId: String, quality: AudioQuality) = "$videoId|${quality.name}"
    private fun videoCacheKey(videoId: String) = "$videoId|VIDEO"

    @Throws(IOException::class)
    fun audioUrl(videoId: String, quality: AudioQuality = AudioQuality.HIGH): String {
        val key = cacheKey(videoId, quality)
        cache[key]?.let { (url, at) ->
            if (System.currentTimeMillis() - at < TTL_MS) return url
        }
        ensureInit()

        var lastError: IOException? = null
        for (attempt in 1..MAX_ATTEMPTS) {
            try {
                val url = resolveAudioOnce(videoId, quality)
                if (cache.size >= MAX_CACHE) cache.clear()
                cache[key] = url to System.currentTimeMillis()
                return url
            } catch (e: IOException) {
                lastError = e
                cache.remove(key)
            } catch (e: Exception) {
                lastError = IOException(e.message ?: "Gagal mengambil audio", e)
                cache.remove(key)
            }
            if (attempt < MAX_ATTEMPTS) {
                runCatching { Thread.sleep(RETRY_DELAY_MS * attempt) }
            }
        }
        throw lastError ?: IOException("Audio tidak tersedia untuk $videoId")
    }

    /**
     * Mengambil URL stream Video Musik (muxed MP4 video + audio, 360p–720p)
     * agar pengguna bisa beralih Audio ⇄ Video di layar pemutar tanpa login.
     */
    @Throws(IOException::class)
    fun videoUrl(videoId: String): String {
        val key = videoCacheKey(videoId)
        cache[key]?.let { (url, at) ->
            if (System.currentTimeMillis() - at < TTL_MS) return url
        }
        ensureInit()

        var lastError: IOException? = null
        for (attempt in 1..MAX_ATTEMPTS) {
            try {
                val url = resolveVideoOnce(videoId)
                if (cache.size >= MAX_CACHE) cache.clear()
                cache[key] = url to System.currentTimeMillis()
                return url
            } catch (e: IOException) {
                lastError = e
                cache.remove(key)
            } catch (e: Exception) {
                lastError = IOException(e.message ?: "Video tidak tersedia untuk lagu ini", e)
                cache.remove(key)
            }
            if (attempt < MAX_ATTEMPTS) {
                runCatching { Thread.sleep(RETRY_DELAY_MS * attempt) }
            }
        }
        throw lastError ?: IOException("Video klip tidak tersedia untuk lagu ini")
    }

    private fun resolveAudioOnce(videoId: String, quality: AudioQuality): String {
        val info = StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=$videoId")
        val candidates = info.audioStreams.mapNotNull { stream ->
            val url = stream.content
            if (url.isNullOrBlank()) {
                null
            } else {
                AudioCandidate(
                    url = url,
                    bitrateKbps = stream.averageBitrate.takeIf { it > 0 } ?: 0,
                    mimeType = stream.format?.mimeType,
                    isProgressive = stream.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP,
                )
            }
        }
        return AudioStreamSelector.pick(candidates, quality)?.url
            ?: throw IOException("Audio tidak tersedia untuk $videoId")
    }

    private fun resolveVideoOnce(videoId: String): String {
        val info = StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=$videoId")
        val muxed = info.videoStreams
            .filter { !it.content.isNullOrBlank() }
            .sortedWith(
                compareByDescending<org.schabi.newpipe.extractor.stream.VideoStream> {
                    it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP
                }.thenByDescending {
                    val h = it.height
                    when {
                        h in 480..720 -> 1000 + h
                        h in 360..479 -> 800 + h
                        h in 1..1080 -> h
                        else -> 0
                    }
                }
            )
        return muxed.firstOrNull()?.content
            ?: throw IOException("Video klip tidak tersedia untuk lagu ini")
    }

    private class OkHttpDownloader(private val client: OkHttpClient) : Downloader() {
        override fun execute(request: Request): Response {
            val builder = okhttp3.Request.Builder()
                .url(request.url())
                .method(request.httpMethod(), request.dataToSend()?.toRequestBody())
                .header("User-Agent", YTMusic.USER_AGENT)
            request.headers().forEach { (name, values) ->
                builder.removeHeader(name)
                values.forEach { builder.addHeader(name, it) }
            }
            client.newCall(builder.build()).execute().use { resp ->
                return Response(resp.code, resp.message, resp.headers.toMultimap(), resp.body?.string(), resp.request.url.toString())
            }
        }
    }
}
