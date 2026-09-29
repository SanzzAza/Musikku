package com.example.musikku.player

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musikku.AppModule
import com.example.musikku.data.model.Album
import com.example.musikku.data.model.Artist
import com.example.musikku.data.model.Track
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class NowPlaying(
    val track: Track? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val error: String? = null,
) {
    val hasMedia: Boolean get() = track != null
    val progress: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

/**
 * Jembatan antara UI (Compose) dan PlaybackService lewat MediaController.
 * Di-scope ke Activity, jadi semua layar berbagi satu instance.
 */
class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(NowPlaying())
    val state: StateFlow<NowPlaying> = _state.asStateFlow()

    private var controller: MediaController? = null
    private val controllerFuture: ListenableFuture<MediaController>

    /** Menyimpan objek Track lengkap berdasarkan mediaId. */
    private val trackCache = mutableMapOf<String, Track>()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = syncState()
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            _state.update { it.copy(error = "Gagal memutar: ${error.errorCodeName}") }
        }
    }

    init {
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(app, token).buildAsync()
        controllerFuture.addListener({
            controller = controllerFuture.get().also { it.addListener(listener) }
            syncState()
        }, ContextCompat.getMainExecutor(app))

        // Update posisi progress bar setiap 500ms selama lagu diputar
        viewModelScope.launch {
            while (isActive) {
                controller?.let { c ->
                    if (c.isPlaying) {
                        _state.update {
                            it.copy(
                                positionMs = c.currentPosition,
                                durationMs = c.duration.coerceAtLeast(0)
                            )
                        }
                    }
                }
                delay(500)
            }
        }
    }

    /** Putar daftar lagu mulai dari [startIndex]. */
    fun play(tracks: List<Track>, startIndex: Int = 0) {
        val c = controller ?: return
        val playable = tracks.filter { it.isPlayable }
        if (playable.isEmpty()) return
        val startId = tracks.getOrNull(startIndex)?.id
        val start = playable.indexOfFirst { it.id == startId }.coerceAtLeast(0)

        playable.forEach { trackCache[it.id.toString()] = it }
        c.shuffleModeEnabled = false
        c.setMediaItems(playable.map { it.toMediaItem() }, start, 0L)
        c.prepare()
        c.play()
        _state.update { it.copy(error = null) }
    }

    /** Putar acak. */
    fun shuffle(tracks: List<Track>) {
        val c = controller ?: return
        val playable = tracks.filter { it.isPlayable }
        if (playable.isEmpty()) return
        playable.forEach { trackCache[it.id.toString()] = it }
        c.setMediaItems(playable.map { it.toMediaItem() }, playable.indices.random(), 0L)
        c.shuffleModeEnabled = true
        c.prepare()
        c.play()
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0, 0L)
            c.play()
        }
    }

    fun next() { controller?.seekToNext() }
    fun previous() { controller?.seekToPrevious() }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _state.update { it.copy(positionMs = positionMs) }
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun toggleFavorite() {
        _state.value.track?.let { AppModule.favorites.toggle(it) }
    }

    private fun syncState() {
        val c = controller ?: return
        val item = c.currentMediaItem
        _state.update {
            it.copy(
                track = item?.let { mi -> trackCache[mi.mediaId] ?: mi.toTrack() },
                isPlaying = c.isPlaying,
                isBuffering = c.playbackState == Player.STATE_BUFFERING,
                positionMs = c.currentPosition,
                durationMs = c.duration.coerceAtLeast(0),
                hasNext = c.hasNextMediaItem(),
                hasPrevious = c.hasPreviousMediaItem() || c.currentPosition > 3000,
                shuffle = c.shuffleModeEnabled,
                repeatMode = c.repeatMode,
            )
        }
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        MediaController.releaseFuture(controllerFuture)
        super.onCleared()
    }
}

private fun Track.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist?.name)
        .setAlbumTitle(album?.title)
        .setArtworkUri((album?.coverXl ?: album?.coverBig ?: album?.coverMedium)?.let(Uri::parse))
        .setExtras(
            bundleOf(
                "artistId" to (artist?.id ?: 0L),
                "albumId" to (album?.id ?: 0L),
                "cover" to album?.coverMedium,
            )
        )
        .build()
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(PlayerUris.forTrack(id, title, artist?.name, duration))
        .setMediaMetadata(metadata)
        .build()
}

/** Fallback bila app dibuka ulang saat service masih memutar lagu. */
private fun MediaItem.toTrack(): Track {
    val md = mediaMetadata
    val extras = md.extras
    return Track(
        id = mediaId.toLongOrNull() ?: 0L,
        title = md.title?.toString(),
        preview = "resolved-by-player",
        artist = Artist(id = extras?.getLong("artistId") ?: 0L, name = md.artist?.toString()),
        album = Album(
            id = extras?.getLong("albumId") ?: 0L,
            title = md.albumTitle?.toString(),
            coverMedium = extras?.getString("cover") ?: md.artworkUri?.toString(),
            coverXl = md.artworkUri?.toString(),
        ),
    )
}
