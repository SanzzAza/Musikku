package com.vyora.music.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DataSaverOn
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vyora.music.AppModule
import com.vyora.music.R
import com.vyora.music.data.local.AppFontStyle
import com.vyora.music.data.local.StreamCacheLimit
import com.vyora.music.data.ytmusic.AudioQuality
import com.vyora.music.data.local.ThemeMode
import com.vyora.music.data.local.UiPreferences
import com.vyora.music.data.local.UiStyle
import com.vyora.music.data.offline.StreamCacheHolder
import com.vyora.music.data.updater.AppUpdater
import com.vyora.music.data.updater.ReleaseUpdateInfo
import com.vyora.music.ui.theme.JetBrainsMonoFamily
import com.vyora.music.ui.theme.PlayfairFamily
import com.vyora.music.ui.theme.PlusJakartaFamily
import com.vyora.music.ui.theme.warmWash
import kotlinx.coroutines.launch
import java.util.Locale
import com.vyora.music.ui.theme.EditorialBackground
import com.vyora.music.ui.theme.LiquidGlassSwitch
import com.vyora.music.ui.theme.LocalCoverPalette
import com.vyora.music.ui.theme.ObsidianBackground
import com.vyora.music.ui.theme.WarmFaint
import com.vyora.music.ui.theme.WarmHairline
import com.vyora.music.ui.theme.WarmInk
import com.vyora.music.ui.theme.WarmMuted
import com.vyora.music.ui.theme.WarmSurface
import com.vyora.music.ui.theme.WarmSurfaceElevated

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSheet(
    prefs: UiPreferences,
    onDismiss: () -> Unit,
    onOpenAudioFx: () -> Unit = {},
    onOpenDiscord: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = MaterialTheme.colorScheme.primary
    val discordPrefs by AppModule.discordPrefs.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appVersion = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.1"
    }
    var cacheUsedBytes by remember { mutableLongStateOf(StreamCacheHolder.usedBytes(context)) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<Result<ReleaseUpdateInfo>?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WarmSurface,
        dragHandle = {
            Box(
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
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp),
        ) {
            Text(
                text = "TAMPILAN & GAYA",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "Dua tampilan, ",
                    style = MaterialTheme.typography.headlineMedium,
                    color = WarmInk,
                )
                Text(
                    text = "satu VYORA.",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = accent,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Pilih gaya antarmuka favoritmu — bisa diganti kapan saja.",
                style = MaterialTheme.typography.bodyMedium,
                color = WarmMuted,
            )

            Spacer(Modifier.height(18.dp))

            // Kartu pilihan gaya visual berdampingan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StylePreviewCard(
                    style = UiStyle.LIQUID_GLASS,
                    selected = prefs.style == UiStyle.LIQUID_GLASS,
                    onClick = { AppModule.uiPrefs.setStyle(UiStyle.LIQUID_GLASS) },
                    modifier = Modifier.weight(1f),
                )
                StylePreviewCard(
                    style = UiStyle.EDITORIAL,
                    selected = prefs.style == UiStyle.EDITORIAL,
                    onClick = { AppModule.uiPrefs.setStyle(UiStyle.EDITORIAL) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(22.dp))
            SectionEyebrow("MODE TEMA")
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeMode.values().forEach { mode ->
                    ThemeModeChip(
                        mode = mode,
                        selected = prefs.themeMode == mode,
                        onClick = { AppModule.uiPrefs.setThemeMode(mode) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            SectionEyebrow("FITUR VISUAL")
            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(WarmSurface)
                    .border(1.dp, WarmHairline, RoundedCornerShape(20.dp))
            ) {
                SettingToggleRow(
                    icon = Icons.Rounded.Palette,
                    title = "Tema ikut cover",
                    subtitle = "Warna aksen & kilau latar otomatis mengikuti sampul lagu aktif",
                    checked = prefs.dynamicCoverTheme,
                    onCheckedChange = { AppModule.uiPrefs.setDynamicCoverTheme(it) },
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(1.dp)
                        .background(WarmHairline)
                )
                SettingToggleRow(
                    icon = Icons.Rounded.ViewAgenda,
                    title = "Navbar melipat saat scroll",
                    subtitle = "Navbar kaca melipat jadi lingkaran dengan mini player di sebelahnya",
                    checked = prefs.collapseNavbarOnScroll,
                    onCheckedChange = { AppModule.uiPrefs.setCollapseNavbarOnScroll(it) },
                    enabled = prefs.isGlass,
                )
            }

            Spacer(Modifier.height(22.dp))
            SectionEyebrow("PEMUTARAN")
            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(WarmSurface)
                    .border(1.dp, WarmHairline, RoundedCornerShape(20.dp))
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "Kualitas audio",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = WarmInk,
                    )
                    Text(
                        text = "Bitrate stream yang diambil dari YouTube Music",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmMuted,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AudioQuality.entries.forEach { quality ->
                            val selected = prefs.audioQuality == quality
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (selected) accent.copy(alpha = 0.20f) else WarmSurfaceElevated
                                    )
                                    .border(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) accent else WarmHairline,
                                        shape = RoundedCornerShape(14.dp),
                                    )
                                    .clickable { AppModule.uiPrefs.setAudioQuality(quality) }
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    text = quality.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) accent else WarmInk,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = quality.hint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WarmMuted,
                                    lineHeight = 15.sp,
                                )
                            }
                        }
                    }
                }

                SectionDivider()

                SettingToggleRow(
                    icon = Icons.Rounded.DataSaverOn,
                    title = "Hemat data di seluler",
                    subtitle = "Pakai kualitas terendah saat memakai data seluler; Wi-Fi tetap memakai pilihan di atas",
                    checked = prefs.dataSaverOnMobile,
                    onCheckedChange = { AppModule.uiPrefs.setDataSaverOnMobile(it) },
                )

                SectionDivider()

                SettingToggleRow(
                    icon = Icons.Rounded.FastForward,
                    title = "Lewati bagian hening",
                    subtitle = "Melompati bagian tanpa suara di awal dan akhir lagu",
                    checked = prefs.skipSilence,
                    onCheckedChange = { AppModule.uiPrefs.setSkipSilence(it) },
                )

                SectionDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenAudioFx)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Equalizer, null, tint = accent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Equalizer & Studio Audio VYORA",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = WarmInk,
                        )
                        Text(
                            text = "Slowed + Reverb, Nightcore, AutoEQ TWS, Bass Boost & Pitch",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmMuted,
                        )
                    }
                }

                SectionDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenDiscord)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Headphones,
                        null,
                        tint = Color(0xFF5865F2),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Discord Rich Presence",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = WarmInk,
                        )
                        Text(
                            text = if (discordPrefs.isLoggedIn) {
                                val handle = discordPrefs.username.takeIf { it.isNotBlank() }?.let { "@$it • " }.orEmpty()
                                "${handle}${if (discordPrefs.enabled) discordPrefs.listeningMode.label else "Nonaktif"}"
                            } else {
                                "Tampilkan \"Listening to VYORA\", sampul & durasi jalan di profil Discord"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (discordPrefs.isActive) accent else WarmMuted,
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            SectionEyebrow("PENYIMPANAN & AUTO-CACHE OFFLINE")
            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(WarmSurface)
                    .border(1.dp, WarmHairline, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Storage, null, tint = accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Batas Auto-Cache Lagu",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = WarmInk,
                        )
                        val usedMb = cacheUsedBytes / (1024.0 * 1024.0)
                        Text(
                            text = "Terpakai ${String.format(Locale.US, "%.1f MB", usedMb)} • Putar ulang hemat kuota",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmMuted,
                        )
                    }
                    if (cacheUsedBytes > 0L) {
                        TextButton(
                            onClick = {
                                StreamCacheHolder.clear(context)
                                cacheUsedBytes = StreamCacheHolder.usedBytes(context)
                                Toast.makeText(context, "Cache stream dibersihkan", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Bersihkan", color = accent)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StreamCacheLimit.entries.forEach { limit ->
                        val selected = prefs.streamCacheLimit == limit
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) accent.copy(alpha = 0.20f) else WarmSurfaceElevated)
                                .border(1.dp, if (selected) accent else WarmHairline, RoundedCornerShape(12.dp))
                                .clickable { AppModule.uiPrefs.setStreamCacheLimit(limit) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = limit.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) accent else WarmInk,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            SectionEyebrow("GAYA HURUF")
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AppFontStyle.entries.forEach { font ->
                    val selected = prefs.fontStyle == font
                    val family = when (font) {
                        AppFontStyle.MODERN -> PlusJakartaFamily
                        AppFontStyle.EDITORIAL, AppFontStyle.CLASSIC_SERIF -> PlayfairFamily
                        AppFontStyle.MONO_CLEAN -> JetBrainsMonoFamily
                    }
                    Surface(
                        onClick = { AppModule.uiPrefs.setFontStyle(font) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) accent.copy(alpha = 0.22f) else WarmSurface,
                        border = BorderStroke(1.dp, if (selected) accent else WarmHairline),
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                text = "Aa",
                                fontFamily = family,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) accent else WarmInk,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = font.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = WarmInk,
                            )
                            Text(
                                text = font.preview,
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmMuted,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            SectionEyebrow("TENTANG")
            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(WarmSurface)
                    .border(1.dp, WarmHairline, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.vyora_logo),
                        contentDescription = "Logo VYORA",
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "VYORA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WarmInk,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "v$appVersion",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                    )
                }
                Text(
                    text = "Pemutar musik mode tamu (tanpa perlu login) — dilengkapi Karaoke Per-Kata, Terjemahan ID, Unduhan Offline, Video Klip, Equalizer Studio, VYORA Recap, dan Widget Layar Utama.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmMuted,
                )
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {
                        if (!checkingUpdate) {
                            checkingUpdate = true
                            scope.launch {
                                updateResult = AppUpdater.checkLatest(appVersion)
                                checkingUpdate = false
                            }
                        }
                    },
                    border = BorderStroke(1.dp, WarmHairline),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (checkingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = accent,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Memeriksa GitHub Releases…", color = WarmInk)
                    } else {
                        Icon(Icons.Rounded.SystemUpdate, null, tint = accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cek Pembaruan VYORA", color = WarmInk, fontWeight = FontWeight.SemiBold)
                    }
                }

                updateResult?.let { res ->
                    res.onSuccess { rel ->
                        if (!rel.isNewer) {
                            Text(
                                text = "✓ Kamu sudah memakai versi terbaru (v$appVersion).",
                                style = MaterialTheme.typography.bodySmall,
                                color = accent,
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(accent.copy(alpha = 0.14f))
                                    .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "Tersedia pembaruan: ${rel.releaseTitle}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = accent,
                                )
                                if (rel.changelog.isNotBlank()) {
                                    Text(
                                        text = rel.changelog.take(260),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WarmInk,
                                    )
                                }
                                Button(
                                    onClick = {
                                        val url = rel.apkDownloadUrl.ifBlank { rel.htmlUrl }
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accent,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Unduh APK Terbaru", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }.onFailure { err ->
                        Text(
                            text = "Gagal memeriksa pembaruan: ${err.message ?: "Cek koneksi internet"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Dibangun dengan Jetpack Compose, Media3/ExoPlayer, dan NewPipeExtractor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmFaint,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Dibuat untuk dipakai sendiri. Tidak berafiliasi dengan YouTube, Spotify, atau Apple Music.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmFaint,
                )
            }
        }
    }
}

@Composable
private fun SectionDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(WarmHairline)
    )
}

@Composable
private fun ThemeModeChip(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accent.copy(alpha = 0.16f) else WarmSurface)
            .border(
                1.dp,
                if (selected) accent else WarmHairline,
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = mode.label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) accent else WarmInk,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = mode.hint,
            style = MaterialTheme.typography.bodySmall,
            color = WarmFaint,
            maxLines = 1,
        )
    }
}

@Composable
private fun SectionEyebrow(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = WarmFaint,
    )
}

@Composable
private fun StylePreviewCard(
    style: UiStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val palette = LocalCoverPalette.current
    val borderColor by animateColorAsState(
        if (selected) accent else WarmHairline,
        tween(220),
        label = "styleBorder",
    )
    val isGlassCard = style == UiStyle.LIQUID_GLASS

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) accent.copy(alpha = 0.12f) else WarmSurface)
            .border(if (selected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        // Mini pratinjau layar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isGlassCard) {
                        Brush.radialGradient(
                            listOf(
                                palette.ambient.copy(alpha = 0.85f),
                                ObsidianBackground,
                            )
                        )
                    } else {
                        Brush.verticalGradient(listOf(EditorialBackground, Color(0xFF0E0D0C)))
                    }
                )
                .border(1.dp, WarmHairline, RoundedCornerShape(14.dp))
                .padding(8.dp),
        ) {
            // Grid mini tiles di atas
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                repeat(3) { idx ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(if (isGlassCard) 8.dp else 5.dp))
                            .background(
                                when (idx) {
                                    0 -> palette.accent.copy(alpha = 0.45f)
                                    1 -> palette.secondaryAccent.copy(alpha = 0.35f)
                                    else -> warmWash(0.12f)
                                }
                            )
                    )
                }
            }

            // Mini player + navbar preview di bawah
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(if (isGlassCard) CircleShape else RoundedCornerShape(6.dp))
                        .background(
                            if (isGlassCard) warmWash(0.16f) else WarmSurfaceElevated
                        )
                        .border(
                            0.75.dp,
                            if (isGlassCard) warmWash(0.35f) else WarmHairline,
                            if (isGlassCard) CircleShape else RoundedCornerShape(6.dp),
                        )
                        .padding(horizontal = 5.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(if (isGlassCard) CircleShape else RoundedCornerShape(3.dp))
                            .background(accent)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(if (isGlassCard) CircleShape else RoundedCornerShape(5.dp))
                        .background(
                            if (isGlassCard) warmWash(0.12f) else Color(0xFF181513)
                        )
                        .border(
                            0.75.dp,
                            if (isGlassCard) warmWash(0.30f) else WarmHairline,
                            if (isGlassCard) CircleShape else RoundedCornerShape(5.dp),
                        )
                        .padding(2.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(
                        Modifier
                            .width(34.dp)
                            .height(12.dp)
                            .clip(if (isGlassCard) CircleShape else RoundedCornerShape(3.dp))
                            .background(accent.copy(alpha = if (isGlassCard) 0.42f else 0.32f))
                    )
                }
            }

            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Check, null, tint = Color(0xFF140826), modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (isGlassCard) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(15.dp))
            }
            Text(
                text = style.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WarmInk,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = style.tagline,
            style = MaterialTheme.typography.bodySmall,
            color = WarmMuted,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = if (enabled) 0.16f else 0.07f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) accent else WarmFaint,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) WarmInk else WarmFaint,
            )
            Text(
                text = if (enabled) subtitle else "Tersedia saat mode Liquid Glass aktif",
                style = MaterialTheme.typography.bodySmall,
                color = WarmMuted.copy(alpha = if (enabled) 1f else 0.6f),
            )
        }
        Spacer(Modifier.width(10.dp))
        LiquidGlassSwitch(
            checked = checked && enabled,
            onCheckedChange = { if (enabled) onCheckedChange(it) },
        )
    }
}
