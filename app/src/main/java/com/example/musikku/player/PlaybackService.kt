package com.example.musikku.player

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
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
import com.example.musikku.MainActivity
import com.example.musikku.data.ytmusic.StreamResolver
import com.example.musikku.data.ytmusic.YTMusic
import java.util.concurrent.Executors

/**
 * Service pemutar musik. Berjalan di background, otomatis menampilkan
 * notifikasi media + kontrol di lockscreen berkat MediaSessionService.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val snapshotHandler = Handler(Looper.getMainLooper())
    private lateinit var snapshot: PlaybackSnapshot
    private val saveTick = object : Runnable {
        override fun run() {
            mediaSession?.player?.let { if (it.isPlaying) snapshot.save(it) }
            snapshotHandler.postDelayed(this, 5_000L)
        }
    }
    private val prefetchExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate() {
        super.onCreate()

        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(YTMusic.USER_AGENT)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)

        // Media item memakai URI "ytm://song/{videoId}". Tepat sebelum diputar,
        // URI ini di-resolve menjadi URL audio full dari YouTube.
        val resolvingFactory = ResolvingDataSource.Factory(httpFactory) { dataSpec -> resolve(dataSpec) }

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

        snapshot = PlaybackSnapshot(this)
        snapshot.restore(player)
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.containsAny(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION,
                        Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_PLAY_WHEN_READY_CHANGED,
                        Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED, Player.EVENT_REPEAT_MODE_CHANGED)) {
                    snapshot.save(player)
                }
            }
        })
        SleepTimer.attach(player)

        // Siapkan URL lagu berikutnya lebih awal supaya perpindahan lagu mulus
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val next = player.nextMediaItemIndex
                if (next != C.INDEX_UNSET) prefetch(player.getMediaItemAt(next))
            }
        })

        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openAppIntent)
            .build()
        snapshotHandler.postDelayed(saveTick, 5_000L)
    }

    private fun resolve(dataSpec: DataSpec): DataSpec {
        val uri = dataSpec.uri
        if (uri.scheme != PlayerUris.SCHEME) return dataSpec
        val videoId = uri.lastPathSegment ?: return dataSpec
        return dataSpec.withUri(Uri.parse(StreamResolver.audioUrl(videoId)))
    }

    private fun prefetch(item: MediaItem) {
        val uri = item.localConfiguration?.uri ?: return
        prefetchExecutor.execute { runCatching { resolve(DataSpec(uri)) } }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        snapshotHandler.removeCallbacks(saveTick)
        SleepTimer.detach()
        mediaSession?.run {
            snapshot.save(player)
            player.release()
            release()
        }
        mediaSession = null
        prefetchExecutor.shutdownNow()
        super.onDestroy()
    }
}

object PlayerUris {
    const val SCHEME = "ytm"
    fun forSong(videoId: String): Uri = Uri.parse("$SCHEME://song/$videoId")
}
