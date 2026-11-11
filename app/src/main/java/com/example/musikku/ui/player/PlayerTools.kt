package com.example.musikku.ui.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.musikku.player.NowPlaying

@Composable
fun SleepTimerDialog(remainingMs: Long?, onSet: (Int) -> Unit, onCancelTimer: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep timer") },
        text = {
            Column {
                Text(if (remainingMs == null) "Musik akan dijeda otomatis." else "Sisa waktu: ${(remainingMs + 59_999) / 60_000} menit")
                listOf(15, 30, 45, 60).forEach { minutes ->
                    TextButton(onClick = { onSet(minutes); onDismiss() }) { Text("$minutes menit") }
                }
                if (remainingMs != null) {
                    TextButton(onClick = { onCancelTimer(); onDismiss() }) { Text("Batalkan timer") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
    )
}

@Composable
fun QueueDialog(state: NowPlaying, onPlay: (Int) -> Unit, onRemove: (Int) -> Unit, onMove: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Antrean lagu") },
        text = {
            Column {
                Text(
                    if (state.shuffle) "Urutan daftar; mode acak aktif sehingga urutan putar bisa berbeda." else "Ketuk judul untuk memutar. Gunakan panah untuk mengubah urutan.",
                    style = MaterialTheme.typography.bodySmall,
                )
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(state.queue, key = { it.index }) { entry ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onPlay(entry.index) }, modifier = Modifier.weight(1f)) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(entry.song.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        color = if (entry.index == state.currentQueueIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                    Text(entry.song.artistsText, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            IconButton(onClick = { onMove(entry.index, -1) }, enabled = entry.index > 0) {
                                Icon(Icons.Default.ArrowUpward, "Pindahkan ke atas")
                            }
                            IconButton(onClick = { onMove(entry.index, 1) }, enabled = entry.index < state.queue.lastIndex) {
                                Icon(Icons.Default.ArrowDownward, "Pindahkan ke bawah")
                            }
                            IconButton(onClick = { onRemove(entry.index) }, enabled = entry.index != state.currentQueueIndex) {
                                Icon(Icons.Default.Close, "Hapus dari antrean")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
    )
}
