package com.example.musikku.ui.playlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musikku.AppModule
import com.example.musikku.data.ytmusic.SongItem
import com.example.musikku.ui.artist.BackButton
import com.example.musikku.ui.components.PlayShuffleButtons
import com.example.musikku.ui.components.SongRow
import com.example.musikku.ui.components.EmptyState
import com.example.musikku.ui.theme.WarmSurface
import com.example.musikku.ui.theme.WarmSurfaceElevated
import com.example.musikku.ui.theme.LocalBottomOverlayPadding
import com.example.musikku.ui.theme.LocalCoverPalette
import com.example.musikku.ui.theme.WarmHairline
import com.example.musikku.ui.theme.WarmInk
import com.example.musikku.ui.theme.WarmMuted

/** id khusus untuk halaman "Lagu yang Disukai". */
const val LIKED_PLAYLIST_ID = "liked"

@Composable
fun PlaylistScreen(
    playlistId: String,
    currentSongId: String?,
    onPlaySongs: (List<SongItem>, Int) -> Unit,
    onShuffle: (List<SongItem>) -> Unit,
    onFindSongs: () -> Unit,
    onBack: () -> Unit,
) {
    val isLiked = playlistId == LIKED_PLAYLIST_ID
    val liked by AppModule.favorites.items.collectAsStateWithLifecycle()
    val playlists by AppModule.playlists.items.collectAsStateWithLifecycle()
    val playlist = playlists.firstOrNull { it.id == playlistId }
    val palette = LocalCoverPalette.current
    val bottomPadding = LocalBottomOverlayPadding.current

    if (!isLiked && playlist == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Playlist tidak ditemukan", color = WarmInk)
        }
        BackButton(onBack, Modifier.statusBarsPadding().padding(10.dp))
        return
    }

    val songs = if (isLiked) liked else playlist!!.songs
    val title = if (isLiked) "Lagu yang Disukai" else playlist!!.name
    val totalSec = songs.sumOf { it.durationSec }

    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPadding + 20.dp)) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    palette.ambient.copy(alpha = if (isLiked) 0.90f else 0.55f),
                                    MaterialTheme.colorScheme.background,
                                )
                            )
                        )
                        .statusBarsPadding()
                        .padding(top = 56.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(18.dp, RoundedCornerShape(22.dp))
                            .clip(RoundedCornerShape(22.dp))
                            .border(1.dp, WarmHairline, RoundedCornerShape(22.dp))
                            .padding(6.dp),
                    ) {
                        if (isLiked) {
                            Box(
                                Modifier
                                    .size(204.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Brush.linearGradient(listOf(palette.accent, palette.secondaryAccent))),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.Favorite, null, Modifier.size(76.dp), tint = Color.White)
                            }
                        } else {
                            PlaylistCover(playlist!!, 204.dp)
                        }
                    }
                }
            }
            item {
                Row(Modifier.padding(start = 18.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.headlineMedium, color = WarmInk)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            buildString {
                                append("${songs.size} lagu")
                                if (totalSec > 0) append(" • ${formatTotal(totalSec)}")
                            },
                            color = WarmMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (!isLiked) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Rounded.MoreVert, "Opsi playlist", tint = WarmInk)
                            }
                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false },
                                containerColor = WarmSurfaceElevated,
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Ganti nama", color = WarmInk) },
                                    leadingIcon = { Icon(Icons.Rounded.Edit, null, tint = WarmInk) },
                                    onClick = { menuOpen = false; renaming = true },
                                )
                                DropdownMenuItem(
                                    text = { Text("Hapus playlist", color = WarmInk) },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = WarmInk) },
                                    onClick = { menuOpen = false; confirmDelete = true },
                                )
                            }
                        }
                    }
                }
            }
            if (songs.isNotEmpty()) {
                item {
                    PlayShuffleButtons(
                        onPlay = { onPlaySongs(songs, 0) },
                        onShuffle = { onShuffle(songs) },
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                }
                itemsIndexed(songs, key = { _, s -> s.id }) { i, song ->
                    SongRow(
                        song = song,
                        isCurrent = song.id == currentSongId,
                        onClick = { onPlaySongs(songs, i) },
                        extraMenu = if (isLiked) emptyList()
                        else listOf("Hapus dari playlist ini" to { AppModule.playlists.removeSong(playlistId, song.id) }),
                    )
                }
            } else {
                item {
                    EmptyState(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        icon = Icons.Rounded.QueueMusic,
                        title = "Ayo isi playlist ini",
                        message = if (isLiked) {
                            "Ketuk ikon hati di pemutar atau mini player untuk menyimpan lagu ke sini."
                        } else {
                            "Ketuk ⋮ di lagu mana pun, lalu pilih \"Tambahkan ke playlist\"."
                        },
                        actionLabel = "Cari lagu",
                        onAction = onFindSongs,
                    )
                }
            }
        }
        BackButton(onBack, Modifier.statusBarsPadding().padding(10.dp))
    }

    if (renaming && playlist != null) {
        PlaylistNameDialog(
            title = "Ganti nama playlist",
            initial = playlist.name,
            confirmText = "Simpan",
            onDismiss = { renaming = false },
            onConfirm = { AppModule.playlists.rename(playlistId, it); renaming = false },
        )
    }
    if (confirmDelete && playlist != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = WarmSurface,
            title = { Text("Hapus playlist?", color = WarmInk) },
            text = { Text("\"${playlist.name}\" akan dihapus dari Pustaka.", color = WarmMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onBack()
                        AppModule.playlists.delete(playlistId)
                    }
                ) { Text("Hapus", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Batal", color = WarmMuted) }
            },
        )
    }
}

private fun formatTotal(sec: Int): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    return if (h > 0) "$h j $m mnt" else "$m mnt"
}
