package com.vyora.music.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.vyora.music.player.NowPlaying
import com.vyora.music.ui.player.MiniPlayer
import com.vyora.music.ui.theme.warmWash
import com.vyora.music.ui.theme.EditorialBackground
import com.vyora.music.ui.theme.LocalCoverPalette
import com.vyora.music.ui.theme.LocalHazeState
import com.vyora.music.ui.theme.LocalUiPrefs
import com.vyora.music.ui.theme.WarmHairline
import com.vyora.music.ui.theme.WarmInk
import com.vyora.music.ui.theme.WarmMuted
import com.vyora.music.ui.theme.adaptiveSurface
import kotlin.math.roundToInt

data class NavTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

/**
 * Container bar bawah adaptif:
 * - Liquid Glass: MiniPlayer melayang + Navbar lensa kaca (bisa digeser & melipat saat scroll)
 * - Editorial: MiniPlayer kartu obsidian rapi + Bottom bar matte dengan indikator kapsul
 */
@Composable
fun FloatingBottomChrome(
    tabs: List<NavTab>,
    currentRoute: String?,
    now: NowPlaying,
    isFavorite: Boolean,
    collapsed: Boolean,
    onExpandNav: () -> Unit,
    onSelectTab: (String) -> Unit,
    onOpenPlayer: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val prefs = LocalUiPrefs.current
    val isGlass = prefs.isGlass
    val shouldCollapse = isGlass && prefs.collapseNavbarOnScroll && collapsed

    if (isGlass) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedContent(
                targetState = shouldCollapse,
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.94f, animationSpec = tween(240)))
                        .togetherWith(fadeOut(tween(180)) + scaleOut(targetScale = 0.95f, animationSpec = tween(180)))
                },
                label = "glassBottomChrome",
            ) { isCompactMode ->
                if (isCompactMode) {
                    val activeTab = tabs.firstOrNull { it.route == currentRoute } ?: tabs.first()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CollapsedGlassNavOrb(
                            tab = activeTab,
                            onClick = onExpandNav,
                        )
                        if (now.hasMedia) {
                            MiniPlayer(
                                state = now,
                                isFavorite = isFavorite,
                                onClick = onOpenPlayer,
                                onPlayPause = onPlayPause,
                                onNext = onNext,
                                onToggleFavorite = onToggleFavorite,
                                compact = true,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (now.hasMedia) {
                            MiniPlayer(
                                state = now,
                                isFavorite = isFavorite,
                                onClick = onOpenPlayer,
                                onPlayPause = onPlayPause,
                                onNext = onNext,
                                onToggleFavorite = onToggleFavorite,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        LiquidLensNavBar(
                            tabs = tabs,
                            currentRoute = currentRoute,
                            onSelectTab = onSelectTab,
                        )
                    }
                }
            }
        }
    } else {
        // Mode Editorial: tenang, presisi, tanpa blur berat
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            EditorialBackground.copy(alpha = 0.92f),
                            EditorialBackground,
                        )
                    )
                ),
        ) {
            if (now.hasMedia) {
                MiniPlayer(
                    state = now,
                    isFavorite = isFavorite,
                    onClick = onOpenPlayer,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onToggleFavorite = onToggleFavorite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            EditorialBottomBar(
                tabs = tabs,
                currentRoute = currentRoute,
                onSelectTab = onSelectTab,
            )
        }
    }
}

/** Tombol bulat lensa saat navbar melipat ketika pengguna menggulir ke bawah. */
@Composable
private fun CollapsedGlassNavOrb(
    tab: NavTab,
    onClick: () -> Unit,
) {
    val hazeState = LocalHazeState.current
    val accent = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .size(58.dp)
            .adaptiveSurface(
                shape = CircleShape,
                hazeState = hazeState,
                isGlass = true,
                accentGlow = accent,
                elevated = true,
                blurRadius = 24.dp,
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = tab.selectedIcon,
            contentDescription = "Buka navigasi (${tab.label})",
            tint = accent,
            modifier = Modifier.size(25.dp),
        )
    }
}

/**
 * Navbar Lensa Liquid Glass — indikator kaca bercahaya di belakang tab aktif
 * yang bisa diketuk ATAU digeser (drag) secara horizontal antar tab.
 */
@Composable
private fun LiquidLensNavBar(
    tabs: List<NavTab>,
    currentRoute: String?,
    onSelectTab: (String) -> Unit,
) {
    val hazeState = LocalHazeState.current
    val palette = LocalCoverPalette.current
    val accent = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val selectedIndex = tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(selectedIndex.toFloat()) }

    val activeFraction by animateFloatAsState(
        targetValue = if (isDragging) dragFraction else selectedIndex.toFloat(),
        animationSpec = if (isDragging) {
            spring(stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioNoBouncy)
        } else {
            spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.78f)
        },
        label = "lensSlide",
    )
    val lensScale by animateFloatAsState(
        targetValue = if (isDragging) 1.07f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "lensScale",
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .adaptiveSurface(
                shape = CircleShape,
                hazeState = hazeState,
                isGlass = true,
                accentGlow = accent,
                elevated = false,
                blurRadius = 26.dp,
                shadowElevation = 22.dp,
            )
            .padding(5.dp)
            .pointerInput(tabs, selectedIndex) {
                var lastHoverIndex = selectedIndex
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val slotW = size.width.toFloat() / tabs.size.coerceAtLeast(1)
                        val frac = ((offset.x / slotW) - 0.5f).coerceIn(0f, (tabs.size - 1).toFloat())
                        dragFraction = frac
                        lastHoverIndex = frac.roundToInt().coerceIn(0, tabs.lastIndex)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val slotW = size.width.toFloat() / tabs.size.coerceAtLeast(1)
                        val frac = ((change.position.x / slotW) - 0.5f).coerceIn(0f, (tabs.size - 1).toFloat())
                        dragFraction = frac
                        val hover = frac.roundToInt().coerceIn(0, tabs.lastIndex)
                        if (hover != lastHoverIndex) {
                            lastHoverIndex = hover
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    onDragEnd = {
                        val target = dragFraction.roundToInt().coerceIn(0, tabs.lastIndex)
                        isDragging = false
                        onSelectTab(tabs[target].route)
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                )
            },
    ) {
        val slotWidthPx = with(density) { (maxWidth / tabs.size).toPx() }
        val slotWidthDp = maxWidth / tabs.size

        // Indikator lensa kaca meluncur di belakang tab aktif
        val lensShape = CircleShape
        Box(
            modifier = Modifier
                .offset { IntOffset((activeFraction * slotWidthPx).roundToInt(), 0) }
                .width(slotWidthDp)
                .fillMaxHeight()
                .scale(lensScale)
                .clip(lensShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDragging) 0.24f else 0.18f),
                            palette.accent.copy(alpha = if (isDragging) 0.26f else 0.16f),
                            warmWash(0.08f),
                        )
                    )
                )
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                warmWash(0.45f),
                                palette.secondaryAccent.copy(alpha = 0.28f),
                                warmWash(0.12f),
                            )
                        ),
                    ),
                    lensShape,
                )
        )

        Row(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                val isHighlighted = if (isDragging) {
                    dragFraction.roundToInt().coerceIn(0, tabs.lastIndex) == index
                } else {
                    currentRoute == tab.route
                }
                val iconTint by animateColorAsState(
                    if (isHighlighted) accent else WarmInk.copy(alpha = 0.72f),
                    tween(200),
                    label = "tabIconTint",
                )
                val labelTint by animateColorAsState(
                    if (isHighlighted) WarmInk else WarmMuted,
                    tween(200),
                    label = "tabLabelTint",
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectTab(tab.route)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (isHighlighted) tab.selectedIcon else tab.icon,
                        contentDescription = tab.label,
                        tint = iconTint,
                        modifier = Modifier.size(23.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                        color = labelTint,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Bottom bar mode Editorial: datar, hangat, dengan kapsul aktif ala majalah editorial. */
@Composable
private fun EditorialBottomBar(
    tabs: List<NavTab>,
    currentRoute: String?,
    onSelectTab: (String) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(EditorialBackground)
            .navigationBarsPadding(),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(WarmHairline))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                val pillWidth by animateDpAsState(
                    if (selected) 120.dp else 56.dp,
                    spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.8f),
                    label = "editorialPill",
                )
                val bg by animateColorAsState(
                    if (selected) accent.copy(alpha = 0.20f) else Color.Transparent,
                    tween(200),
                    label = "editorialBg",
                )
                val border by animateColorAsState(
                    if (selected) accent.copy(alpha = 0.45f) else Color.Transparent,
                    tween(200),
                    label = "editorialBorder",
                )

                Row(
                    modifier = Modifier
                        .height(44.dp)
                        .width(pillWidth)
                        .clip(RoundedCornerShape(14.dp))
                        .background(bg)
                        .border(1.dp, border, RoundedCornerShape(14.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectTab(tab.route)
                        }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.icon,
                        contentDescription = tab.label,
                        tint = if (selected) accent else WarmMuted,
                        modifier = Modifier.size(22.dp),
                    )
                    if (selected) {
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
