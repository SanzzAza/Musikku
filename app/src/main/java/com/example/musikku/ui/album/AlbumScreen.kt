package com.example.musikku.ui.album

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musikku.data.model.Track
import com.example.musikku.ui.artist.BackButton
import com.example.musikku.ui.components.Artwork
import com.example.musikku.ui.components.ErrorBox
import com.example.musikku.ui.components.LoadingBox
import com.example.musikku.ui.components.PlayShuffleButtons
import com.example.musikku.ui.components.TrackRow

@Composable
fun AlbumScreen(
    albumId: Long,
    currentTrackId: Long?,
    onPlay: (List<Track>, Int) -> Unit,
    onShuffle: (List<Track>) -> Unit,
    onArtistClick: (Long) -> Unit,
    onBack: () -> Unit,
    vm: AlbumViewModel = viewModel(),
) {
    LaunchedEffect(albumId) { vm.load(albumId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val album = state.album
    val tracks = album?.tracks?.data.orEmpty()

    Box(Modifier.fillMaxSize()) {
        when {
            state.loading && album == null -> LoadingBox()
            state.error != null && album == null ->
                ErrorBox(state.error!!, onRetry = { vm.load(albumId, force = true) })
            album != null -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF3F3F46), MaterialTheme.colorScheme.background)
                                )
                            )
                            .statusBarsPadding()
                            .padding(top = 56.dp, bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Artwork(album.coverXl ?: album.coverBig, Modifier.size(230.dp))
                    }
                }
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text(album.title.orEmpty(), style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            album.artist?.name.orEmpty(),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { album.artist?.id?.let(onArtistClick) }
                        )
                        Text(
                            listOfNotNull(
                                album.recordType?.replaceFirstChar { it.uppercase() },
                                album.releaseDate?.take(4),
                                "${tracks.size} lagu"
                            ).joinToString(" • "),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                item {
                    PlayShuffleButtons(
                        onPlay = { onPlay(tracks, 0) },
                        onShuffle = { onShuffle(tracks) },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                itemsIndexed(tracks, key = { _, t -> t.id }) { i, track ->
                    TrackRow(
                        track = track,
                        index = i + 1,
                        showArtwork = false,
                        isCurrent = track.id == currentTrackId,
                        onClick = { onPlay(tracks, i) }
                    )
                }
            }
        }
        BackButton(onBack, Modifier.statusBarsPadding().padding(8.dp))
    }
}
