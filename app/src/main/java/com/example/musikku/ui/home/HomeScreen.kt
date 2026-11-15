package com.example.musikku.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musikku.AppModule
import com.example.musikku.data.ytmusic.SongItem
import com.example.musikku.data.ytmusic.YTItem
import com.example.musikku.player.NowPlaying
import com.example.musikku.ui.components.Artwork
import com.example.musikku.ui.components.SectionView
import com.example.musikku.ui.components.SongRow
import com.example.musikku.ui.theme.GlassChip
import com.example.musikku.ui.theme.LocalBottomOverlayPadding
import com.example.musikku.ui.theme.LocalUiPrefs
import com.example.musikku.ui.theme.WarmFaint
import com.example.musikku.ui.theme.WarmHairline
import com.example.musikku.ui.theme.WarmInk
import com.example.musikku.ui.theme.WarmMuted
import com.example.musikku.ui.theme.WarmSurface

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    currentSongId: String?,
    onPlaySongs: (List<SongItem>, Int) -> Unit,
    onShuffleSongs: (List<SongItem>) -> Unit,
    onOpenItem: (YTItem) -> Unit,
    onOpenCollection: (String) -> Unit,
    now: NowPlaying,
    onOpenPlayer: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val recents by AppModule.recents.items.collectAsStateWithLifecycle()
    val favorites by AppModule.favorites.items.collectAsStateWithLifecycle()
    val bottomPadding = LocalBottomOverlayPadding.current

    // Kumpulan lagu untuk grid Panggilan Cepat (3x3) & Pilihan Cepat
    val homeSongs = remember(state.sections) {
        state.sections
            .flatMap { it.items }
            .filterIsInstance<SongItem>()
            .distinctBy { it.id }
    }
    val speedDialPool = remember(recents, favorites, homeSongs) {
        (recents.filterIsInstance<SongItem>() + favorites + homeSongs)
            .distinctBy { it.id }
            .take(24)
    }
    val quickPickSongs = remember(state.selectedMoodId, state.moodSongs, homeSongs, favorites) {
        when {
            state.selectedMoodId != null -> state.moodSongs
            homeSongs.isNotEmpty() -> homeSongs.take(16)
            else -> favorites.take(12)
        }
    }
    // Hilangkan section pertama jika semua lagunya sudah ditampilkan di Pilihan Cepat agar tidak dobel
    val displayedSections = remember(state.selectedMoodId, state.sections, state.moodSections, quickPickSongs) {
        if (state.selectedMoodId != null) {
            state.moodSections
        } else {
            val firstSongs = state.sections.firstOrNull()?.items?.filterIsInstance<SongItem>().orEmpty()
            val skipFirst = firstSongs.size >= 4 && firstSongs.firstOrNull()?.id == quickPickSongs.firstOrNull()?.id
            if (skipFirst) state.sections.drop(1) else state.sections
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = bottomPadding + 18.dp),
    ) {
        // Header atas ala Auralys: "Beranda" + ikon Riwayat, Sedang Diputar, Pengaturan
        item(key = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Beranda",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    color = WarmInk,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onOpenHistory) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = "Riwayat putar",
                        tint = WarmInk.copy(alpha = 0.86f),
                        modifier = Modifier.size(23.dp),
                    )
                }
                IconButton(
                    onClick = {
                        if (now.hasMedia) onOpenPlayer()
                        else if (speedDialPool.isNotEmpty()) onShuffleSongs(speedDialPool)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Equalizer,
                        contentDescription = if (now.hasMedia) "Buka pemutar" else "Putar acak",
                        tint = if (now.isPlaying) MaterialTheme.colorScheme.primary else WarmInk.copy(alpha = 0.86f),
                        modifier = Modifier.size(23.dp),
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Pengaturan tampilan",
                        tint = WarmInk.copy(alpha = 0.86f),
                        modifier = Modifier.size(23.dp),
                    )
                }
            }
        }

        // Deretan Mood Filter Chips ala Auralys
        item(key = "moods") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HOME_MOODS.forEach { mood ->
                    GlassChip(
                        label = mood.label,
                        selected = state.selectedMoodId == mood.id,
                        onClick = { vm.selectMood(mood) },
                    )
                }
            }
        }

        // Panggilan Cepat (Speed Dial 3×3 Grid + Kartu Dadu Acak)
        if (state.selectedMoodId == null && speedDialPool.isNotEmpty()) {
            item(key = "speed-dial") {
                SpeedDialSection(
                    songs = speedDialPool,
                    currentSongId = currentSongId,
                    onPlaySong = { song ->
                        val idx = speedDialPool.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                        onPlaySongs(speedDialPool, idx)
                    },
                    onShuffleAll = { onShuffleSongs(speedDialPool) },
                )
            }
        }

        // Loading / Error state
        if ((state.loading && state.sections.isEmpty()) || state.moodLoading) {
            item(key = "skeleton") { HomeSkeleton() }
        } else if (state.error != null && state.sections.isEmpty()) {
            item(key = "error") {
                Column(
                    Modifier.fillMaxWidth().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Belum bisa memuat rekomendasi", style = MaterialTheme.typography.titleMedium, color = WarmInk)
                    Spacer(Modifier.height(4.dp))
                    Text(state.error.orEmpty(), style = MaterialTheme.typography.bodySmall, color = WarmMuted)
                    OutlinedButton(onClick = vm::load, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Coba lagi", color = WarmInk)
                    }
                }
            }
        }

        // Pilihan cepat (Quick picks) dengan tombol pill "Putar semua"
        if (quickPickSongs.isNotEmpty() && !state.moodLoading) {
            item(key = "quick-picks") {
                val activeMoodLabel = HOME_MOODS.firstOrNull { it.id == state.selectedMoodId }?.label
                QuickPicksSection(
                    title = if (activeMoodLabel != null) "Pilihan $activeMoodLabel" else "Pilihan cepat",
                    songs = quickPickSongs,
                    currentSongId = currentSongId,
                    onPlaySongs = onPlaySongs,
                )
            }
        }

        // Bagian rekomendasi YouTube Music lainnya
        itemsIndexed(displayedSections, key = { i, section -> "home-$i-${section.title}" }) { _, section ->
            SectionView(section, currentSongId, onPlaySongs, onOpenItem, onOpenCollection)
        }
    }
}

/**
 * Grid 3×3 "Panggilan cepat" ala Auralys:
 * 8 kartu lagu + 1 kartu dadu (5 titik aksen) di kanan bawah untuk putar acak instan,
 * lengkap dengan paginasi geser horizontal dan titik indikator di bawahnya.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpeedDialSection(
    songs: List<SongItem>,
    currentSongId: String?,
    onPlaySong: (SongItem) -> Unit,
    onShuffleAll: () -> Unit,
) {
    val pages = remember(songs) { songs.chunked(8).take(3) }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val accent = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Text(
            text = "Panggilan cepat",
            style = MaterialTheme.typography.titleLarge,
            color = WarmInk,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
        )

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 14.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { pageIndex ->
            val pageSongs = pages[pageIndex]
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (row in 0 until 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        for (col in 0 until 3) {
                            val slot = row * 3 + col
                            if (slot == 8) {
                                // Slot ke-9: Kartu Dadu Acak (5 titik hangat)
                                ShuffleDiceTile(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onShuffleAll()
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                val song = pageSongs.getOrNull(slot)
                                if (song != null) {
                                    SpeedDialTile(
                                        song = song,
                                        isCurrent = song.id == currentSongId,
                                        onClick = { onPlaySong(song) },
                                        modifier = Modifier.weight(1f),
                                    )
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Indikator halaman (pil terakota untuk halaman aktif)
        if (pages.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pages.size) { idx ->
                    val selected = pagerState.currentPage == idx
                    Box(
                        modifier = Modifier
                            .height(5.dp)
                            .width(if (selected) 20.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (selected) accent else WarmFaint.copy(alpha = 0.45f))
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedDialTile(
    song: SongItem,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val prefs = LocalUiPrefs.current
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(if (prefs.isGlass) 16.dp else 13.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .border(
                    width = if (isCurrent) 1.5.dp else 0.8.dp,
                    color = if (isCurrent) accent else Color.White.copy(alpha = 0.10f),
                    shape = shape,
                )
        ) {
            Artwork(song.thumbnail, Modifier.fillMaxSize(), shape = shape)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isCurrent) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(2.dp))
            }
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                ),
                color = if (isCurrent) accent else WarmInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Kartu dadu 5-titik di sudut kanan bawah grid Panggilan Cepat untuk memutar acak seketika. */
@Composable
private fun ShuffleDiceTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val prefs = LocalUiPrefs.current
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(if (prefs.isGlass) 16.dp else 13.dp)

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(if (prefs.isGlass) Color.White.copy(alpha = 0.08f) else WarmSurface)
                .border(1.dp, if (prefs.isGlass) Color.White.copy(alpha = 0.16f) else WarmHairline, shape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                val r = size.minDimension * 0.105f
                val p1 = size.width * 0.22f
                val p2 = size.width * 0.78f
                val c = size.width * 0.50f
                val py1 = size.height * 0.22f
                val py2 = size.height * 0.78f
                val cy = size.height * 0.50f
                listOf(
                    Offset(p1, py1),
                    Offset(p2, py1),
                    Offset(c, cy),
                    Offset(p1, py2),
                    Offset(p2, py2),
                ).forEach { pos ->
                    drawCircle(color = accent, radius = r, center = pos)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Acak cepat",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Medium),
            color = WarmMuted,
            maxLines = 1,
        )
    }
}

@Composable
private fun QuickPicksSection(
    title: String,
    songs: List<SongItem>,
    currentSongId: String?,
    onPlaySongs: (List<SongItem>, Int) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val prefs = LocalUiPrefs.current

    Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = WarmInk,
                modifier = Modifier.weight(1f),
            )
            Surface(
                onClick = { onPlaySongs(songs, 0) },
                shape = if (prefs.isGlass) CircleShape else RoundedCornerShape(10.dp),
                color = accent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
            ) {
                Text(
                    text = "Putar semua",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }

        val rows = minOf(4, songs.size)
        LazyHorizontalGrid(
            rows = GridCells.Fixed(rows),
            contentPadding = PaddingValues(horizontal = 6.dp),
            modifier = Modifier.height((68 * rows).dp),
        ) {
            itemsIndexed(songs, key = { i, s -> "qp-$i-${s.id}" }) { i, song ->
                Column(Modifier.width(326.dp)) {
                    SongRow(
                        song = song,
                        isCurrent = song.id == currentSongId,
                        onClick = { onPlaySongs(songs, i) },
                        showAlbum = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if ((i + 1) % rows != 0) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 78.dp, end = 16.dp)
                                .height(0.6.dp)
                                .background(WarmHairline)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSkeleton() {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
        Box(Modifier.width(150.dp).height(22.dp).clip(RoundedCornerShape(8.dp)).background(WarmSurface))
        Spacer(Modifier.height(14.dp))
        repeat(2) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(3) {
                    Column(Modifier.weight(1f)) {
                        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(WarmSurface))
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth(0.75f).height(11.dp).clip(RoundedCornerShape(6.dp)).background(WarmSurface))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
