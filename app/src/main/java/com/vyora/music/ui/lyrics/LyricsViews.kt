package com.vyora.music.ui.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vyora.music.AppModule
import com.vyora.music.data.lyrics.LyricLine
import com.vyora.music.data.lyrics.Lyrics
import com.vyora.music.data.lyrics.LyricsRomanizer
import com.vyora.music.player.LyricsUiState
import com.vyora.music.ui.theme.PlayfairFamily
import com.vyora.music.ui.theme.PlusJakartaFamily
import com.vyora.music.ui.theme.WarmHairline
import com.vyora.music.ui.theme.WarmInk
import com.vyora.music.ui.theme.WarmMuted
import com.vyora.music.ui.theme.warmWash

private val EditorialLyricStyle = TextStyle(
    fontFamily = PlayfairFamily,
    fontSize = 26.sp,
    lineHeight = 36.sp,
    fontWeight = FontWeight.SemiBold,
    textAlign = TextAlign.Center,
)

private val SubLyricStyle = TextStyle(
    fontFamily = PlusJakartaFamily,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.Medium,
    textAlign = TextAlign.Center,
)

private val PlainLyricStyle = TextStyle(
    fontFamily = PlayfairFamily,
    fontSize = 21.sp,
    lineHeight = 32.sp,
    fontWeight = FontWeight.Normal,
    textAlign = TextAlign.Center,
)

private fun currentLineIndex(lines: List<LyricLine>, positionMs: Long): Int =
    lines.indexOfLast { it.timeMs <= positionMs + 220 }

/**
 * Lirik Flagship VYORA:
 * - Mendukung Karaoke Per-Kata (`BetterLyrics` TTML & Enhanced LRC)
 * - Multi-sumber (`BetterLyrics` -> `LRCLIB` -> `KuGou` + Offline Cache)
 * - Saklar Romanisasi Otomatis (Korea Hangul & Jepang Kana -> Romaji)
 * - Saklar Terjemahan Bahasa Indonesia Otomatis
 */
@Composable
fun EditorialLyricsStage(
    state: LyricsUiState,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    onRetry: () -> Unit,
    fadeColor: Color,
    offsetMs: Long = 0L,
    trackId: String = "",
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    var showTranslation by rememberSaveable { mutableStateOf(false) }
    var showRomanization by rememberSaveable { mutableStateOf(true) }
    var translations by remember(trackId) { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var translating by remember(trackId) { mutableStateOf(false) }

    val loadedLyrics = (state as? LyricsUiState.Loaded)?.lyrics
    val syncedLines = (loadedLyrics as? Lyrics.Synced)?.lines.orEmpty()
    val hasAsian = remember(syncedLines) { LyricsRomanizer.hasRomanizableLines(syncedLines) }

    LaunchedEffect(trackId, showTranslation, syncedLines) {
        if (showTranslation && syncedLines.isNotEmpty() && translations.isEmpty() && trackId.isNotBlank()) {
            translating = true
            translations = AppModule.lyricsTranslator.translateLines(
                trackId = trackId,
                lines = syncedLines.map { it.text },
                targetLang = "id",
            )
            translating = false
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        when (state) {
            LyricsUiState.Idle, LyricsUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = accent,
                            strokeWidth = 2.5.dp,
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Mencari lirik terbaik…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmMuted,
                        )
                    }
                }
            }

            LyricsUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            "Gagal memuat lirik",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = PlayfairFamily),
                            color = WarmInk,
                        )
                        OutlinedButton(onClick = onRetry) {
                            Text("Coba lagi", color = WarmInk)
                        }
                    }
                }
            }

            is LyricsUiState.Loaded -> when (val lyrics = state.lyrics) {
                is Lyrics.Synced -> SyncedEditorialLyricsList(
                    lines = lyrics.lines,
                    positionMs = positionMs - offsetMs,
                    onSeek = { lineTimeMs -> onSeek(lineTimeMs + offsetMs) },
                    showRomanization = showRomanization && hasAsian,
                    translations = if (showTranslation) translations else emptyMap(),
                    modifier = Modifier.fillMaxSize(),
                )
                is Lyrics.Plain -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 52.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = lyrics.text,
                        style = PlainLyricStyle,
                        color = WarmInk.copy(alpha = 0.90f),
                    )
                }
                Lyrics.Instrumental -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "♪ Lagu instrumental",
                        style = EditorialLyricStyle.copy(fontStyle = FontStyle.Italic),
                        color = WarmMuted,
                    )
                }
                Lyrics.NotFound -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Lirik belum tersedia\nuntuk lagu ini.",
                        style = EditorialLyricStyle.copy(fontSize = 22.sp, lineHeight = 32.sp),
                        color = WarmMuted,
                    )
                }
            }
        }

        // Gradasi fade lembut di atas & bawah area lirik
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(52.dp)
                .background(Brush.verticalGradient(listOf(fadeColor, Color.Transparent)))
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(44.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, fadeColor)))
        )

        // Baris chip fitur lirik di bagian atas (Sumber, Terjemahan ID, Romaji)
        if (loadedLyrics is Lyrics.Synced) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 6.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Badge Sumber Lirik
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(warmWash(0.08f))
                        .border(0.8.dp, WarmHairline, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (loadedLyrics.hasWordSync) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = loadedLyrics.source,
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.4.sp),
                        color = if (loadedLyrics.hasWordSync) accent else WarmMuted,
                    )
                }

                // Saklar Terjemahan Bahasa Indonesia
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (showTranslation) accent.copy(alpha = 0.22f) else warmWash(0.08f))
                        .border(0.8.dp, if (showTranslation) accent else WarmHairline, CircleShape)
                        .clickable { showTranslation = !showTranslation }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (translating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(11.dp),
                            strokeWidth = 1.6.dp,
                            color = accent,
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Translate,
                            contentDescription = null,
                            tint = if (showTranslation) accent else WarmInk,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Terjemahan ID",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.4.sp),
                        color = if (showTranslation) accent else WarmInk,
                    )
                }

                // Saklar Romanisasi otomatis bila lirik berbahasa Korea / Jepang
                if (hasAsian) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (showRomanization) accent.copy(alpha = 0.22f) else warmWash(0.08f))
                            .border(0.8.dp, if (showRomanization) accent else WarmHairline, CircleShape)
                            .clickable { showRomanization = !showRomanization }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = "Romaji",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.4.sp),
                            color = if (showRomanization) accent else WarmInk,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncedEditorialLyricsList(
    lines: List<LyricLine>,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    showRomanization: Boolean,
    translations: Map<Int, String>,
    modifier: Modifier = Modifier,
) {
    val current = remember(positionMs, lines) { currentLineIndex(lines, positionMs) }
    val listState = rememberLazyListState()
    val accent = MaterialTheme.colorScheme.primary

    LaunchedEffect(current) {
        if (current >= 0 && !listState.isScrollInProgress) {
            listState.animateScrollToItem((current - 1).coerceAtLeast(0))
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 58.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        itemsIndexed(lines) { index, line ->
            val distance = if (current >= 0) kotlin.math.abs(index - current) else index
            val isActive = index == current
            val targetColor = when {
                isActive -> WarmInk
                distance == 1 -> WarmInk.copy(alpha = 0.36f)
                distance == 2 -> WarmInk.copy(alpha = 0.24f)
                else -> WarmInk.copy(alpha = 0.16f)
            }
            val color by animateColorAsState(
                targetValue = targetColor,
                animationSpec = tween(300),
                label = "editorialLyricColor",
            )
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.03f else 0.96f,
                animationSpec = tween(300),
                label = "editorialLyricScale",
            )

            val romaji = remember(line.text, showRomanization) {
                if (showRomanization) LyricsRomanizer.romanizeLine(line.text) else null
            }
            val translated = translations[index]

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSeek(line.timeMs) }
                    .padding(vertical = 6.dp, horizontal = 8.dp),
            ) {
                if (isActive && line.words.size > 1) {
                    // Karaoke Per-Kata: kata yang sudah & sedang dinyanyikan menyala!
                    val karaokeText = buildAnnotatedString {
                        line.words.forEach { w ->
                            val wordStyle = when {
                                positionMs >= w.endMs -> SpanStyle(color = WarmInk, fontWeight = FontWeight.Bold)
                                positionMs >= w.startMs - 40L -> SpanStyle(color = accent, fontWeight = FontWeight.ExtraBold)
                                else -> SpanStyle(color = WarmInk.copy(alpha = 0.38f), fontWeight = FontWeight.Medium)
                            }
                            withStyle(wordStyle) {
                                append(w.text)
                            }
                        }
                    }
                    Text(
                        text = karaokeText,
                        style = EditorialLyricStyle,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        text = line.text.ifBlank { "♪" },
                        style = EditorialLyricStyle,
                        color = color,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (romaji != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = romaji,
                        style = SubLyricStyle,
                        color = if (isActive) accent.copy(alpha = 0.92f) else WarmMuted.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (translated != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = translated,
                        style = SubLyricStyle.copy(fontStyle = FontStyle.Italic),
                        color = if (isActive) WarmInk.copy(alpha = 0.80f) else WarmMuted.copy(alpha = 0.32f),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
