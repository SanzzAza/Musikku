package com.example.musikku.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musikku.data.model.Track
import com.example.musikku.ui.components.AlbumCard
import com.example.musikku.ui.components.ArtistCircle
import com.example.musikku.ui.components.ErrorBox
import com.example.musikku.ui.components.LoadingBox
import com.example.musikku.ui.components.SectionTitle
import com.example.musikku.ui.components.TrackRow
import java.util.Calendar

@Composable
fun HomeScreen(
    currentTrackId: Long?,
    onPlay: (List<Track>, Int) -> Unit,
    onArtistClick: (Long) -> Unit,
    onAlbumClick: (Long) -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    when {
        state.loading && state.tracks.isEmpty() -> LoadingBox()
        state.error != null && state.tracks.isEmpty() -> ErrorBox(state.error!!, onRetry = vm::load)
        else -> LazyColumn(
            modifier = Modifier.statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text(
                    greeting(),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, end = 16.dp)
                )
            }

            if (state.artists.isNotEmpty()) {
                item { SectionTitle("Artis Populer") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
                        items(state.artists, key = { it.id }) { artist ->
                            ArtistCircle(artist, onClick = { onArtistClick(artist.id) })
                        }
                    }
                }
            }

            if (state.albums.isNotEmpty()) {
                item { SectionTitle("Album Populer") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
                        items(state.albums, key = { it.id }) { album ->
                            AlbumCard(
                                album,
                                subtitle = album.artist?.name.orEmpty(),
                                onClick = { onAlbumClick(album.id) }
                            )
                        }
                    }
                }
            }

            item { SectionTitle("Lagu Trending") }
            itemsIndexed(state.tracks, key = { _, t -> "t${t.id}" }) { index, track ->
                TrackRow(
                    track = track,
                    index = index + 1,
                    isCurrent = track.id == currentTrackId,
                    onClick = { onPlay(state.tracks, index) }
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 4..10 -> "Selamat pagi"
    in 11..14 -> "Selamat siang"
    in 15..17 -> "Selamat sore"
    else -> "Selamat malam"
}
