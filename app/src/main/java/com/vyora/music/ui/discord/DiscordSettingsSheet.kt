package com.vyora.music.ui.discord

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vyora.music.AppModule
import com.vyora.music.R
import com.vyora.music.data.discord.DiscordConnectionStatus
import com.vyora.music.data.discord.DiscordListeningMode
import com.vyora.music.player.NowPlaying
import com.vyora.music.ui.components.Artwork
import com.vyora.music.ui.components.formatMs
import com.vyora.music.ui.theme.LiquidGlassSwitch
import com.vyora.music.ui.theme.WarmFaint
import com.vyora.music.ui.theme.WarmHairline
import com.vyora.music.ui.theme.WarmInk
import com.vyora.music.ui.theme.WarmMuted
import com.vyora.music.ui.theme.WarmSurface
import com.vyora.music.ui.theme.WarmSurfaceElevated
import com.vyora.music.ui.theme.warmWash
import kotlinx.coroutines.launch

private val DiscordBlurple = Color(0xFF5865F2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordSettingsSheet(
    now: NowPlaying,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val prefs by AppModule.discordPrefs.state.collectAsStateWithLifecycle()
    val connStatus by AppModule.discordRpc.status.collectAsStateWithLifecycle()
    val accent = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showWebLogin by remember { mutableStateOf(false) }
    var showTokenDialog by remember { mutableStateOf(false) }
    var verifying by remember { mutableStateOf(false) }

    val handleCapturedToken: (String) -> Unit = { rawToken ->
        if (!verifying) {
            verifying = true
            scope.launch {
                val res = AppModule.discordRpc.verifyAndFetchProfile(rawToken)
                verifying = false
                res.onSuccess {
                    if (now.song != null && now.isPlaying) {
                        AppModule.discordRpc.updateNowPlaying(
                            song = now.song,
                            isPlaying = true,
                            positionMs = now.positionMs,
                            durationMs = now.durationMs,
                        )
                    }
                    Toast.makeText(context, "Discord berhasil terhubung ke VYORA!", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    Toast.makeText(
                        context,
                        err.message ?: "Gagal menghubungkan akun Discord",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    if (showWebLogin) {
        DiscordWebLoginDialog(
            onTokenExtracted = { token ->
                showWebLogin = false
                handleCapturedToken(token)
            },
            onDismiss = { showWebLogin = false },
        )
    }

    if (showTokenDialog) {
        ManualTokenDialog(
            onConfirm = { token ->
                showTokenDialog = false
                handleCapturedToken(token)
            },
            onDismiss = { showTokenDialog = false },
        )
    }

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
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = "DISCORD RICH PRESENCE",
                style = MaterialTheme.typography.labelSmall,
                color = DiscordBlurple,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Status Listening di Discord",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                color = WarmInk,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tampilkan lagu, sampul album, dan durasi berjalan di profil Discord saat kamu memutar musik di VYORA.",
                style = MaterialTheme.typography.bodySmall,
                color = WarmMuted,
            )

            Spacer(Modifier.height(16.dp))

            // Pratinjau Kartu Profil Discord (persis seperti di profil Discord asli!)
            DiscordProfilePreviewCard(
                now = now,
                listeningMode = prefs.listeningMode,
                showVyoraBadge = prefs.showVyoraBadge,
            )

            Spacer(Modifier.height(16.dp))

            // Kartu Status Akun Discord
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(WarmSurfaceElevated)
                    .border(1.dp, WarmHairline, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!prefs.isLoggedIn) {
                    Text(
                        text = "Hubungkan Akun Discord",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WarmInk,
                    )
                    Text(
                        text = "Pemutar musik VYORA tetap 100% Guest tanpa akun YouTube. Login Discord di bawah ini khusus agar Discord mengizinkan VYORA memperbarui kartu status profilmu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmMuted,
                    )
                    if (verifying) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = DiscordBlurple,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Memverifikasi sesi Discord…", color = WarmInk)
                        }
                    } else {
                        Button(
                            onClick = { showWebLogin = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DiscordBlurple,
                                contentColor = Color.White,
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.Link, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Login Discord di Aplikasi", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { showTokenDialog = true },
                            border = BorderStroke(1.dp, WarmHairline),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.Key, null, tint = WarmInk, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Masukkan Token Manual", color = WarmInk)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (prefs.avatarUrl.isNotBlank()) {
                            Artwork(
                                url = prefs.avatarUrl,
                                modifier = Modifier.size(46.dp),
                                shape = CircleShape,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(DiscordBlurple.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.Headphones, null, tint = DiscordBlurple)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = prefs.displayName.ifBlank { "Akun Discord Terhubung" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = WarmInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (prefs.username.isNotBlank()) {
                                Text(
                                    text = "@${prefs.username}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WarmMuted,
                                )
                            }
                            val statusColor = when (connStatus) {
                                DiscordConnectionStatus.BROADCASTING,
                                DiscordConnectionStatus.CONNECTED -> Color(0xFF3BA55D)
                                DiscordConnectionStatus.CONNECTING -> accent
                                DiscordConnectionStatus.INVALID_TOKEN,
                                DiscordConnectionStatus.ERROR -> MaterialTheme.colorScheme.error
                                DiscordConnectionStatus.DISCONNECTED -> WarmFaint
                            }
                            Text(
                                text = "● ${connStatus.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                            )
                        }
                        IconButton(
                            onClick = {
                                AppModule.discordRpc.disconnect()
                                AppModule.discordPrefs.logout()
                                CookieManager.getInstance().removeAllCookies(null)
                            }
                        ) {
                            Icon(
                                Icons.Rounded.Logout,
                                contentDescription = "Putuskan Discord",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    Box(Modifier.fillMaxWidth().height(1.dp).background(WarmHairline))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Tampilkan Status Mendengarkan",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = WarmInk,
                            )
                            Text(
                                text = "Otomatis aktif saat play lagu & hilang saat di-pause",
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmMuted,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        LiquidGlassSwitch(
                            checked = prefs.enabled,
                            onCheckedChange = { enabled ->
                                AppModule.discordPrefs.setEnabled(enabled)
                                if (!enabled) {
                                    AppModule.discordRpc.disconnect()
                                } else if (now.song != null && now.isPlaying) {
                                    AppModule.discordRpc.updateNowPlaying(
                                        song = now.song,
                                        isPlaying = true,
                                        positionMs = now.positionMs,
                                        durationMs = now.durationMs,
                                    )
                                }
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("JUDUL KARTU DI DISCORD", style = MaterialTheme.typography.labelSmall, color = WarmFaint)
            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DiscordListeningMode.entries.forEach { mode ->
                    val selected = prefs.listeningMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) accent.copy(alpha = 0.16f) else WarmSurfaceElevated)
                            .border(1.dp, if (selected) accent else WarmHairline, RoundedCornerShape(14.dp))
                            .clickable {
                                AppModule.discordPrefs.setListeningMode(mode)
                                if (prefs.isActive && now.song != null && now.isPlaying) {
                                    AppModule.discordRpc.updateNowPlaying(
                                        song = now.song,
                                        isPlaying = true,
                                        positionMs = now.positionMs,
                                        durationMs = now.durationMs,
                                    )
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (selected) accent else WarmFaint,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = WarmInk,
                            )
                            Text(
                                text = mode.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmMuted,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Opsi Badge Logo VYORA di Sampul Album
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(WarmSurfaceElevated)
                    .border(1.dp, WarmHairline, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Badge Logo VYORA di Sampul",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = WarmInk,
                    )
                    Text(
                        text = "Tampilkan lencana bundar logo VYORA di pojok kanan bawah sampul album",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmMuted,
                    )
                }
                Spacer(Modifier.width(8.dp))
                LiquidGlassSwitch(
                    checked = prefs.showVyoraBadge,
                    onCheckedChange = { show ->
                        AppModule.discordPrefs.setShowVyoraBadge(show)
                        if (prefs.isActive && now.song != null && now.isPlaying) {
                            AppModule.discordRpc.updateNowPlaying(
                                song = now.song,
                                isPlaying = true,
                                positionMs = now.positionMs,
                                durationMs = now.durationMs,
                            )
                        }
                    },
                )
            }

            Spacer(Modifier.height(10.dp))

            // Opsi Tombol Dengarkan Lagu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(WarmSurfaceElevated)
                    .border(1.dp, WarmHairline, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Tombol \"Dengarkan Lagu\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = WarmInk,
                    )
                    Text(
                        text = "Teman di Discord bisa mengeklik lagu yang sedang kamu putar",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmMuted,
                    )
                }
                Spacer(Modifier.width(8.dp))
                LiquidGlassSwitch(
                    checked = prefs.showListenButton,
                    onCheckedChange = { show ->
                        AppModule.discordPrefs.setShowListenButton(show)
                        if (prefs.isActive && now.song != null && now.isPlaying) {
                            AppModule.discordRpc.updateNowPlaying(
                                song = now.song,
                                isPlaying = true,
                                positionMs = now.positionMs,
                                durationMs = now.durationMs,
                            )
                        }
                    },
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Catatan: Pastikan status akun Discord kamu Online / Idle / Do Not Disturb (bukan Invisible) dan opsi \"Share your detected activities with others\" di Activity Privacy Discord menyala.",
                style = MaterialTheme.typography.bodySmall,
                color = WarmFaint,
            )
        }
    }
}

/**
 * Pratinjau visual kartu profil Discord (persis seperti di screenshot Discord Mobile).
 */
@Composable
private fun DiscordProfilePreviewCard(
    now: NowPlaying,
    listeningMode: DiscordListeningMode,
    showVyoraBadge: Boolean,
) {
    val song = now.song
    val headerLabel = when (listeningMode) {
        DiscordListeningMode.VYORA -> "Listening to VYORA"
        DiscordListeningMode.ARTIST -> "Listening to ${song?.artistsText?.ifBlank { "Bruno Mars" } ?: "Bruno Mars"}"
        DiscordListeningMode.YOUTUBE_MUSIC -> "Listening to YouTube Music"
    }
    val title = song?.title ?: "Just the Way You Are"
    val artist = song?.artistsText ?: "Bruno Mars"
    val posMs = if (song != null) now.positionMs else 175_000L
    val durMs = if (song != null && now.durationMs > 0L) now.durationMs else 220_000L
    val progress = (posMs.toFloat() / durMs.coerceAtLeast(1L).toFloat()).coerceIn(0.05f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(WarmSurfaceElevated)
            .border(1.dp, DiscordBlurple.copy(alpha = 0.40f), RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = headerLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = WarmMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Rounded.Headphones,
                contentDescription = null,
                tint = DiscordBlurple,
                modifier = Modifier.size(15.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(66.dp)) {
                Artwork(
                    url = song?.thumbnail,
                    modifier = Modifier
                        .size(62.dp)
                        .align(Alignment.TopStart),
                    shape = RoundedCornerShape(12.dp),
                )
                if (showVyoraBadge) {
                    Image(
                        painter = painterResource(R.drawable.vyora_logo),
                        contentDescription = "Badge Logo VYORA",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .border(2.dp, WarmSurfaceElevated, CircleShape),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WarmInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape),
            color = DiscordBlurple,
            trackColor = warmWash(0.14f),
        )
        Spacer(Modifier.height(5.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatMs(posMs), style = MaterialTheme.typography.labelSmall, color = WarmMuted)
            Text(formatMs(durMs), style = MaterialTheme.typography.labelSmall, color = WarmMuted)
        }
    }
}

@Composable
private fun ManualTokenDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WarmSurfaceElevated,
        title = { Text("Masukkan Token Discord", color = WarmInk) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Token disimpan 100% lokal di HP kamu dan hanya dikirim langsung ke server resmi Discord.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmMuted,
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Tempel token akun Discord…") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = input.trim().length >= 24,
                onClick = { onConfirm(input) },
            ) {
                Text("Hubungkan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = WarmMuted)
            }
        },
    )
}

/**
 * Layar Login WebView Discord penuh:
 * Menyuntikkan penyadap `Storage.prototype.setItem` + pemindai `webpackChunkdiscord_app`
 * & `iframe.contentWindow.localStorage.token` sehingga begitu pengguna selesai login
 * di halaman resmi `discord.com/login`, token langsung tertangkap otomatis.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DiscordWebLoginDialog(
    onTokenExtracted: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var delivered by remember { mutableStateOf(false) }

    val deliverToken: (String?) -> Unit = { raw ->
        val clean = raw?.trim()?.removeSurrounding("\"")?.trim().orEmpty()
        if (!delivered && clean.length >= 24 && clean != "null" && clean != "undefined") {
            delivered = true
            onTokenExtracted(clean)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = WarmSurface,
        ) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarmSurfaceElevated)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = WarmInk)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Login Discord",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = WarmInk,
                        )
                        Text(
                            text = "discord.com/login • Otomatis kembali setelah masuk",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmMuted,
                        )
                    }
                    TextButton(
                        onClick = {
                            webViewRef?.evaluateJavascript(EXTRACT_TOKEN_JS) { res ->
                                deliverToken(res)
                            }
                        }
                    ) {
                        Text("Selesai", color = DiscordBlurple, fontWeight = FontWeight.Bold)
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        runCatching {
                            webViewRef?.stopLoading()
                            webViewRef?.destroy()
                        }
                    }
                }

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true

                            addJavascriptInterface(
                                DiscordJsBridge { token ->
                                    post { deliverToken(token) }
                                },
                                "VyoraDiscordBridge",
                            )

                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    view?.evaluateJavascript(HOOK_STORAGE_JS, null)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    view?.evaluateJavascript(HOOK_STORAGE_JS, null)
                                    if (url != null && !url.contains("/login") && !url.contains("/register")) {
                                        view?.evaluateJavascript(EXTRACT_TOKEN_JS) { res ->
                                            deliverToken(res)
                                        }
                                    }
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                ): Boolean {
                                    val target = request?.url?.toString().orEmpty()
                                    if (target.startsWith("intent://") || target.startsWith("discord://")) {
                                        // Cegah loncat ke aplikasi Discord luar agar login selesai di WebView ini
                                        return true
                                    }
                                    if (target.contains("/app") || target.contains("/channels/")) {
                                        view?.evaluateJavascript(EXTRACT_TOKEN_JS) { res ->
                                            deliverToken(res)
                                        }
                                    }
                                    return false
                                }
                            }

                            loadUrl("https://discord.com/login")
                            webViewRef = this
                        }
                    },
                )
            }
        }
    }
}

class DiscordJsBridge(
    private val onCaptured: (String?) -> Unit,
) {
    @JavascriptInterface
    fun onToken(token: String?) {
        onCaptured(token)
    }
}

private const val HOOK_STORAGE_JS = """
(function() {
    if (window.__vyoraHooked) return;
    window.__vyoraHooked = true;
    const origSetItem = Storage.prototype.setItem;
    Storage.prototype.setItem = function(key, val) {
        if (key === 'token' && val && val.length > 20) {
            try { VyoraDiscordBridge.onToken(JSON.parse(val)); } catch(e) { VyoraDiscordBridge.onToken(val); }
        }
        return origSetItem.apply(this, arguments);
    };
})();
"""

private const val EXTRACT_TOKEN_JS = """
(function() {
    try {
        let t = null;
        if (window.webpackChunkdiscord_app) {
            window.webpackChunkdiscord_app.push([[Math.random()], {}, function(req) {
                for (const k of Object.keys(req.c)) {
                    const exp = req.c[k] && req.c[k].exports;
                    if (!exp) continue;
                    if (exp.default && typeof exp.default.getToken === 'function') {
                        t = exp.default.getToken();
                        if (t) break;
                    }
                    if (typeof exp.getToken === 'function') {
                        t = exp.getToken();
                        if (t) break;
                    }
                }
            }]);
            window.webpackChunkdiscord_app.pop();
        }
        if (!t) {
            const f = document.createElement('iframe');
            f.style.display = 'none';
            document.body.appendChild(f);
            const raw = f.contentWindow.localStorage.getItem('token');
            f.remove();
            if (raw) {
                try { t = JSON.parse(raw); } catch(e) { t = raw; }
            }
        }
        if (t && window.VyoraDiscordBridge) {
            window.VyoraDiscordBridge.onToken(t);
        }
        return t || '';
    } catch (err) {
        return '';
    }
})();
"""
