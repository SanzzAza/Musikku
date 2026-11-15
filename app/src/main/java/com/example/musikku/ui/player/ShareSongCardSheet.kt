package com.example.musikku.ui.player

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
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
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.musikku.data.lyrics.Lyrics
import com.example.musikku.data.ytmusic.SongItem
import com.example.musikku.player.LyricsUiState
import com.example.musikku.ui.components.Artwork
import com.example.musikku.ui.theme.GlassChip
import com.example.musikku.ui.theme.LocalCoverPalette
import com.example.musikku.ui.theme.LocalUiPrefs
import com.example.musikku.ui.theme.ObsidianBackground
import com.example.musikku.ui.theme.WarmHairline
import com.example.musikku.ui.theme.WarmInk
import com.example.musikku.ui.theme.WarmMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Kartu Bagikan Lagu (siap buat Story / WhatsApp) dengan pilihan gaya Liquid Glass & Editorial
 * serta kutipan lirik opsional.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSongCardSheet(
    song: SongItem,
    lyricsState: LyricsUiState,
    positionMs: Long,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = LocalUiPrefs.current
    val palette = LocalCoverPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var glassCard by remember { mutableStateOf(prefs.isGlass) }
    var busy by remember { mutableStateOf(false) }

    // Ambil kutipan lirik terdekat dari posisi pemutaran saat ini (jika tersedia)
    val availableQuote = remember(lyricsState, positionMs) {
        val loaded = (lyricsState as? LyricsUiState.Loaded)?.lyrics
        when (loaded) {
            is Lyrics.Synced -> {
                val idx = loaded.lines.indexOfLast { it.timeMs <= positionMs + 300 }.coerceAtLeast(0)
                loaded.lines.drop(idx).take(2)
                    .map { it.text.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString("\n")
                    .ifBlank { null }
            }
            is Lyrics.Plain -> loaded.text.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(2)
                .joinToString("\n")
                .ifBlank { null }
            else -> null
        }
    }
    var includeQuote by remember(availableQuote) { mutableStateOf(availableQuote != null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF171412),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.24f))
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "KARTU BAGIKAN LAGU",
                style = MaterialTheme.typography.labelSmall,
                color = palette.accent,
                modifier = Modifier.align(Alignment.Start),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Siap buat Story",
                style = MaterialTheme.typography.titleLarge,
                color = WarmInk,
                modifier = Modifier.align(Alignment.Start),
            )
            Spacer(Modifier.height(14.dp))

            // Pratinjau Kartu
            val cardShape = RoundedCornerShape(if (glassCard) 28.dp else 18.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(
                        if (glassCard) {
                            Brush.linearGradient(
                                listOf(
                                    palette.ambient,
                                    Color(0xFF1A1513),
                                    ObsidianBackground,
                                )
                            )
                        } else {
                            Brush.verticalGradient(listOf(Color(0xFF1E1A17), Color(0xFF141110)))
                        }
                    )
                    .border(
                        1.dp,
                        if (glassCard) Color.White.copy(alpha = 0.26f) else WarmHairline,
                        cardShape,
                    )
                    .padding(20.dp),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                                .padding(4.dp)
                        ) {
                            Artwork(song.thumbnail, Modifier.size(64.dp), shape = RoundedCornerShape(12.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "SEDANG DIPUTAR",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.accent,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = if (glassCard) FontFamily.SansSerif else FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = WarmInk,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = song.artistsText,
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (includeQuote && availableQuote != null) {
                        Spacer(Modifier.height(18.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = if (glassCard) 0.08f else 0.04f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "“$availableQuote”",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontStyle = FontStyle.Italic,
                                    lineHeight = 24.sp,
                                ),
                                color = WarmInk,
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "MUSIKKU",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmMuted.copy(alpha = 0.7f),
                        )
                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .width(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(listOf(palette.accent, palette.secondaryAccent))
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Pengalih gaya kartu & opsi lirik
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlassChip(
                    label = "Liquid Glass",
                    selected = glassCard,
                    onClick = { glassCard = true },
                )
                GlassChip(
                    label = "Editorial",
                    selected = !glassCard,
                    onClick = { glassCard = false },
                )
                if (availableQuote != null) {
                    GlassChip(
                        label = if (includeQuote) "Dengan Lirik" else "Tanpa Lirik",
                        selected = includeQuote,
                        onClick = { includeQuote = !includeQuote },
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        shareSongAsText(context, song, if (includeQuote) availableQuote else null)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, WarmHairline),
                ) {
                    Icon(Icons.Rounded.Share, null, Modifier.size(18.dp), tint = WarmInk)
                    Spacer(Modifier.width(6.dp))
                    Text("Teks", color = WarmInk)
                }
                Button(
                    onClick = {
                        if (busy) return@Button
                        busy = true
                        scope.launch {
                            val ok = shareSongCardImage(
                                context = context,
                                song = song,
                                quote = if (includeQuote) availableQuote else null,
                                palette = palette,
                                glassCard = glassCard,
                            )
                            busy = false
                            if (ok) {
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Gagal membuat kartu gambar, membagikan teks.", Toast.LENGTH_SHORT).show()
                                shareSongAsText(context, song, if (includeQuote) availableQuote else null)
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier.weight(1.4f).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accent,
                        contentColor = Color(0xFF1E0B04),
                    ),
                ) {
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF1E0B04))
                    } else {
                        Icon(Icons.Rounded.Image, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Bagikan Kartu", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun shareSongAsText(context: Context, song: SongItem, quote: String?) {
    val text = buildString {
        if (!quote.isNullOrBlank()) {
            append("“$quote”\n\n")
        }
        append("♪ ${song.title} — ${song.artistsText}\n")
        append("https://music.youtube.com/watch?v=${song.id}")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan lagu"))
}

private suspend fun shareSongCardImage(
    context: Context,
    song: SongItem,
    quote: String?,
    palette: CoverPalette,
    glassCard: Boolean,
): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val coverBitmap = song.thumbnail?.let { url ->
            val req = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .size(640, 640)
                .build()
            val res = context.imageLoader.execute(req) as? SuccessResult
            (res?.drawable as? BitmapDrawable)?.bitmap
        }

        val width = 1080
        val height = if (quote.isNullOrBlank()) 1280 else 1480
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Latar kartu
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    if (glassCard) palette.ambient.toArgb() else 0xFF211C19.toInt(),
                    0xFF141110.toInt(),
                    0xFF0D0C0B.toInt(),
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Panel dalam
        val cardRect = RectF(76f, 92f, width - 76f, height - 92f)
        val radius = if (glassCard) 64f else 42f
        val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (glassCard) 0x26FFFFFF else 0xFF1B1815.toInt()
        }
        canvas.drawRoundRect(cardRect, radius, radius, panelPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = if (glassCard) 0x44FFFFFF else 0x26F5F1EC
        }
        canvas.drawRoundRect(cardRect, radius, radius, borderPaint)

        // Sampul album di tengah atas
        val artSize = 680f
        val artLeft = (width - artSize) / 2f
        val artTop = 176f
        val frameRect = RectF(artLeft - 18f, artTop - 18f, artLeft + artSize + 18f, artTop + artSize + 18f)
        canvas.drawRoundRect(frameRect, 48f, 48f, borderPaint)

        val artRect = RectF(artLeft, artTop, artLeft + artSize, artTop + artSize)
        if (coverBitmap != null) {
            val path = android.graphics.Path().apply {
                addRoundRect(artRect, 36f, 36f, android.graphics.Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(path)
            val src = Rect(0, 0, coverBitmap.width, coverBitmap.height)
            canvas.drawBitmap(coverBitmap, src, artRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
        } else {
            val fallbackArt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.accent.toArgb() }
            canvas.drawRoundRect(artRect, 36f, 36f, fallbackArt)
        }

        // Judul & Artis
        val eyebrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accent.toArgb()
            textSize = 30f
            letterSpacing = 0.16f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        var cursorY = artTop + artSize + 84f
        canvas.drawText("SEDANG DIPUTAR", 142f, cursorY, eyebrowPaint)

        cursorY += 20f
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = WarmInk.toArgb()
            textSize = 58f
            typeface = Typeface.create(
                if (glassCard) Typeface.SANS_SERIF else Typeface.SERIF,
                Typeface.BOLD,
            )
        }
        val titleLayout = StaticLayout.Builder.obtain(song.title, 0, song.title.length, titlePaint, width - 284)
            .setMaxLines(2)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        canvas.save()
        canvas.translate(142f, cursorY)
        titleLayout.draw(canvas)
        canvas.restore()
        cursorY += titleLayout.height + 16f

        val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = WarmMuted.toArgb()
            textSize = 38f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText(song.artistsText.take(44), 142f, cursorY + 34f, artistPaint)
        cursorY += 74f

        // Kutipan lirik (opsional)
        if (!quote.isNullOrBlank()) {
            val quoteText = "“$quote”"
            val quotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = WarmInk.toArgb()
                textSize = 42f
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            }
            val quoteLayout = StaticLayout.Builder.obtain(quoteText, 0, quoteText.length, quotePaint, width - 284)
                .setMaxLines(3)
                .build()
            canvas.save()
            canvas.translate(142f, cursorY)
            quoteLayout.draw(canvas)
            canvas.restore()
        }

        // Watermark kecil di bawah
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xAAFFFFFF.toInt()
            textSize = 28f
            letterSpacing = 0.22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("MUSIKKU", 142f, height - 142f, brandPaint)

        val dir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        val file = File(dir, "musikku-card-${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        bitmap.recycle()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "♪ ${song.title} — ${song.artistsText}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        withContext(Dispatchers.Main) {
            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Kartu Lagu"))
        }
        true
    }.getOrDefault(false)
}
