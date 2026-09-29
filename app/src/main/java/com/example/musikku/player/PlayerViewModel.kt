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
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musikku.AppModule
import com.example.musikku.data.lyrics.Lyrics
import com.example.musikku.data.ytmusic.AlbumRef
import com.example.musikku.data.ytmusic.ArtistRef
import com.example.musikku.data.ytmusic.SongItem
import com.google.common.util.concurrent.ListenableFuture
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

data class NowPlaying(
    val song: SongItem? = null,
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
    val hasMedia: Boolean get() = song != null
    val progress: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

/**
 * Jembatan antara UI (Compose) dan PlaybackService lewat MediaController.
 * Di-scope ke Activity, jadi semua layar berbagi satu instance.
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

    /** Objek SongItem lengkap berdasarkan videoId. */
    private val songCache = mutableMapOf<String, SongItem>()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = syncState()
        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(error = "Gagal memutar lagu ini (${error.errorCodeName})") }
            // Lewati ke lagu berikutnya kalau ada
            controller?.let { c -> if (c.hasNextMediaItem()) { c.seekToNextMediaItem(); c.prepare(); c.play() } }
        }
    }

    init {
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(app, token).buildAsync()
        controllerFuture.addListener({
            controller = controllerFuture.get().also { it.addListener(listener) }
            syncState()
        }, ContextCompat.getMainExecutor(app))

        // Update posisi progress bar & lirik
        viewModelScope.launch {
            while (isActive) {
                controller?.let { c ->
                    if (c.isPlaying) {
                        _state.update {
                            it.copy(positionMs = c.currentPosition, durationMs = c.duration.coerceAtLeast(0))
                        }
                    }
                }
                delay(250)
            }
        }
    }

    /** Putar daftar lagu (album, playlist, lagu teratas) mulai dari [startIndex]. */
    fun play(songs: List<SongItem>, startIndex: Int = 0) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        radioJob?.cancel()
        songs.forEach { songCache[it.id] = it }
        c.shuffleModeEnabled = false
        c.setMediaItems(songs.map { it.toMediaItem() }, startIndex.coerceIn(0, songs.lastIndex), 0L)
        c.prepare()
        c.play()
        _state.update { it.copy(error = null) }
    }

    /** Putar satu lagu lalu isi antrian otomatis dengan lagu serupa (radio ala YT Music). */
    fun playWithRadio(song: SongItem) {
        play(listOf(song))
        radioJob = viewModelScope.launch {
            val related = runCatching { AppModule.ytMusic.radio(song.id) }.getOrDefault(emptyList())
                .filter { it.id != song.id }
            val c = controller ?: return@launch
            if (related.isEmpty() || c.currentMediaItem?.mediaId != song.id) return@launch
            related.forEach { songCache[it.id] = it }
            c.addMediaItems(related.map { it.toMediaItem() })
        }
    }

    /** Putar acak. */
    fun shuffle(songs: List<SongItem>) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        radioJob?.cancel()
        songs.forEach { songCache[it.id] = it }
        c.setMediaItems(songs.map { it.toMediaItem() }, songs.indices.random(), 0L)
        c.shuffleModeEnabled = true
        c.prepare()
        c.play()
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
    fun previous() { controller?.seekToPrevious() }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _state.update { it.copy(positionMs = positionMs) }
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

    /** Muat lirik untuk lagu yang sedang diputar (sekali per lagu). */
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
        _state.update {
            it.copy(
                song = item?.let { mi -> songCache[mi.mediaId] ?: mi.toSong() },
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
        loadLyricsFor(_state.value.song)
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        MediaController.releaseFuture(controllerFuture)
        super.onCleared()
    }
}

private fun SongItem.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artistsText)
        .setAlbumTitle(album?.name)
        .setArtworkUri(thumbnail?.let(Uri::parse))
        .setExtras(
            bundleOf(
                "artistId" to artists.firstOrNull()?.id,
                "albumId" to album?.id,
                "duration" to durationSec,
            )
        )
        .build()
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(PlayerUris.forSong(id))
        .setMediaMetadata(metadata)
        .build()
}

/** Fallback bila app dibuka ulang saat service masih memutar lagu. */
private fun MediaItem.toSong(): SongItem {
    val md = mediaMetadata
    val extras = md.extras
    return SongItem(
        id = mediaId,
        title = md.title?.toString().orEmpty(),
        artists = listOf(ArtistRef(extras?.getString("artistId"), md.artist?.toString().orEmpty())),
        album = md.albumTitle?.let { AlbumRef(extras?.getString("albumId"), it.toString()) },
        durationSec = extras?.getInt("duration") ?: 0,
        thumbnail = md.artworkUri?.toString(),
    )
}
