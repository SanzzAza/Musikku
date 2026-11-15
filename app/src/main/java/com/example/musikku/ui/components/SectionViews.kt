package com.example.musikku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musikku.data.ytmusic.Section
import com.example.musikku.data.ytmusic.SongItem
import com.example.musikku.data.ytmusic.YTItem
import com.example.musikku.ui.theme.WarmHairline

/** Bisa dibuka sebagai halaman album/playlist di aplikasi? */
fun isOpenableCollection(browseId: String?) =
    browseId != null && (browseId.startsWith("VL") || browseId.startsWith("MPRE"))

/**
 * Menampilkan satu Section:
 * - kalau isinya daftar lagu → grid horizontal 4 baris rapi dengan garis pemisah halus
 * - selain itu → carousel kartu membulat
 */
@Composable
fun SectionView(
    section: Section,
    currentSongId: String?,
    onPlaySongs: (List<SongItem>, Int) -> Unit,
    onOpenItem: (YTItem) -> Unit,
    onOpenCollection: (String) -> Unit,
) {
    val more = section.moreBrowseId?.takeIf { isOpenableCollection(it) }
    SectionTitle(
        section.title,
        action = if (more != null) "Lihat semua" else null,
        onAction = more?.let { id -> { onOpenCollection(id) } },
    )

    val songs = section.items.filterIsInstance<SongItem>()
    val songList = songs.size == section.items.size && songs.size >= 4 &&
        songs.none { it.thumbnail?.contains("i.ytimg.com") == true }

    if (songList) {
        val rows = minOf(4, songs.size)
        LazyHorizontalGrid(
            rows = GridCells.Fixed(rows),
            contentPadding = PaddingValues(horizontal = 6.dp),
            modifier = Modifier.height((68 * rows).dp),
        ) {
            itemsIndexed(songs, key = { i, s -> "${section.title}-$i-${s.id}" }) { i, song ->
                Column(Modifier.width(326.dp)) {
                    SongRow(
                        song = song,
                        isCurrent = song.id == currentSongId,
                        onClick = { onPlaySongs(songs, i) },
                        showAlbum = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if ((i + 1) % rows != 0) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 78.dp, end = 16.dp)
                                .height(0.6.dp)
                                .background(WarmHairline)
                        )
                    }
                }
            }
        }
    } else {
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp)) {
            itemsIndexed(section.items, key = { i, item -> "${section.title}-$i-${item.id}" }) { _, item ->
                ItemCard(item, onClick = { onOpenItem(item) })
            }
        }
    }
}
