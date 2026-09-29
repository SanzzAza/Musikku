package com.example.musikku.ui.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.musikku.data.ytmusic.SongItem
import com.example.musikku.data.ytmusic.YTItem
import com.example.musikku.ui.components.ErrorBox
import com.example.musikku.ui.components.LoadingBox
import com.example.musikku.ui.components.PlayShuffleButtons
import com.example.musikku.ui.components.SectionTitle
import com.example.musikku.ui.components.SectionView
import com.example.musikku.ui.components.SongRow
import com.example.musikku.ui.components.isOpenableCollection

@Composable
fun ArtistScreen(
    artistId: String,
    currentSongId: String?,
    onPlaySongs: (List<SongItem>, Int) -> Unit,
    onShuffle: (List<SongItem>) -> Unit,
    onOpenItem: (YTItem) -> Unit,
    onOpenCollection: (String) -> Unit,
    onBack: () -> Unit,
    vm: ArtistViewModel = viewModel(),
) {
    LaunchedEffect(artistId) { vm.load(artistId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val page = state.page
    var descExpanded by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        when {
            state.loading && page == null -> LoadingBox()
            state.error != null && page == null ->
                ErrorBox(state.error!!, onRetry = { vm.load(artistId, force = true) })
            page != null -> {
                val top = page.topSongs
                val topSongs = top?.items?.filterIsInstance<SongItem>().orEmpty()
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    // Header foto artis
                    item {
                        Box(Modifier.fillMaxWidth().height(360.dp)) {
                            AsyncImage(
                                model = page.thumbnail,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(
                                        0f to Color.Black.copy(alpha = 0.3f),
                                        0.45f to Color.Transparent,
                                        1f to MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                            Text(
                                page.name,
                                style = MaterialTheme.typography.headlineLarge,
                                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                            )
                        }
                    }
                    page.subscribers?.let { subs ->
                        item {
                            Text(
                                "$subs subscriber",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    if (topSongs.isNotEmpty()) {
                        item {
                            PlayShuffleButtons(
                                onPlay = { onPlaySongs(topSongs, 0) },
                                onShuffle = { onShuffle(topSongs) },
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        item {
                            val more = top?.moreBrowseId?.takeIf { isOpenableCollection(it) }
                            SectionTitle(
                                top?.title?.ifBlank { null } ?: "Lagu teratas",
                                action = if (more != null) "Lihat semua" else null,
                                onAction = more?.let { id -> { onOpenCollection(id) } }
                            )
                        }
                        itemsIndexed(topSongs, key = { i, s -> "top-$i-${s.id}" }) { i, song ->
                            SongRow(
                                song = song,
                                index = i + 1,
                                isCurrent = song.id == currentSongId,
                                onClick = { onPlaySongs(topSongs, i) }
                            )
                        }
                    }
                    // Album, Single & EP, Video, Playlist, Artis serupa, dll
                    val others = page.sections.filter { it !== top }
                    itemsIndexed(others, key = { i, s -> "sec-$i-${s.title}" }) { _, section ->
                        SectionView(section, currentSongId, onPlaySongs, onOpenItem, onOpenCollection)
                    }
                    page.description?.let { desc ->
                        item { SectionTitle("Tentang") }
                        item {
                            Text(
                                desc,
                                maxLines = if (descExpanded) Int.MAX_VALUE else 4,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .clickable { descExpanded = !descExpanded }
                            )
                        }
                    }
                }
            }
        }
        BackButton(onBack, Modifier.statusBarsPadding().padding(8.dp))
    }
}

@Composable
fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onBack,
        modifier = modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.4f))
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = Color.White)
    }
}
