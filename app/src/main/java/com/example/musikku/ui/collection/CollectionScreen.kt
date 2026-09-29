package com.example.musikku.ui.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import com.example.musikku.ui.playlist.PlaylistPicker
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musikku.data.ytmusic.SongItem
import com.example.musikku.ui.artist.BackButton
import com.example.musikku.ui.components.Artwork
import com.example.musikku.ui.components.ErrorBox
import com.example.musikku.ui.components.LoadingBox
import com.example.musikku.ui.components.PlayShuffleButtons
import com.example.musikku.ui.components.SongRow

/** Halaman album ATAU playlist YouTube Music. */
@Composable
fun CollectionScreen(
    browseId: String,
    currentSongId: String?,
    onPlaySongs: (List<SongItem>, Int) -> Unit,
    onShuffle: (List<SongItem>) -> Unit,
    onArtistClick: (String) -> Unit,
    onBack: () -> Unit,
    vm: CollectionViewModel = viewModel(),
) {
    LaunchedEffect(browseId) { vm.load(browseId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val page = state.page
    val isAlbum = browseId.startsWith("MPRE")

    Box(Modifier.fillMaxSize()) {
        when {
            state.loading && page == null -> LoadingBox()
            state.error != null && page == null ->
                ErrorBox(state.error!!, onRetry = { vm.load(browseId, force = true) })
            page != null -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color(0xFF3F3F46), MaterialTheme.colorScheme.background)))
                            .statusBarsPadding()
                            .padding(top = 56.dp, bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Artwork(page.thumbnail, Modifier.size(230.dp))
                    }
                }
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text(page.title, style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(6.dp))
                        page.author?.takeIf { it.name.isNotBlank() }?.let { author ->
                            Text(
                                author.name,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable(enabled = author.id != null) { author.id?.let(onArtistClick) }
                            )
                        }
                        Text(
                            listOf(page.subtitle, page.secondSubtitle).filter { it.isNotBlank() }.joinToString(" • "),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (page.songs.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { PlaylistPicker.open(page.songs) }, modifier = Modifier.padding(start = 8.dp)) {
                                Icon(Icons.AutoMirrored.Filled.PlaylistAdd, "Simpan ke playlist", Modifier.size(28.dp))
                            }
                            Spacer(Modifier.weight(1f))
                            PlayShuffleButtons(
                                onPlay = { onPlaySongs(page.songs, 0) },
                                onShuffle = { onShuffle(page.songs) },
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            "Playlist ini kosong.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
                itemsIndexed(page.songs, key = { i, s -> "$i-${s.id}" }) { i, song ->
                    SongRow(
                        song = song,
                        index = if (isAlbum) i + 1 else null,
                        showArtwork = !isAlbum,
                        showAlbum = !isAlbum,
                        isCurrent = song.id == currentSongId,
                        onClick = { onPlaySongs(page.songs, i) }
                    )
                }
            }
        }
        BackButton(onBack, Modifier.statusBarsPadding().padding(8.dp))
    }
}
