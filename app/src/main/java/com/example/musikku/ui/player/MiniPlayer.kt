package com.example.musikku.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.musikku.player.NowPlaying
import com.example.musikku.ui.components.Artwork
import com.example.musikku.ui.theme.warmWash
import com.example.musikku.ui.theme.LocalCoverPalette
import com.example.musikku.ui.theme.LocalHazeState
import com.example.musikku.ui.theme.LocalUiPrefs
import com.example.musikku.ui.theme.WarmInk
import com.example.musikku.ui.theme.WarmMuted
import com.example.musikku.ui.theme.adaptiveSurface

@Composable
fun MiniPlayer(
    state: NowPlaying,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val song = state.song ?: return
    val prefs = LocalUiPrefs.current
    val hazeState = LocalHazeState.current
    val palette = LocalCoverPalette.current
    val accent = MaterialTheme.colorScheme.primary

    val shape = if (prefs.isGlass) RoundedCornerShape(28.dp) else RoundedCornerShape(16.dp)
    val playBtnShape = if (prefs.isGlass) CircleShape else RoundedCornerShape(12.dp)
    val heartTint by animateColorAsState(
        if (isFavorite) accent else WarmInk.copy(alpha = 0.78f),
        tween(200),
        label = "miniHeart",
    )

    Column(
        modifier = modifier
            .adaptiveSurface(
                shape = shape,
                hazeState = hazeState,
                isGlass = prefs.isGlass,
                accentGlow = accent,
                elevated = true,
                blurRadius = 24.dp,
                shadowElevation = if (prefs.isGlass) 20.dp else 6.dp,
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (compact) 8.dp else 10.dp,
                    end = if (compact) 6.dp else 8.dp,
                    top = 7.dp,
                    bottom = 6.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(
                url = song.thumbnail,
                modifier = Modifier.size(if (compact) 40.dp else 44.dp),
                shape = RoundedCornerShape(if (prefs.isGlass) 14.dp else 10.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    color = WarmInk,
                    style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = song.artistsText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmMuted,
                )
            }
            if (!compact) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isFavorite) "Hapus dari disukai" else "Suka",
                        tint = heartTint,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(2.dp))
            }
            Box(
                modifier = Modifier
                    .size(if (compact) 36.dp else 38.dp)
                    .clip(playBtnShape)
                    .background(
                        if (prefs.isGlass) {
                            Brush.linearGradient(listOf(palette.accent, palette.secondaryAccent))
                        } else {
                            Brush.linearGradient(listOf(accent, accent))
                        }
                    )
                    .border(
                        width = if (prefs.isGlass) 0.75.dp else 0.dp,
                        color = if (prefs.isGlass) warmWash(0.35f) else Color.Transparent,
                        shape = playBtnShape,
                    )
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                if (state.isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF1E0B04),
                    )
                } else {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Jeda" else "Putar",
                        tint = Color(0xFF1E0B04),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            IconButton(
                onClick = onNext,
                enabled = state.hasNext,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Berikutnya",
                    tint = if (state.hasNext) WarmInk else WarmMuted.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        // Garis progres tipis di tepi bawah mini player
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (prefs.isGlass) 18.dp else 10.dp)
                .padding(bottom = 2.dp)
                .height(2.dp)
                .clip(CircleShape)
                .background(warmWash(0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(state.progress.coerceIn(0f, 1f))
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(listOf(palette.accent, palette.secondaryAccent))
                    )
            )
        }
    }
}
