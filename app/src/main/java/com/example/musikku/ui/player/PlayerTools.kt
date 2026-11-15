package com.example.musikku.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.musikku.player.NowPlaying
import com.example.musikku.ui.components.Artwork
import com.example.musikku.ui.theme.WarmFaint
import com.example.musikku.ui.theme.WarmHairline
import com.example.musikku.ui.theme.WarmInk
import com.example.musikku.ui.theme.WarmMuted
import com.example.musikku.ui.theme.WarmSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    remainingMs: Long?,
    onSet: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF171412),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.24f))
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
            Text("WAKTU TIDUR", style = MaterialTheme.typography.labelSmall, color = accent)
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (remainingMs == null) "Jeda musik otomatis"
                else "Tersisa ${(remainingMs + 59_999) / 60_000} menit",
                style = MaterialTheme.typography.titleLarge,
                color = WarmInk,
            )
            Spacer(Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 45, 60).forEach { minutes ->
                    Surface(
                        onClick = {
                            onSet(minutes)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = WarmSurface,
                        border = BorderStroke(1.dp, WarmHairline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Bedtime, null, tint = accent, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "$minutes menit",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = WarmInk,
                            )
                        }
                    }
                }
            }

            if (remainingMs != null) {
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = {
                        onCancelTimer()
                        onDismiss()
                    },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Batalkan timer", color = accent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    state: NowPlaying,
    onPlay: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF171412),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.24f))
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                Text("ANTREAN • ${state.queue.size} LAGU", style = MaterialTheme.typography.labelSmall, color = accent)
                Spacer(Modifier.height(4.dp))
                Text("Berikutnya", style = MaterialTheme.typography.titleLarge, color = WarmInk)
                if (state.shuffle) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Mode acak aktif — urutan putar dapat berbeda dari daftar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmMuted,
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.heightIn(max = 460.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(state.queue, key = { "${it.index}-${it.song.id}" }) { entry ->
                    val isCurrent = entry.index == state.currentQueueIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isCurrent) accent.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { onPlay(entry.index) }
                            .padding(start = 20.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Artwork(entry.song.thumbnail, Modifier.size(44.dp), shape = RoundedCornerShape(10.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isCurrent) {
                                    Icon(
                                        Icons.Rounded.GraphicEq,
                                        null,
                                        tint = accent,
                                        modifier = Modifier.size(16.dp).padding(end = 4.dp),
                                    )
                                }
                                Text(
                                    text = entry.song.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrent) accent else WarmInk,
                                )
                            }
                            Text(
                                text = entry.song.artistsText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmMuted,
                            )
                        }
                        IconButton(
                            onClick = { onMove(entry.index, -1) },
                            enabled = entry.index > 0,
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                Icons.Rounded.ArrowUpward,
                                "Ke atas",
                                tint = if (entry.index > 0) WarmMuted else WarmFaint.copy(alpha = 0.35f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(
                            onClick = { onMove(entry.index, 1) },
                            enabled = entry.index < state.queue.lastIndex,
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                Icons.Rounded.ArrowDownward,
                                "Ke bawah",
                                tint = if (entry.index < state.queue.lastIndex) WarmMuted else WarmFaint.copy(alpha = 0.35f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(
                            onClick = { onRemove(entry.index) },
                            enabled = !isCurrent,
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                "Hapus",
                                tint = if (!isCurrent) WarmFaint else Color.Transparent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
