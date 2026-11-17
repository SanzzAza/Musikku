package com.example.musikku.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.musikku.ui.theme.Radius
import com.example.musikku.ui.theme.Space
import com.example.musikku.ui.theme.WarmFaint
import com.example.musikku.ui.theme.WarmInk
import com.example.musikku.ui.theme.WarmMuted
import com.example.musikku.ui.theme.warmWash

/**
 * Keadaan kosong yang seragam untuk seluruh aplikasi.
 *
 * Sebelumnya tiap layar menulis pesan kosong sendiri-sendiri (teks saja,
 * tanpa ikon, tanpa aksi), sehingga terasa tidak konsisten.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier.padding(horizontal = Space.huge, vertical = Space.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(warmWash(0.07f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent.copy(alpha = 0.85f),
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(Space.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = WarmInk,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = WarmMuted,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Space.lg))
            Button(onClick = onAction, shape = RoundedCornerShape(Radius.md)) {
                Text(actionLabel)
            }
        }
    }
}

/** Sapuan kilau untuk placeholder pemuatan. */
@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-shift",
    )
    val base = warmWash(0.06f)
    val highlight = warmWash(0.14f)
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(shift * 520f, 0f),
        end = Offset(shift * 520f + 420f, 380f),
    )
}

@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Radius.sm),
) {
    Box(modifier.clip(shape).background(shimmerBrush()))
}

/**
 * Daftar baris tiruan (sampul + dua baris teks) untuk ditampilkan saat data
 * pertama kali dimuat, bukan spinner kosong.
 */
@Composable
fun SkeletonRowList(
    rows: Int = 7,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = Space.lg, vertical = Space.sm),
        verticalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        repeat(rows) { index ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonBlock(
                    modifier = Modifier.size(50.dp),
                    shape = RoundedCornerShape(Radius.sm),
                )
                Spacer(Modifier.size(Space.md))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Space.xs),
                ) {
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth(if (index % 3 == 0) 0.62f else 0.78f)
                            .height(12.dp),
                    )
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth(if (index % 2 == 0) 0.32f else 0.44f)
                            .height(10.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(Space.xs))
        Text(
            text = "Memuat…",
            style = MaterialTheme.typography.labelSmall,
            color = WarmFaint,
            modifier = Modifier.padding(start = Space.xxs),
        )
    }
}
