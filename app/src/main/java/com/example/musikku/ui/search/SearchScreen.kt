package com.example.musikku.ui.search

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musikku.AppModule
import com.example.musikku.data.ytmusic.AlbumItem
import com.example.musikku.data.ytmusic.ArtistItem
import com.example.musikku.data.ytmusic.SongItem
import com.example.musikku.data.ytmusic.YTItem
import com.example.musikku.ui.components.Artwork
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
    val recents by AppModule.recents.items.collectAsStateWithLifecycle()
    val liked by AppModule.favorites.items.collectAsStateWithLifecycle()

    // Mode cari aktif = kolom pencarian dibuka (seperti Spotify)
    var searchMode by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val exitSearch = {
        vm.onQueryChange("")
        focus.clearFocus()
        searchMode = false
    }
    BackHandler(enabled = searchMode) { exitSearch() }

    // Setiap item yang dibuka dari pencarian masuk ke "Baru Diputar"
    val openSong: (SongItem) -> Unit = { AppModule.recents.add(it); focus.clearFocus(); onPlaySong(it) }
    val openArtist: (ArtistItem) -> Unit = { AppModule.recents.add(it); focus.clearFocus(); onArtistClick(it.id) }
    val openCollection: (AlbumItem) -> Unit = { AppModule.recents.add(it); focus.clearFocus(); onCollectionClick(it.id) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        if (!searchMode) {
            Text(
                "Cari",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 12.dp)
            )
            // Kotak putih: ketuk untuk membuka mode cari
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .clickable { searchMode = true }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = Color.Black)
                Spacer(Modifier.width(12.dp))
                Text("Apa yang ingin kamu dengarkan?", color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
            }
            BrowseGrid(
                history = history,
                onHistoryClick = { searchMode = true; vm.searchNow(it) },
                onHistoryRemove = { AppModule.searchHistory.remove(it) },
                onClearHistory = { AppModule.searchHistory.clear() },
                onCategoryClick = { searchMode = true; vm.searchNow(it) },
            )
            return@Column
        }

        LaunchedEffect(Unit) { if (state.query.isBlank()) focusRequester.requestFocus() }

        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F1F23))
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = exitSearch) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
            }
            TextField(
                value = state.query,
                onValueChange = vm::onQueryChange,
                placeholder = { Text("Apa yang ingin kamu dengarkan?", maxLines = 1) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { vm.onQueryChange(""); focusRequester.requestFocus() }) {
                            Icon(Icons.Default.Clear, "Hapus")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(50),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.searchNow(); focus.clearFocus() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF2E2E33),
                    unfocusedContainerColor = Color(0xFF2E2E33),
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.weight(1f).focusRequester(focusRequester)
            )
        }

        if (state.query.isBlank()) {
            RecentList(
                items = recents,
                likedIds = liked.map { it.id }.toSet(),
                onOpen = { item ->
                    when (item) {
                        is SongItem -> openSong(item)
                        is ArtistItem -> openArtist(item)
                        is AlbumItem -> openCollection(item)
                    }
                },
                onRemove = { AppModule.recents.remove(it) },
                onClearAll = { AppModule.recents.clear() },
                onToggleLike = { AppModule.favorites.toggle(it) },
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
                            onClick = { openSong(song) }
                        )
                    }
                    SearchTab.ARTISTS -> items(state.artists, key = { it.id }) { artist ->
                        ListItemRow(
                            imageUrl = artist.thumbnail,
                            title = artist.title,
                            subtitle = "Artis • ${artist.subtitle}",
                            circle = true,
                            onClick = { openArtist(artist) }
                        )
                    }
                    SearchTab.ALBUMS -> items(state.albums, key = { it.id }) { album ->
                        ListItemRow(
                            imageUrl = album.thumbnail,
                            title = album.title,
                            subtitle = album.subtitle,
                            onClick = { openCollection(album) }
                        )
                    }
                    SearchTab.PLAYLISTS -> items(state.playlists, key = { it.id }) { pl ->
                        ListItemRow(
                            imageUrl = pl.thumbnail,
                            title = pl.title,
                            subtitle = pl.subtitle,
                            onClick = { openCollection(pl) }
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

/** Daftar "Baru Diputar" ala Spotify. */
@Composable
private fun RecentList(
    items: List<YTItem>,
    likedIds: Set<String>,
    onOpen: (YTItem) -> Unit,
    onRemove: (YTItem) -> Unit,
    onClearAll: () -> Unit,
    onToggleLike: (SongItem) -> Unit,
) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Putar apa yang kamu suka", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Cari artis, lagu, album, dan playlist.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Text(
                "Baru Diputar",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
        }
        items(items, key = { "recent-${it.id}" }) { item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(item) }
                    .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isArtist = item is ArtistItem
                Artwork(
                    item.thumbnail,
                    Modifier.size(56.dp),
                    shape = if (isArtist) CircleShape else RoundedCornerShape(4.dp),
                    placeholder = if (isArtist) Icons.Default.Person else Icons.Default.MusicNote
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            item.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isArtist) {
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Default.Verified, null, Modifier.size(16.dp), tint = Color(0xFF7FD7A6))
                        }
                    }
                    Text(
                        recentSubtitle(item), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item is SongItem) {
                    val isLiked = item.id in likedIds
                    IconButton(onClick = { onToggleLike(item) }) {
                        Icon(
                            if (isLiked) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                            if (isLiked) "Hapus dari Koleksi" else "Simpan ke Koleksi",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { onRemove(item) }) {
                    Icon(Icons.Default.Close, "Hapus dari riwayat", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                OutlinedButton(onClick = onClearAll) { Text("Hapus riwayat") }
            }
        }
    }
}

private fun recentSubtitle(item: YTItem): String = when (item) {
    is ArtistItem -> "Artis"
    is SongItem -> listOf("Lagu", item.artistsText).filter { it.isNotBlank() }.joinToString(" • ")
    is AlbumItem -> if (item.isPlaylist) "Playlist" else {
        val parts = item.subtitle.split(" • ").map { it.trim() }.filter { it.isNotBlank() }
        (listOf("Album") + parts.filterNot { it.equals("Album", true) || it.equals("Single", true) || it.equals("EP", true) }.take(1)).joinToString(" • ")
    }
}
