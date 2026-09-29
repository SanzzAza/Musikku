package com.example.musikku.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musikku.data.model.Track
import com.example.musikku.ui.components.ErrorBox
import com.example.musikku.ui.components.ListItemRow
import com.example.musikku.ui.components.TrackRow
import com.example.musikku.ui.components.formatFans

private val browseCategories = listOf(
    "Pop Indonesia" to Color(0xFFE13300),
    "Dangdut" to Color(0xFF8C1932),
    "K-Pop" to Color(0xFFE8115B),
    "Hip Hop" to Color(0xFFBA5D07),
    "Rock" to Color(0xFF477D95),
    "Jazz" to Color(0xFF1E3264),
    "Indie" to Color(0xFF608108),
    "Lo-fi" to Color(0xFF503750),
    "Galau" to Color(0xFF148A08),
    "Workout" to Color(0xFF777777),
)

@Composable
fun SearchScreen(
    currentTrackId: Long?,
    onPlay: (List<Track>, Int) -> Unit,
    onArtistClick: (Long) -> Unit,
    onAlbumClick: (Long) -> Unit,
    vm: SearchViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            "Cari",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 12.dp)
        )

        TextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            placeholder = { Text("Artis, lagu, atau album") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { vm.onQueryChange("") }) { Icon(Icons.Default.Clear, "Hapus") }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.searchNow(); focus.clearFocus() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedLeadingIconColor = Color.Black,
                unfocusedLeadingIconColor = Color.Black,
                focusedTrailingIconColor = Color.Black,
                unfocusedTrailingIconColor = Color.Black,
                focusedPlaceholderColor = Color.DarkGray,
                unfocusedPlaceholderColor = Color.DarkGray,
                cursorColor = Color.Black,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        if (state.query.isBlank()) {
            BrowseGrid(onCategoryClick = { vm.searchNow(it); focus.clearFocus() })
            return@Column
        }

        TabRow(
            selectedTabIndex = state.tab.ordinal,
            containerColor = Color.Transparent,
            indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(positions[state.tab.ordinal]),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            SearchTab.entries.forEach { tab ->
                Tab(
                    selected = state.tab == tab,
                    onClick = { vm.onTabSelected(tab) },
                    text = { Text(tab.label, fontWeight = FontWeight.SemiBold) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = MaterialTheme.colorScheme.primary)
        }

        when {
            state.error != null -> ErrorBox(state.error!!, onRetry = { vm.searchNow() })
            state.searched && !state.loading && isEmpty(state) -> EmptyResult(state.query)
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                when (state.tab) {
                    SearchTab.SONGS -> itemsIndexed(state.tracks, key = { _, t -> t.id }) { i, track ->
                        TrackRow(
                            track = track,
                            isCurrent = track.id == currentTrackId,
                            onClick = { focus.clearFocus(); onPlay(state.tracks, i) }
                        )
                    }
                    SearchTab.ARTISTS -> items(state.artists, key = { it.id }) { artist ->
                        ListItemRow(
                            imageUrl = artist.pictureMedium,
                            title = artist.name.orEmpty(),
                            subtitle = "Artis • ${formatFans(artist.nbFan)}",
                            circle = true,
                            onClick = { focus.clearFocus(); onArtistClick(artist.id) }
                        )
                    }
                    SearchTab.ALBUMS -> items(state.albums, key = { it.id }) { album ->
                        ListItemRow(
                            imageUrl = album.coverMedium,
                            title = album.title.orEmpty(),
                            subtitle = "${album.recordType?.replaceFirstChar { it.uppercase() } ?: "Album"} • ${album.artist?.name.orEmpty()}",
                            onClick = { focus.clearFocus(); onAlbumClick(album.id) }
                        )
                    }
                }
            }
        }
    }
}

private fun isEmpty(s: SearchUiState) = when (s.tab) {
    SearchTab.SONGS -> s.tracks.isEmpty()
    SearchTab.ARTISTS -> s.artists.isEmpty()
    SearchTab.ALBUMS -> s.albums.isEmpty()
}

@Composable
private fun EmptyResult(query: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Tidak ada hasil untuk \"$query\"", style = MaterialTheme.typography.titleMedium)
            Text(
                "Coba kata kunci lain atau periksa ejaan.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun BrowseGrid(onCategoryClick: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text("Jelajahi semua", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(browseCategories) { (name, color) ->
            Box(
                Modifier
                    .height(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
                    .clickable { onCategoryClick(name) }
                    .padding(12.dp)
            ) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
