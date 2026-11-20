package com.vyora.music.ui.stats

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextPaint
import android.text.TextUtils
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vyora.music.AppModule
import com.vyora.music.data.local.StatsPeriod
import com.vyora.music.data.local.StatsSummary
import com.vyora.music.data.ytmusic.SongItem
import com.vyora.music.ui.components.Artwork
import com.vyora.music.ui.components.EmptyState
import com.vyora.music.ui.theme.GlassChip
import com.vyora.music.ui.theme.WarmFaint
import com.vyora.music.ui.theme.WarmHairline
import com.vyora.music.ui.theme.WarmInk
import com.vyora.music.ui.theme.WarmMuted
import com.vyora.music.ui.theme.WarmSurface
import com.vyora.music.ui.theme.WarmSurfaceElevated
import com.vyora.music.ui.theme.warmWash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsSheet(
    onDismiss: () -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val rawData by AppModule.stats.data.collectAsStateWithLifecycle()
    var period by remember { mutableStateOf(StatsPeriod.MONTH) }
    val summary = remember(rawData, period) { AppModule.stats.summarize(period) }
    val accent = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
            Text("VYORA RECAP & STATISTIK", style = MaterialTheme.typography.labelSmall, color = accent)
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Jejak Musikmu",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                color = WarmInk,
            )

            Spacer(Modifier.height(12.dp))

            // Filter Periode
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatsPeriod.entries.forEach { p ->
                    GlassChip(
                        label = p.label,
                        selected = period == p,
                        onClick = { period = p },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Kartu Streak Harian
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                accent.copy(alpha = 0.28f),
                                WarmSurfaceElevated,
                            )
                        )
                    )
                    .border(1.dp, accent.copy(alpha = 0.42f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${summary.streakTier.badge} ${summary.streakTier.title}",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                    )
                    Text(
                        text = "${summary.currentStreakDays} hari beruntun mendengarkan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WarmInk,
                    )
                    Text(
                        text = "Rekor terpanjang: ${summary.longestStreakDays} hari",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmMuted,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Grid 2x2 Metrik Utama
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatMetricCard(
                    value = "${summary.totalMinutes}",
                    unit = "Menit Dengar",
                    modifier = Modifier.weight(1f),
                )
                StatMetricCard(
                    value = "${summary.totalPlays}",
                    unit = "Lagu Diputar",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatMetricCard(
                    value = "${summary.uniqueSongs}",
                    unit = "Judul Unik",
                    modifier = Modifier.weight(1f),
                )
                StatMetricCard(
                    value = "${summary.uniqueArtists}",
                    unit = "Artis Unik",
                    modifier = Modifier.weight(1f),
                )
            }

            if (summary.topSongs.isEmpty()) {
                Spacer(Modifier.height(18.dp))
                EmptyState(
                    icon = Icons.Rounded.Insights,
                    title = "Belum cukup riwayat putar",
                    message = "Putar beberapa lagu favoritmu di VYORA — daftar Top Lagu, Top Artis, dan kartu Story VYORA Recap akan otomatis terisi!",
                )
            } else {
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        scope.launch {
                            shareRecapCard(context, summary)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Bagikan Kartu Story VYORA Recap", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Rounded.Share, null, modifier = Modifier.size(16.dp))
                }

                Spacer(Modifier.height(20.dp))
                Text("LAGU TERATAS (${period.label.uppercase()})", style = MaterialTheme.typography.labelSmall, color = WarmFaint)
                Spacer(Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    summary.topSongs.take(10).forEachIndexed { idx, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(WarmSurfaceElevated)
                                .border(1.dp, WarmHairline, RoundedCornerShape(14.dp))
                                .clickable {
                                    onDismiss()
                                    onPlaySong(item.song)
                                }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "#${idx + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (idx < 3) accent else WarmMuted,
                                modifier = Modifier.width(34.dp),
                            )
                            Artwork(
                                url = item.song.thumbnail,
                                modifier = Modifier.size(44.dp),
                                shape = RoundedCornerShape(10.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = item.song.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WarmInk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = item.song.artistsText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WarmMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                text = "${item.playCount}×",
                                style = MaterialTheme.typography.labelLarge,
                                color = accent,
                            )
                        }
                    }
                }

                if (summary.topArtists.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Text("ARTIS TERFAVORIT", style = MaterialTheme.typography.labelSmall, color = WarmFaint)
                    Spacer(Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        summary.topArtists.take(5).forEachIndexed { idx, artist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(WarmSurfaceElevated)
                                    .border(1.dp, WarmHairline, RoundedCornerShape(14.dp))
                                    .clickable(enabled = artist.artistId != null) {
                                        artist.artistId?.let { id ->
                                            onDismiss()
                                            onOpenArtist(id)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "#${idx + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (idx == 0) accent else WarmMuted,
                                    modifier = Modifier.width(34.dp),
                                )
                                Artwork(
                                    url = artist.thumbnail,
                                    modifier = Modifier.size(42.dp),
                                    shape = CircleShape,
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = artist.artistName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = WarmInk,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${artist.playCount} putaran • ${artist.totalSec / 60} menit",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WarmMuted,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMetricCard(
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(WarmSurfaceElevated)
            .border(1.dp, WarmHairline, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
            fontWeight = FontWeight.ExtraBold,
            color = WarmInk,
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.bodySmall,
            color = WarmMuted,
        )
    }
}

/**
 * Merender Kartu Story VYORA Recap 1080x1920 bergaya Obsidian-Violet Liquid Glass
 * lalu menyimpannya ke Galeri (`Pictures/VYORA`) dan membuka sheet Share Android.
 */
private suspend fun shareRecapCard(context: Context, summary: StatsSummary) {
    val result = withContext(Dispatchers.IO) {
        runCatching {
            val w = 1080
            val h = 1920
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            // Latar Obsidian-Violet mewah
            canvas.drawRect(
                0f, 0f, w.toFloat(), h.toFloat(),
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, 0f, w.toFloat(), h.toFloat(),
                        intArrayOf(
                            AColor.rgb(16, 12, 28),
                            AColor.rgb(34, 20, 66),
                            AColor.rgb(13, 11, 20),
                        ),
                        floatArrayOf(0f, 0.52f, 1f),
                        Shader.TileMode.CLAMP,
                    )
                }
            )

            // Kartu kaca utama
            val cardRect = RectF(72f, 130f, w - 72f, h - 150f)
            canvas.drawRoundRect(
                cardRect, 56f, 56f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AColor.argb(185, 24, 19, 38)
                }
            )
            canvas.drawRoundRect(
                cardRect, 56f, 56f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                    color = AColor.argb(95, 201, 160, 255)
                }
            )

            val eyebrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AColor.rgb(201, 160, 255)
                textSize = 30f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                letterSpacing = 0.14f
            }
            canvas.drawText("∿ VYORA RECAP • ${summary.period.label.uppercase()}", 132f, 225f, eyebrowPaint)

            val bigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AColor.WHITE
                textSize = 112f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            canvas.drawText("${summary.totalMinutes} Menit", 132f, 365f, bigPaint)

            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AColor.rgb(185, 178, 206)
                textSize = 36f
            }
            canvas.drawText(
                "${summary.totalPlays} lagu diputar • Streak ${summary.currentStreakDays} hari ${summary.streakTier.badge}",
                132f, 430f, subPaint,
            )

            // Seksi Top 5 Lagu
            canvas.drawText("LAGU TERATAS", 132f, 540f, eyebrowPaint)
            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AColor.WHITE
                textSize = 40f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AColor.rgb(179, 173, 196)
                textSize = 30f
            }
            val rankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AColor.rgb(157, 107, 255)
                textSize = 44f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }

            var y = 620f
            summary.topSongs.take(5).forEachIndexed { i, s ->
                canvas.drawText("#${i + 1}", 132f, y, rankPaint)
                val t = TextUtils.ellipsize(s.song.title, titlePaint, 640f, TextUtils.TruncateAt.END).toString()
                val a = TextUtils.ellipsize("${s.song.artistsText} • ${s.playCount}×", artistPaint, 640f, TextUtils.TruncateAt.END).toString()
                canvas.drawText(t, 230f, y - 10f, titlePaint)
                canvas.drawText(a, 230f, y + 32f, artistPaint)
                y += 126f
            }

            // Seksi Top 3 Artis
            if (summary.topArtists.isNotEmpty()) {
                y += 40f
                canvas.drawText("ARTIS TERFAVORIT", 132f, y, eyebrowPaint)
                y += 72f
                summary.topArtists.take(3).forEachIndexed { i, ar ->
                    canvas.drawText("${i + 1}. ${ar.artistName} (${ar.playCount}×)", 132f, y, titlePaint)
                    y += 72f
                }
            }

            // Footer watermark VYORA
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AColor.argb(220, 255, 255, 255)
                textSize = 34f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("VYORA MUSIC", w / 2f, h - 78f, footerPaint)

            // Simpan ke MediaStore Pictures/VYORA (Android 10+) + file cache untuk Intent.ACTION_SEND
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "VYORA_Recap_${System.currentTimeMillis()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/VYORA")
                }
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
            }

            val shareDir = File(context.cacheDir, "shared_posters").apply { mkdirs() }
            val shareFile = File(shareDir, "VYORA_Recap.png")
            FileOutputStream(shareFile).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", shareFile)
        }
    }

    result.onSuccess { uri ->
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Jejak musikku di VYORA (${summary.totalMinutes} menit)!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Bagikan VYORA Recap"))
    }.onFailure {
        Toast.makeText(context, "Gagal membuat kartu Recap", Toast.LENGTH_SHORT).show()
    }
}
