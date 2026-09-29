package com.example.musikku.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musikku.AppModule
import com.example.musikku.data.ytmusic.SongItem
import com.example.musikku.ui.components.ErrorBox
import com.example.musikku.ui.components.ListItemRow
import com.example.musikku.ui.components.SongRow

private val browseCategories = listOf(
    "Pop Indonesia" to Color(0xFFE13300),
    "Dangdut" to Color(0xFF8C1932),
    "K-Pop" to Color(0xFFE8115B),
    "Hip Hop" to Color(0xFFBA5D07),
    "Rock" to Color(0xFF477D95),
    "Jazz" to Color(0xFF1E3264),
    "Indie" to Color(0xFF608108),
    "Lo-fi" to Color(0xFF503750),
    "Lagu Galau" to Color(0xFF148A08),
    "Band Indonesia" to Color(0xFF777777),
    "Pop Sunda" to Color(0xFF8D67AB),
    "Koplo" to Color(0xFFDC148C),
)

@Composable
fun SearchScreen(
    currentSongId: String?,
    onPlaySong: (SongItem) -> Unit,
    onArtistClick: (String) -> Unit,
    onCollectionClick: (String) -> Unit,
    vm: SearchViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val history by AppModule.searchHistory.queries.collectAsStateWithLifecycle()
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
            placeholder = { Text("Lagu, artis, album, playlist") },
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
            BrowseGrid(
                history = history,
                onHistoryClick = { vm.searchNow(it); focus.clearFocus() },
                onHistoryRemove = { AppModule.searchHistory.remove(it) },
                onClearHistory = { AppModule.searchHistory.clear() },
                onCategoryClick = { vm.searchNow(it); focus.clearFocus() },
            )
            return@Column
        }

        // Saran kata kunci tampil selagi mengetik; begitu pencarian selesai diganti hasil.
        val showSuggestions = state.suggestions.isNotEmpty() && state.searchedQuery != state.query.trim()
        if (showSuggestions) {
            SuggestionsList(state.suggestions, onClick = { vm.searchNow(it); focus.clearFocus() })
            return@Column
        }

        ScrollableTabRow(
            selectedTabIndex = state.tab.ordinal,
            edgePadding = 8.dp,
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
                    SearchTab.SONGS -> items(state.songs, key = { it.id }) { song ->
                        SongRow(
                            song = song,
                            isCurrent = song.id == currentSongId,
                            onClick = { focus.clearFocus(); onPlaySong(song) }
                        )
                    }
                    SearchTab.ARTISTS -> items(state.artists, key = { it.id }) { artist ->
                        ListItemRow(
                            imageUrl = artist.thumbnail,
                            title = artist.title,
                            subtitle = "Artis • ${artist.subtitle}",
                            circle = true,
                            onClick = { focus.clearFocus(); onArtistClick(artist.id) }
                        )
                    }
                    SearchTab.ALBUMS -> items(state.albums, key = { it.id }) { album ->
                        ListItemRow(
                            imageUrl = album.thumbnail,
                            title = album.title,
                            subtitle = album.subtitle,
                            onClick = { focus.clearFocus(); onCollectionClick(album.id) }
                        )
                    }
                    SearchTab.PLAYLISTS -> items(state.playlists, key = { it.id }) { pl ->
                        ListItemRow(
                            imageUrl = pl.thumbnail,
                            title = pl.title,
                            subtitle = pl.subtitle,
                            onClick = { focus.clearFocus(); onCollectionClick(pl.id) }
                        )
                    }
                }
            }
        }
    }
}

private fun isEmpty(s: SearchUiState) = when (s.tab) {
    SearchTab.SONGS -> s.songs.isEmpty()
    SearchTab.ARTISTS -> s.artists.isEmpty()
    SearchTab.ALBUMS -> s.albums.isEmpty()
    SearchTab.PLAYLISTS -> s.playlists.isEmpty()
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

/** Daftar saran kata kunci ala YouTube Music — tap untuk langsung mencari. */
@Composable
private fun SuggestionsList(suggestions: List<String>, onClick: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
        items(suggestions, key = { it }) { text ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onClick(text) }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun BrowseGrid(
    history: List<String>,
    onHistoryClick: (String) -> Unit,
    onHistoryRemove: (String) -> Unit,
    onClearHistory: () -> Unit,
    onCategoryClick: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (history.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "history-header") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Pencarian terakhir",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Hapus semua",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onClearHistory)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            history.forEach { query ->
                item(span = { GridItemSpan(maxLineSpan) }, key = "history-$query") {
                    HistoryRow(
                        query = query,
                        onClick = { onHistoryClick(query) },
                        onRemove = { onHistoryRemove(query) },
                    )
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }, key = "browse-header") {
            Text(
                "Jelajahi semua",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = if (history.isNotEmpty()) Modifier.padding(top = 12.dp) else Modifier
            )
        }
        items(browseCategories, key = { it.first }) { (name, color) ->
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

/** Satu baris riwayat pencarian: ikon jam, kata kunci, tombol hapus. */
@Composable
private fun HistoryRow(query: String, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(
            query,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, "Hapus dari riwayat", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
