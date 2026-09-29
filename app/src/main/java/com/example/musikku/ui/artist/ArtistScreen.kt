package com.example.musikku.ui.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.musikku.data.model.Track
import com.example.musikku.ui.components.AlbumCard
import com.example.musikku.ui.components.ArtistCircle
import com.example.musikku.ui.components.ErrorBox
import com.example.musikku.ui.components.LoadingBox
import com.example.musikku.ui.components.PlayShuffleButtons
import com.example.musikku.ui.components.SectionTitle
import com.example.musikku.ui.components.TrackRow
import com.example.musikku.ui.components.formatFans

@Composable
fun ArtistScreen(
    artistId: Long,
    currentTrackId: Long?,
    onPlay: (List<Track>, Int) -> Unit,
    onShuffle: (List<Track>) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onArtistClick: (Long) -> Unit,
    onBack: () -> Unit,
    vm: ArtistViewModel = viewModel(),
) {
    LaunchedEffect(artistId) { vm.load(artistId) }
    val state by vm.state.collectAsStateWithLifecycle()
    var showAllTracks by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        when {
            state.loading && state.artist == null -> LoadingBox()
            state.error != null && state.artist == null ->
                ErrorBox(state.error!!, onRetry = { vm.load(artistId, force = true) })
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                // Header foto artis
                item {
                    Box(Modifier.fillMaxWidth().height(340.dp)) {
                        AsyncImage(
                            model = state.artist?.pictureXl ?: state.artist?.pictureBig,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 0.3f),
                                    0.5f to Color.Transparent,
                                    1f to MaterialTheme.colorScheme.background
                                )
                            )
                        )
                        Text(
                            state.artist?.name.orEmpty(),
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                        )
                    }
                }
                item {
                    Text(
                        formatFans(state.artist?.nbFan ?: 0),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                item {
                    PlayShuffleButtons(
                        onPlay = { onPlay(state.topTracks, 0) },
                        onShuffle = { onShuffle(state.topTracks) },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (state.topTracks.isNotEmpty()) {
                    item { SectionTitle("Populer") }
                    val shown = if (showAllTracks) state.topTracks else state.topTracks.take(5)
                    itemsIndexed(shown, key = { _, t -> "top${t.id}" }) { i, track ->
                        TrackRow(
                            track = track,
                            index = i + 1,
                            isCurrent = track.id == currentTrackId,
                            onClick = { onPlay(state.topTracks, i) }
                        )
                    }
                    if (state.topTracks.size > 5) {
                        item {
                            TextButton(
                                onClick = { showAllTracks = !showAllTracks },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) { Text(if (showAllTracks) "Tampilkan lebih sedikit" else "Lihat semua") }
                        }
                    }
                }

                if (state.albums.isNotEmpty()) {
                    item { SectionTitle("Diskografi") }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
                            items(state.albums, key = { it.id }) { album ->
                                AlbumCard(
                                    album,
                                    subtitle = listOfNotNull(
                                        album.releaseDate?.take(4),
                                        album.recordType?.replaceFirstChar { it.uppercase() }
                                    ).joinToString(" • "),
                                    onClick = { onAlbumClick(album.id) }
                                )
                            }
                        }
                    }
                }

                if (state.related.isNotEmpty()) {
                    item { SectionTitle("Penggemar juga menyukai") }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
                            items(state.related, key = { it.id }) { artist ->
                                ArtistCircle(artist, onClick = { onArtistClick(artist.id) })
                            }
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
