package com.example.musikku.ui.import

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import com.example.musikku.AppModule
import com.example.musikku.data.importer.PlaylistImporter
import com.example.musikku.ui.theme.warmWash
import com.example.musikku.ui.theme.WarmFaint
import com.example.musikku.ui.theme.WarmHairline
import com.example.musikku.ui.theme.WarmInk
import com.example.musikku.ui.theme.WarmMuted
import com.example.musikku.ui.theme.WarmSurface
import kotlinx.coroutines.launch

/**
 * Impor playlist dari link Spotify / Apple Music / YouTube Music.
 *
 * Tiap lagu dicarikan padanannya di YouTube Music, lalu disimpan sebagai
 * playlist lokal. Berjalan bertahap dengan progres karena butuh puluhan
 * permintaan pencarian.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPlaylistSheet(
    onDismiss: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = MaterialTheme.colorScheme.primary
    val scope = rememberCoroutineScope()

    var link by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0 to 0) }
    var result by remember { mutableStateOf<PlaylistImporter.Result?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WarmSurface,
        dragHandle = {
            Spacer(
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
            Text(
                text = "IMPOR PLAYLIST",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tempel link playlist",
                style = MaterialTheme.typography.headlineSmall,
                color = WarmInk,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Mendukung Spotify, Apple Music, dan YouTube Music. Playlist harus publik.",
                style = MaterialTheme.typography.bodySmall,
                color = WarmMuted,
            )

            Spacer(Modifier.height(14.dp))

            val clipboard = LocalClipboardManager.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = "Tempel dari papan klip",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !busy) {
                            clipboard.getText()?.text?.let { pasted ->
                                if (pasted.isNotBlank()) link = pasted.trim()
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(6.dp))

            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                singleLine = false,
                maxLines = 3,
                placeholder = { Text("https://open.spotify.com/playlist/…", color = WarmFaint) },
                leadingIcon = { Icon(Icons.Rounded.Link, null, tint = accent) },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = WarmInk,
                    unfocusedTextColor = WarmInk,
                    focusedBorderColor = accent,
                    unfocusedBorderColor = WarmHairline,
                    cursorColor = accent,
                ),
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        result = null
                        progress = 0 to 0
                        result = AppModule.playlistImporter.import(link.trim()) { done, total ->
                            progress = done to total
                        }
                        busy = false
                    }
                },
                enabled = link.isNotBlank() && !busy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(if (busy) "Mengimpor…" else "Impor playlist")
            }

            if (busy) {
                Spacer(Modifier.height(14.dp))
                val total = progress.second
                Text(
                    text = if (total > 0) {
                        "Mencari lagu di YouTube Music… ${progress.first} dari $total"
                    } else {
                        "Membaca playlist…"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmMuted,
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp)),
                    color = accent,
                    trackColor = WarmSurface,
                    progress = {
                        if (progress.second > 0) {
                            (progress.first.toFloat() / progress.second).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                    },
                )
            }

            result?.let { res ->
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(WarmSurface)
                        .padding(14.dp),
                ) {
                    if (res.success) {
                        Text(
                            text = "Playlist dibuat",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "“${res.name}” — ${res.matched} dari ${res.total} lagu berhasil ditemukan.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmInk,
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { res.playlistId?.let(onOpenPlaylist) },
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Buka playlist")
                        }
                    } else {
                        Text(
                            text = res.error ?: "Gagal mengimpor playlist.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmMuted,
                        )
                    }
                }
            }
        }
    }
}
