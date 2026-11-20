package com.vyora.music.ui.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vyora.music.data.ytmusic.SongItem
import com.vyora.music.ui.artist.BackButton
import com.vyora.music.ui.components.Artwork
import com.vyora.music.ui.components.ErrorBox
import com.vyora.music.ui.components.LoadingBox
import com.vyora.music.ui.components.PlayShuffleButtons
import com.vyora.music.ui.components.SongRow
import com.vyora.music.ui.player.rememberCoverPalette
import com.vyora.music.ui.playlist.PlaylistPicker
import com.vyora.music.ui.theme.LocalBottomOverlayPadding
import com.vyora.music.ui.theme.WarmHairline
import com.vyora.music.ui.theme.WarmInk
import com.vyora.music.ui.theme.WarmMuted

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
    val bottomPadding = LocalBottomOverlayPadding.current
    val coverPalette = rememberCoverPalette(page?.thumbnail)

    Box(Modifier.fillMaxSize()) {
        when {
            state.loading && page == null -> LoadingBox()
            state.error != null && page == null ->
                ErrorBox(state.error!!, onRetry = { vm.load(browseId, force = true) })
            page != null -> LazyColumn(contentPadding = PaddingValues(bottom = bottomPadding + 20.dp)) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(coverPalette.ambient.copy(alpha = 0.85f), MaterialTheme.colorScheme.background)
                                )
                            )
                            .statusBarsPadding()
                            .padding(top = 56.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .shadow(20.dp, RoundedCornerShape(22.dp))
                                .clip(RoundedCornerShape(22.dp))
                                .border(1.dp, WarmHairline, RoundedCornerShape(22.dp))
                                .padding(6.dp),
                        ) {
                            Artwork(page.thumbnail, Modifier.size(220.dp), shape = RoundedCornerShape(16.dp))
                        }
                    }
                }
                item {
                    Column(Modifier.padding(horizontal = 18.dp)) {
                        Text(page.title, style = MaterialTheme.typography.headlineMedium, color = WarmInk)
                        Spacer(Modifier.height(6.dp))
                        page.author?.takeIf { it.name.isNotBlank() }?.let { author ->
                            Text(
                                author.name,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable(enabled = author.id != null) { author.id?.let(onArtistClick) },
                            )
                        }
                        Text(
                            listOf(page.subtitle, page.secondSubtitle).filter { it.isNotBlank() }.joinToString(" • "),
                            color = WarmMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                if (page.songs.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                            IconButton(onClick = { PlaylistPicker.open(page.songs) }, modifier = Modifier.padding(start = 8.dp)) {
                                Icon(Icons.AutoMirrored.Filled.PlaylistAdd, "Simpan ke playlist", Modifier.size(26.dp), tint = WarmInk)
                            }
                            Spacer(Modifier.weight(1f))
                            PlayShuffleButtons(
                                onPlay = { onPlaySongs(page.songs, 0) },
                                onShuffle = { onShuffle(page.songs) },
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            "Playlist ini kosong.",
                            color = WarmMuted,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
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
                        onClick = { onPlaySongs(page.songs, i) },
                    )
                }
            }
        }
        BackButton(onBack, Modifier.statusBarsPadding().padding(10.dp))
    }
}
