package com.example.musikku.ui.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musikku.data.lyrics.LyricLine
import com.example.musikku.data.lyrics.Lyrics
import com.example.musikku.player.LyricsUiState
import com.example.musikku.ui.theme.WarmInk
import com.example.musikku.ui.theme.WarmMuted

private val EditorialLyricStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 26.sp,
    lineHeight = 36.sp,
    fontWeight = FontWeight.Medium,
    textAlign = TextAlign.Center,
)

private val PlainLyricStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 21.sp,
    lineHeight = 32.sp,
    fontWeight = FontWeight.Normal,
    textAlign = TextAlign.Center,
)

/** Index baris yang sedang dinyanyikan. */
private fun currentLineIndex(lines: List<LyricLine>, positionMs: Long): Int =
    lines.indexOfLast { it.timeMs <= positionMs + 250 }

/**
 * Tampilan Lirik Editorial bergaya Auralys:
 * Tipografi serif rata tengah dengan sorotan baris aktif terang,
 * transisi fade vertikal atas-bawah, dan ketuk baris untuk melompat ke detik tersebut.
 */
@Composable
fun EditorialLyricsStage(
    state: LyricsUiState,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    onRetry: () -> Unit,
    fadeColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        when (state) {
            LyricsUiState.Idle, LyricsUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp,
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Memuat lirik…",
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
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
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
                    positionMs = positionMs,
                    onSeek = onSeek,
                    modifier = Modifier.fillMaxSize(),
                )
                is Lyrics.Plain -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 40.dp),
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
                .height(44.dp)
                .background(Brush.verticalGradient(listOf(fadeColor, Color.Transparent)))
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(44.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, fadeColor)))
        )
    }
}

@Composable
private fun SyncedEditorialLyricsList(
    lines: List<LyricLine>,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = remember(positionMs, lines) { currentLineIndex(lines, positionMs) }
    val listState = rememberLazyListState()

    LaunchedEffect(current) {
        if (current >= 0 && !listState.isScrollInProgress) {
            listState.animateScrollToItem((current - 1).coerceAtLeast(0))
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 56.dp),
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
                animationSpec = tween(320),
                label = "editorialLyricColor",
            )
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.03f else 0.96f,
                animationSpec = tween(320),
                label = "editorialLyricScale",
            )

            Text(
                text = line.text.ifBlank { "♪" },
                style = EditorialLyricStyle,
                color = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSeek(line.timeMs) }
                    .padding(vertical = 6.dp, horizontal = 8.dp),
            )
        }
    }
}
