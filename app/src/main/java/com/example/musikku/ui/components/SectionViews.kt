package com.example.musikku.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musikku.data.ytmusic.Section
import com.example.musikku.data.ytmusic.SongItem
import com.example.musikku.data.ytmusic.YTItem

/** Bisa dibuka sebagai halaman album/playlist di aplikasi? */
fun isOpenableCollection(browseId: String?) =
    browseId != null && (browseId.startsWith("VL") || browseId.startsWith("MPRE"))

/**
 * Menampilkan satu Section:
 * - kalau isinya daftar lagu → grid horizontal 4 baris (ala "Pilihan cepat" YT Music)
 * - selain itu → carousel kartu
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
        onAction = more?.let { id -> { onOpenCollection(id) } }
    )

    val songs = section.items.filterIsInstance<SongItem>()
    val songList = songs.size == section.items.size && songs.size >= 4 &&
        songs.none { it.thumbnail?.contains("i.ytimg.com") == true } // video musik tetap pakai kartu

    if (songList) {
        val rows = minOf(4, songs.size)
        LazyHorizontalGrid(
            rows = GridCells.Fixed(rows),
            modifier = Modifier.height((66 * rows).dp),
        ) {
            itemsIndexed(songs, key = { i, s -> "${section.title}-$i-${s.id}" }) { i, song ->
                SongRow(
                    song = song,
                    isCurrent = song.id == currentSongId,
                    onClick = { onPlaySongs(songs, i) },
                    showAlbum = false,
                    modifier = Modifier.width(320.dp)
                )
            }
        }
    } else {
        LazyRow(contentPadding = PaddingValues(horizontal = 10.dp)) {
            items(section.items, key = { "${section.title}-${it.id}" }) { item ->
                ItemCard(item, onClick = { onOpenItem(item) })
            }
        }
    }
}
