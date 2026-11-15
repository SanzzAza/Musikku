package com.example.musikku.ui.home

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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musikku.AppModule
import com.example.musikku.data.ytmusic.ArtistItem
import com.example.musikku.data.ytmusic.YTItem
import com.example.musikku.ui.components.Artwork
import com.example.musikku.ui.components.itemSubtitle
import com.example.musikku.ui.theme.WarmFaint
import com.example.musikku.ui.theme.WarmInk
import com.example.musikku.ui.theme.WarmMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorySheet(
    onOpenItem: (YTItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val recents by AppModule.recents.items.collectAsStateWithLifecycle()
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("RIWAYAT", style = MaterialTheme.typography.labelSmall, color = accent)
                    Text("Baru dibuka", style = MaterialTheme.typography.titleLarge, color = WarmInk)
                }
                if (recents.isNotEmpty()) {
                    TextButton(onClick = { AppModule.recents.clear() }) {
                        Text("Bersihkan", color = accent)
                    }
                }
            }

            if (recents.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.History, null, tint = WarmFaint, modifier = Modifier.size(36.dp))
                    Text("Belum ada riwayat", style = MaterialTheme.typography.titleMedium, color = WarmInk)
                    Text(
                        "Lagu, artis, atau album yang kamu buka akan muncul di sini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmMuted,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 460.dp),
                    contentPadding = PaddingValues(vertical = 6.dp),
                ) {
                    items(recents, key = { it.id }) { item ->
                        val isArtist = item is ArtistItem
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    onOpenItem(item)
                                }
                                .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Artwork(
                                url = item.thumbnail,
                                modifier = Modifier.size(50.dp),
                                shape = if (isArtist) CircleShape else RoundedCornerShape(10.dp),
                                placeholder = if (isArtist) Icons.Rounded.Person else Icons.Rounded.MusicNote,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WarmInk,
                                )
                                Text(
                                    text = itemSubtitle(item),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WarmMuted,
                                )
                            }
                            IconButton(onClick = { AppModule.recents.remove(item) }) {
                                Icon(Icons.Rounded.Close, "Hapus", tint = WarmFaint, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
