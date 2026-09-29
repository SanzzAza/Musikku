package com.example.musikku.ui.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musikku.data.lyrics.LyricLine
import com.example.musikku.data.lyrics.Lyrics
import com.example.musikku.player.LyricsUiState

val LyricsCardColor = Color(0xFF4A3F6B)

private val LyricsTextStyle = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
private val PreviewTextStyle = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)

/** Index baris yang sedang dinyanyikan. */
private fun currentLineIndex(lines: List<LyricLine>, positionMs: Long): Int =
    lines.indexOfLast { it.timeMs <= positionMs + 250 }

/** Kartu lirik kecil di bawah pemutar (ala Spotify). */
@Composable
fun LyricsPreviewCard(
    state: LyricsUiState,
    positionMs: Long,
    onExpand: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lyrics = (state as? LyricsUiState.Loaded)?.lyrics
    val expandable = lyrics is Lyrics.Synced || lyrics is Lyrics.Plain

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LyricsCardColor)
            .clickable(enabled = expandable, onClick = onExpand)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Lirik", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (expandable) Icon(Icons.Default.OpenInFull, "Buka lirik", Modifier.padding(4.dp))
        }
        Spacer(Modifier.height(12.dp))

        when (state) {
            LyricsUiState.Idle, LyricsUiState.Loading -> Box(
                Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp) }

            LyricsUiState.Error -> Column {
                Text("Gagal memuat lirik.", color = Color.White.copy(alpha = 0.8f))
                TextButton(onClick = onRetry) { Text("Coba lagi", color = Color.White) }
            }

            is LyricsUiState.Loaded -> when (val l = state.lyrics) {
                is Lyrics.Synced -> {
                    val current = currentLineIndex(l.lines, positionMs)
                    val start = current.coerceAtLeast(0)
                    Column(Modifier.heightIn(min = 120.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        l.lines.drop(start).take(4).forEachIndexed { offset, line ->
                            val active = start + offset == current
                            Text(
                                line.text.ifBlank { "♪" },
                                style = PreviewTextStyle,
                                color = if (active) Color.White else Color.Black.copy(alpha = 0.55f)
                            )
                        }
                    }
                }
                is Lyrics.Plain -> Text(
                    l.text.lineSequence().take(6).joinToString("\n"),
                    style = PreviewTextStyle,
                    color = Color.White
                )
                Lyrics.Instrumental -> Text("🎵 Lagu instrumental", color = Color.White.copy(alpha = 0.8f))
                Lyrics.NotFound -> Text("Lirik belum tersedia untuk lagu ini.", color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

/** Lirik layar penuh: auto-scroll & highlight baris aktif, ketuk baris untuk lompat ke bagian itu. */
@Composable
fun FullLyrics(
    lyrics: Lyrics,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (lyrics) {
        is Lyrics.Synced -> SyncedLyricsList(lyrics.lines, positionMs, onSeek, modifier)
        is Lyrics.Plain -> Text(
            lyrics.text,
            style = LyricsTextStyle,
            color = Color.White,
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        )
        else -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Lirik belum tersedia", color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun SyncedLyricsList(
    lines: List<LyricLine>,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = remember(positionMs, lines) { currentLineIndex(lines, positionMs) }
    val listState = rememberLazyListState()

    // Auto-scroll: baris aktif selalu berada di sepertiga atas layar
    LaunchedEffect(current) {
        if (current >= 0 && !listState.isScrollInProgress) {
            listState.animateScrollToItem((current - 2).coerceAtLeast(0))
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 240.dp)
    ) {
        itemsIndexed(lines) { i, line ->
            val color by animateColorAsState(
                targetValue = when {
                    i == current -> Color.White
                    i < current -> Color.White.copy(alpha = 0.6f)
                    else -> Color.Black.copy(alpha = 0.55f)
                },
                animationSpec = tween(300),
                label = "lyricColor"
            )
            Text(
                line.text.ifBlank { "♪" },
                style = LyricsTextStyle,
                color = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSeek(line.timeMs) }
                    .padding(vertical = 8.dp)
            )
        }
    }
}
