package com.example.musikku.ui.poster

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.musikku.AppModule
import com.example.musikku.data.lyrics.Lyrics
import com.example.musikku.data.ytmusic.SongItem
import com.example.musikku.player.LyricsUiState
import com.example.musikku.ui.components.Artwork
import com.example.musikku.ui.components.formatMs
import com.example.musikku.ui.player.CoverPalette
import com.example.musikku.ui.player.rememberCoverPalette
import com.example.musikku.ui.theme.LiquidGlassSwitch
import com.example.musikku.ui.theme.LocalUiPrefs
import com.example.musikku.ui.theme.WarmHairline
import com.example.musikku.ui.theme.WarmInk
import com.example.musikku.ui.theme.WarmMuted
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class PhotoStudioTab(val label: String, val icon: ImageVector) {
    PHOTO("Foto & Latar", Icons.Outlined.Image),
    CARDS("Kartu Lagu", Icons.Rounded.Layers),
    GLASS("Gaya Kaca", Icons.Rounded.Tune),
}

private enum class PosterStudioTab(val label: String, val icon: ImageVector) {
    STYLE("Gaya", Icons.Outlined.Palette),
    LYRICS("Lirik", Icons.Outlined.FormatQuote),
    TEXT("Teks", Icons.Outlined.TextFields),
}

/**
 * Host global Studio Poster & Kartu Lagu — dipasang di root [com.example.musikku.ui.MusikkuApp]
 * sehingga dapat dibuka dari Pemutar, Lirik, maupun menu ⋮ lagu mana pun.
 */
@Composable
fun SongPosterHost() {
    val request by SongPosterPicker.request.collectAsState()
    val active = request ?: return
    SongPosterSheet(
        request = active,
        onDismiss = SongPosterPicker::dismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongPosterSheet(
    request: PosterRequest,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = LocalUiPrefs.current
    val song = request.song
    val coverUrl = remember(song.thumbnail) { song.thumbnail?.replace("w120-h120", "w544-h544") }
    val palette = rememberCoverPalette(coverUrl)

    val favorites by AppModule.favorites.items.collectAsState()
    val recents by AppModule.recents.items.collectAsState()
    val likedIds = remember(favorites) { favorites.map { it.id }.toSet() }
    val candidateSongs = remember(favorites, recents, song.id) {
        (recents.filterIsInstance<SongItem>() + favorites)
            .filter { it.id != song.id }
            .distinctBy { it.id }
    }

    val primaryProgress = remember(request.positionMs, song.durationSec) {
        val totalMs = (song.durationSec.takeIf { it > 0 } ?: 210) * 1000L
        if (request.positionMs > 0L && totalMs > 0L) {
            (request.positionMs.toFloat() / totalMs).coerceIn(0.06f, 0.92f)
        } else {
            0.34f
        }
    }

    var config by remember(request) {
        mutableStateOf(
            PosterConfig(
                mode = request.initialMode,
                background = if (prefs.isGlass) PosterBackground.LIQUID_GLASS else PosterBackground.EDITORIAL_PAPER,
                aspect = if (request.initialMode == PosterMode.PHOTO_CARDS) PosterAspect.STORY_9_16 else PosterAspect.POSTER_4_5,
            )
        )
    }

    var photoTab by remember { mutableStateOf(PhotoStudioTab.PHOTO) }
    var posterTab by remember(request) {
        mutableStateOf(
            if (request.initialMode == PosterMode.LYRIC_POSTER) PosterStudioTab.LYRICS else PosterStudioTab.STYLE
        )
    }

    // ---- State Mode Kartu Foto Liquid Glass ----
    var cards by remember(request) {
        mutableStateOf(
            GlassCardSlot.buildForPreset(
                preset = CardCollagePreset.SINGLE,
                songs = candidateSongs,
                primarySong = song,
                primaryProgress = primaryProgress,
                likedIds = likedIds,
            )
        )
    }
    var selectedSlotId by remember(request) { mutableStateOf(0) }
    var pickingForSlotId by remember { mutableStateOf<Int?>(null) } // null = tertutup, -1 = tambah kartu baru
    var customPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var customPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var maskState by remember { mutableStateOf<SubjectMaskState>(SubjectMaskState.Idle) }
    var maskRetryCounter by remember { mutableStateOf(0) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            customPhotoUri = uri
        }
    }

    // Muat foto pilihan pengguna (dengan koreksi orientasi EXIF lewat Coil)
    LaunchedEffect(customPhotoUri) {
        val uri = customPhotoUri
        if (uri == null) {
            customPhotoBitmap = null
            maskState = SubjectMaskState.Idle
        } else {
            customPhotoBitmap = PosterBitmapRenderer.loadBitmap(context, uri, maxSize = 1440)
        }
    }

    // Jalankan ML Kit Subject Segmentation otomatis saat foto & rasio siap dan "Subjek di Depan" aktif
    LaunchedEffect(customPhotoBitmap, config.aspect, config.photoCards.subjectInFront, maskRetryCounter) {
        val src = customPhotoBitmap
        if (src == null || !config.photoCards.subjectInFront) {
            if (src == null) maskState = SubjectMaskState.Idle
            return@LaunchedEffect
        }
        maskState = SubjectMaskState.Processing
        val cropped = SubjectSegmenterHelper.centerCropBitmap(
            src,
            targetW = (config.aspect.widthPx * 0.65f).roundToInt(),
            targetH = (config.aspect.heightPx * 0.65f).roundToInt(),
        )
        maskState = SubjectSegmenterHelper.segmentForeground(cropped)
    }

    // ---- State Lirik (untuk Poster Lagu & Poster Lirik) ----
    var lyricsState by remember(request) { mutableStateOf<LyricsUiState>(request.initialLyrics ?: LyricsUiState.Loading) }
    suspend fun fetchLyricsForSong(): LyricsUiState = runCatching {
        LyricsUiState.Loaded(
            AppModule.lyrics.getLyrics(
                trackId = song.id,
                title = song.title,
                artist = song.artistsText,
                album = song.album?.name,
                durationSec = song.durationSec,
            )
        )
    }.getOrElse { LyricsUiState.Error }

    LaunchedEffect(song.id) {
        if (request.initialLyrics == null || request.initialLyrics is LyricsUiState.Idle || request.initialLyrics is LyricsUiState.Loading) {
            lyricsState = LyricsUiState.Loading
            lyricsState = fetchLyricsForSong()
        }
    }
    val allLyricLines = remember(lyricsState) {
        val loaded = (lyricsState as? LyricsUiState.Loaded)?.lyrics
        when (loaded) {
            is Lyrics.Synced -> loaded.lines.map { it.text }.filter { it.isNotBlank() }
            is Lyrics.Plain -> loaded.text.lines().map { it.trim() }.filter { it.isNotBlank() }
            else -> emptyList()
        }
    }
    var selectedIndices by remember(song.id) { mutableStateOf<List<Int>>(emptyList()) }
    LaunchedEffect(allLyricLines) {
        if (allLyricLines.isNotEmpty() && selectedIndices.isEmpty()) {
            val loaded = (lyricsState as? LyricsUiState.Loaded)?.lyrics
            val startIdx = when (loaded) {
                is Lyrics.Synced -> {
                    val activeIdx = loaded.lines.indexOfLast { it.timeMs <= request.positionMs }.coerceAtLeast(0)
                    activeIdx.coerceAtMost((allLyricLines.size - 1).coerceAtLeast(0))
                }
                else -> 0
            }
            val count = min(3, allLyricLines.size - startIdx)
            selectedIndices = (startIdx until startIdx + count).toList()
        }
    }
    val selectedLines = remember(allLyricLines, selectedIndices) {
        selectedIndices.sorted().mapNotNull { allLyricLines.getOrNull(it) }
    }

    var busyAction by remember { mutableStateOf<String?>(null) }

    // Dialog pilih/ganti lagu untuk kolase Kartu Foto
    val pickTarget = pickingForSlotId
    if (pickTarget != null) {
        PosterSongPickerDialog(
            title = if (pickTarget == -1) "Tambah Kartu Lagu" else "Ganti Lagu Kartu",
            onSelect = { picked ->
                if (pickTarget == -1 && cards.size < 5) {
                    val nextId = (cards.maxOfOrNull { it.slotId } ?: -1) + 1
                    val newSlot = GlassCardSlot(
                        slotId = nextId,
                        song = picked,
                        xFraction = 0.50f + ((nextId % 3) - 1) * 0.16f,
                        yFraction = 0.44f + ((nextId % 2) * 0.12f),
                        scale = 0.54f,
                        rotationDeg = if (nextId % 2 == 0) -4f else 4f,
                        isLiked = picked.id in likedIds,
                        sparkleCorner = if (nextId % 2 == 0) SparkleCorner.TOP_RIGHT else SparkleCorner.NONE,
                    )
                    cards = cards + newSlot
                    selectedSlotId = nextId
                } else {
                    cards = cards.map {
                        if (it.slotId == pickTarget) it.copy(song = picked, isLiked = picked.id in likedIds) else it
                    }
                }
                pickingForSlotId = null
            },
            onDismiss = { pickingForSlotId = null },
        )
    }

    suspend fun renderCurrentHighResBitmap(): Bitmap {
        val primaryCover = PosterBitmapRenderer.loadBitmap(context, coverUrl, maxSize = 720)
        return if (config.mode == PosterMode.PHOTO_CARDS) {
            val coverMap = coroutineScope {
                cards.map { it.song }.distinctBy { it.id }.map { s ->
                    async {
                        val hiUrl = s.thumbnail?.replace("w120-h120", "w544-h544")
                        s.id to PosterBitmapRenderer.loadBitmap(context, hiUrl, maxSize = 640)
                    }
                }.awaitAll().toMap()
            }
            val fgMask = (maskState as? SubjectMaskState.Ready)?.maskBitmap
            PosterBitmapRenderer.renderPhotoCardsBitmap(
                config = config,
                cards = cards,
                cardCovers = coverMap,
                palette = palette,
                customPhoto = customPhotoBitmap,
                foregroundMask = fgMask,
                primaryCover = primaryCover,
            )
        } else {
            PosterBitmapRenderer.renderBitmap(
                song = song,
                cover = primaryCover,
                palette = palette,
                config = config,
                selectedLyrics = selectedLines,
                positionMs = request.positionMs,
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF131116),
        contentColor = WarmInk,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            // Header Studio
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Studio Poster & Kartu",
                        style = MaterialTheme.typography.titleLarge,
                        color = WarmInk,
                    )
                    Text(
                        text = "${song.title} · ${song.artistsText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, "Tutup", tint = WarmMuted)
                }
            }

            // Saklar 3 Mode (Kartu Foto | Poster Lagu | Poster Lirik)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, WarmHairline, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PosterMode.entries.forEach { mode ->
                    val active = config.mode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) palette.accent else Color.Transparent)
                            .clickable {
                                config = config.copy(
                                    mode = mode,
                                    aspect = if (mode == PosterMode.PHOTO_CARDS) PosterAspect.STORY_9_16 else config.aspect,
                                )
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (active) Color(0xFF1A0A04) else WarmInk,
                            maxLines = 1,
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Area Scrollable: Live Preview + Panel Kustomisasi
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                // Pratinjau Langsung (WYSIWYG)
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (config.mode == PosterMode.PHOTO_CARDS) {
                        PhotoCardsLivePreview(
                            config = config,
                            cards = cards,
                            selectedSlotId = selectedSlotId,
                            palette = palette,
                            primaryCoverUrl = coverUrl,
                            customPhoto = customPhotoBitmap,
                            maskState = maskState,
                            onSelectSlot = { selectedSlotId = it },
                            onDragSlot = { slotId, dxFrac, dyFrac ->
                                cards = cards.map { s ->
                                    if (s.slotId == slotId) {
                                        s.copy(
                                            xFraction = (s.xFraction + dxFrac).coerceIn(0.08f, 0.92f),
                                            yFraction = (s.yFraction + dyFrac).coerceIn(0.10f, 0.88f),
                                        )
                                    } else {
                                        s
                                    }
                                }
                            },
                            onPickPhoto = { photoPickerLauncher.launch("image/*") },
                            modifier = Modifier.heightIn(max = 395.dp),
                        )
                    } else {
                        PosterLivePreview(
                            song = song,
                            coverUrl = coverUrl,
                            palette = palette,
                            config = config,
                            selectedLines = selectedLines,
                            positionMs = request.positionMs,
                            modifier = Modifier.heightIn(max = 355.dp),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (config.mode == PosterMode.PHOTO_CARDS) {
                    // Tab Kontrol Mode Kartu Foto
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PhotoStudioTab.entries.forEach { tab ->
                            val selected = photoTab == tab
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f))
                                    .border(
                                        1.dp,
                                        if (selected) palette.accent.copy(alpha = 0.65f) else WarmHairline,
                                        RoundedCornerShape(14.dp),
                                    )
                                    .clickable { photoTab = tab }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(tab.icon, null, tint = if (selected) palette.accent else WarmMuted, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) WarmInk else WarmMuted,
                                    maxLines = 1,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    when (photoTab) {
                        PhotoStudioTab.PHOTO -> PhotoBackdropPanel(
                            config = config,
                            hasPhoto = customPhotoBitmap != null,
                            maskState = maskState,
                            accent = palette.accent,
                            onPickPhoto = { photoPickerLauncher.launch("image/*") },
                            onClearPhoto = { customPhotoUri = null },
                            onRetryMask = { maskRetryCounter++ },
                            onChange = { config = it },
                        )

                        PhotoStudioTab.CARDS -> PhotoCardsLayoutPanel(
                            config = config,
                            cards = cards,
                            selectedSlotId = selectedSlotId,
                            accent = palette.accent,
                            onPreset = { preset ->
                                config = config.copy(photoCards = config.photoCards.copy(preset = preset))
                                cards = GlassCardSlot.buildForPreset(
                                    preset = preset,
                                    songs = candidateSongs,
                                    primarySong = song,
                                    primaryProgress = primaryProgress,
                                    likedIds = likedIds,
                                )
                                selectedSlotId = cards.firstOrNull { it.song.id == song.id }?.slotId ?: 0
                            },
                            onSelectSlot = { selectedSlotId = it },
                            onSwapSong = { pickingForSlotId = it },
                            onAddCard = { pickingForSlotId = -1 },
                            onUpdateSlot = { updated ->
                                cards = cards.map { if (it.slotId == updated.slotId) updated else it }
                            },
                            onRemoveSlot = { id ->
                                if (cards.size > 1) {
                                    cards = cards.filterNot { it.slotId == id }
                                    selectedSlotId = cards.first().slotId
                                }
                            },
                        )

                        PhotoStudioTab.GLASS -> PhotoGlassStylePanel(
                            photoCfg = config.photoCards,
                            accent = palette.accent,
                            onChange = { config = config.copy(photoCards = it) },
                        )
                    }
                } else {
                    // Tab Kontrol Mode Poster Lagu & Poster Lirik
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PosterStudioTab.entries.forEach { tab ->
                            val selected = posterTab == tab
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f))
                                    .border(
                                        1.dp,
                                        if (selected) palette.accent.copy(alpha = 0.65f) else WarmHairline,
                                        RoundedCornerShape(14.dp),
                                    )
                                    .clickable { posterTab = tab }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(tab.icon, null, tint = if (selected) palette.accent else WarmMuted, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(tab.label, style = MaterialTheme.typography.labelLarge, color = if (selected) WarmInk else WarmMuted)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    when (posterTab) {
                        PosterStudioTab.STYLE -> StyleControlsPanel(
                            config = config,
                            accent = palette.accent,
                            onChange = { config = it },
                        )

                        PosterStudioTab.LYRICS -> LyricsPickerPanel(
                            lyricsState = lyricsState,
                            lines = allLyricLines,
                            selectedIndices = selectedIndices,
                            accent = palette.accent,
                            onToggleIndex = { idx ->
                                selectedIndices = when {
                                    idx in selectedIndices -> selectedIndices - idx
                                    selectedIndices.size < 4 -> (selectedIndices + idx).sorted()
                                    else -> (selectedIndices.drop(1) + idx).sorted()
                                }
                            },
                            onRetry = {
                                scope.launch {
                                    lyricsState = LyricsUiState.Loading
                                    lyricsState = fetchLyricsForSong()
                                }
                            },
                        )

                        PosterStudioTab.TEXT -> TypographyControlsPanel(
                            config = config,
                            accent = palette.accent,
                            onChange = { config = it },
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
            }

            // Bilah Aksi Ekspor Bawah (Simpan ke Galeri · Bagikan PNG · Bagikan Teks)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF19161D))
                    .border(BorderStroke(0.8.dp, WarmHairline))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    enabled = busyAction == null,
                    onClick = {
                        scope.launch {
                            busyAction = "save"
                            val bmp = renderCurrentHighResBitmap()
                            val res = PosterBitmapRenderer.saveToGallery(context, bmp, song.title)
                            busyAction = null
                            Toast.makeText(
                                context,
                                if (res.isSuccess) "Tersimpan di Galeri (Pictures/Musikku)" else "Gagal menyimpan gambar",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    border = BorderStroke(1.dp, WarmHairline),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    if (busyAction == "save") {
                        CircularProgressIndicator(strokeWidth = 2.dp, color = WarmInk, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Rounded.Download, null, tint = WarmInk, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Simpan", color = WarmInk, maxLines = 1)
                }

                Button(
                    enabled = busyAction == null,
                    onClick = {
                        scope.launch {
                            busyAction = "share"
                            val bmp = renderCurrentHighResBitmap()
                            val res = PosterBitmapRenderer.shareImage(context, bmp, song)
                            busyAction = null
                            if (res.isFailure) {
                                Toast.makeText(context, "Gagal membagikan gambar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color(0xFF1A0A04)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1.25f),
                ) {
                    if (busyAction == "share") {
                        CircularProgressIndicator(strokeWidth = 2.dp, color = Color(0xFF1A0A04), modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Outlined.Share, null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (config.mode == PosterMode.PHOTO_CARDS && config.background.isTransparent && customPhotoBitmap == null) {
                            "Stiker PNG"
                        } else {
                            "Bagikan"
                        },
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }

                IconButton(
                    onClick = { PosterBitmapRenderer.shareAsText(context, song, selectedLines) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(1.dp, WarmHairline, RoundedCornerShape(14.dp)),
                ) {
                    Icon(Icons.Outlined.TextFields, "Bagikan teks", tint = WarmInk, modifier = Modifier.size(19.dp))
                }
            }
        }
    }
}

// =============================================================================
// PRATINJAU INTERAKTIF MODE KARTU FOTO LIQUID GLASS (DRAG + AI DEPTH!)
// =============================================================================

@Composable
private fun PhotoCardsLivePreview(
    config: PosterConfig,
    cards: List<GlassCardSlot>,
    selectedSlotId: Int,
    palette: CoverPalette,
    primaryCoverUrl: String?,
    customPhoto: Bitmap?,
    maskState: SubjectMaskState,
    onSelectSlot: (Int) -> Unit,
    onDragSlot: (Int, Float, Float) -> Unit,
    onPickPhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(config.aspect.ratio)
            .shadow(22.dp, shape, ambientColor = palette.ambient, spotColor = Color.Black)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape),
    ) {
        val boxW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val boxH = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        // 1. Latar Belakang (Foto Sendiri / Transparan Checkerboard / Latar Bawaan)
        when {
            customPhoto != null -> {
                Image(
                    bitmap = customPhoto.asImageBitmap(),
                    contentDescription = "Foto latar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            config.background.isTransparent -> {
                CheckerboardBackdrop(Modifier.fillMaxSize())
            }

            else -> {
                PosterBackgroundLayer(
                    bg = config.background,
                    palette = palette,
                    coverUrl = primaryCoverUrl,
                )
            }
        }

        // 2. Kartu-kartu Pemutar Liquid Glass (bisa digeser langsung dengan jari!)
        val baseRefPx = min(boxW, boxH * 0.60f)
        cards.forEach { slot ->
            val cardWPx = baseRefPx * 0.62f * slot.scale
            val cardHPx = cardWPx * 1.54f
            val cardWDp = with(density) { cardWPx.toDp() }
            val cardHDp = with(density) { cardHPx.toDp() }
            val leftPx = (boxW * slot.xFraction - cardWPx / 2f).roundToInt()
            val topPx = (boxH * slot.yFraction - cardHPx / 2f).roundToInt()
            val isSelected = slot.slotId == selectedSlotId

            Box(
                modifier = Modifier
                    .offset { IntOffset(leftPx, topPx) }
                    .size(cardWDp, cardHDp)
                    .zIndex(if (isSelected) 4f else 2f)
                    .graphicsLayer { rotationZ = slot.rotationDeg }
                    .pointerInput(slot.slotId, boxW, boxH) {
                        detectDragGestures(
                            onDragStart = { onSelectSlot(slot.slotId) },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                // Rotasi vektor drag mengikuti orientasi kartu di layar
                                val rad = Math.toRadians(slot.rotationDeg.toDouble())
                                val cosR = cos(rad).toFloat()
                                val sinR = sin(rad).toFloat()
                                val worldDx = dragAmount.x * cosR - dragAmount.y * sinR
                                val worldDy = dragAmount.x * sinR + dragAmount.y * cosR
                                onDragSlot(slot.slotId, worldDx / boxW, worldDy / boxH)
                            },
                        )
                    }
                    .clickable { onSelectSlot(slot.slotId) },
            ) {
                GlassPlayerCardPreview(
                    slot = slot,
                    photoCfg = config.photoCards,
                    selected = isSelected && cards.size > 1,
                    accent = palette.accent,
                    cardWPx = cardWPx,
                )
            }
        }

        // 3. Lapisan Subjek Depan (AI Subject Mask — melapis DI ATAS kartu!)
        if (customPhoto != null && config.photoCards.subjectInFront && maskState is SubjectMaskState.Ready) {
            Image(
                bitmap = maskState.maskBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(8f),
            )
        }

        // 4. Watermark Musikku di bawah tengah
        if (config.photoCards.showWatermark && !config.background.isTransparent) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (customPhoto == null) 46.dp else 14.dp)
                    .zIndex(9f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFFF7F5C), Color(0xFFFFB85C)))),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(9.dp)) {
                        val p = Path().apply {
                            moveTo(0f, size.height * 0.55f)
                            cubicTo(
                                size.width * 0.35f, size.height * 0.1f,
                                size.width * 0.65f, size.height * 0.9f,
                                size.width, size.height * 0.45f,
                            )
                        }
                        drawPath(p, Color.White, style = Stroke(width = 2.4f, cap = StrokeCap.Round))
                    }
                }
                Text(
                    text = "Musikku",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    color = Color.White.copy(alpha = 0.94f),
                )
            }
        }

        // 5. Pil cepat "+ Tambahkan foto Anda sendiri" bila belum memilih foto (persis gaya screenshot!)
        if (customPhoto == null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .zIndex(10f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.58f))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onPickPhoto)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.Rounded.AddPhotoAlternate, null, tint = Color.White, modifier = Modifier.size(14.dp))
                Text(
                    text = "+ Tambahkan foto Anda sendiri",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
            }
        }

        // Indikator proses ML Kit Subject Segmentation
        if (maskState is SubjectMaskState.Processing && customPhoto != null && config.photoCards.subjectInFront) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .zIndex(11f)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.68f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CircularProgressIndicator(strokeWidth = 1.8.dp, color = palette.accent, modifier = Modifier.size(12.dp))
                Text("Mendeteksi subjek…", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }
    }
}

/** Satu Kartu Pemutar Liquid Glass di Pratinjau — anatomi identik dengan hasil render PNG resolusi tinggi. */
@Composable
private fun GlassPlayerCardPreview(
    slot: GlassCardSlot,
    photoCfg: PhotoCardsConfig,
    selected: Boolean,
    accent: Color,
    cardWPx: Float,
) {
    val density = LocalDensity.current
    val scaleFactor = (cardWPx / 180f).coerceIn(0.45f, 1.35f)
    val corner = (14f * scaleFactor).dp
    val pad = (10f * scaleFactor).dp
    val shape = RoundedCornerShape(corner)
    val opacity = photoCfg.glassOpacity.coerceIn(0.32f, 0.88f)
    val (elapsed, remain) = remember(slot) { slot.formattedTimestamps() }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .shadow((10f * scaleFactor).dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF2C2A2E).copy(alpha = opacity),
                            Color(0xFF141317).copy(alpha = (opacity + 0.14f).coerceAtMost(0.94f)),
                        )
                    )
                )
                .border(
                    width = if (selected) 1.6.dp else 1.dp,
                    brush = if (selected) {
                        Brush.linearGradient(listOf(accent, Color.White.copy(alpha = 0.85f)))
                    } else {
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.56f),
                                Color.White.copy(alpha = 0.16f),
                                Color.White.copy(alpha = 0.36f),
                            )
                        )
                    },
                    shape = shape,
                )
                .padding(pad),
        ) {
            // 1. Sampul Album Persegi
            Artwork(
                url = slot.song.thumbnail?.replace("w120-h120", "w544-h544"),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .border(0.6.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape((8f * scaleFactor).dp)),
                shape = RoundedCornerShape((8f * scaleFactor).dp),
            )

            Spacer(Modifier.height((4.5f * scaleFactor).dp))

            // 2. Micro-brand "~ Musikku"
            Text(
                text = "∿ Musikku",
                color = Color.White.copy(alpha = 0.62f),
                fontSize = (6.8f * scaleFactor).sp,
                lineHeight = (8f * scaleFactor).sp,
                maxLines = 1,
            )

            // 3. Judul Lagu + Ikon Hati
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = slot.song.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (11.2f * scaleFactor).sp,
                    lineHeight = (12.8f * scaleFactor).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (photoCfg.showHeart) {
                    Spacer(Modifier.width((3f * scaleFactor).dp))
                    Icon(
                        imageVector = if (slot.isLiked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = if (slot.isLiked) Color(0xFFFF765C) else Color.White.copy(alpha = 0.88f),
                        modifier = Modifier.size((11.5f * scaleFactor).dp),
                    )
                }
            }

            // 4. Nama Artis
            Text(
                text = slot.song.artistsText.ifBlank { "Musikku" },
                color = Color.White.copy(alpha = 0.72f),
                fontSize = (7.6f * scaleFactor).sp,
                lineHeight = (9.2f * scaleFactor).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.weight(1f))

            // 5. Seekbar + Timestamp
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((5.5f * scaleFactor).dp),
            ) {
                val cy = size.height / 2f
                val stroke = (1.6f * scaleFactor).coerceAtLeast(1f)
                drawLine(
                    color = Color.White.copy(alpha = 0.28f),
                    start = Offset(0f, cy),
                    end = Offset(size.width, cy),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                val px = size.width * slot.progressFraction.coerceIn(0.08f, 0.90f)
                drawLine(
                    color = Color.White,
                    start = Offset(0f, cy),
                    end = Offset(px, cy),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawCircle(color = Color.White, radius = (2.6f * scaleFactor).coerceAtLeast(1.8f), center = Offset(px, cy))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(elapsed, color = Color.White.copy(alpha = 0.58f), fontSize = (5.8f * scaleFactor).sp, lineHeight = (6.8f * scaleFactor).sp)
                Text(remain, color = Color.White.copy(alpha = 0.58f), fontSize = (5.8f * scaleFactor).sp, lineHeight = (6.8f * scaleFactor).sp)
            }

            Spacer(Modifier.height((2.5f * scaleFactor).dp))

            // 6. Tombol Kontrol (⏮ ⏸ ⏭)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.FastRewind, null, tint = Color.White, modifier = Modifier.size((12f * scaleFactor).dp))
                Icon(Icons.Rounded.Pause, null, tint = Color.White, modifier = Modifier.size((14f * scaleFactor).dp))
                Icon(Icons.Rounded.FastForward, null, tint = Color.White, modifier = Modifier.size((12f * scaleFactor).dp))
            }

            Spacer(Modifier.height((2f * scaleFactor).dp))

            // 7. Slider Volume Bawah (🔈 ——●—— 🔊)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((3f * scaleFactor).dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.VolumeDown, null, tint = Color.White.copy(alpha = 0.78f), modifier = Modifier.size((8.5f * scaleFactor).dp))
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height((4.5f * scaleFactor).dp),
                ) {
                    val cy = size.height / 2f
                    val st = (1.3f * scaleFactor).coerceAtLeast(1f)
                    drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, cy), Offset(size.width, cy), st, StrokeCap.Round)
                    val vx = size.width * slot.volumeFraction
                    drawLine(Color.White.copy(alpha = 0.9f), Offset(0f, cy), Offset(vx, cy), st, StrokeCap.Round)
                    drawCircle(Color.White, (2f * scaleFactor).coerceAtLeast(1.4f), Offset(vx, cy))
                }
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = Color.White.copy(alpha = 0.78f), modifier = Modifier.size((8.5f * scaleFactor).dp))
            }
        }

        // 8. Kilau Sudut 4-Ujung (`✦`)
        if (photoCfg.showSparkles && slot.sparkleCorner != SparkleCorner.NONE) {
            Canvas(Modifier.fillMaxSize()) {
                val r = with(density) { corner.toPx() } * 0.28f
                val cx = when (slot.sparkleCorner) {
                    SparkleCorner.TOP_RIGHT, SparkleCorner.BOTTOM_RIGHT -> size.width - r
                    SparkleCorner.TOP_LEFT -> r
                    SparkleCorner.NONE -> 0f
                }
                val cy = when (slot.sparkleCorner) {
                    SparkleCorner.TOP_RIGHT, SparkleCorner.TOP_LEFT -> r
                    SparkleCorner.BOTTOM_RIGHT -> size.height - r
                    SparkleCorner.NONE -> 0f
                }
                val starR = size.width * 0.12f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.72f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = starR,
                    ),
                    radius = starR,
                    center = Offset(cx, cy),
                )
                val star = Path()
                for (i in 0 until 8) {
                    val angle = Math.toRadians(i * 45.0)
                    val dist = if (i % 2 == 0) starR else starR * 0.14f
                    val px = cx + (cos(angle) * dist).toFloat()
                    val py = cy + (sin(angle) * dist).toFloat()
                    if (i == 0) star.moveTo(px, py) else star.lineTo(px, py)
                }
                star.close()
                drawPath(star, Color.White)
            }
        }
    }
}

@Composable
private fun CheckerboardBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val cell = 14.dp.toPx()
        val cols = (size.width / cell).toInt() + 1
        val rows = (size.height / cell).toInt() + 1
        for (r in 0..rows) {
            for (c in 0..cols) {
                val dark = (r + c) % 2 == 0
                drawRect(
                    color = if (dark) Color(0xFF232128) else Color(0xFF2F2D36),
                    topLeft = Offset(c * cell, r * cell),
                    size = Size(cell, cell),
                )
            }
        }
    }
}

// =============================================================================
// PANEL KONTROL MODE KARTU FOTO (FOTO & LATAR / KARTU LAGU / GAYA KACA)
// =============================================================================

@Composable
private fun PhotoBackdropPanel(
    config: PosterConfig,
    hasPhoto: Boolean,
    maskState: SubjectMaskState,
    accent: Color,
    onPickPhoto: () -> Unit,
    onClearPhoto: () -> Unit,
    onRetryMask: () -> Unit,
    onChange: (PosterConfig) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Tombol Pilih Foto Sendiri dari Galeri
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onPickPhoto,
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF1A0A04)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.AddPhotoAlternate, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(if (hasPhoto) "Ganti Foto Sendiri" else "Pilih Foto dari Galeri", fontWeight = FontWeight.Bold)
            }
            if (hasPhoto) {
                OutlinedButton(
                    onClick = onClearPhoto,
                    border = BorderStroke(1.dp, WarmHairline),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Rounded.DeleteOutline, "Hapus foto", tint = WarmInk, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Efek Kedalaman "Subjek di Depan" (AI Subject Segmentation!)
        if (hasPhoto) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, WarmHairline, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Subjek di Depan (Efek Kedalaman)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = WarmInk,
                        )
                        val subtitle = when {
                            !config.photoCards.subjectInFront -> "Kartu tampil di atas seluruh foto"
                            maskState is SubjectMaskState.Processing -> "Memisahkan subjek foto secara otomatis…"
                            maskState is SubjectMaskState.Ready -> "Aktif — kartu melayang di belakang subjek!"
                            maskState is SubjectMaskState.Unavailable -> maskState.reason
                            else -> "Otomatis deteksi orang/objek di foto"
                        }
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = WarmMuted)
                    }
                    Spacer(Modifier.width(8.dp))
                    LiquidGlassSwitch(
                        checked = config.photoCards.subjectInFront,
                        onCheckedChange = {
                            onChange(config.copy(photoCards = config.photoCards.copy(subjectInFront = it)))
                        },
                    )
                }
                if (config.photoCards.subjectInFront && maskState is SubjectMaskState.Unavailable) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(accent.copy(alpha = 0.16f))
                            .clickable(onClick = onRetryMask)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(Icons.Rounded.Refresh, null, tint = accent, modifier = Modifier.size(14.dp))
                        Text("Coba deteksi lagi", style = MaterialTheme.typography.labelSmall, color = accent)
                    }
                }
            }
        }

        // Rasio Kanvas
        StudioLabel("Rasio Kanvas")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PosterAspect.entries.forEach { aspect ->
                val selected = config.aspect == aspect
                OptionChip(
                    label = "${aspect.label} ${aspect.ratioText}",
                    selected = selected,
                    accent = accent,
                    onClick = { onChange(config.copy(aspect = aspect)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Latar Tanpa Foto (termasuk Transparan PNG Stiker!)
        if (!hasPhoto) {
            StudioLabel("Latar Belakang (Tanpa Foto)")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PosterBackground.entries.forEach { bg ->
                    OptionChip(
                        label = if (bg.isTransparent) "Transparan (Stiker)" else bg.label,
                        selected = config.background == bg,
                        accent = accent,
                        onClick = { onChange(config.copy(background = bg)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoCardsLayoutPanel(
    config: PosterConfig,
    cards: List<GlassCardSlot>,
    selectedSlotId: Int,
    accent: Color,
    onPreset: (CardCollagePreset) -> Unit,
    onSelectSlot: (Int) -> Unit,
    onSwapSong: (Int) -> Unit,
    onAddCard: () -> Unit,
    onUpdateSlot: (GlassCardSlot) -> Unit,
    onRemoveSlot: (Int) -> Unit,
) {
    val activeSlot = cards.firstOrNull { it.slotId == selectedSlotId } ?: cards.firstOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StudioLabel("Preset Kolase Kartu (geser kartu langsung di pratinjau)")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CardCollagePreset.entries.forEach { preset ->
                OptionChip(
                    label = preset.label,
                    selected = cards.size == preset.count && config.photoCards.preset == preset,
                    accent = accent,
                    onClick = { onPreset(preset) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Daftar Slot Kartu + Tambah Kartu (hingga 5)
        StudioLabel("Pilih Kartu yang Diatur (${cards.size}/5)")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            cards.forEachIndexed { idx, slot ->
                OptionChip(
                    label = "#${idx + 1} ${slot.song.title.take(12)}",
                    selected = slot.slotId == activeSlot?.slotId,
                    accent = accent,
                    onClick = { onSelectSlot(slot.slotId) },
                )
            }
            if (cards.size < 5) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.16f))
                        .border(1.dp, accent.copy(alpha = 0.48f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onAddCard)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Rounded.Add, null, tint = accent, modifier = Modifier.size(15.dp))
                    Text("Tambah", style = MaterialTheme.typography.labelMedium, color = accent)
                }
            }
        }

        // Kontrol Kartu Terpilih: Ganti Lagu, Kemiringan, Ukuran, Hapus
        if (activeSlot != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, WarmHairline, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Artwork(activeSlot.song.thumbnail, Modifier.size(36.dp), RoundedCornerShape(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            activeSlot.song.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = WarmInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            activeSlot.song.artistsText,
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    OutlinedButton(
                        onClick = { onSwapSong(activeSlot.slotId) },
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Rounded.SwapHoriz, null, tint = accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ganti", color = accent, style = MaterialTheme.typography.labelMedium)
                    }
                    if (cards.size > 1) {
                        IconButton(onClick = { onRemoveSlot(activeSlot.slotId) }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Rounded.DeleteOutline, "Hapus kartu", tint = WarmMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Slider Kemiringan (-25° .. +25°)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Miring ${activeSlot.rotationDeg.roundToInt()}°",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmMuted,
                        modifier = Modifier.width(74.dp),
                    )
                    Slider(
                        value = activeSlot.rotationDeg,
                        onValueChange = { onUpdateSlot(activeSlot.copy(rotationDeg = it)) },
                        valueRange = -25f..25f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = accent),
                        modifier = Modifier.weight(1f),
                    )
                }

                // Slider Ukuran (0.38 .. 1.10)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Ukuran ${(activeSlot.scale * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmMuted,
                        modifier = Modifier.width(74.dp),
                    )
                    Slider(
                        value = activeSlot.scale,
                        onValueChange = { onUpdateSlot(activeSlot.copy(scale = it)) },
                        valueRange = 0.38f..1.10f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = accent),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoGlassStylePanel(
    photoCfg: PhotoCardsConfig,
    accent: Color,
    onChange: (PhotoCardsConfig) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Kegelapan Kaca",
                style = MaterialTheme.typography.labelMedium,
                color = WarmInk,
                modifier = Modifier.width(114.dp),
            )
            Slider(
                value = photoCfg.glassOpacity,
                onValueChange = { onChange(photoCfg.copy(glassOpacity = it)) },
                valueRange = 0.34f..0.84f,
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = accent),
                modifier = Modifier.weight(1f),
            )
        }
        StudioToggleRow("Kilau sudut kaca (✦)", photoCfg.showSparkles, accent) {
            onChange(photoCfg.copy(showSparkles = it))
        }
        StudioToggleRow("Tampilkan ikon suka (♡)", photoCfg.showHeart, accent) {
            onChange(photoCfg.copy(showHeart = it))
        }
        StudioToggleRow("Watermark Musikku di bawah", photoCfg.showWatermark, accent) {
            onChange(photoCfg.copy(showWatermark = it))
        }
    }
}

// =============================================================================
// PRATINJAU & PANEL MODE POSTER LAGU + POSTER LIRIK
// =============================================================================

@Composable
private fun PosterLivePreview(
    song: SongItem,
    coverUrl: String?,
    palette: CoverPalette,
    config: PosterConfig,
    selectedLines: List<String>,
    positionMs: Long,
    modifier: Modifier = Modifier,
) {
    val effectiveBg = if (config.background.isTransparent) PosterBackground.LIQUID_GLASS else config.background
    val effectiveCfg = config.copy(background = effectiveBg)
    val isPaper = effectiveBg.isLightPaper
    val primaryText = effectiveCfg.resolvePrimaryTextColor(palette)
    val secondaryText = effectiveCfg.resolveSecondaryTextColor(palette)
    val accentColor = if (isPaper) Color(0xFFB44C2A) else palette.accent
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .aspectRatio(config.aspect.ratio)
            .shadow(20.dp, shape, ambientColor = palette.ambient, spotColor = Color.Black)
            .clip(shape)
            .border(1.dp, if (isPaper) Color(0x261A1613) else Color.White.copy(alpha = 0.16f), shape),
    ) {
        PosterBackgroundLayer(bg = effectiveBg, palette = palette, coverUrl = coverUrl)

        if (effectiveBg == PosterBackground.LIQUID_GLASS) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp)),
            )
        } else if (isPaper) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .border(0.8.dp, Color(0x2A1A1613), RoundedCornerShape(12.dp)),
            )
        }

        if (config.mode == PosterMode.SONG_POSTER) {
            SongPosterPreviewBody(
                song = song,
                coverUrl = coverUrl,
                palette = palette,
                config = effectiveCfg,
                quoteLine = selectedLines.firstOrNull(),
                positionMs = positionMs,
                primaryText = primaryText,
                secondaryText = secondaryText,
                accentColor = accentColor,
            )
        } else {
            LyricPosterPreviewBody(
                song = song,
                coverUrl = coverUrl,
                palette = palette,
                config = effectiveCfg,
                lines = selectedLines,
                primaryText = primaryText,
                secondaryText = secondaryText,
                accentColor = accentColor,
            )
        }
    }
}

@Composable
private fun PosterBackgroundLayer(bg: PosterBackground, palette: CoverPalette, coverUrl: String?) {
    when (bg) {
        PosterBackground.LIQUID_GLASS, PosterBackground.TRANSPARENT -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                palette.ambient.copy(alpha = 0.55f),
                                Color(0xFF100E14),
                                Color(0xFF0A090D),
                            )
                        )
                    ),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(palette.secondaryAccent.copy(alpha = 0.45f), Color.Transparent),
                            radius = 480f,
                        )
                    ),
            )
        }

        PosterBackground.EDITORIAL_PAPER -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFFF6F0E7), Color(0xFFEDE5D8)))),
            )
        }

        PosterBackground.OBSIDIAN -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF151312), Color(0xFF0B0A09)))),
            )
        }

        PosterBackground.COVER_BLUR -> {
            Artwork(url = coverUrl, modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp))
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x880A090C),
                                Color(0xAA0C0A0E),
                                Color(0xDE08070A),
                            )
                        )
                    ),
            )
        }
    }
}

@Composable
private fun SongPosterPreviewBody(
    song: SongItem,
    coverUrl: String?,
    palette: CoverPalette,
    config: PosterConfig,
    quoteLine: String?,
    positionMs: Long,
    primaryText: Color,
    secondaryText: Color,
    accentColor: Color,
) {
    val isPaper = config.background.isLightPaper
    val isSquare = config.aspect == PosterAspect.SQUARE_1_1
    val swatches = remember(palette, isPaper) { PosterPaletteSwatches.buildSwatches(palette, isPaper) }
    val horizontalAlign = when (config.align) {
        PosterAlign.START -> Alignment.Start
        PosterAlign.CENTER -> Alignment.CenterHorizontally
        PosterAlign.END -> Alignment.End
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = (song.album?.name?.takeIf { it.isNotBlank() } ?: "SEDANG DIPUTAR").uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold),
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = config.aspect.ratioText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontFamily = FontFamily.Monospace),
                color = secondaryText,
            )
        }

        Spacer(Modifier.height(if (isSquare) 8.dp else 12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(if (isSquare) 0.56f else 0.84f)
                .aspectRatio(1f)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isPaper) Color(0xC0FFFCF7) else Color.White.copy(alpha = 0.10f))
                .border(0.8.dp, if (isPaper) Color(0x241A1613) else Color.White.copy(alpha = 0.24f), RoundedCornerShape(14.dp))
                .padding(5.dp),
        ) {
            Artwork(url = coverUrl, modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(10.dp))
        }

        if (config.showPaletteBar) {
            Spacer(Modifier.height(if (isSquare) 8.dp else 10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                swatches.forEach { c ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(if (isSquare) 5.dp else 7.dp)
                            .clip(CircleShape)
                            .background(c),
                    )
                }
            }
        }

        Spacer(Modifier.height(if (isSquare) 8.dp else 10.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = horizontalAlign,
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = config.font.family,
                    fontWeight = config.weight.weight,
                    fontStyle = config.resolveFontStyle(),
                    fontSize = if (isSquare) 16.sp else 19.sp,
                    lineHeight = if (isSquare) 19.sp else 23.sp,
                ),
                textAlign = config.align.textAlign,
                color = primaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artistsText.ifBlank { "Musikku" },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                textAlign = config.align.textAlign,
                color = secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!quoteLine.isNullOrBlank() && !isSquare) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "“$quoteLine”",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp,
                    ),
                    textAlign = config.align.textAlign,
                    color = primaryText.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        if (config.showProgressBar) {
            val totalMs = (song.durationSec.takeIf { it > 0 } ?: 210) * 1000L
            val prog = if (positionMs > 0L && totalMs > 0L) (positionMs.toFloat() / totalMs).coerceIn(0.06f, 0.94f) else 0.38f
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(if (isPaper) Color(0x241A1613) else Color.White.copy(alpha = 0.20f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(prog)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                )
            }
            Spacer(Modifier.height(3.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = formatMs(if (positionMs > 0L) positionMs else (totalMs * prog).toLong()),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, fontFamily = FontFamily.Monospace),
                    color = secondaryText,
                )
                Text(
                    text = formatMs(totalMs),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, fontFamily = FontFamily.Monospace),
                    color = secondaryText,
                )
            }
            Spacer(Modifier.height(6.dp))
        }

        if (config.showWatermark) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(accentColor))
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "MUSIKKU",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold),
                    color = primaryText,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Poster Lagu",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic),
                    color = secondaryText,
                )
            }
        }
    }
}

@Composable
private fun LyricPosterPreviewBody(
    song: SongItem,
    coverUrl: String?,
    palette: CoverPalette,
    config: PosterConfig,
    lines: List<String>,
    primaryText: Color,
    secondaryText: Color,
    accentColor: Color,
) {
    val isPaper = config.background.isLightPaper
    val swatches = remember(palette, isPaper) { PosterPaletteSwatches.buildSwatches(palette, isPaper) }
    val body = remember(lines, song) {
        lines.filter { it.isNotBlank() }.ifEmpty { listOf(song.title, song.artistsText.ifBlank { "Sedang diputar di Musikku" }) }.joinToString("\n")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Artwork(url = coverUrl, modifier = Modifier.size(34.dp), shape = RoundedCornerShape(8.dp))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                    color = primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artistsText.ifBlank { "Musikku" },
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    color = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment = when (config.align) {
                PosterAlign.START -> Alignment.CenterStart
                PosterAlign.CENTER -> Alignment.Center
                PosterAlign.END -> Alignment.CenterEnd
            },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = when (config.align) {
                    PosterAlign.START -> Alignment.Start
                    PosterAlign.CENTER -> Alignment.CenterHorizontally
                    PosterAlign.END -> Alignment.End
                },
            ) {
                Text(
                    text = "“",
                    style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Serif, fontSize = 30.sp, lineHeight = 24.sp),
                    color = accentColor.copy(alpha = 0.8f),
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = config.font.family,
                        fontWeight = config.weight.weight,
                        fontStyle = config.resolveFontStyle(),
                        fontSize = when {
                            body.length < 55 -> 19.sp
                            body.length < 110 -> 16.sp
                            else -> 13.5.sp
                        },
                        lineHeight = when {
                            body.length < 55 -> 24.sp
                            body.length < 110 -> 21.sp
                            else -> 18.sp
                        },
                    ),
                    textAlign = config.align.textAlign,
                    color = primaryText,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (config.showPaletteBar) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                swatches.forEach { c ->
                    Box(Modifier.size(7.dp).clip(CircleShape).background(c))
                }
            }
            Spacer(Modifier.height(7.dp))
        }

        if (config.showWatermark) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(accentColor))
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "MUSIKKU",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold),
                    color = primaryText,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Poster Lirik",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic),
                    color = secondaryText,
                )
            }
        }
    }
}

@Composable
private fun StyleControlsPanel(
    config: PosterConfig,
    accent: Color,
    onChange: (PosterConfig) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StudioLabel("Gaya Latar")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PosterBackground.entries.filterNot { it.isTransparent }.forEach { bg ->
                OptionChip(
                    label = bg.label,
                    selected = config.background == bg,
                    accent = accent,
                    onClick = { onChange(config.copy(background = bg)) },
                )
            }
        }

        StudioLabel("Ukuran & Rasio")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PosterAspect.entries.forEach { aspect ->
                OptionChip(
                    label = "${aspect.label} ${aspect.ratioText}",
                    selected = config.aspect == aspect,
                    accent = accent,
                    onClick = { onChange(config.copy(aspect = aspect)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        StudioToggleRow("Strip palet warna sampul", config.showPaletteBar, accent) {
            onChange(config.copy(showPaletteBar = it))
        }
        if (config.mode == PosterMode.SONG_POSTER) {
            StudioToggleRow("Bar durasi lagu", config.showProgressBar, accent) {
                onChange(config.copy(showProgressBar = it))
            }
        }
        StudioToggleRow("Watermark Musikku", config.showWatermark, accent) {
            onChange(config.copy(showWatermark = it))
        }
    }
}

@Composable
private fun LyricsPickerPanel(
    lyricsState: LyricsUiState,
    lines: List<String>,
    selectedIndices: List<Int>,
    accent: Color,
    onToggleIndex: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    val isMissingOrError = lyricsState is LyricsUiState.Error ||
        (lyricsState as? LyricsUiState.Loaded)?.lyrics is Lyrics.NotFound
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StudioLabel("Pilih hingga 4 baris lirik (${selectedIndices.size}/4)")
            Spacer(Modifier.weight(1f))
            if (isMissingOrError) {
                Text(
                    text = "Muat ulang",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    modifier = Modifier.clickable(onClick = onRetry),
                )
            }
        }

        when {
            lyricsState is LyricsUiState.Loading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp, color = accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Mencari lirik lagu…", style = MaterialTheme.typography.bodyMedium, color = WarmMuted)
                }
            }

            lines.isEmpty() -> {
                Text(
                    text = "Lirik tidak ditemukan untuk lagu ini. Anda tetap bisa membuat Kartu Foto atau Poster Lagu!",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmMuted,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 210.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    lines.forEachIndexed { idx, line ->
                        val picked = idx in selectedIndices
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (picked) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f))
                                .border(
                                    1.dp,
                                    if (picked) accent.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(12.dp),
                                )
                                .clickable { onToggleIndex(idx) }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (picked) WarmInk else WarmMuted,
                                modifier = Modifier.weight(1f),
                            )
                            if (picked) {
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Rounded.Check, null, tint = accent, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypographyControlsPanel(
    config: PosterConfig,
    accent: Color,
    onChange: (PosterConfig) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StudioLabel("Gaya Huruf")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PosterFont.entries.forEach { font ->
                OptionChip(
                    label = font.label,
                    selected = config.font == font,
                    accent = accent,
                    onClick = { onChange(config.copy(font = font)) },
                )
            }
        }

        StudioLabel("Ketebalan")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PosterWeight.entries.forEach { w ->
                OptionChip(
                    label = w.label,
                    selected = config.weight == w,
                    accent = accent,
                    onClick = { onChange(config.copy(weight = w)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        StudioLabel("Rata Teks")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PosterAlign.entries.forEach { align ->
                OptionChip(
                    label = align.label,
                    selected = config.align == align,
                    accent = accent,
                    onClick = { onChange(config.copy(align = align)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        StudioLabel("Warna Teks")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PosterTextTone.entries.forEach { tone ->
                OptionChip(
                    label = tone.label,
                    selected = config.textTone == tone,
                    accent = accent,
                    onClick = { onChange(config.copy(textTone = tone)) },
                )
            }
        }
    }
}

@Composable
private fun StudioLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = WarmMuted,
    )
}

@Composable
private fun OptionChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f))
            .border(
                1.dp,
                if (selected) accent.copy(alpha = 0.78f) else WarmHairline,
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
            color = if (selected) WarmInk else WarmMuted,
            maxLines = 1,
        )
    }
}

@Composable
private fun StudioToggleRow(
    title: String,
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = WarmInk,
            modifier = Modifier.weight(1f),
        )
        LiquidGlassSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
