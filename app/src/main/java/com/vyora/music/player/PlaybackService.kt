package com.vyora.music.player

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.vyora.music.AppModule
import com.vyora.music.MainActivity
import com.vyora.music.data.offline.StreamCacheHolder
import com.vyora.music.data.ytmusic.AudioQuality
import com.vyora.music.data.ytmusic.StreamResolver
import com.vyora.music.data.ytmusic.YTMusic
import com.vyora.music.widget.VyoraMusicWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Service pemutar musik VYORA.
 * Mendukung:
 * - Pemutaran offline instan dari unduhan permanen (`OfflineDownloadManager`)
 * - Auto-Cache pintar (`SimpleCache`) agar lagu yang diputar ulang hemat kuota & jalan offline
 * - Mode Audio ⇄ Video Musik (MV)
 * - Audio Studio DSP (`AudioFxController`: Equalizer, Bass, Reverb, Loudness, Speed & Pitch)
 * - Pencatatan Statistik Mendengarkan & Streak harian otomatis
 * - Pembaruan Home Screen Widget VYORA Liquid Glass
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val snapshotHandler = Handler(Looper.getMainLooper())
    private lateinit var snapshot: PlaybackSnapshot
    private var audioFxController: AudioFxController? = null
    private var lastWidgetArtUrl: String? = null
    private var cachedWidgetBitmap: Bitmap? = null

    private val saveTick = object : Runnable {
        override fun run() {
            mediaSession?.player?.let { p ->
                if (p.isPlaying) {
                    snapshot.save(p)
                    p.currentMediaItem?.toSong()?.let { song ->
                        AppModule.stats.addListenSeconds(song, 5)
                    }
                }
            }
            snapshotHandler.postDelayed(this, 5_000L)
        }
    }

    private val prefetchExecutor = Executors.newSingleThreadExecutor()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var exoPlayer: ExoPlayer

    override fun onCreate() {
        super.onCreate()

        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(YTMusic.USER_AGENT)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(25_000)

        val localDefaultFactory = DefaultDataSource.Factory(this, httpFactory)
        val audioResolvingFactory = ResolvingDataSource.Factory(httpFactory) { spec -> resolveAudio(spec) }
        val videoResolvingFactory = ResolvingDataSource.Factory(httpFactory) { spec -> resolveVideo(spec) }

        val smartFactory = DataSource.Factory {
            VyoraRoutingDataSource(
                localDefaultFactory = localDefaultFactory,
                audioResolvingFactory = audioResolvingFactory,
                videoResolvingFactory = videoResolvingFactory,
            )
        }

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(smartFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        exoPlayer = player
        player.skipSilenceEnabled = AppModule.uiPrefs.state.value.skipSilence

        val fx = AudioFxController(player).also {
            it.attach(AppModule.audioFx.state.value)
        }
        audioFxController = fx

        snapshot = PlaybackSnapshot(this)
        snapshot.restore(player)

        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.containsAny(
                        Player.EVENT_TIMELINE_CHANGED,
                        Player.EVENT_MEDIA_ITEM_TRANSITION,
                        Player.EVENT_POSITION_DISCONTINUITY,
                        Player.EVENT_PLAY_WHEN_READY_CHANGED,
                        Player.EVENT_IS_PLAYING_CHANGED,
                        Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                        Player.EVENT_REPEAT_MODE_CHANGED,
                    )
                ) {
                    snapshot.save(player)
                    syncHomeWidget(player)
                }
                if (events.containsAny(
                        Player.EVENT_MEDIA_ITEM_TRANSITION,
                        Player.EVENT_IS_PLAYING_CHANGED,
                        Player.EVENT_PLAYBACK_STATE_CHANGED,
                        Player.EVENT_POSITION_DISCONTINUITY,
                        Player.EVENT_PLAYBACK_PARAMETERS_CHANGED,
                    )
                ) {
                    syncDiscordPresence(player)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val next = player.nextMediaItemIndex
                if (next != C.INDEX_UNSET) prefetch(player.getMediaItemAt(next))
            }
        })

        SleepTimer.attach(player)

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openAppIntent)
            .build()

        snapshotHandler.postDelayed(saveTick, 5_000L)
        syncHomeWidget(player)

        // Pantau preferensi pemutaran & Audio FX secara real-time
        serviceScope.launch {
            AppModule.uiPrefs.state.collect { prefs ->
                exoPlayer.skipSilenceEnabled = prefs.skipSilence
            }
        }
        serviceScope.launch {
            AppModule.audioFx.state.collect { fxPrefs ->
                audioFxController?.apply(fxPrefs)
            }
        }
        serviceScope.launch {
            AppModule.discordPrefs.state.collect {
                syncDiscordPresence(exoPlayer)
            }
        }
    }

    private fun syncDiscordPresence(player: Player) {
        val song = player.currentMediaItem?.toSong()
        val isPlaying = player.isPlaying
        val pos = player.currentPosition.coerceAtLeast(0L)
        val dur = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
        val speed = player.playbackParameters.speed
        AppModule.discordRpc.updateNowPlaying(
            song = song,
            isPlaying = isPlaying,
            positionMs = pos,
            durationMs = dur,
            playbackSpeed = speed,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val res = super.onStartCommand(intent, flags, startId)
        val p = mediaSession?.player
        if (p != null) {
            when (intent?.action) {
                VyoraMusicWidget.ACTION_PLAY_PAUSE -> {
                    if (p.isPlaying) p.pause() else {
                        if (p.playbackState == Player.STATE_ENDED) p.seekTo(0, 0L)
                        if (p.playbackState == Player.STATE_IDLE) p.prepare()
                        p.play()
                    }
                }
                VyoraMusicWidget.ACTION_NEXT -> if (p.hasNextMediaItem()) p.seekToNext()
                VyoraMusicWidget.ACTION_PREV -> {
                    if (p.currentPosition > 3000) p.seekTo(0L)
                    else if (p.hasPreviousMediaItem()) p.seekToPrevious()
                }
            }
        }
        return res
    }

    private fun syncHomeWidget(player: Player) {
        val md = player.currentMediaItem?.mediaMetadata
        val title = md?.title?.toString()
        val artist = md?.artist?.toString()
        val artUrl = md?.artworkUri?.toString()
        val isPlaying = player.isPlaying

        if (artUrl == null) {
            lastWidgetArtUrl = null
            cachedWidgetBitmap = null
            VyoraMusicWidget.pushUpdate(this, title, artist, isPlaying, null)
            return
        }
        if (artUrl == lastWidgetArtUrl && cachedWidgetBitmap != null) {
            VyoraMusicWidget.pushUpdate(this, title, artist, isPlaying, cachedWidgetBitmap)
            return
        }

        // Update teks cepat dulu, lalu susulkan sampul album
        VyoraMusicWidget.pushUpdate(this, title, artist, isPlaying, cachedWidgetBitmap)
        serviceScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                runCatching {
                    val req = ImageRequest.Builder(this@PlaybackService)
                        .data(artUrl)
                        .allowHardware(false)
                        .size(192, 192)
                        .build()
                    val result = imageLoader.execute(req) as? SuccessResult
                    (result?.drawable as? BitmapDrawable)?.bitmap
                }.getOrNull()
            }
            if (bmp != null) {
                lastWidgetArtUrl = artUrl
                cachedWidgetBitmap = bmp
                VyoraMusicWidget.pushUpdate(this@PlaybackService, title, artist, exoPlayer.isPlaying, bmp)
            }
        }
    }

    private inner class VyoraRoutingDataSource(
        private val localDefaultFactory: DataSource.Factory,
        private val audioResolvingFactory: DataSource.Factory,
        private val videoResolvingFactory: DataSource.Factory,
    ) : DataSource {

        private var active: DataSource? = null
        private val listeners = ArrayList<TransferListener>()

        override fun addTransferListener(transferListener: TransferListener) {
            listeners.add(transferListener)
            active?.addTransferListener(transferListener)
        }

        override fun open(dataSpec: DataSpec): Long {
            val uri = dataSpec.uri
            val chosen: Pair<DataSource, DataSpec> = if (uri.scheme == PlayerUris.SCHEME) {
                val videoId = uri.lastPathSegment.orEmpty()
                val isVideo = uri.getQueryParameter("mode") == "video"
                if (isVideo) {
                    videoResolvingFactory.createDataSource() to dataSpec
                } else {
                    val offlineFile = AppModule.downloads.localAudioFile(videoId)
                    if (offlineFile != null) {
                        FileDataSource() to dataSpec.withUri(Uri.fromFile(offlineFile))
                    } else {
                        val maxCache = AppModule.uiPrefs.state.value.streamCacheLimit.maxBytes
                        val simpleCache = StreamCacheHolder.get(this@PlaybackService, maxCache)
                        if (simpleCache != null) {
                            val cacheDs = CacheDataSource.Factory()
                                .setCache(simpleCache)
                                .setUpstreamDataSourceFactory(audioResolvingFactory)
                                .setCacheKeyFactory { spec ->
                                    val id = spec.uri.lastPathSegment ?: spec.uri.toString()
                                    "ytm_audio_$id"
                                }
                                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                                .createDataSource()
                            cacheDs to dataSpec
                        } else {
                            audioResolvingFactory.createDataSource() to dataSpec
                        }
                    }
                }
            } else {
                localDefaultFactory.createDataSource() to dataSpec
            }

            val (ds, targetSpec) = chosen
            listeners.forEach { ds.addTransferListener(it) }
            active = ds
            return ds.open(targetSpec)
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            active?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT

        override fun getUri(): Uri? = active?.uri

        override fun getResponseHeaders(): Map<String, List<String>> =
            active?.responseHeaders ?: emptyMap()

        override fun close() {
            try {
                active?.close()
            } finally {
                active = null
            }
        }
    }

    private fun resolveAudio(dataSpec: DataSpec): DataSpec {
        val uri = dataSpec.uri
        if (uri.scheme != PlayerUris.SCHEME) return dataSpec
        val videoId = uri.lastPathSegment ?: return dataSpec
        return dataSpec.withUri(Uri.parse(StreamResolver.audioUrl(videoId, effectiveQuality())))
    }

    private fun resolveVideo(dataSpec: DataSpec): DataSpec {
        val uri = dataSpec.uri
        if (uri.scheme != PlayerUris.SCHEME) return dataSpec
        val videoId = uri.lastPathSegment ?: return dataSpec
        return dataSpec.withUri(Uri.parse(StreamResolver.videoUrl(videoId)))
    }

    private fun effectiveQuality(): AudioQuality {
        val prefs = AppModule.uiPrefs.state.value
        if (!prefs.dataSaverOnMobile) return prefs.audioQuality
        return try {
            val cm = getSystemService(ConnectivityManager::class.java)
            val caps = cm?.let { it.getNetworkCapabilities(it.activeNetwork) }
            val metered = caps != null &&
                !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            if (metered) AudioQuality.LOW else prefs.audioQuality
        } catch (_: Exception) {
            prefs.audioQuality
        }
    }

    private fun prefetch(item: MediaItem) {
        val uri = item.localConfiguration?.uri ?: return
        if (uri.scheme != PlayerUris.SCHEME) return
        val videoId = uri.lastPathSegment ?: return
        if (AppModule.downloads.isDownloaded(videoId)) return
        prefetchExecutor.execute {
            runCatching { StreamResolver.audioUrl(videoId, effectiveQuality()) }
        }
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
        AppModule.discordRpc.clearPresence()
        audioFxController?.release()
        audioFxController = null
        mediaSession?.run {
            snapshot.save(player)
            VyoraMusicWidget.pushUpdate(
                this@PlaybackService,
                player.currentMediaItem?.mediaMetadata?.title?.toString(),
                player.currentMediaItem?.mediaMetadata?.artist?.toString(),
                false,
                cachedWidgetBitmap,
            )
            player.release()
            release()
        }
        mediaSession = null
        serviceScope.cancel()
        prefetchExecutor.shutdownNow()
        super.onDestroy()
    }
}

object PlayerUris {
    const val SCHEME = "ytm"
    fun forSong(videoId: String): Uri = Uri.parse("$SCHEME://song/$videoId")
    fun forVideo(videoId: String): Uri = Uri.parse("$SCHEME://song/$videoId?mode=video")
}
