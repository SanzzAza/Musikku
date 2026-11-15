package com.example.musikku.ui.player

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.musikku.player.LyricsUiState
import com.example.musikku.player.NowPlaying
import com.example.musikku.ui.components.Artwork
import com.example.musikku.ui.components.formatMs
import com.example.musikku.ui.lyrics.EditorialLyricsStage
import com.example.musikku.ui.theme.EditorialBackground
import com.example.musikku.ui.theme.GlassCircleIconButton
import com.example.musikku.ui.theme.LocalCoverPalette
import com.example.musikku.ui.theme.LocalUiPrefs
import com.example.musikku.ui.theme.ObsidianBackground
import com.example.musikku.ui.theme.WarmFaint
import com.example.musikku.ui.theme.WarmHairline
import com.example.musikku.ui.theme.WarmInk
import com.example.musikku.ui.theme.WarmMuted

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val prefs = LocalUiPrefs.current
    val palette = LocalCoverPalette.current
    val accent = MaterialTheme.colorScheme.primary

    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var showTimer by rememberSaveable { mutableStateOf(false) }
    var showQueue by rememberSaveable { mutableStateOf(false) }
    var showShareCard by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = showLyrics) { showLyrics = false }

    if (showTimer) {
        SleepTimerSheet(sleepRemainingMs, onSetSleepTimer, onCancelSleepTimer) { showTimer = false }
    }
    if (showQueue) {
        QueueSheet(state, onPlayQueueItem, onRemoveQueueItem, onMoveQueueItem) { showQueue = false }
    }
    if (showShareCard && song != null) {
        ShareSongCardSheet(song, lyrics, state.positionMs) { showShareCard = false }
    }

    val cover = song?.thumbnail
    val baseBg = if (prefs.isGlass) ObsidianBackground else EditorialBackground

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(baseBg)
    ) {
        // Latar blur sampul lembut pada mode Liquid Glass (Android 12+)
        if (prefs.isGlass && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && cover != null) {
            AsyncImage(
                model = cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(72.dp),
            )
        }

        // Overlay obsidian dengan semburat ambient dari sampul
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to palette.ambient.copy(alpha = if (prefs.isGlass) 0.72f else 0.30f),
                        0.45f to baseBg.copy(alpha = if (prefs.isGlass) 0.90f else 0.97f),
                        1f to baseBg.copy(alpha = 0.98f),
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 22.dp),
        ) {
            // Area atas & tengah: berpindah mulus antara Sampul (Artwork) dan Lirik Editorial
            AnimatedContent(
                targetState = showLyrics,
                transitionSpec = {
                    fadeIn(tween(260)) togetherWith fadeOut(tween(220))
                },
                modifier = Modifier.weight(1f),
                label = "playerLyricsSwitch",
            ) { lyricsMode ->
                if (!lyricsMode) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Header ala Auralys: "SEDANG DIPUTAR" + teks miring artis/album
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "SEDANG DIPUTAR",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                                    color = accent,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = song?.album?.name?.takeIf { it.isNotBlank() }
                                        ?: song?.artistsText?.ifBlank { "Musikku" }
                                        ?: "Musikku",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontStyle = FontStyle.Italic,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                    ),
                                    color = WarmInk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            GlassCircleIconButton(
                                icon = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Tutup pemutar",
                                onClick = onCollapse,
                                size = 40.dp,
                                iconSize = 24.dp,
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        // Sampul album berbingkai ganda (Signature Auralys frame)
                        val frameShape = RoundedCornerShape(if (prefs.isGlass) 26.dp else 22.dp)
                        val innerArtShape = RoundedCornerShape(if (prefs.isGlass) 18.dp else 15.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .sizeIn(maxWidth = 344.dp)
                                .aspectRatio(1f)
                                .shadow(
                                    elevation = if (prefs.isGlass) 28.dp else 14.dp,
                                    shape = frameShape,
                                    ambientColor = palette.ambient,
                                    spotColor = Color.Black,
                                )
                                .clip(frameShape)
                                .background(
                                    if (prefs.isGlass) Color.White.copy(alpha = 0.06f) else Color(0xFF1A1715)
                                )
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (prefs.isGlass) {
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.White.copy(alpha = 0.28f),
                                                    Color.White.copy(alpha = 0.08f),
                                                )
                                            )
                                        } else {
                                            Brush.verticalGradient(listOf(WarmHairline, WarmHairline))
                                        },
                                    ),
                                    frameShape,
                                )
                                .padding(9.dp),
                        ) {
                            Artwork(
                                url = cover,
                                modifier = Modifier.fillMaxSize(),
                                shape = innerArtShape,
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        // Judul + Artis + Hati + Menu ⋮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = song?.title.orEmpty(),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                                    color = WarmInk,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee(),
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = song?.artistsText.orEmpty(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = WarmMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable {
                                        song?.artists?.firstOrNull { it.id != null }?.id?.let(onArtistClick)
                                    },
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = onToggleFavorite) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = if (isFavorite) "Hapus dari disukai" else "Suka",
                                    tint = if (isFavorite) accent else WarmInk,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                            Box {
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(Icons.Rounded.MoreVert, "Opsi lagu", tint = WarmInk)
                                }
                                PlayerOverflowMenu(
                                    expanded = menuOpen,
                                    onDismiss = { menuOpen = false },
                                    hasArtist = song?.artists?.any { it.id != null } == true,
                                    onAddToPlaylist = onAddToPlaylist,
                                    onOpenArtist = {
                                        song?.artists?.firstOrNull { it.id != null }?.id?.let(onArtistClick)
                                    },
                                    onShare = { showShareCard = true },
                                )
                            }
                        }
                    }
                } else {
                    // Mode Lirik Layar Penuh ala Auralys (Serif terpusat + header ringkas di bawah lirik)
                    Column(Modifier.fillMaxSize()) {
                        EditorialLyricsStage(
                            state = lyrics,
                            positionMs = state.positionMs,
                            onSeek = onSeek,
                            onRetry = onRetryLyrics,
                            fadeColor = baseBg,
                            modifier = Modifier.weight(1f),
                        )

                        Spacer(Modifier.height(6.dp))
                        val eyebrow = song?.album?.name?.takeIf { it.isNotBlank() }
                            ?: song?.artistsText?.takeIf { it.isNotBlank() }
                            ?: "SEDANG DIPUTAR"
                        Text(
                            text = eyebrow.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Artwork(cover, Modifier.size(46.dp), shape = RoundedCornerShape(10.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = song?.title.orEmpty(),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 20.sp,
                                    ),
                                    color = WarmInk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = song?.artistsText.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WarmMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = { showLyrics = false }) {
                                Icon(Icons.Rounded.CropFree, "Kembali ke sampul", tint = WarmInk)
                            }
                            Box {
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(Icons.Rounded.MoreVert, "Opsi", tint = WarmInk)
                                }
                                PlayerOverflowMenu(
                                    expanded = menuOpen,
                                    onDismiss = { menuOpen = false },
                                    hasArtist = song?.artists?.any { it.id != null } == true,
                                    onAddToPlaylist = onAddToPlaylist,
                                    onOpenArtist = {
                                        song?.artists?.firstOrNull { it.id != null }?.id?.let(onArtistClick)
                                    },
                                    onShare = { showShareCard = true },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Seek Bar tipis modern (thumb kaca membesar halus saat digeser)
            var dragValue by remember { mutableStateOf<Float?>(null) }
            val duration = state.durationMs.coerceAtLeast(1L)
            val shownPos = dragValue?.let { (it * duration).toLong() } ?: state.positionMs
            val remaining = (state.durationMs - shownPos).coerceAtLeast(0L)
            val thumbWidth by animateDpAsState(
                targetValue = if (dragValue != null) (if (prefs.isGlass) 26.dp else 18.dp) else 14.dp,
                animationSpec = tween(160),
                label = "thumbW",
            )
            val thumbHeight by animateDpAsState(
                targetValue = if (dragValue != null) 16.dp else 14.dp,
                animationSpec = tween(160),
                label = "thumbH",
            )
            val sliderColors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.16f),
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
                        modifier = Modifier
                            .width(thumbWidth)
                            .height(thumbHeight)
                            .shadow(if (prefs.isGlass) 8.dp else 2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                if (prefs.isGlass && dragValue != null) Color.White.copy(alpha = 0.92f) else accent
                            )
                            .border(
                                width = if (prefs.isGlass) 1.dp else 0.dp,
                                color = if (prefs.isGlass) Color.White.copy(alpha = 0.6f) else Color.Transparent,
                                shape = CircleShape,
                            )
                    )
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        modifier = Modifier.height(3.5.dp),
                        colors = sliderColors,
                        drawStopIndicator = null,
                        thumbTrackGapSize = 0.dp,
                    )
                },
                modifier = Modifier.fillMaxWidth().height(26.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatMs(shownPos),
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmMuted,
                )
                Text(
                    text = if (state.durationMs > 0) "-${formatMs(remaining)}" else "0:00",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmMuted,
                )
            }

            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(0.8.dp).background(WarmHairline))

            // Baris Kontrol Utama dengan tombol Squircle Terakota di tengah
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Acak",
                        tint = if (state.shuffle) accent else WarmMuted,
                        modifier = Modifier.size(24.dp),
                    )
                }
                IconButton(onClick = onPrevious, modifier = Modifier.size(52.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Sebelumnya",
                        tint = WarmInk,
                        modifier = Modifier.size(34.dp),
                    )
                }

                val playShape = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .shadow(if (prefs.isGlass) 18.dp else 8.dp, playShape, ambientColor = accent, spotColor = accent)
                        .clip(playShape)
                        .background(
                            if (prefs.isGlass) {
                                Brush.linearGradient(listOf(palette.accent, palette.secondaryAccent))
                            } else {
                                Brush.linearGradient(listOf(accent, accent))
                            }
                        )
                        .border(
                            width = if (prefs.isGlass) 1.dp else 0.dp,
                            color = if (prefs.isGlass) Color.White.copy(alpha = 0.38f) else Color.Transparent,
                            shape = playShape,
                        )
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Color.White,
                            strokeWidth = 3.dp,
                        )
                    } else {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (state.isPlaying) "Jeda" else "Putar",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp),
                        )
                    }
                }

                IconButton(onClick = onNext, enabled = state.hasNext, modifier = Modifier.size(52.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Berikutnya",
                        tint = if (state.hasNext) WarmInk else WarmFaint.copy(alpha = 0.45f),
                        modifier = Modifier.size(34.dp),
                    )
                }
                IconButton(onClick = onCycleRepeat) {
                    Icon(
                        imageVector = if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "Ulangi",
                        tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) accent else WarmMuted,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            // Pesan error + tombol coba ulang jika terjadi kendala stream
            state.error?.let { err ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onRetryPlayback) {
                        Text("Coba lagi", color = accent, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Dock Aksi 4-Tab di Bawah: LIRIK • ANTREAN • BAGIKAN • WAKTU TIDUR
            val dockShape = RoundedCornerShape(if (prefs.isGlass) 22.dp else 16.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(dockShape)
                    .background(
                        if (prefs.isGlass) Color.White.copy(alpha = 0.06f) else Color(0xFF191614)
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (prefs.isGlass) {
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.24f),
                                        Color.White.copy(alpha = 0.06f),
                                    )
                                )
                            } else {
                                Brush.verticalGradient(listOf(WarmHairline, WarmHairline))
                            },
                        ),
                        dockShape,
                    )
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerDockAction(
                    icon = if (showLyrics) Icons.Rounded.Mic else Icons.Outlined.Mic,
                    label = "LIRIK",
                    active = showLyrics,
                    onClick = { showLyrics = !showLyrics },
                    modifier = Modifier.weight(1f),
                )
                PlayerDockAction(
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    label = if (state.queue.size > 1) "ANTREAN" else "ANTREAN",
                    active = showQueue,
                    onClick = { showQueue = true },
                    modifier = Modifier.weight(1f),
                )
                PlayerDockAction(
                    icon = Icons.Outlined.Share,
                    label = "BAGIKAN",
                    active = showShareCard,
                    onClick = { if (song != null) showShareCard = true },
                    modifier = Modifier.weight(1f),
                )
                val timerActive = sleepRemainingMs != null
                PlayerDockAction(
                    icon = if (timerActive) Icons.Rounded.Bedtime else Icons.Outlined.Bedtime,
                    label = if (timerActive) "${(sleepRemainingMs!! + 59_999) / 60_000} MNT" else "WAKTU TIDUR",
                    active = timerActive,
                    onClick = { showTimer = true },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PlayerDockAction(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val tint by animateColorAsState(
        if (active) accent else WarmMuted,
        tween(200),
        label = "dockTint",
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(21.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                letterSpacing = 0.8.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
            ),
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PlayerOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    hasArtist: Boolean,
    onAddToPlaylist: () -> Unit,
    onOpenArtist: () -> Unit,
    onShare: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF211D1A),
    ) {
        DropdownMenuItem(
            text = { Text("Tambahkan ke playlist", color = WarmInk) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, tint = WarmInk) },
            onClick = { onDismiss(); onAddToPlaylist() },
        )
        if (hasArtist) {
            DropdownMenuItem(
                text = { Text("Lihat halaman artis", color = WarmInk) },
                leadingIcon = { Icon(Icons.Rounded.Person, null, tint = WarmInk) },
                onClick = { onDismiss(); onOpenArtist() },
            )
        }
        DropdownMenuItem(
            text = { Text("Kartu bagikan lagu", color = WarmInk) },
            leadingIcon = { Icon(Icons.Outlined.Share, null, tint = WarmInk) },
            onClick = { onDismiss(); onShare() },
        )
    }
}
