package com.example.musikku.player

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.musikku.AppModule
import com.example.musikku.MainActivity
import com.example.musikku.data.youtube.YouTubeAudioResolver
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Service pemutar musik. Berjalan di background, otomatis menampilkan
 * notifikasi media + kontrol di lockscreen berkat MediaSessionService.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    /** Cache URL preview: trackId -> (url, waktu diambil). */
    private val previewCache = ConcurrentHashMap<Long, Pair<String, Long>>()
    private val prefetchExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate() {
        super.onCreate()

        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0")
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)

        // Media item memakai URI "deezer://track/{id}". Sebelum diputar, URI ini
        // di-resolve ke URL preview terbaru, karena URL preview Deezer cepat kedaluwarsa.
        val resolvingFactory = ResolvingDataSource.Factory(httpFactory) { dataSpec ->
            resolve(dataSpec)
        }

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(resolvingFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true) // pause saat headset dicabut
            .build()

        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Siapkan URL lagu berikutnya lebih awal supaya perpindahan lagu mulus
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val next = player.nextMediaItemIndex
                if (next != C.INDEX_UNSET) prefetch(player.getMediaItemAt(next))
            }
        })

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openAppIntent)
            .build()
    }

    /**
     * URI "deezer://track/{id}?title=..&artist=..&dur=.." di-resolve jadi URL audio:
     *  1. Audio FULL dari YouTube Music (dicocokkan lewat judul, artis & durasi)
     *  2. Kalau gagal → fallback ke preview 30 detik Deezer
     */
    private fun resolve(dataSpec: DataSpec): DataSpec {
        val uri = dataSpec.uri
        if (uri.scheme != PlayerUris.SCHEME) return dataSpec
        val id = uri.lastPathSegment?.toLongOrNull() ?: return dataSpec

        val now = SystemClock.elapsedRealtime()
        val cached = previewCache[id]
        val url = if (cached != null && now - cached.second < CACHE_TTL_MS) {
            cached.first
        } else {
            val fullUrl = YouTubeAudioResolver.resolve(
                trackId = id,
                title = uri.getQueryParameter("title").orEmpty(),
                artist = uri.getQueryParameter("artist").orEmpty(),
                durationSec = uri.getQueryParameter("dur")?.toIntOrNull() ?: 0,
            )
            val resolved = fullUrl
                ?: AppModule.repository.freshPreviewUrlBlocking(id)
                ?: throw IOException("Lagu $id tidak bisa diputar")
            previewCache[id] = resolved to now
            resolved
        }
        return dataSpec.withUri(Uri.parse(url))
    }

    private fun prefetch(item: MediaItem) {
        val uri = item.localConfiguration?.uri ?: return
        prefetchExecutor.execute {
            runCatching { resolve(DataSpec(uri)) }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        prefetchExecutor.shutdownNow()
        super.onDestroy()
    }

    private companion object {
        const val CACHE_TTL_MS = 45 * 60 * 1000L
    }
}

object PlayerUris {
    const val SCHEME = "deezer"
    fun forTrack(id: Long, title: String?, artist: String?, durationSec: Int): Uri =
        Uri.Builder()
            .scheme(SCHEME)
            .authority("track")
            .appendPath(id.toString())
            .appendQueryParameter("title", title.orEmpty())
            .appendQueryParameter("artist", artist.orEmpty())
            .appendQueryParameter("dur", durationSec.toString())
            .build()
}
