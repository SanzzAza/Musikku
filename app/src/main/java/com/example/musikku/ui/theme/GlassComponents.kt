package com.example.musikku.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

/**
 * Permukaan adaptif Liquid Glass / Editorial:
 * - Liquid Glass: blur backdrop nyata via Haze (Android 12+) + kilau kaca + garis tepi spekular
 * - Editorial: permukaan matte obsidian hangat dengan border 1dp tegas & rapi
 */
fun Modifier.adaptiveSurface(
    shape: Shape,
    hazeState: HazeState? = null,
    isGlass: Boolean = true,
    accentGlow: Color = Color.Transparent,
    elevated: Boolean = false,
    blurRadius: Dp = 22.dp,
    shadowElevation: Dp = if (isGlass) 18.dp else 4.dp,
): Modifier = composed {
    if (isGlass) {
        // Tint & fallback mengikuti tema; di mode terang kaca jadi putih susu,
        // bukan gelap transparan yang bikin teks tak terbaca.
        val baseTint = GlassTintDark
        val tint = if (elevated) baseTint else baseTint.copy(alpha = baseTint.alpha * 0.78f)
        val baseFallback = GlassFallbackDark
        val fallback = if (elevated) {
            baseFallback.copy(alpha = 0.92f)
        } else {
            baseFallback.copy(alpha = 0.85f)
        }
        val style = remember(tint, fallback, blurRadius) {
            HazeStyle(
                backgroundColor = ObsidianBackground,
                tint = HazeTint(tint),
                blurRadius = blurRadius,
                noiseFactor = 0.06f,
                fallbackTint = HazeTint(fallback),
            )
        }
        val glassRim = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.34f),
                accentGlow.copy(alpha = if (accentGlow == Color.Transparent) 0f else 0.22f).let {
                    if (it.alpha == 0f) Color.White.copy(alpha = 0.12f) else it
                },
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.22f),
            ),
            start = Offset(0f, 0f),
            end = Offset(0f, 220f),
        )
        val sheen = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.11f),
                Color.White.copy(alpha = 0.02f),
                Color.Black.copy(alpha = 0.14f),
            )
        )
        this
            .shadow(shadowElevation, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .then(
                if (hazeState != null) {
                    Modifier.hazeChild(state = hazeState, style = style)
                } else {
                    Modifier.background(fallback, shape)
                }
            )
            .background(sheen, shape)
            .border(BorderStroke(1.dp, glassRim), shape)
    } else {
        val fill = if (elevated) WarmSurfaceElevated else WarmSurface
        this
            .shadow(shadowElevation, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(fill, shape)
            .border(BorderStroke(1.dp, WarmHairline), shape)
    }
}

/**
 * Latar belakang utama aplikasi dengan kilau ambient halus mengikuti warna sampul
 * pada mode Liquid Glass, atau matte obsidian hangat tenang pada mode Editorial.
 */
@Composable
fun AmbientBackdrop(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    content: @Composable BoxScope.() -> Unit,
) {
    val prefs = LocalUiPrefs.current
    val palette = LocalCoverPalette.current
    val bg = MaterialTheme.colorScheme.background
    // Di latar terang, semburat warna harus jauh lebih tipis agar tidak jadi kusam.
    val glow = if (WarmTheme.isLight) 0.26f else 1f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
            .drawBehind {
                if (prefs.isGlass) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.ambient.copy(alpha = 0.62f * intensity * glow),
                                palette.accent.copy(alpha = 0.14f * intensity * glow),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.24f, size.height * 0.06f),
                            radius = size.maxDimension * 0.72f,
                        )
                    )
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.secondaryAccent.copy(alpha = 0.12f * intensity * glow),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.88f, size.height * 0.34f),
                            radius = size.maxDimension * 0.50f,
                        )
                    )
                } else {
                    // Editorial: semburat hangat sangat tipis agar tetap tenang di mata
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                palette.ambient.copy(alpha = 0.20f * intensity * glow),
                                Color.Transparent,
                            ),
                            startY = 0f,
                            endY = size.height * 0.38f,
                        )
                    )
                }
            },
        content = content,
    )
}

/**
 * Saklar (Toggle) bergaya iOS 26 / Liquid Glass dengan lintasan kaca bening,
 * tepi spekular, dan tombol meluncur pegas.
 */
@Composable
fun LiquidGlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val prefs = LocalUiPrefs.current
    val palette = LocalCoverPalette.current
    val haptic = LocalHapticFeedback.current

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 3.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "switchOffset",
    )
    val trackColor by animateColorAsState(
        targetValue = when {
            checked -> palette.accent
            // warmWash otomatis jadi hitam tipis di mode terang, supaya lintasan
            // saklar tetap kelihatan (dulu putih transparan di atas latar terang).
            prefs.isGlass -> warmWash(0.16f)
            else -> WarmSurfaceElevated
        },
        animationSpec = tween(240),
        label = "switchTrack",
    )
    val borderBrush = if (prefs.isGlass) {
        Brush.verticalGradient(
            listOf(
                warmWash(if (checked) 0.45f else 0.34f),
                warmWash(0.10f),
            )
        )
    } else {
        Brush.verticalGradient(listOf(WarmHairline, WarmHairline))
    }

    Box(
        modifier = modifier
            .width(54.dp)
            .height(31.dp)
            .clip(CircleShape)
            .background(
                if (checked && prefs.isGlass) {
                    Brush.horizontalGradient(listOf(palette.accent, palette.secondaryAccent))
                } else {
                    Brush.horizontalGradient(listOf(trackColor, trackColor))
                }
            )
            .border(1.dp, borderBrush, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(25.dp)
                .shadow(if (prefs.isGlass) 6.dp else 2.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    if (prefs.isGlass) {
                        Brush.verticalGradient(listOf(Color.White, Color(0xFFEBE4DC)))
                    } else {
                        Brush.verticalGradient(listOf(WarmInk, WarmInk))
                    }
                )
                .border(1.dp, warmWash(0.18f), CircleShape)
                .border(0.75.dp, Color.White.copy(alpha = 0.7f), CircleShape)
        )
    }
}

/** Pill chip filter (mood di Beranda, filter di Pustaka & Cari). */
@Composable
fun GlassChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val prefs = LocalUiPrefs.current
    val accent = MaterialTheme.colorScheme.primary
    val shape = if (prefs.isGlass) CircleShape else RoundedCornerShape(10.dp)
    val bg by animateColorAsState(
        targetValue = when {
            selected -> accent.copy(alpha = if (prefs.isGlass) 0.28f else 0.22f)
            prefs.isGlass -> Color.White.copy(alpha = 0.07f)
            else -> WarmSurface
        },
        animationSpec = tween(220),
        label = "chipBg",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> accent.copy(alpha = 0.75f)
            prefs.isGlass -> Color.White.copy(alpha = 0.20f)
            else -> WarmHairline
        },
        animationSpec = tween(220),
        label = "chipBorder",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) WarmInk else WarmMuted,
        animationSpec = tween(220),
        label = "chipText",
    )

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        color = bg,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

/** Tombol ikon bulat bergaya kaca/editorial untuk bar atas dan kontrol overlay. */
@Composable
fun GlassCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = WarmInk,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    filled: Boolean = false,
) {
    val prefs = LocalUiPrefs.current
    val shape = if (prefs.isGlass) CircleShape else RoundedCornerShape(12.dp)
    val bg = when {
        filled -> MaterialTheme.colorScheme.primary
        prefs.isGlass -> Color.White.copy(alpha = 0.08f)
        else -> WarmSurface
    }
    val border = when {
        filled -> Color.White.copy(alpha = 0.28f)
        prefs.isGlass -> Color.White.copy(alpha = 0.16f)
        else -> WarmHairline
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (filled) Color(0xFF1E0B04) else tint,
            modifier = Modifier.size(iconSize),
        )
    }
}
