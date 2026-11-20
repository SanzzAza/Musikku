package com.vyora.music.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vyora.music.AppModule
import com.vyora.music.data.ytmusic.AlbumItem
import com.vyora.music.data.ytmusic.ArtistItem
import com.vyora.music.data.ytmusic.SongItem
import com.vyora.music.data.ytmusic.YTItem
import com.vyora.music.ui.components.Artwork
import com.vyora.music.ui.components.ErrorBox
import com.vyora.music.ui.components.ListItemRow
import com.vyora.music.ui.components.SongRow
import com.vyora.music.ui.components.EmptyState
import com.vyora.music.ui.components.SkeletonRowList
import com.vyora.music.ui.theme.warmWash
import com.vyora.music.ui.theme.GlassChip
import com.vyora.music.ui.theme.LocalBottomOverlayPadding
import com.vyora.music.ui.theme.LocalUiPrefs
import com.vyora.music.ui.theme.WarmFaint
import com.vyora.music.ui.theme.WarmHairline
import com.vyora.music.ui.theme.WarmInk
import com.vyora.music.ui.theme.WarmMuted
import com.vyora.music.ui.theme.WarmSurface

private val browseCategories = listOf(
    "Pop Indonesia" to Color(0xFFB64B30),
    "Lagu Galau" to Color(0xFF7B3E52),
    "Indie" to Color(0xFF476A55),
    "Lo-fi" to Color(0xFF4A4469),
    "K-Pop" to Color(0xFF9F3E68),
    "Jazz" to Color(0xFF3A5472),
    "Rock" to Color(0xFF7D4838),
    "Hip Hop" to Color(0xFF8C5B2B),
    "Band Indonesia" to Color(0xFF5A524C),
    "Dangdut" to Color(0xFF853742),
    "Pop Sunda" to Color(0xFF5A6D4A),
    "Koplo" to Color(0xFF94445B),
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
    val prefs = LocalUiPrefs.current
    val bottomPadding = LocalBottomOverlayPadding.current
    val accent = MaterialTheme.colorScheme.primary

    var searchMode by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val exitSearch = {
        vm.onQueryChange("")
        focus.clearFocus()
        searchMode = false
    }
    BackHandler(enabled = searchMode) { exitSearch() }

    val openSong: (SongItem) -> Unit = { AppModule.recents.add(it); focus.clearFocus(); onPlaySong(it) }
    val openArtist: (ArtistItem) -> Unit = { AppModule.recents.add(it); focus.clearFocus(); onArtistClick(it.id) }
    val openCollection: (AlbumItem) -> Unit = { AppModule.recents.add(it); focus.clearFocus(); onCollectionClick(it.id) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        if (!searchMode) {
            Text(
                text = "Cari",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                color = WarmInk,
                modifier = Modifier.padding(start = 18.dp, top = 16.dp, bottom = 12.dp),
            )

            // Kotak pencarian kaca / editorial hangat
            val barShape = if (prefs.isGlass) CircleShape else RoundedCornerShape(14.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(barShape)
                    .background(if (prefs.isGlass) warmWash(0.09f) else WarmSurface)
                    .border(
                        1.dp,
                        if (prefs.isGlass) warmWash(0.20f) else WarmHairline,
                        barShape,
                    )
                    .clickable { searchMode = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = accent, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    "Cari lagu, artis, album, atau lirik…",
                    color = WarmMuted,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            BrowseGrid(
                history = history,
                bottomPadding = bottomPadding + 16.dp,
                onHistoryClick = { searchMode = true; vm.searchNow(it) },
                onHistoryRemove = { AppModule.searchHistory.remove(it) },
                onClearHistory = { AppModule.searchHistory.clear() },
                onCategoryClick = { searchMode = true; vm.searchNow(it) },
            )
            return@Column
        }

        LaunchedEffect(Unit) { if (state.query.isBlank()) focusRequester.requestFocus() }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = exitSearch) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali", tint = WarmInk)
            }
            val fieldShape = if (prefs.isGlass) CircleShape else RoundedCornerShape(14.dp)
            TextField(
                value = state.query,
                onValueChange = vm::onQueryChange,
                placeholder = { Text("Apa yang ingin kamu dengarkan?", maxLines = 1, color = WarmMuted) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { vm.onQueryChange(""); focusRequester.requestFocus() }) {
                            Icon(Icons.Rounded.Clear, "Hapus", tint = WarmMuted)
                        }
                    }
                },
                singleLine = true,
                shape = fieldShape,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.searchNow(); focus.clearFocus() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = if (prefs.isGlass) warmWash(0.09f) else WarmSurface,
                    unfocusedContainerColor = if (prefs.isGlass) warmWash(0.07f) else WarmSurface,
                    focusedTextColor = WarmInk,
                    unfocusedTextColor = WarmInk,
                    cursorColor = accent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier
                    .weight(1f)
                    .border(
                        1.dp,
                        if (prefs.isGlass) warmWash(0.18f) else WarmHairline,
                        fieldShape,
                    )
                    .focusRequester(focusRequester),
            )
        }

        if (state.query.isBlank()) {
            RecentList(
                items = recents,
                likedIds = liked.map { it.id }.toSet(),
                bottomPadding = bottomPadding + 16.dp,
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

        val showSuggestions = state.suggestions.isNotEmpty() && state.searchedQuery != state.query.trim()
        if (showSuggestions) {
            SuggestionsList(
                suggestions = state.suggestions,
                bottomPadding = bottomPadding + 16.dp,
                onClick = { vm.searchNow(it); focus.clearFocus() },
            )
            return@Column
        }

        // Tab hasil pencarian bergaya Glass Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SearchTab.entries.forEach { tab ->
                GlassChip(
                    label = tab.label,
                    selected = state.tab == tab,
                    onClick = { vm.onTabSelected(tab) },
                )
            }
        }

        if (state.loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = accent,
                trackColor = Color.Transparent,
            )
        }

        when {
            state.error != null -> ErrorBox(state.error!!, onRetry = { vm.searchNow() })
            // Pencarian pertama: tampilkan kerangka baris, bukan layar kosong.
            state.loading && !state.searched -> SkeletonRowList(
                rows = 7,
                modifier = Modifier.fillMaxSize(),
            )
            state.searched && !state.loading && isEmpty(state) -> EmptyResult(state.query)
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 6.dp, bottom = bottomPadding + 16.dp),
            ) {
                when (state.tab) {
                    SearchTab.SONGS -> items(state.songs, key = { it.id }) { song ->
                        SongRow(
                            song = song,
                            isCurrent = song.id == currentSongId,
                            onClick = { openSong(song) },
                        )
                    }
                    SearchTab.ARTISTS -> items(state.artists, key = { it.id }) { artist ->
                        ListItemRow(
                            imageUrl = artist.thumbnail,
                            title = artist.title,
                            subtitle = "Artis • ${artist.subtitle}",
                            circle = true,
                            onClick = { openArtist(artist) },
                        )
                    }
                    SearchTab.ALBUMS -> items(state.albums, key = { it.id }) { album ->
                        ListItemRow(
                            imageUrl = album.thumbnail,
                            title = album.title,
                            subtitle = album.subtitle,
                            onClick = { openCollection(album) },
                        )
                    }
                    SearchTab.PLAYLISTS -> items(state.playlists, key = { it.id }) { pl ->
                        ListItemRow(
                            imageUrl = pl.thumbnail,
                            title = pl.title,
                            subtitle = pl.subtitle,
                            onClick = { openCollection(pl) },
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
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            icon = Icons.Rounded.SearchOff,
            title = "Tidak ada hasil untuk \"$query\"",
            message = "Coba kata kunci lain atau periksa ejaan.",
        )
    }
}

@Composable
private fun SuggestionsList(
    suggestions: List<String>,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onClick: (String) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 6.dp, bottom = bottomPadding),
    ) {
        items(suggestions, key = { it }) { text ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onClick(text) }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = WarmMuted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis, color = WarmInk)
            }
        }
    }
}

@Composable
private fun BrowseGrid(
    history: List<String>,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onHistoryClick: (String) -> Unit,
    onHistoryRemove: (String) -> Unit,
    onClearHistory: () -> Unit,
    onCategoryClick: (String) -> Unit,
) {
    val prefs = LocalUiPrefs.current
    val tileShape = RoundedCornerShape(if (prefs.isGlass) 18.dp else 13.dp)

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottomPadding),
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
                        color = WarmInk,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "Hapus semua",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onClearHistory)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
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
                "Jelajahi suasana & genre",
                style = MaterialTheme.typography.titleLarge,
                color = WarmInk,
                modifier = if (history.isNotEmpty()) Modifier.padding(top = 12.dp) else Modifier,
            )
        }
        items(browseCategories, key = { it.first }) { (name, color) ->
            Box(
                modifier = Modifier
                    .height(86.dp)
                    .clip(tileShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                color.copy(alpha = if (prefs.isGlass) 0.52f else 0.36f),
                                color.copy(alpha = 0.18f),
                                WarmSurface,
                            )
                        )
                    )
                    .border(
                        1.dp,
                        if (prefs.isGlass) warmWash(0.16f) else WarmHairline,
                        tileShape,
                    )
                    .clickable { onCategoryClick(name) }
                    .padding(14.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WarmInk,
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(query: String, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.History, null, tint = WarmMuted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            query,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = WarmInk,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Rounded.Close, "Hapus dari riwayat", tint = WarmFaint, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun RecentList(
    items: List<YTItem>,
    likedIds: Set<String>,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onOpen: (YTItem) -> Unit,
    onRemove: (YTItem) -> Unit,
    onClearAll: () -> Unit,
    onToggleLike: (SongItem) -> Unit,
) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Rounded.Search,
                title = "Putar apa yang kamu suka",
                message = "Cari artis, lagu, album, dan playlist. Riwayat putarmu akan muncul di sini.",
            )
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding),
    ) {
        item {
            Text(
                "Baru Diputar",
                style = MaterialTheme.typography.titleLarge,
                color = WarmInk,
                modifier = Modifier.padding(start = 18.dp, top = 12.dp, bottom = 8.dp),
            )
        }
        items(items, key = { "recent-${it.id}" }) { item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(item) }
                    .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val isArtist = item is ArtistItem
                Artwork(
                    item.thumbnail,
                    Modifier.size(54.dp),
                    shape = if (isArtist) CircleShape else RoundedCornerShape(10.dp),
                    placeholder = if (isArtist) Icons.Rounded.Person else Icons.Rounded.MusicNote,
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            item.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = WarmInk,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (isArtist) {
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Rounded.Verified, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(
                        recentSubtitle(item),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmMuted,
                    )
                }
                if (item is SongItem) {
                    val isLiked = item.id in likedIds
                    IconButton(onClick = { onToggleLike(item) }) {
                        Icon(
                            if (isLiked) Icons.Rounded.CheckCircle else Icons.Rounded.AddCircleOutline,
                            if (isLiked) "Hapus dari Pustaka" else "Simpan ke Pustaka",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else WarmMuted,
                        )
                    }
                }
                IconButton(onClick = { onRemove(item) }) {
                    Icon(Icons.Rounded.Close, "Hapus dari riwayat", tint = WarmFaint, modifier = Modifier.size(18.dp))
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(top = 14.dp), contentAlignment = Alignment.Center) {
                OutlinedButton(
                    onClick = onClearAll,
                    border = BorderStroke(1.dp, WarmHairline),
                ) {
                    Text("Hapus riwayat", color = WarmInk)
                }
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
