package com.vyora.music.ui.poster

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vyora.music.AppModule
import com.vyora.music.data.ytmusic.SearchFilter
import com.vyora.music.data.ytmusic.SongItem
import com.vyora.music.ui.components.Artwork
import com.vyora.music.ui.theme.LocalCoverPalette
import com.vyora.music.ui.theme.WarmHairline
import com.vyora.music.ui.theme.WarmInk
import com.vyora.music.ui.theme.WarmMuted
import kotlinx.coroutines.delay

/**
 * Dialog pemilih lagu cepat untuk mengganti atau menambah kartu pada kolase Kartu Foto.
 * Menyediakan saran dari Favorit + Baru Diputar sekaligus pencarian langsung YouTube Music.
 */
@Composable
internal fun PosterSongPickerDialog(
    title: String,
    onSelect: (SongItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val accent = LocalCoverPalette.current.accent
    val favorites by AppModule.favorites.items.collectAsState()
    val recents by AppModule.recents.items.collectAsState()
    val localPool = remember(favorites, recents) {
        (recents.filterIsInstance<SongItem>() + favorites).distinctBy { it.id }
    }

    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var searchError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            results = emptyList()
            searching = false
            searchError = null
            return@LaunchedEffect
        }
        delay(360)
        searching = true
        searchError = null
        runCatching {
            AppModule.ytMusic.search(trimmed, SearchFilter.SONGS).filterIsInstance<SongItem>()
        }.onSuccess { list ->
            results = list.take(20)
        }.onFailure {
            searchError = "Gagal mencari lagu, periksa koneksi"
        }
        searching = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF17151A),
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, WarmHairline),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = WarmInk,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.Close, "Tutup", tint = WarmMuted)
                    }
                }

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Cari lagu atau artis...", color = WarmMuted) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = WarmMuted) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Rounded.Close, "Hapus", tint = WarmMuted)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = WarmHairline,
                        focusedTextColor = WarmInk,
                        unfocusedTextColor = WarmInk,
                        cursorColor = accent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))

                val showingSearch = query.trim().length >= 2
                val displayed = if (showingSearch) results else localPool

                Text(
                    text = if (showingSearch) "Hasil pencarian" else "Baru diputar & Disukai",
                    style = MaterialTheme.typography.labelMedium,
                    color = WarmMuted,
                )
                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 360.dp),
                ) {
                    when {
                        searching -> CircularProgressIndicator(
                            color = accent,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(28.dp),
                        )

                        searchError != null -> Text(
                            text = searchError.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmMuted,
                            modifier = Modifier.align(Alignment.Center),
                        )

                        displayed.isEmpty() -> Text(
                            text = if (showingSearch) "Lagu tidak ditemukan" else "Ketik judul lagu di kolom pencarian",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmMuted,
                            modifier = Modifier.align(Alignment.Center),
                        )

                        else -> LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(displayed, key = { it.id }) { song ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                                        .clickable { onSelect(song) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Artwork(
                                        url = song.thumbnail,
                                        modifier = Modifier.size(44.dp),
                                        shape = RoundedCornerShape(10.dp),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = WarmInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = song.artistsText.ifBlank { "VYORA" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = WarmMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
