package com.vyora.music.player

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.view.SurfaceView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.vyora.music.AppModule
import com.vyora.music.data.lyrics.Lyrics
import com.vyora.music.data.ytmusic.AlbumRef
import com.vyora.music.data.ytmusic.ArtistRef
import com.vyora.music.data.ytmusic.SongItem
import com.vyora.music.data.ytmusic.StreamResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface LyricsUiState {
    data object Idle : LyricsUiState
    data object Loading : LyricsUiState
    data object Error : LyricsUiState
    data class Loaded(val lyrics: Lyrics) : LyricsUiState
}

data class QueueEntry(val index: Int, val song: SongItem)

data class NowPlaying(
    val song: SongItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isVideoMode: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val error: String? = null,
    val queue: List<QueueEntry> = emptyList(),
    val currentQueueIndex: Int = -1,
) {
    val hasMedia: Boolean get() = song != null
    val progress: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

/**
 * Jembatan antara UI (Compose) dan PlaybackService lewat MediaController.
 */
class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(NowPlaying())
    val state: StateFlow<NowPlaying> = _state.asStateFlow()

    private val _lyrics = MutableStateFlow<LyricsUiState>(LyricsUiState.Idle)
    val lyrics: StateFlow<LyricsUiState> = _lyrics.asStateFlow()
    private var lyricsJob: Job? = null
    private var lyricsSongId: String? = null

    private var radioJob: Job? = null

    private var controller: MediaController? = null
    private val controllerFuture: ListenableFuture<MediaController>

    private var pendingAction: ((MediaController) -> Unit)? = null
    private var consecutiveErrors = 0

    private val songCache = object : LinkedHashMap<String, SongItem>(32, 0.75f, false) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SongItem>?): Boolean = size > MAX_CACHE
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (player.playbackState == Player.STATE_READY) consecutiveErrors = 0
            if (player.isPlaying) _state.update { if (it.error != null) it.copy(error = null) else it }
            syncState()
        }

        override fun onPlayerError(error: PlaybackException) {
            val c = controller
            // Jika mode Video gagal (misal lagu tidak punya video klip muxed), otomatis kembali ke mode Audio murni!
            val currentUri = c?.currentMediaItem?.localConfiguration?.uri
            if (c != null && currentUri?.getQueryParameter("mode") == "video") {
                val idx = c.currentMediaItemIndex
                val song = _state.value.song
                val savedPos = c.currentPosition.coerceAtLeast(0L)
                if (idx >= 0 && song != null) {
                    c.replaceMediaItem(idx, song.toMediaItem(videoMode = false))
                    c.seekTo(idx, savedPos)
                    c.prepare()
                    c.play()
                    _state.update {
                        it.copy(
                            isVideoMode = false,
                            error = "Video klip tidak tersedia untuk lagu ini — kembali ke Audio",
                        )
                    }
                    return
                }
            }

            consecutiveErrors++
            if (c != null && c.hasNextMediaItem() && consecutiveErrors < MAX_CONSECUTIVE_ERRORS) {
                c.seekToNextMediaItem()
                c.prepare()
                c.play()
            } else {
                _state.update { it.copy(error = "Gagal memutar lagu ini (${error.errorCodeName})") }
            }
        }
    }

    init {
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(app, token).buildAsync()
        controllerFuture.addListener({
            val c = runCatching { controllerFuture.get() }.getOrNull() ?: return@addListener
            controller = c.also { it.addListener(listener) }
            syncState()
            val pending = pendingAction
            pendingAction = null
            if (pending != null) pending(c)
        }, ContextCompat.getMainExecutor(app))

        // Update posisi progress bar & lirik dengan resolusi 100ms agar karaoke per-kata sangat mulus
        viewModelScope.launch {
            while (isActive) {
                controller?.let { c ->
                    if (c.isPlaying) {
                        _state.update {
                            it.copy(positionMs = c.currentPosition, durationMs = c.duration.coerceAtLeast(0))
                        }
                    }
                }
                delay(120)
            }
        }
    }

    private fun whenReady(action: (MediaController) -> Unit) {
        val c = controller
        if (c != null) action(c) else pendingAction = action
    }

    fun play(songs: List<SongItem>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        radioJob?.cancel()
        whenReady { c ->
            songs.forEach { songCache[it.id] = it }
            c.shuffleModeEnabled = false
            c.setMediaItems(songs.map { it.toMediaItem() }, startIndex.coerceIn(0, songs.lastIndex), 0L)
            c.prepare()
            c.play()
            _state.update { it.copy(error = null, isVideoMode = false) }
        }
    }

    fun playWithRadio(song: SongItem) {
        radioJob?.cancel()
        whenReady { c ->
            songCache[song.id] = song
            c.shuffleModeEnabled = false
            c.setMediaItems(listOf(song.toMediaItem()), 0, 0L)
            c.prepare()
            c.play()
            _state.update { it.copy(error = null, isVideoMode = false) }
            radioJob = viewModelScope.launch { fillRadioQueue(song) }
        }
    }

    private suspend fun fillRadioQueue(song: SongItem) {
        val related = try {
            AppModule.ytMusic.radio(song.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }.filter { it.id != song.id }
        val c = controller ?: return
        if (related.isEmpty() || c.currentMediaItem?.mediaId != song.id) return
        related.forEach { songCache[it.id] = it }
        c.addMediaItems(related.map { it.toMediaItem() })
    }

    fun shuffle(songs: List<SongItem>) {
        if (songs.isEmpty()) return
        radioJob?.cancel()
        whenReady { c ->
            songs.forEach { songCache[it.id] = it }
            c.setMediaItems(songs.map { it.toMediaItem() }, songs.indices.random(), 0L)
            c.shuffleModeEnabled = true
            c.prepare()
            c.play()
            _state.update { it.copy(error = null, isVideoMode = false) }
        }
    }

    /** Beralih mulus antara mode Audio murni dan Video Musik (MV) tanpa mengulang dari detik 0. */
    fun toggleVideoMode() {
        val c = controller ?: return
        val song = _state.value.song ?: return
        if (song.localUri != null) return // Lagu lokal tidak punya video YouTube
        val idx = c.currentMediaItemIndex
        if (idx < 0) return

        val nextVideo = !_state.value.isVideoMode
        val savedPos = c.currentPosition.coerceAtLeast(0L)
        val wasPlaying = c.isPlaying || c.playWhenReady

        c.replaceMediaItem(idx, song.toMediaItem(videoMode = nextVideo))
        c.seekTo(idx, savedPos)
        c.prepare()
        if (wasPlaying) c.play()
        _state.update { it.copy(isVideoMode = nextVideo, error = null) }
    }

    fun attachVideoSurface(surfaceView: SurfaceView?) {
        val c = controller ?: return
        runCatching {
            if (surfaceView != null) {
                c.setVideoSurfaceView(surfaceView)
            } else {
                c.clearVideoSurface()
            }
        }
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0, 0L)
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun next() { controller?.seekToNext() }

    fun previous() {
        val c = controller ?: return
        if (c.currentPosition > 3000) c.seekTo(0L) else c.seekToPrevious()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _state.update { it.copy(positionMs = positionMs) }
    }

    fun enqueue(song: SongItem, playNext: Boolean) {
        whenReady { c ->
            songCache[song.id] = song
            if (c.mediaItemCount == 0) {
                c.setMediaItem(song.toMediaItem())
                c.prepare()
                c.play()
            } else if (playNext) {
                c.shuffleModeEnabled = false
                c.addMediaItem(c.currentMediaItemIndex + 1, song.toMediaItem())
            } else {
                c.addMediaItem(song.toMediaItem())
            }
        }
    }

    fun playQueueItem(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        c.seekTo(index, 0L)
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
        c.play()
    }

    fun removeQueueItem(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount || index == c.currentMediaItemIndex) return
        c.removeMediaItem(index)
    }

    fun moveQueueItem(index: Int, direction: Int) {
        val c = controller ?: return
        val target = index + direction
        if (direction !in listOf(-1, 1) || index !in 0 until c.mediaItemCount || target !in 0 until c.mediaItemCount) return
        c.moveMediaItem(index, target)
    }

    fun retryPlayback() {
        val c = controller ?: return
        consecutiveErrors = 0
        c.currentMediaItem?.mediaId?.let(StreamResolver::invalidate)
        _state.update { it.copy(error = null) }
        c.prepare()
        c.play()
    }

    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun toggleFavorite() { _state.value.song?.let { AppModule.favorites.toggle(it) } }

    private fun loadLyricsFor(song: SongItem?) {
        if (song == null || song.id == lyricsSongId) return
        lyricsSongId = song.id
        lyricsJob?.cancel()
        _lyrics.value = LyricsUiState.Loading
        lyricsJob = viewModelScope.launch {
            _lyrics.value = try {
                LyricsUiState.Loaded(
                    AppModule.lyrics.getLyrics(
                        trackId = song.id,
                        title = song.title,
                        artist = song.artists.firstOrNull()?.name.orEmpty(),
                        album = song.album?.name,
                        durationSec = song.durationSec.takeIf { it > 0 }
                            ?: (_state.value.durationMs / 1000).toInt(),
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lyricsSongId = null
                LyricsUiState.Error
            }
        }
    }

    fun retryLyrics() {
        lyricsSongId = null
        loadLyricsFor(_state.value.song)
    }

    private fun syncState() {
        val c = controller ?: return
        val item = c.currentMediaItem
        val isVideo = item?.localConfiguration?.uri?.getQueryParameter("mode") == "video"
        val resolvedSong = item?.let { mi ->
            val base = songCache[mi.mediaId] ?: mi.toSong()
            val offlineCover = AppModule.downloads.localCoverUri(base.id)
            if (offlineCover != null && base.thumbnail.isNullOrBlank()) base.copy(thumbnail = offlineCover) else base
        }
        _state.update {
            it.copy(
                song = resolvedSong,
                isPlaying = c.isPlaying,
                isBuffering = c.playbackState == Player.STATE_BUFFERING,
                isVideoMode = isVideo,
                positionMs = c.currentPosition,
                durationMs = c.duration.coerceAtLeast(0),
                hasNext = c.hasNextMediaItem(),
                hasPrevious = c.hasPreviousMediaItem() || c.currentPosition > 3000,
                shuffle = c.shuffleModeEnabled,
                repeatMode = c.repeatMode,
                queue = (0 until c.mediaItemCount).map { index ->
                    val media = c.getMediaItemAt(index)
                    QueueEntry(index, songCache[media.mediaId] ?: media.toSong())
                },
                currentQueueIndex = c.currentMediaItemIndex,
            )
        }
        loadLyricsFor(_state.value.song)
    }

    override fun onCleared() {
        pendingAction = null
        controller?.removeListener(listener)
        MediaController.releaseFuture(controllerFuture)
        super.onCleared()
    }

    private companion object {
        const val MAX_CONSECUTIVE_ERRORS = 3
        const val MAX_CACHE = 500
    }
}

internal fun SongItem.toMediaItem(videoMode: Boolean = false): MediaItem {
    val coverUri = AppModule.downloads.localCoverUri(id) ?: thumbnail
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artistsText)
        .setAlbumTitle(album?.name)
        .setArtworkUri(coverUri?.let(Uri::parse))
        .setExtras(
            bundleOf(
                "artistId" to artists.firstOrNull()?.id,
                "albumId" to album?.id,
                "duration" to durationSec,
                "localUri" to localUri,
            )
        )
        .build()
    val builder = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(metadata)
    when {
        localUri != null -> builder.setUri(Uri.parse(localUri))
        videoMode -> builder.setUri(PlayerUris.forVideo(id))
        else -> builder.setUri(PlayerUris.forSong(id))
    }
    return builder.build()
}

internal fun MediaItem.toSong(): SongItem {
    val md = mediaMetadata
    val extras = md.extras
    return SongItem(
        id = mediaId,
        title = md.title?.toString().orEmpty(),
        artists = listOf(ArtistRef(extras?.getString("artistId"), md.artist?.toString().orEmpty())),
        album = md.albumTitle?.let { AlbumRef(extras?.getString("albumId"), it.toString()) },
        durationSec = extras?.getInt("duration") ?: 0,
        thumbnail = md.artworkUri?.toString(),
        localUri = extras?.getString("localUri"),
    )
}
