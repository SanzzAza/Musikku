package com.example.musikku.data.ytmusic

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
 * Mengambil URL stream audio FULL dari sebuah videoId YouTube (Music)
 * memakai NewPipe Extractor. BLOCKING — panggil dari background thread.
 */
object StreamResolver {

    private const val TTL_MS = 60 * 60 * 1000L // URL YouTube berlaku ± 6 jam, kita refresh tiap 1 jam
    private const val MAX_CACHE = 100

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val cache = ConcurrentHashMap<String, Pair<String, Long>>()

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

    fun audioUrl(videoId: String): String {
        cache[videoId]?.let { (url, at) -> if (System.currentTimeMillis() - at < TTL_MS) return url }
        ensureInit()
        val info = StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=$videoId")
        val url = info.audioStreams
            .filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.isUrl }
            .maxByOrNull { it.averageBitrate }
            ?.content
            ?: throw IOException("Audio tidak tersedia untuk $videoId")
        // Batasi ukuran cache: dipakai berlama-lama, cache tanpa batas memakan memori
        if (cache.size >= MAX_CACHE) cache.clear()
        cache[videoId] = url to System.currentTimeMillis()
        return url
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
