package com.example.musikku.ui.playlist

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
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

    if (!isLiked && playlist == null) {
        // Playlist sudah dihapus
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Playlist tidak ditemukan") }
        BackButton(onBack, Modifier.statusBarsPadding().padding(8.dp))
        return
    }

    val songs = if (isLiked) liked else playlist!!.songs
    val title = if (isLiked) "Lagu yang Disukai" else playlist!!.name
    val totalSec = songs.sumOf { it.durationSec }

    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(if (isLiked) Color(0xFF4A2FBD) else Color(0xFF3F3F46), MaterialTheme.colorScheme.background)
                            )
                        )
                        .statusBarsPadding()
                        .padding(top = 56.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLiked) {
                        Box(
                            Modifier
                                .size(210.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF450AF5), Color(0xFFC4EFD9)))),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Favorite, null, Modifier.size(80.dp), tint = Color.White) }
                    } else {
                        PlaylistCover(playlist!!, 210.dp)
                    }
                }
            }
            item {
                Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            buildString {
                                append("${songs.size} lagu")
                                if (totalSec > 0) append(" • ${formatTotal(totalSec)}")
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (!isLiked) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "Opsi playlist") }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Ganti nama") },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                                    onClick = { menuOpen = false; renaming = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Hapus playlist") },
                                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                                    onClick = { menuOpen = false; confirmDelete = true }
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
                        modifier = Modifier.padding(vertical = 8.dp)
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
                    Column(
                        Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Ayo isi playlist ini", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (isLiked) "Ketuk ⊕ atau ikon hati untuk menyimpan lagu ke sini."
                            else "Ketuk ⋮ di lagu mana pun, lalu pilih \"Tambahkan ke playlist\".",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(onClick = onFindSongs) {
                            Icon(Icons.Default.Search, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Cari lagu")
                        }
                    }
                }
            }
        }
        BackButton(onBack, Modifier.statusBarsPadding().padding(8.dp))
    }

    if (renaming && playlist != null) {
        PlaylistNameDialog(
            title = "Ganti nama playlist",
            initial = playlist.name,
            confirmText = "Simpan",
            onDismiss = { renaming = false },
            onConfirm = { AppModule.playlists.rename(playlistId, it); renaming = false }
        )
    }
    if (confirmDelete && playlist != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Hapus playlist?") },
            text = { Text("\"${playlist.name}\" akan dihapus dari Koleksi.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onBack()
                    AppModule.playlists.delete(playlistId)
                }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Batal") } },
        )
    }
}

private fun formatTotal(sec: Int): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    return if (h > 0) "$h j $m mnt" else "$m mnt"
}
