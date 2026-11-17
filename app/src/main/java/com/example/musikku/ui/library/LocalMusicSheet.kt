package com.example.musikku.ui.library

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.musikku.AppModule
import com.example.musikku.data.ytmusic.SongItem
import com.example.musikku.ui.theme.WarmFaint
import com.example.musikku.ui.theme.WarmHairline
import com.example.musikku.ui.theme.WarmInk
import com.example.musikku.ui.theme.WarmMuted
import com.example.musikku.ui.theme.WarmSurface

private fun audioPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

private fun hasAudioPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, audioPermission()) == PackageManager.PERMISSION_GRANTED

private fun formatDuration(seconds: Int): String =
    "%d:%02d".format(seconds / 60, seconds % 60)

/**
 * Daftar file musik yang ada di HP (MediaStore).
 * Lagu di sini masuk ke antrean pemutar yang sama dengan lagu YouTube Music.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalMusicSheet(
    onDismiss: () -> Unit,
    onPlay: (List<SongItem>, Int) -> Unit,
    onShuffle: (List<SongItem>) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = MaterialTheme.colorScheme.primary

    var granted by remember { mutableStateOf(hasAudioPermission(context)) }
    var songs by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { allowed -> granted = allowed }

    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(audioPermission())
    }

    LaunchedEffect(granted) {
        if (granted) {
            scanning = true
            songs = AppModule.localMusic.load()
            scanning = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF171412),
        dragHandle = {
            Spacer(
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
                .padding(bottom = 20.dp),
        ) {
            Text(
                text = "MUSIK DI PERANGKAT",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (granted) "${songs.size} lagu ditemukan di HP ini" else "Izin membaca audio diperlukan",
                style = MaterialTheme.typography.titleLarge,
                color = WarmInk,
            )

            if (granted && songs.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        onClick = { onPlay(songs, 0) },
                        shape = RoundedCornerShape(14.dp),
                        color = accent.copy(alpha = 0.22f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accent),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = accent, modifier = Modifier.size(18.dp))
                            Text("Putar semua", style = MaterialTheme.typography.labelLarge, color = accent)
                        }
                    }
                    Surface(
                        onClick = { onShuffle(songs) },
                        shape = RoundedCornerShape(14.dp),
                        color = WarmSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarmHairline),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Rounded.Shuffle, null, tint = WarmInk, modifier = Modifier.size(18.dp))
                            Text("Acak", style = MaterialTheme.typography.labelLarge, color = WarmInk)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            when {
                !granted -> {
                    Text(
                        text = "Beri izin akses file audio supaya Musikku bisa memutar MP3/FLAC yang sudah ada di HP-mu.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmMuted,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { permissionLauncher.launch(audioPermission()) }) {
                        Text("Beri izin")
                    }
                }

                scanning -> {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = accent, strokeWidth = 2.5.dp)
                    }
                }

                songs.isEmpty() -> {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.MusicNote,
                                null,
                                tint = WarmFaint,
                                modifier = Modifier.size(40.dp),
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Tidak ada file musik di HP ini.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarmMuted,
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onPlay(songs, index) }
                                    .padding(horizontal = 6.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(WarmSurface),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (song.thumbnail != null) {
                                        AsyncImage(
                                            model = song.thumbnail,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    } else {
                                        Icon(
                                            Icons.Rounded.MusicNote,
                                            null,
                                            tint = WarmFaint,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(11.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = WarmInk,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = song.artistsText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WarmMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = formatDuration(song.durationSec),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = WarmFaint,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
