package com.example.musikku.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musikku.AppModule
import com.example.musikku.ui.playlist.LIKED_PLAYLIST_ID
import com.example.musikku.ui.playlist.PlaylistCover
import com.example.musikku.ui.playlist.PlaylistNameDialog

/** Koleksi ala Spotify: Lagu yang Disukai + playlist buatan sendiri. */
@Composable
fun LibraryScreen(onOpenPlaylist: (String) -> Unit) {
    val liked by AppModule.favorites.items.collectAsStateWithLifecycle()
    val playlists by AppModule.playlists.items.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Koleksi Kamu", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { creating = true }) { Icon(Icons.Default.Add, "Buat playlist", Modifier.size(30.dp)) }
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                LibraryRow(
                    title = "Lagu yang Disukai",
                    subtitle = "${liked.size} lagu",
                    pinned = true,
                    onClick = { onOpenPlaylist(LIKED_PLAYLIST_ID) }
                ) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF450AF5), Color(0xFFC4EFD9)))),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Favorite, null, tint = Color.White) }
                }
            }
            items(playlists, key = { it.id }) { p ->
                LibraryRow(
                    title = p.name,
                    subtitle = "Playlist • ${p.songs.size} lagu",
                    onClick = { onOpenPlaylist(p.id) }
                ) { PlaylistCover(p, 64.dp) }
            }
            item {
                LibraryRow(title = "Buat playlist", subtitle = null, onClick = { creating = true }) {
                    Box(
                        Modifier.size(64.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A2A2E)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Add, null, Modifier.size(32.dp)) }
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
                val p = AppModule.playlists.create(name)
                creating = false
                onOpenPlaylist(p.id)
            }
        )
    }
}

@Composable
private fun LibraryRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    pinned: Boolean = false,
    cover: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        cover()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Row(verticalAlignment = Alignment.CenterVertically) {
                if (pinned) {
                    Icon(Icons.Default.PushPin, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                }
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
