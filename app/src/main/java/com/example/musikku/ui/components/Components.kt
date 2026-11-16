package com.example.musikku.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.musikku.AppModule
import com.example.musikku.data.ytmusic.AlbumItem
import com.example.musikku.data.ytmusic.ArtistItem
import com.example.musikku.data.ytmusic.SongItem
import com.example.musikku.data.ytmusic.YTItem
import com.example.musikku.ui.playlist.PlaylistPicker
import com.example.musikku.ui.poster.SongPosterPicker
import com.example.musikku.ui.theme.LocalCoverPalette
import com.example.musikku.ui.theme.LocalUiPrefs
import com.example.musikku.ui.theme.WarmHairline
import com.example.musikku.ui.theme.WarmInk
import com.example.musikku.ui.theme.WarmMuted
import com.example.musikku.ui.theme.WarmSurface

@Composable
fun Artwork(
    url: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
    placeholder: ImageVector = Icons.Default.MusicNote,
) {
    Box(
        modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(placeholder, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
/** Baris lagu: cover membulat rapi, judul, artis • album • durasi. */
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
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true })
            .padding(start = 16.dp, top = 7.dp, bottom = 7.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (index != null) {
            Text(
                "$index",
                modifier = Modifier.width(30.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) accent else WarmMuted,
            )
        }
        if (showArtwork) {
            Artwork(
                url = song.thumbnail,
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(11.dp),
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isCurrent) accent else WarmInk,
            )
            Text(
                listOfNotNull(
                    song.artistsText.takeIf { it.isNotBlank() },
                    song.album?.name?.takeIf { showAlbum && it.isNotBlank() },
                    song.durationSec.takeIf { it > 0 }?.let { formatDuration(it) },
                ).joinToString(" • "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = WarmMuted,
            )
        }
        if (isCurrent) {
            Icon(Icons.Default.GraphicEq, "Sedang diputar", tint = accent, modifier = Modifier.size(20.dp))
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, "Opsi lagu", tint = WarmMuted)
            }
            SongMenu(song, expanded = menuOpen, onDismiss = { menuOpen = false }, extraMenu = extraMenu)
        }
    }
}

data class QueueActions(val enqueue: (SongItem, Boolean) -> Unit)
val LocalQueueActions = staticCompositionLocalOf<QueueActions?> { null }

/** Menu ⋮ lagu: tambah ke playlist, suka/batal suka, plus opsi tambahan dari layar pemanggil. */
@Composable
fun SongMenu(
    song: SongItem,
    expanded: Boolean,
    onDismiss: () -> Unit,
    extraMenu: List<Pair<String, () -> Unit>> = emptyList(),
) {
    val queueActions = LocalQueueActions.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF211D1A),
    ) {
        if (queueActions != null) {
            DropdownMenuItem(
                text = { Text("Putar berikutnya", color = WarmInk) },
                onClick = { onDismiss(); queueActions.enqueue(song, true) },
            )
            DropdownMenuItem(
                text = { Text("Tambahkan ke antrean", color = WarmInk) },
                onClick = { onDismiss(); queueActions.enqueue(song, false) },
            )
        }
        DropdownMenuItem(
            text = { Text("Buat Poster Lagu", color = WarmInk) },
            leadingIcon = { Icon(Icons.Default.Image, null, tint = WarmInk) },
            onClick = { onDismiss(); SongPosterPicker.open(song) },
        )
        DropdownMenuItem(
            text = { Text("Tambahkan ke playlist", color = WarmInk) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, tint = WarmInk) },
            onClick = { onDismiss(); PlaylistPicker.open(song) },
        )
        val liked = AppModule.favorites.isFavorite(song.id)
        DropdownMenuItem(
            text = { Text(if (liked) "Hapus dari Lagu yang Disukai" else "Simpan ke Lagu yang Disukai", color = WarmInk) },
            leadingIcon = {
                Icon(
                    if (liked) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                    null,
                    tint = if (liked) MaterialTheme.colorScheme.primary else WarmInk,
                )
            },
            onClick = { onDismiss(); AppModule.favorites.toggle(song) },
        )
        extraMenu.forEach { (label, action) ->
            DropdownMenuItem(
                text = { Text(label, color = WarmInk) },
                leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, null, tint = WarmInk) },
                onClick = { onDismiss(); action() },
            )
        }
    }
}

/** Kartu serbaguna untuk carousel (lagu, album, playlist, artis). */
@Composable
fun ItemCard(item: YTItem, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 140.dp) {
    val isArtist = item is ArtistItem
    val prefs = LocalUiPrefs.current
    val artShape = when {
        isArtist -> CircleShape
        prefs.isGlass -> RoundedCornerShape(16.dp)
        else -> RoundedCornerShape(12.dp)
    }
    Column(
        modifier
            .width(size + 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = if (isArtist) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(artShape)
                .border(0.8.dp, Color.White.copy(alpha = 0.10f), artShape)
        ) {
            Artwork(
                url = item.thumbnail,
                modifier = Modifier.fillMaxSize(),
                shape = artShape,
                placeholder = if (isArtist) Icons.Default.Person else Icons.Default.MusicNote,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            item.title,
            maxLines = if (isArtist) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = WarmInk,
            textAlign = if (isArtist) TextAlign.Center else TextAlign.Start,
        )
        Text(
            itemSubtitle(item),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = WarmMuted,
            textAlign = if (isArtist) TextAlign.Center else TextAlign.Start,
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
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(
            imageUrl,
            Modifier.size(54.dp),
            shape = if (circle) CircleShape else RoundedCornerShape(12.dp),
            placeholder = if (circle) Icons.Default.Person else Icons.Default.MusicNote,
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = WarmInk)
            Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = WarmMuted)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    val prefs = LocalUiPrefs.current
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier.fillMaxWidth().padding(start = 18.dp, end = 16.dp, top = 22.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = WarmInk, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Surface(
                onClick = onAction,
                shape = if (prefs.isGlass) CircleShape else RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, WarmHairline),
            ) {
                Text(
                    action,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** Tombol acak + tombol putar squircle hangat khas Liquid Glass / Editorial. */
@Composable
fun PlayShuffleButtons(onPlay: () -> Unit, onShuffle: () -> Unit, modifier: Modifier = Modifier) {
    val prefs = LocalUiPrefs.current
    val palette = LocalCoverPalette.current
    val accent = MaterialTheme.colorScheme.primary
    val playShape = RoundedCornerShape(16.dp)
    val shuffleShape = if (prefs.isGlass) CircleShape else RoundedCornerShape(14.dp)

    Row(
        modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(shuffleShape)
                .background(if (prefs.isGlass) Color.White.copy(alpha = 0.08f) else WarmSurface)
                .border(1.dp, if (prefs.isGlass) Color.White.copy(alpha = 0.16f) else WarmHairline, shuffleShape)
                .clickable(onClick = onShuffle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Shuffle, "Acak", tint = WarmInk, modifier = Modifier.size(22.dp))
        }
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(playShape)
                .background(
                    if (prefs.isGlass) Brush.linearGradient(listOf(palette.accent, palette.secondaryAccent))
                    else Brush.linearGradient(listOf(accent, accent))
                )
                .clickable(onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.PlayArrow, "Putar", tint = Color.White, modifier = Modifier.size(30.dp))
        }
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
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.WifiOff, null, Modifier.size(48.dp), tint = WarmMuted)
        Spacer(Modifier.height(12.dp))
        Text(message, textAlign = TextAlign.Center, color = WarmMuted)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry) { Text("Coba lagi", color = WarmInk) }
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
