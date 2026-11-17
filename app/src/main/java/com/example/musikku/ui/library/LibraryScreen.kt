package com.example.musikku.ui.library

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musikku.AppModule
import com.example.musikku.data.ytmusic.SongItem
import com.example.musikku.ui.import.ImportPlaylistSheet
import com.example.musikku.ui.playlist.LIKED_PLAYLIST_ID
import com.example.musikku.ui.playlist.PlaylistCover
import com.example.musikku.ui.playlist.PlaylistNameDialog
import com.example.musikku.ui.theme.GlassChip
import com.example.musikku.ui.theme.LocalBottomOverlayPadding
import com.example.musikku.ui.theme.LocalCoverPalette
import com.example.musikku.ui.theme.LocalUiPrefs
import com.example.musikku.ui.theme.WarmHairline
import com.example.musikku.ui.theme.WarmInk
import com.example.musikku.ui.theme.WarmMuted
import com.example.musikku.ui.theme.WarmSurface

/** Pustaka: Lagu yang Disukai + playlist buatan sendiri + Backup & Pengaturan Tampilan. */
@Composable
fun LibraryScreen(
    onOpenPlaylist: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onPlaySongs: (List<SongItem>, Int) -> Unit,
    onShuffle: (List<SongItem>) -> Unit,
) {
    val liked by AppModule.favorites.items.collectAsStateWithLifecycle()
    val playlists by AppModule.playlists.items.collectAsStateWithLifecycle()
    val palette = LocalCoverPalette.current
    val prefs = LocalUiPrefs.current
    val bottomPadding = LocalBottomOverlayPadding.current
    var creating by remember { mutableStateOf(false) }
    var showLocalMusic by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf("Semua") }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Pustaka",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                color = WarmInk,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { creating = true }) {
                Icon(Icons.Rounded.Add, "Buat playlist", tint = WarmInk, modifier = Modifier.size(26.dp))
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, "Pengaturan tampilan", tint = WarmInk, modifier = Modifier.size(22.dp))
            }
            BackupControls()
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("Semua", "Playlist", "Disukai").forEach { option ->
                GlassChip(
                    label = option,
                    selected = filter == option,
                    onClick = { filter = option },
                )
            }
        }

        val cardShape = RoundedCornerShape(if (prefs.isGlass) 18.dp else 14.dp)

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = bottomPadding + 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (filter != "Playlist") {
                item(key = "liked-songs") {
                    LibraryCardRow(
                        title = "Lagu yang Disukai",
                        subtitle = "${liked.size} lagu",
                        pinned = true,
                        shape = cardShape,
                        onClick = { onOpenPlaylist(LIKED_PLAYLIST_ID) },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(listOf(palette.accent, palette.secondaryAccent))
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Favorite, null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    }
                }
            }
            if (filter != "Disukai") {
                item(key = "local-music") {
                    LibraryCardRow(
                        title = "Musik di perangkat",
                        subtitle = "Putar MP3/FLAC yang ada di HP",
                        shape = cardShape,
                        onClick = { showLocalMusic = true },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, WarmHairline, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Smartphone,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }
                item(key = "import-playlist") {
                    LibraryCardRow(
                        title = "Impor playlist dari link",
                        subtitle = "Spotify, Apple Music, YouTube Music",
                        shape = cardShape,
                        onClick = { showImport = true },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, WarmHairline, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Link,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }
            }
            if (filter != "Disukai") {
                items(playlists, key = { it.id }) { p ->
                    LibraryCardRow(
                        title = p.name,
                        subtitle = "Playlist • ${p.songs.size} lagu",
                        shape = cardShape,
                        onClick = { onOpenPlaylist(p.id) },
                    ) {
                        PlaylistCover(p, 58.dp)
                    }
                }
            }
            if (filter != "Disukai") {
                item(key = "create-playlist") {
                    LibraryCardRow(
                        title = "Buat playlist baru",
                        subtitle = "Kumpulkan lagu favoritmu",
                        shape = cardShape,
                        onClick = { creating = true },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, WarmHairline, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    }

    if (showLocalMusic) {
        LocalMusicSheet(
            onDismiss = { showLocalMusic = false },
            onPlay = { songs, index ->
                showLocalMusic = false
                onPlaySongs(songs, index)
            },
            onShuffle = { songs ->
                showLocalMusic = false
                onShuffle(songs)
            },
        )
    }

    if (showImport) {
        ImportPlaylistSheet(
            onDismiss = { showImport = false },
            onOpenPlaylist = { id ->
                showImport = false
                onOpenPlaylist(id)
            },
        )
    }

    if (creating) {
        PlaylistNameDialog(
            title = "Beri nama playlist",
            initial = AppModule.playlists.defaultName(),
            confirmText = "Buat",
            onDismiss = { creating = false },
            onConfirm = { name ->
                val p = AppModule.playlists.create(name)
                creating = false
                onOpenPlaylist(p.id)
            },
        )
    }
}

@Composable
private fun LibraryCardRow(
    title: String,
    subtitle: String?,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    pinned: Boolean = false,
    cover: @Composable () -> Unit,
) {
    val prefs = LocalUiPrefs.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (prefs.isGlass) Color.White.copy(alpha = 0.06f) else WarmSurface)
            .border(1.dp, if (prefs.isGlass) Color.White.copy(alpha = 0.14f) else WarmHairline, shape)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cover()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WarmInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (pinned) {
                        Icon(Icons.Rounded.PushPin, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(subtitle, color = WarmMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
