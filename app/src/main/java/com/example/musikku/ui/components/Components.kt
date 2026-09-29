package com.example.musikku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.musikku.AppModule
import com.example.musikku.ui.playlist.PlaylistPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.musikku.data.ytmusic.AlbumItem
import com.example.musikku.data.ytmusic.ArtistItem
import com.example.musikku.data.ytmusic.SongItem
import com.example.musikku.data.ytmusic.YTItem

@Composable
fun Artwork(
    url: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
    placeholder: ImageVector = Icons.Default.MusicNote,
) {
    Box(
        modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(placeholder, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
/** Baris lagu: cover, judul, artis • album • durasi. */
@Composable
fun SongRow(
    song: SongItem,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int? = null,
    showArtwork: Boolean = true,
    showAlbum: Boolean = true,
    extraMenu: List<Pair<String, () -> Unit>> = emptyList(),
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true })
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (index != null) {
            Text(
                "$index",
                modifier = Modifier.width(32.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showArtwork) {
            Artwork(song.thumbnail, Modifier.size(50.dp))
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                listOfNotNull(
                    song.artistsText.takeIf { it.isNotBlank() },
                    song.album?.name?.takeIf { showAlbum && it.isNotBlank() },
                    song.durationSec.takeIf { it > 0 }?.let { formatDuration(it) },
                ).joinToString(" • "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isCurrent) {
            Icon(Icons.Default.GraphicEq, "Sedang diputar", tint = MaterialTheme.colorScheme.primary)
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, "Opsi lagu", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SongMenu(song, expanded = menuOpen, onDismiss = { menuOpen = false }, extraMenu = extraMenu)
        }
    }
}

/** Menu ⋮ lagu: tambah ke playlist, suka/batal suka, plus opsi tambahan dari layar pemanggil. */
@Composable
fun SongMenu(
    song: SongItem,
    expanded: Boolean,
    onDismiss: () -> Unit,
    extraMenu: List<Pair<String, () -> Unit>> = emptyList(),
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Tambahkan ke playlist") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
            onClick = { onDismiss(); PlaylistPicker.open(song) }
        )
        val liked = AppModule.favorites.isFavorite(song.id)
        DropdownMenuItem(
            text = { Text(if (liked) "Hapus dari Lagu yang Disukai" else "Simpan ke Lagu yang Disukai") },
            leadingIcon = {
                Icon(
                    if (liked) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline, null,
                    tint = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            },
            onClick = { onDismiss(); AppModule.favorites.toggle(song) }
        )
        extraMenu.forEach { (label, action) ->
            DropdownMenuItem(
                text = { Text(label) },
                leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, null) },
                onClick = { onDismiss(); action() }
            )
        }
    }
}

/** Kartu serbaguna untuk carousel (lagu, album, playlist, artis). */
@Composable
fun ItemCard(item: YTItem, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 140.dp) {
    val isArtist = item is ArtistItem
    Column(
        modifier
            .width(size + 12.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = if (isArtist) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Artwork(
            item.thumbnail,
            Modifier.size(size),
            shape = if (isArtist) CircleShape else RoundedCornerShape(8.dp),
            placeholder = if (isArtist) Icons.Default.Person else Icons.Default.MusicNote
        )
        Spacer(Modifier.height(8.dp))
        Text(
            item.title,
            maxLines = if (isArtist) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = if (isArtist) TextAlign.Center else TextAlign.Start
        )
        Text(
            itemSubtitle(item),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = if (isArtist) TextAlign.Center else TextAlign.Start
        )
    }
}

fun itemSubtitle(item: YTItem): String = when (item) {
    is SongItem -> item.artistsText
    is AlbumItem -> item.subtitle
    is ArtistItem -> item.subtitle.ifBlank { "Artis" }
}

@Composable
fun ListItemRow(
    imageUrl: String?,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    circle: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Artwork(
            imageUrl, Modifier.size(56.dp),
            shape = if (circle) CircleShape else RoundedCornerShape(6.dp),
            placeholder = if (circle) Icons.Default.Person else Icons.Default.MusicNote
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onAction).padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

/** Tombol shuffle + tombol play bulat besar. */
@Composable
fun PlayShuffleButtons(onPlay: () -> Unit, onShuffle: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onShuffle) { Icon(Icons.Default.Shuffle, "Acak", Modifier.size(28.dp)) }
        FilledIconButton(
            onClick = onPlay,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) { Icon(Icons.Default.PlayArrow, "Putar", Modifier.size(32.dp)) }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun ErrorBox(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.WifiOff, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry) { Text("Coba lagi") }
    }
}

fun formatDuration(seconds: Int): String =
    if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60, seconds % 60)
    else "%d:%02d".format(seconds / 60, seconds % 60)

fun formatMs(ms: Long): String = formatDuration((ms / 1000).toInt())

fun Throwable.toUserMessage(): String = when (this) {
    is java.net.UnknownHostException -> "Tidak ada koneksi internet"
    is java.net.SocketTimeoutException -> "Koneksi timeout, coba lagi"
    else -> message ?: "Terjadi kesalahan"
}
