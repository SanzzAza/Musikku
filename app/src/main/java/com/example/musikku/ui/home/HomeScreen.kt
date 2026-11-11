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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musikku.AppModule
import com.example.musikku.data.ytmusic.SongItem
import com.example.musikku.data.ytmusic.YTItem
import com.example.musikku.player.NowPlaying
import com.example.musikku.ui.components.Artwork
import com.example.musikku.ui.components.ItemCard
import com.example.musikku.ui.components.SectionTitle
import com.example.musikku.ui.components.SectionView
import java.util.Calendar

@Composable
fun HomeScreen(
    currentSongId: String?,
    onPlaySongs: (List<SongItem>, Int) -> Unit,
    onOpenItem: (YTItem) -> Unit,
    onOpenCollection: (String) -> Unit,
    now: NowPlaying,
    onOpenPlayer: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val recents by AppModule.recents.items.collectAsStateWithLifecycle()

    LazyColumn(modifier = Modifier.statusBarsPadding(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 4.dp)) {
                Text("MUSIKKU  /  MUSIK UNTUKMU", style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Text(greeting(), style = MaterialTheme.typography.headlineMedium)
                Text("Mau dengar apa hari ini?", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        now.song?.let { song ->
            item {
                Column(Modifier.padding(top = 8.dp)) {
                    SectionTitle("Lanjutkan mendengarkan")
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF24483E), Color(0xFF202725))))
                            .clickable(onClick = onOpenPlayer).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Artwork(song.thumbnail, Modifier.size(60.dp), shape = RoundedCornerShape(10.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artistsText, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.Default.PlayArrow, "Buka pemutar", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
        if (recents.isNotEmpty()) {
            item { SectionTitle("Dibuka dari pencarian") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
                    itemsIndexed(recents.take(10), key = { i, item -> "$i-${item.id}" }) { _, item ->
                        ItemCard(item, onClick = { onOpenItem(item) }, size = 122.dp)
                    }
                }
            }
        }
        if (state.loading && state.sections.isEmpty()) {
            item { HomeSkeleton() }
        } else if (state.error != null && state.sections.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Belum bisa memuat rekomendasi", style = MaterialTheme.typography.titleMedium)
                    Text(state.error.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = vm::load, modifier = Modifier.padding(top = 8.dp)) { Text("Coba lagi") }
                }
            }
        }
        itemsIndexed(state.sections, key = { i, section -> "$i-${section.title}" }) { _, section ->
            SectionView(section, currentSongId, onPlaySongs, onOpenItem, onOpenCollection)
        }
    }
}

@Composable
private fun HomeSkeleton() {
    Column(Modifier.padding(top = 24.dp)) {
        Box(Modifier.padding(start = 16.dp).width(170.dp).height(22.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
        Row(Modifier.padding(start = 16.dp, top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
                Column {
                    Box(Modifier.size(120.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.width(95.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                }
            }
        }
    }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 4..10 -> "Selamat pagi"
    in 11..14 -> "Selamat siang"
    in 15..17 -> "Selamat sore"
    else -> "Selamat malam"
}
