package com.example.musikku.ui.player

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.musikku.player.LyricsUiState
import com.example.musikku.ui.lyrics.FullLyrics
import com.example.musikku.ui.lyrics.LyricsCardColor
import com.example.musikku.ui.lyrics.LyricsPreviewCard
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.musikku.player.NowPlaying
import com.example.musikku.ui.components.Artwork
import com.example.musikku.ui.components.formatMs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    state: NowPlaying,
    isFavorite: Boolean,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onArtistClick: (String) -> Unit,
    lyrics: LyricsUiState,
    onRetryLyrics: () -> Unit,
    sleepRemainingMs: Long?,
    onSetSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRetryPlayback: () -> Unit,
) {
    val song = state.song
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var showTimer by rememberSaveable { mutableStateOf(false) }
    var showQueue by rememberSaveable { mutableStateOf(false) }
    if (showTimer) SleepTimerDialog(sleepRemainingMs, onSetSleepTimer, onCancelSleepTimer) { showTimer = false }
    if (showQueue) QueueDialog(state, onPlayQueueItem, onRemoveQueueItem, onMoveQueueItem) { showQueue = false }
    BackHandler(enabled = showLyrics) { showLyrics = false }
    val cover = song?.thumbnail
    val coverTint by animateColorAsState(rememberCoverTint(cover), animationSpec = tween(600), label = "coverTint")

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Latar blur dari cover album (blur butuh Android 12+, di bawahnya hanya gelap)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AsyncImage(
                model = cover, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(60.dp)
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(coverTint.copy(alpha = 0.90f), Color(0xFF101715).copy(alpha = 0.97f), MaterialTheme.colorScheme.background)
                )
            )
        )

        // Halaman pemutar bisa di-scroll ke bawah untuk melihat kartu lirik
        LazyColumn(Modifier.fillMaxSize().systemBarsPadding()) {
        item {
        Column(
            Modifier.fillParentMaxHeight(0.93f).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Default.KeyboardArrowDown, "Tutup", Modifier.size(32.dp))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SEDANG DIPUTAR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        song?.album?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onAddToPlaylist) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, "Tambahkan ke playlist")
                }
            }

            Spacer(Modifier.weight(1f))
            Artwork(cover, Modifier.fillMaxWidth().sizeIn(maxWidth = 360.dp).aspectRatio(1f).shadow(20.dp, RoundedCornerShape(18.dp)), shape = RoundedCornerShape(18.dp))
            Spacer(Modifier.weight(1f))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        song?.title.orEmpty(), style = MaterialTheme.typography.titleLarge,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        song?.artistsText.orEmpty(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable {
                            song?.artists?.firstOrNull { it.id != null }?.id?.let(onArtistClick)
                        }
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        "Suka",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Seek bar — nilai lokal saat digeser supaya tidak "loncat-loncat"
            var dragValue by remember { mutableStateOf<Float?>(null) }
            val duration = state.durationMs.coerceAtLeast(1L)
            val sliderColors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
            )
            val interaction = remember { MutableInteractionSource() }
            Slider(
                value = dragValue ?: state.progress,
                onValueChange = { dragValue = it },
                onValueChangeFinished = {
                    dragValue?.let { onSeek((it * duration).toLong()) }
                    dragValue = null
                },
                interactionSource = interaction,
                thumb = {
                    Box(
                        Modifier
                            .size(if (dragValue != null) 16.dp else 12.dp)
                            .background(Color.White, CircleShape)
                    )
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        modifier = Modifier.height(4.dp),
                        colors = sliderColors,
                        drawStopIndicator = null,
                        thumbTrackGapSize = 0.dp,
                    )
                },
                modifier = Modifier.padding(top = 12.dp).height(24.dp)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val shownPos = dragValue?.let { (it * duration).toLong() } ?: state.positionMs
                Text(formatMs(shownPos), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatMs(state.durationMs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        Icons.Default.Shuffle, "Acak",
                        tint = if (state.shuffle) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
                IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipPrevious, "Sebelumnya", Modifier.size(40.dp))
                }
                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White, contentColor = Color.Black
                    )
                ) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(Modifier.size(28.dp), color = Color.Black, strokeWidth = 3.dp)
                    } else {
                        Icon(
                            if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (state.isPlaying) "Jeda" else "Putar",
                            Modifier.size(40.dp)
                        )
                    }
                }
                IconButton(onClick = onNext, enabled = state.hasNext, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipNext, "Berikutnya", Modifier.size(40.dp))
                }
                IconButton(onClick = onCycleRepeat) {
                    Icon(
                        if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        "Ulangi",
                        tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                Surface(onClick = { showQueue = true }, shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.10f)) {
                    Row(Modifier.defaultMinSize(minHeight = 48.dp).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, null, Modifier.size(18.dp), tint = Color.White)
                        Spacer(Modifier.width(7.dp))
                        Text("Antrean ${state.queue.size}", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
                Surface(onClick = { showTimer = true }, shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.10f)) {
                    Row(Modifier.defaultMinSize(minHeight = 48.dp).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bedtime, null, Modifier.size(18.dp), tint = Color.White)
                        Spacer(Modifier.width(7.dp))
                        Text(if (sleepRemainingMs == null) "Timer" else "${(sleepRemainingMs + 59_999) / 60_000} mnt", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
            }

            // Hanya tampil kalau ada error
            state.error?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TextButton(onClick = onRetryPlayback) { Text("Coba putar lagi") }
            }
            Spacer(Modifier.height(16.dp))
        }
        }
        item {
            LyricsPreviewCard(
                state = lyrics,
                positionMs = state.positionMs,
                onExpand = { showLyrics = true },
                onRetry = onRetryLyrics,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        item { Spacer(Modifier.height(32.dp)) }
        }

        // Lirik layar penuh
        val loaded = (lyrics as? LyricsUiState.Loaded)?.lyrics
        AnimatedVisibility(
            visible = showLyrics && loaded != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            Column(Modifier.fillMaxSize().background(LyricsCardColor).systemBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showLyrics = false }) {
                        Icon(Icons.Default.KeyboardArrowDown, "Tutup lirik", Modifier.size(32.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            song?.title.orEmpty(), fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            song?.artistsText.orEmpty(), style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (loaded != null) {
                    FullLyrics(loaded, state.positionMs, onSeek, Modifier.weight(1f))
                }
                Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        drawStopIndicator = {},
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(formatMs(state.positionMs), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                        FilledIconButton(
                            onClick = onPlayPause,
                            modifier = Modifier.size(56.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black)
                        ) {
                            Icon(
                                if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                if (state.isPlaying) "Jeda" else "Putar", Modifier.size(32.dp)
                            )
                        }
                        Text(formatMs(state.durationMs), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}
