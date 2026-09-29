package com.example.musikku.ui.playlist

import android.widget.Toast
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.musikku.AppModule
import com.example.musikku.data.local.UserPlaylist
import com.example.musikku.data.ytmusic.SongItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pemicu global "Tambahkan ke playlist". Layar mana pun cukup memanggil
 * [PlaylistPicker.open]; sheet-nya ditampilkan sekali di MusikkuApp.
 */
object PlaylistPicker {
    private val _pending = MutableStateFlow<List<SongItem>?>(null)
    val pending = _pending.asStateFlow()
    fun open(songs: List<SongItem>) { if (songs.isNotEmpty()) _pending.value = songs }
    fun open(song: SongItem) = open(listOf(song))
    fun close() { _pending.value = null }
}

/** Cover playlist: mozaik 2x2 kalau ada ≥4 cover, selain itu cover pertama / ikon. */
@Composable
fun PlaylistCover(playlist: UserPlaylist, size: Dp, modifier: Modifier = Modifier) {
    val covers = playlist.mosaic
    Box(
        modifier.size(size).clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A2A2E)),
        contentAlignment = Alignment.Center
    ) {
        when {
            covers.size >= 4 -> Column(Modifier.fillMaxSize()) {
                for (r in 0..1) Row(Modifier.weight(1f)) {
                    for (c in 0..1) AsyncImage(
                        covers[r * 2 + c], null, contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxSize()
                    )
                }
            }
            covers.isNotEmpty() -> AsyncImage(covers[0], null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else -> Icon(Icons.AutoMirrored.Filled.QueueMusic, null, Modifier.size(size / 2.2f), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Dialog isi nama (dipakai untuk buat playlist & ganti nama). */
@Composable
fun PlaylistNameDialog(
    title: String,
    initial: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                placeholder = { Text("Nama playlist") },
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.text) }, enabled = value.text.isNotBlank()) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

/** Bottom sheet "Tambahkan ke playlist". Pasang sekali di root aplikasi. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistHost() {
    val songs by PlaylistPicker.pending.collectAsStateWithLifecycle()
    val pending = songs ?: return
    val playlists by AppModule.playlists.items.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var creating by remember { mutableStateOf(false) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    ModalBottomSheet(
        onDismissRequest = { PlaylistPicker.close() },
        sheetState = sheetState,
        containerColor = Color(0xFF1C1C1F),
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Text(
                "Tambahkan ke playlist",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 4.dp)
            )
            Text(
                if (pending.size == 1) pending[0].title else "${pending.size} lagu",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                item {
                    Row(
                        Modifier.fillMaxWidth().clickable { creating = true }.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(56.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A2A2E)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Add, null, Modifier.size(30.dp)) }
                        Spacer(Modifier.width(14.dp))
                        Text("Playlist baru", style = MaterialTheme.typography.titleMedium)
                    }
                }
                items(playlists, key = { it.id }) { p ->
                    val allIn = pending.all { s -> p.songs.any { it.id == s.id } }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                val added = AppModule.playlists.addSongs(p.id, pending)
                                toast(if (added == 0) "Sudah ada di ${p.name}" else "Ditambahkan ke ${p.name}")
                                PlaylistPicker.close()
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        PlaylistCover(p, 56.dp)
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${p.songs.size} lagu", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (allIn) Icon(Icons.Default.CheckCircle, "Sudah ada", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    if (creating) {
        PlaylistNameDialog(
            title = "Beri nama playlist",
            initial = AppModule.playlists.defaultName(),
            confirmText = "Buat",
            onDismiss = { creating = false },
            onConfirm = { name ->
                val p = AppModule.playlists.create(name, pending)
                toast("Ditambahkan ke ${p.name}")
                creating = false
                PlaylistPicker.close()
            }
        )
    }
}
