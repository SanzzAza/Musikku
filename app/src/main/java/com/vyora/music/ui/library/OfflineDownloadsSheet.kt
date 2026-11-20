package com.vyora.music.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vyora.music.AppModule
import com.vyora.music.data.ytmusic.SongItem
import com.vyora.music.ui.components.Artwork
import com.vyora.music.ui.components.EmptyState
import com.vyora.music.ui.theme.WarmFaint
import com.vyora.music.ui.theme.WarmHairline
import com.vyora.music.ui.theme.WarmInk
import com.vyora.music.ui.theme.WarmMuted
import com.vyora.music.ui.theme.WarmSurface
import com.vyora.music.ui.theme.WarmSurfaceElevated
import com.vyora.music.ui.theme.warmWash
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineDownloadsSheet(
    onDismiss: () -> Unit,
    onPlay: (List<SongItem>, Int) -> Unit,
    onShuffle: (List<SongItem>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val downloaded by AppModule.downloads.items.collectAsStateWithLifecycle()
    val activeProgress by AppModule.downloads.progress.collectAsStateWithLifecycle()
    val accent = MaterialTheme.colorScheme.primary
    val totalMb = downloaded.sumOf { it.sizeBytes } / (1024.0 * 1024.0)
    val songs = downloaded.map { it.song }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WarmSurface,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(warmWash(0.24f))
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("TANPA KUOTA & SINYAL", style = MaterialTheme.typography.labelSmall, color = accent)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Unduhan Offline",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                        color = WarmInk,
                    )
                    Text(
                        text = "${downloaded.size} lagu tersimpan • ${String.format(Locale.US, "%.1f MB", totalMb)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmMuted,
                    )
                }
                if (downloaded.isNotEmpty()) {
                    TextButton(onClick = { AppModule.downloads.clearAll() }) {
                        Text("Hapus semua", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (activeProgress.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.14f))
                        .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = accent,
                    )
                    Spacer(Modifier.width(10.dp))
                    val avgPct = (activeProgress.values.average() * 100).toInt().coerceIn(1, 99)
                    Text(
                        text = "Sedang mengunduh ${activeProgress.size} lagu ($avgPct%)…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmInk,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            if (downloaded.isEmpty() && activeProgress.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.DownloadForOffline,
                    title = "Belum ada unduhan offline",
                    message = "Ketuk ikon Unduh di layar Pemutar atau menu titik tiga (⋮) lagu mana saja supaya bisa diputar kapan pun tanpa sinyal.",
                )
            } else if (downloaded.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { onPlay(songs, 0) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Putar semua", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onShuffle(songs) },
                        border = BorderStroke(1.dp, WarmHairline),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Shuffle, null, tint = WarmInk, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Acak", color = WarmInk, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(downloaded, key = { _, item -> item.song.id }) { idx, entry ->
                        val mb = entry.sizeBytes / (1024.0 * 1024.0)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(WarmSurfaceElevated)
                                .border(1.dp, WarmHairline, RoundedCornerShape(14.dp))
                                .clickable { onPlay(songs, idx) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Artwork(
                                url = entry.song.thumbnail,
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(10.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = entry.song.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WarmInk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.CloudDone,
                                        null,
                                        tint = accent,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "${entry.song.artistsText} • ${String.format(Locale.US, "%.1f MB", mb)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WarmMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            IconButton(onClick = { AppModule.downloads.remove(entry.song.id) }) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = "Hapus unduhan",
                                    tint = WarmFaint,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
