package com.example.musikku.player

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
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
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Service pemutar musik. Berjalan di background, otomatis menampilkan
 * notifikasi media + kontrol di lockscreen berkat MediaSessionService.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    /** Cache URL preview: trackId -> (url, waktu diambil). */
    private val previewCache = ConcurrentHashMap<Long, Pair<String, Long>>()

    override fun onCreate() {
        super.onCreate()

        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)

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

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openAppIntent)
            .build()
    }

    private fun resolve(dataSpec: DataSpec): DataSpec {
        val uri = dataSpec.uri
        if (uri.scheme != PlayerUris.SCHEME) return dataSpec
        val id = uri.lastPathSegment?.toLongOrNull() ?: return dataSpec

        val now = SystemClock.elapsedRealtime()
        val cached = previewCache[id]
        val url = if (cached != null && now - cached.second < CACHE_TTL_MS) {
            cached.first
        } else {
            val fresh = AppModule.repository.freshPreviewUrlBlocking(id)
                ?: throw IOException("Preview lagu $id tidak tersedia")
            previewCache[id] = fresh to now
            fresh
        }
        return dataSpec.withUri(Uri.parse(url))
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
        super.onDestroy()
    }

    private companion object {
        const val CACHE_TTL_MS = 20 * 60 * 1000L
    }
}

object PlayerUris {
    const val SCHEME = "deezer"
    fun forTrack(id: Long): Uri = Uri.parse("$SCHEME://track/$id")
}
