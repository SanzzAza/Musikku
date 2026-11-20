package com.vyora.music.ui.poster

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.vyora.music.data.ytmusic.SongItem
import com.vyora.music.ui.components.formatMs
import com.vyora.music.ui.player.CoverPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import android.graphics.Color as AColor

/**
 * Renderer PNG resolusi tinggi (1080×1920 / 1080×1350 / 1080×1080) untuk:
 * 1. **Kartu Foto Liquid Glass** — 1, 3, atau 5 kartu pemutar kaca frosted melayang di atas foto sendiri
 *    (atau latar transparan / bawaan), dengan pembiasan blur asli di balik kartu, kilau sudut ✦,
 *    dan dukungan lapisan **Subjek di Depan (AI Depth)**!
 * 2. **Poster Lagu Editorial** — sampul berbingkai, 5 swatch palet, kutipan lirik opsional, bar durasi.
 * 3. **Poster Lirik** — 1–4 baris lirik terpilih dengan tipografi penuh (font, ketebalan, rata, warna).
 */
object PosterBitmapRenderer {

    suspend fun loadBitmap(context: Context, data: Any?, maxSize: Int = 1080): Bitmap? = withContext(Dispatchers.IO) {
        if (data == null) return@withContext null
        runCatching {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(data)
                .size(maxSize, maxSize)
                .allowHardware(false)
                .build()
            val result = loader.execute(request) as? SuccessResult
            (result?.drawable as? BitmapDrawable)?.bitmap
        }.getOrNull()
    }

    /** Membuat versi blur dari [source] untuk efek frosted glass di belakang kartu. */
    fun createFrostedBackdrop(source: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val cropped = SubjectSegmenterHelper.centerCropBitmap(source, targetW, targetH)
        return blurBitmap(cropped, scaleDown = 8, radius = 14)
    }

    /**
     * Merender mode **Kartu Foto Liquid Glass** ke bitmap resolusi tinggi.
     * Lapisan render:
     * 1. Latar (Foto sendiri center-crop / Latar bawaan / Transparan PNG)
     * 2. Kartu-kartu Pemutar Liquid Glass (pembiasan blur backdrop asli + specular rim + sparkle ✦)
     * 3. Potongan Subjek Depan (AI Subject Mask) bila aktif sehingga kartu melayang di belakang objek!
     * 4. Watermark VYORA di bawah tengah (opsional)
     */
    suspend fun renderPhotoCardsBitmap(
        config: PosterConfig,
        cards: List<GlassCardSlot>,
        cardCovers: Map<String, Bitmap?>,
        palette: CoverPalette,
        customPhoto: Bitmap?,
        foregroundMask: Bitmap?,
        primaryCover: Bitmap?,
    ): Bitmap = withContext(Dispatchers.Default) {
        val w = config.aspect.widthPx
        val h = config.aspect.heightPx
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        // 1. Siapkan latar & peta blur untuk refraksi kaca
        val backdropBitmap: Bitmap? = when {
            customPhoto != null -> SubjectSegmenterHelper.centerCropBitmap(customPhoto, w, h)
            config.background.isTransparent -> null
            else -> {
                val bg = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                drawBackground(Canvas(bg), w, h, config.background, palette, primaryCover)
                bg
            }
        }

        if (backdropBitmap != null) {
            canvas.drawBitmap(backdropBitmap, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
        }

        val blurredBackdrop: Bitmap? = backdropBitmap?.let {
            blurBitmap(it, scaleDown = 8, radius = 14)
        }

        // 2. Gambar setiap Kartu Pemutar Liquid Glass
        val baseRef = min(w.toFloat(), h * 0.60f)
        for (slot in cards) {
            val cardW = baseRef * 0.62f * slot.scale
            val cardH = cardW * 1.54f
            val cx = w * slot.xFraction
            val cy = h * slot.yFraction
            drawGlassPlayerCard(
                canvas = canvas,
                canvasW = w,
                canvasH = h,
                slot = slot,
                cover = cardCovers[slot.song.id] ?: primaryCover,
                blurredBackdrop = blurredBackdrop,
                cx = cx,
                cy = cy,
                cardW = cardW,
                cardH = cardH,
                photoCfg = config.photoCards,
                isTransparentCanvas = backdropBitmap == null,
            )
        }

        // 3. Lapisan Subjek Depan (kartu tampak melayang di belakang orang/objek di foto!)
        if (customPhoto != null && config.photoCards.subjectInFront && foregroundMask != null) {
            val dst = Rect(0, 0, w, h)
            canvas.drawBitmap(foregroundMask, null, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }

        // 4. Watermark bawah tengah (ala badge Auralys di foto)
        if (config.photoCards.showWatermark && !config.background.isTransparent) {
            drawPhotoWatermark(canvas, w, h)
        }

        out
    }

    /** Menggambar satu Kartu Pemutar Liquid Glass lengkap (sampul, ~VYORA, judul+hati, artis, seekbar, tombol putar, volume, kilau). */
    private fun drawGlassPlayerCard(
        canvas: Canvas,
        canvasW: Int,
        canvasH: Int,
        slot: GlassCardSlot,
        cover: Bitmap?,
        blurredBackdrop: Bitmap?,
        cx: Float,
        cy: Float,
        cardW: Float,
        cardH: Float,
        photoCfg: PhotoCardsConfig,
        isTransparentCanvas: Boolean,
    ) {
        val halfW = cardW / 2f
        val halfH = cardH / 2f
        val rect = RectF(-halfW, -halfH, halfW, halfH)
        val radius = cardW * 0.088f

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(slot.rotationDeg)

        // Bayangan lembut bertingkat di bawah kartu kaca
        for (step in 3 downTo 1) {
            val spread = step * cardW * 0.014f
            val offsetY = step * cardW * 0.012f
            val shadowRect = RectF(
                rect.left - spread,
                rect.top - spread * 0.4f + offsetY,
                rect.right + spread,
                rect.bottom + spread + offsetY,
            )
            val shadowAlpha = if (isTransparentCanvas) (24 / step) else (44 / step)
            canvas.drawRoundRect(
                shadowRect,
                radius + spread * 0.6f,
                radius + spread * 0.6f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AColor.argb(shadowAlpha, 0, 0, 0) },
            )
        }

        val cardPath = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }

        // Bagian dalam kartu (dibatasi clipPath membulat)
        canvas.save()
        canvas.clipPath(cardPath)

        // Refraksi backdrop blur asli tepat di belakang posisi kartu ini
        if (blurredBackdrop != null) {
            canvas.save()
            canvas.rotate(-slot.rotationDeg)
            canvas.translate(-cx, -cy)
            canvas.drawBitmap(
                blurredBackdrop,
                null,
                Rect(0, 0, canvasW, canvasH),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
            )
            canvas.restore()
        }

        // Lapisan tint kaca frosted (lebih pekat bila latar transparan agar tetap terbaca saat ditempel di Story)
        val baseOpacity = if (isTransparentCanvas) {
            max(photoCfg.glassOpacity + 0.14f, 0.68f).coerceAtMost(0.90f)
        } else {
            photoCfg.glassOpacity.coerceIn(0.32f, 0.85f)
        }
        val topAlpha = (baseOpacity * 255).roundToInt().coerceIn(70, 235)
        val botAlpha = ((baseOpacity + 0.12f).coerceAtMost(0.94f) * 255).roundToInt().coerceIn(95, 242)
        val glassTintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                intArrayOf(
                    AColor.argb(topAlpha, 44, 42, 46),
                    AColor.argb(((topAlpha + botAlpha) / 2), 28, 27, 30),
                    AColor.argb(botAlpha, 18, 17, 20),
                ),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(rect, glassTintPaint)

        // Kilau diagonal tipis di bagian atas-kiri kaca
        val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                rect.left, rect.top, rect.right * 0.65f, rect.top + cardH * 0.45f,
                intArrayOf(
                    AColor.argb(46, 255, 255, 255),
                    AColor.argb(12, 255, 255, 255),
                    AColor.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(rect, sheenPaint)

        // --- Konten di dalam kartu pemutar ---
        val pad = cardW * 0.072f
        val contentLeft = rect.left + pad
        val contentRight = rect.right - pad
        val contentW = contentRight - contentLeft

        // 1. Sampul Album Persegi
        val artTop = rect.top + pad
        val artRect = RectF(contentLeft, artTop, contentLeft + contentW, artTop + contentW)
        val artRadius = cardW * 0.055f
        drawRoundedArtwork(canvas, cover, artRect, artRadius, isLightPaper = false)
        canvas.drawRoundRect(
            artRect,
            artRadius,
            artRadius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = max(1f, cardW * 0.004f)
                color = AColor.argb(56, 255, 255, 255)
            },
        )

        // 2. Micro-brand "~ VYORA"
        var curY = artRect.bottom + cardW * 0.056f
        val waveW = cardW * 0.038f
        val waveY = curY - cardW * 0.011f
        val wavePath = Path().apply {
            moveTo(contentLeft, waveY)
            cubicTo(
                contentLeft + waveW * 0.30f, waveY - cardW * 0.014f,
                contentLeft + waveW * 0.70f, waveY + cardW * 0.014f,
                contentLeft + waveW, waveY,
            )
        }
        canvas.drawPath(
            wavePath,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = max(1.2f, cardW * 0.0055f)
                strokeCap = Paint.Cap.ROUND
                color = AColor.argb(165, 255, 255, 255)
            },
        )
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.argb(165, 255, 255, 255)
            textSize = cardW * 0.036f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText("VYORA", contentLeft + waveW + cardW * 0.018f, curY, brandPaint)

        // 3. Judul Lagu + Ikon Hati (♡ / ♥)
        curY += cardW * 0.072f
        val heartReserve = if (photoCfg.showHeart) cardW * 0.115f else 0f
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.WHITE
            textSize = cardW * 0.068f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val ellipsizedTitle = TextUtils.ellipsize(
            slot.song.title,
            titlePaint,
            (contentW - heartReserve).coerceAtLeast(24f),
            TextUtils.TruncateAt.END,
        ).toString()
        canvas.drawText(ellipsizedTitle, contentLeft, curY, titlePaint)

        if (photoCfg.showHeart) {
            val heartSize = cardW * 0.066f
            drawHeartIcon(
                canvas = canvas,
                cx = contentRight - heartSize * 0.55f,
                cy = curY - cardW * 0.020f,
                size = heartSize,
                filled = slot.isLiked,
            )
        }

        // 4. Nama Artis
        curY += cardW * 0.056f
        val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.argb(190, 244, 240, 236)
            textSize = cardW * 0.044f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val ellipsizedArtist = TextUtils.ellipsize(
            slot.song.artistsText.ifBlank { "VYORA" },
            artistPaint,
            (contentW - heartReserve * 0.4f).coerceAtLeast(24f),
            TextUtils.TruncateAt.END,
        ).toString()
        canvas.drawText(ellipsizedArtist, contentLeft, curY, artistPaint)

        // 5. Progress Seekbar + Timestamp ("1:11" dan "-2:26")
        val seekY = curY + cardW * 0.062f
        val barH = max(2f, cardW * 0.011f)
        val trackRect = RectF(contentLeft, seekY - barH / 2f, contentRight, seekY + barH / 2f)
        canvas.drawRoundRect(
            trackRect,
            barH,
            barH,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AColor.argb(72, 255, 255, 255) },
        )
        val prog = slot.progressFraction.coerceIn(0.08f, 0.90f)
        val fillRight = contentLeft + contentW * prog
        canvas.drawRoundRect(
            RectF(contentLeft, seekY - barH / 2f, fillRight, seekY + barH / 2f),
            barH,
            barH,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AColor.WHITE },
        )
        canvas.drawCircle(
            fillRight,
            seekY,
            cardW * 0.019f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AColor.WHITE },
        )

        val (elapsedText, remainText) = slot.formattedTimestamps()
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.argb(155, 255, 255, 255)
            textSize = cardW * 0.032f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val timeY = seekY + cardW * 0.048f
        canvas.drawText(elapsedText, contentLeft, timeY, timePaint)
        val remW = timePaint.measureText(remainText)
        canvas.drawText(remainText, contentRight - remW, timeY, timePaint)

        // 6. Baris Kontrol Pemutar (⏮  ⏸  ⏭)
        val ctrlY = timeY + cardW * 0.072f
        val ctrlCenterX = 0f
        val ctrlSpacing = contentW * 0.25f
        val iconSize = cardW * 0.066f
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.WHITE
            style = Paint.Style.FILL
        }
        drawRewindNextIcon(canvas, ctrlCenterX - ctrlSpacing, ctrlY, iconSize, forward = false, paint = iconPaint)
        drawPauseIcon(canvas, ctrlCenterX, ctrlY, iconSize * 1.15f, paint = iconPaint)
        drawRewindNextIcon(canvas, ctrlCenterX + ctrlSpacing, ctrlY, iconSize, forward = true, paint = iconPaint)

        // 7. Baris Slider Volume Bawah (🔈 ——●—— 🔊)
        val volY = rect.bottom - pad * 0.85f
        val spkSize = cardW * 0.044f
        val spkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.argb(205, 255, 255, 255)
            style = Paint.Style.FILL
        }
        drawSpeakerIcon(canvas, contentLeft + spkSize * 0.45f, volY, spkSize, high = false, paint = spkPaint)
        drawSpeakerIcon(canvas, contentRight - spkSize * 0.55f, volY, spkSize, high = true, paint = spkPaint)

        val volLeft = contentLeft + spkSize * 1.25f
        val volRight = contentRight - spkSize * 1.45f
        val volH = max(1.8f, cardW * 0.009f)
        canvas.drawRoundRect(
            RectF(volLeft, volY - volH / 2f, volRight, volY + volH / 2f),
            volH,
            volH,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AColor.argb(64, 255, 255, 255) },
        )
        val volPos = volLeft + (volRight - volLeft) * slot.volumeFraction.coerceIn(0.2f, 0.9f)
        canvas.drawRoundRect(
            RectF(volLeft, volY - volH / 2f, volPos, volY + volH / 2f),
            volH,
            volH,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AColor.argb(235, 255, 255, 255) },
        )
        canvas.drawCircle(
            volPos,
            volY,
            cardW * 0.015f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AColor.WHITE },
        )

        canvas.restore() // selesai clipPath

        // 8. Bingkai Kaca Specular (Rim Liquid Glass)
        val rimStroke = max(1.6f, cardW * 0.0075f)
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = rimStroke
            shader = LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                intArrayOf(
                    AColor.argb(158, 255, 255, 255),
                    AColor.argb(46, 255, 255, 255),
                    AColor.argb(96, 255, 255, 255),
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(rect, radius, radius, rimPaint)

        // 9. Kilau Bintang 4-Ujung (✦ Sparkle Glint) di sudut kaca
        if (photoCfg.showSparkles && slot.sparkleCorner != SparkleCorner.NONE) {
            val sx = when (slot.sparkleCorner) {
                SparkleCorner.TOP_RIGHT, SparkleCorner.BOTTOM_RIGHT -> rect.right - radius * 0.25f
                SparkleCorner.TOP_LEFT -> rect.left + radius * 0.25f
                SparkleCorner.NONE -> 0f
            }
            val sy = when (slot.sparkleCorner) {
                SparkleCorner.TOP_RIGHT, SparkleCorner.TOP_LEFT -> rect.top + radius * 0.25f
                SparkleCorner.BOTTOM_RIGHT -> rect.bottom - radius * 0.25f
                SparkleCorner.NONE -> 0f
            }
            drawSparkleGlint(canvas, sx, sy, cardW * 0.13f)
        }

        canvas.restore()
    }

    private fun drawHeartIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, filled: Boolean) {
        val r = size / 2f
        val path = Path().apply {
            moveTo(cx, cy + r * 0.72f)
            cubicTo(cx - r * 1.15f, cy + r * 0.05f, cx - r * 1.12f, cy - r * 0.85f, cx, cy - r * 0.28f)
            cubicTo(cx + r * 1.12f, cy - r * 0.85f, cx + r * 1.15f, cy + r * 0.05f, cx, cy + r * 0.72f)
            close()
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            if (filled) {
                style = Paint.Style.FILL
                color = AColor.rgb(255, 118, 92)
            } else {
                style = Paint.Style.STROKE
                strokeWidth = max(1.4f, size * 0.105f)
                strokeJoin = Paint.Join.ROUND
                color = AColor.argb(230, 255, 255, 255)
            }
        }
        canvas.drawPath(path, paint)
    }

    private fun drawPauseIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val barW = size * 0.26f
        val barH = size * 0.84f
        val gap = size * 0.14f
        val corner = barW * 0.35f
        canvas.drawRoundRect(
            RectF(cx - gap - barW, cy - barH / 2f, cx - gap, cy + barH / 2f),
            corner, corner, paint,
        )
        canvas.drawRoundRect(
            RectF(cx + gap, cy - barH / 2f, cx + gap + barW, cy + barH / 2f),
            corner, corner, paint,
        )
    }

    private fun drawRewindNextIcon(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        forward: Boolean,
        paint: Paint,
    ) {
        val dir = if (forward) 1f else -1f
        val halfH = size * 0.36f
        val triW = size * 0.44f
        // Segitiga 1
        val t1 = Path().apply {
            moveTo(cx - dir * triW * 0.95f, cy - halfH)
            lineTo(cx - dir * triW * 0.05f, cy)
            lineTo(cx - dir * triW * 0.95f, cy + halfH)
            close()
        }
        // Segitiga 2
        val t2 = Path().apply {
            moveTo(cx - dir * triW * 0.02f, cy - halfH)
            lineTo(cx + dir * triW * 0.88f, cy)
            lineTo(cx - dir * triW * 0.02f, cy + halfH)
            close()
        }
        canvas.drawPath(t1, paint)
        canvas.drawPath(t2, paint)
    }

    private fun drawSpeakerIcon(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        high: Boolean,
        paint: Paint,
    ) {
        val r = size * 0.42f
        val body = Path().apply {
            moveTo(cx - r * 0.95f, cy - r * 0.38f)
            lineTo(cx - r * 0.42f, cy - r * 0.38f)
            lineTo(cx + r * 0.15f, cy - r * 0.88f)
            lineTo(cx + r * 0.15f, cy + r * 0.88f)
            lineTo(cx - r * 0.42f, cy + r * 0.38f)
            lineTo(cx - r * 0.95f, cy + r * 0.38f)
            close()
        }
        canvas.drawPath(body, paint)
        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paint.color
            style = Paint.Style.STROKE
            strokeWidth = max(1.2f, size * 0.11f)
            strokeCap = Paint.Cap.ROUND
        }
        val inner = RectF(cx - r * 0.12f, cy - r * 0.48f, cx + r * 0.62f, cy + r * 0.48f)
        canvas.drawArc(inner, -45f, 90f, false, arcPaint)
        if (high) {
            val outer = RectF(cx - r * 0.12f, cy - r * 0.82f, cx + r * 0.98f, cy + r * 0.82f)
            canvas.drawArc(outer, -48f, 96f, false, arcPaint)
        }
    }

    /** Kilau bintang 4-ujung lembut (`✦`) ala refleksi sudut Liquid Glass. */
    private fun drawSparkleGlint(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        // Halo cahaya lembut
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, radius * 0.95f,
                intArrayOf(
                    AColor.argb(190, 255, 255, 255),
                    AColor.argb(72, 255, 245, 232),
                    AColor.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.36f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(cx, cy, radius * 0.95f, haloPaint)

        // Bintang 4-ujung tajam
        val star = Path()
        val points = 8
        for (i in 0 until points) {
            val angle = Math.toRadians(i * 45.0)
            val dist = if (i % 2 == 0) radius else radius * 0.12f
            val px = cx + (cos(angle) * dist).toFloat()
            val py = cy + (sin(angle) * dist).toFloat()
            if (i == 0) star.moveTo(px, py) else star.lineTo(px, py)
        }
        star.close()
        canvas.drawPath(
            star,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AColor.argb(242, 255, 255, 255)
                style = Paint.Style.FILL
            },
        )
    }

    /** Watermark "VYORA" di bawah tengah untuk mode Kartu Foto. */
    private fun drawPhotoWatermark(canvas: Canvas, w: Int, h: Int) {
        val centerY = h * 0.925f
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.argb(235, 255, 255, 255)
            textSize = w * 0.040f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            setShadowLayer(12f, 0f, 3f, AColor.argb(170, 0, 0, 0))
        }
        val label = "VYORA"
        val textW = textPaint.measureText(label)
        val badgeSize = w * 0.044f
        val gap = w * 0.018f
        val totalW = badgeSize + gap + textW
        val startX = (w - totalW) / 2f

        // Emblem lingkaran gradien hangat
        val badgeCx = startX + badgeSize / 2f
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                startX, centerY - badgeSize / 2f,
                startX + badgeSize, centerY + badgeSize / 2f,
                intArrayOf(AColor.rgb(124, 77, 255), AColor.rgb(201, 160, 255)),
                null,
                Shader.TileMode.CLAMP,
            )
            setShadowLayer(10f, 0f, 3f, AColor.argb(145, 0, 0, 0))
        }
        canvas.drawCircle(badgeCx, centerY, badgeSize / 2f, badgePaint)

        // Gelombang putih di dalam emblem
        val wPath = Path().apply {
            val hw = badgeSize * 0.27f
            moveTo(badgeCx - hw, centerY + badgeSize * 0.04f)
            cubicTo(
                badgeCx - hw * 0.35f, centerY - badgeSize * 0.22f,
                badgeCx + hw * 0.35f, centerY + badgeSize * 0.22f,
                badgeCx + hw, centerY - badgeSize * 0.04f,
            )
        }
        canvas.drawPath(
            wPath,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = badgeSize * 0.11f
                strokeCap = Paint.Cap.ROUND
                color = AColor.WHITE
            },
        )

        val fm = textPaint.fontMetrics
        val baseline = centerY - (fm.ascent + fm.descent) / 2f
        canvas.drawText(label, startX + badgeSize + gap, baseline, textPaint)
    }

    // ---------------------------------------------------------------------------
    // Mode Poster Lagu & Poster Lirik (Editorial / Liquid Glass / Obsidian / Blur)
    // ---------------------------------------------------------------------------

    /** Merender poster lengkap (Poster Lagu atau Poster Lirik) ke Bitmap resolusi tinggi. */
    suspend fun renderBitmap(
        song: SongItem,
        cover: Bitmap?,
        palette: CoverPalette,
        config: PosterConfig,
        selectedLyrics: List<String>,
        positionMs: Long,
    ): Bitmap = withContext(Dispatchers.Default) {
        val w = config.aspect.widthPx
        val h = config.aspect.heightPx
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        val effectiveBg = if (config.background.isTransparent) PosterBackground.LIQUID_GLASS else config.background
        drawBackground(canvas, w, h, effectiveBg, palette, cover)

        when (config.mode) {
            PosterMode.SONG_POSTER, PosterMode.PHOTO_CARDS -> drawSongPosterContent(
                canvas = canvas,
                w = w,
                h = h,
                song = song,
                cover = cover,
                palette = palette,
                config = config.copy(background = effectiveBg),
                quoteLine = selectedLyrics.firstOrNull(),
                positionMs = positionMs,
            )

            PosterMode.LYRIC_POSTER -> drawLyricPosterContent(
                canvas = canvas,
                w = w,
                h = h,
                song = song,
                cover = cover,
                palette = palette,
                config = config.copy(background = effectiveBg),
                lines = selectedLyrics,
            )
        }

        out
    }

    private fun drawBackground(
        canvas: Canvas,
        w: Int,
        h: Int,
        bg: PosterBackground,
        palette: CoverPalette,
        cover: Bitmap?,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        when (bg) {
            PosterBackground.LIQUID_GLASS -> {
                paint.shader = LinearGradient(
                    0f, 0f, w.toFloat(), h.toFloat(),
                    intArrayOf(
                        blendColor(AColor.rgb(12, 11, 14), palette.ambient.toArgb(), 0.42f),
                        blendColor(AColor.rgb(16, 14, 19), palette.secondaryAccent.toArgb(), 0.24f),
                        AColor.rgb(10, 9, 13),
                    ),
                    floatArrayOf(0f, 0.52f, 1f),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

                val orb1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        w * 0.22f, h * 0.16f, w * 0.85f,
                        intArrayOf(withAlpha(palette.secondaryAccent.toArgb(), 138), AColor.TRANSPARENT),
                        floatArrayOf(0f, 1f),
                        Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), orb1)

                val orb2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        w * 0.84f, h * 0.78f, w * 0.78f,
                        intArrayOf(withAlpha(palette.accent.toArgb(), 112), AColor.TRANSPARENT),
                        floatArrayOf(0f, 1f),
                        Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), orb2)
            }

            PosterBackground.EDITORIAL_PAPER -> {
                paint.shader = LinearGradient(
                    0f, 0f, 0f, h.toFloat(),
                    intArrayOf(AColor.rgb(246, 240, 231), AColor.rgb(237, 229, 216)),
                    null,
                    Shader.TileMode.CLAMP,
                )
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

                val tint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        w * 0.85f, h * 0.12f, w * 0.9f,
                        intArrayOf(withAlpha(palette.accent.toArgb(), 34), AColor.TRANSPARENT),
                        null,
                        Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), tint)
            }

            PosterBackground.OBSIDIAN -> {
                paint.shader = LinearGradient(
                    0f, 0f, 0f, h.toFloat(),
                    intArrayOf(AColor.rgb(20, 18, 17), AColor.rgb(11, 10, 9)),
                    null,
                    Shader.TileMode.CLAMP,
                )
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

                val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        w * 0.5f, h * 0.34f, w * 0.72f,
                        intArrayOf(withAlpha(palette.accent.toArgb(), 64), AColor.TRANSPARENT),
                        null,
                        Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), halo)
            }

            PosterBackground.COVER_BLUR, PosterBackground.TRANSPARENT -> {
                if (cover != null) {
                    val blurred = blurBitmap(cover, scaleDown = 8, radius = 12)
                    drawCenterCrop(canvas, blurred, RectF(0f, 0f, w.toFloat(), h.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
                    val scrim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(
                            0f, 0f, 0f, h.toFloat(),
                            intArrayOf(
                                AColor.argb(118, 10, 9, 12),
                                AColor.argb(148, 12, 10, 14),
                                AColor.argb(218, 8, 7, 10),
                            ),
                            floatArrayOf(0f, 0.5f, 1f),
                            Shader.TileMode.CLAMP,
                        )
                    }
                    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), scrim)
                } else {
                    drawBackground(canvas, w, h, PosterBackground.LIQUID_GLASS, palette, null)
                }
            }
        }
    }

    private fun drawSongPosterContent(
        canvas: Canvas,
        w: Int,
        h: Int,
        song: SongItem,
        cover: Bitmap?,
        palette: CoverPalette,
        config: PosterConfig,
        quoteLine: String?,
        positionMs: Long,
    ) {
        val isStory = config.aspect == PosterAspect.STORY_9_16
        val isSquare = config.aspect == PosterAspect.SQUARE_1_1
        val isPaper = config.background.isLightPaper
        val primaryColor = config.resolvePrimaryTextColor(palette).toArgb()
        val secondaryColor = config.resolveSecondaryTextColor(palette).toArgb()
        val accentColor = if (isPaper) blendColor(palette.accent.toArgb(), AColor.rgb(160, 67, 42), 0.35f) else palette.accent.toArgb()

        val marginX = w * 0.088f
        val topPad = if (isStory) h * 0.075f else h * 0.062f
        val bottomPad = if (isStory) h * 0.065f else h * 0.056f
        val contentW = w - marginX * 2

        if (config.background == PosterBackground.LIQUID_GLASS) {
            val panelRect = RectF(marginX * 0.52f, topPad * 0.64f, w - marginX * 0.52f, h - bottomPad * 0.64f)
            drawGlassPanel(canvas, panelRect, 56f)
        } else if (isPaper) {
            val frameRect = RectF(marginX * 0.56f, topPad * 0.62f, w - marginX * 0.56f, h - bottomPad * 0.62f)
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
                color = AColor.argb(46, 26, 22, 19)
            }
            canvas.drawRoundRect(frameRect, 36f, 36f, borderPaint)
        }

        val eyebrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 24f
            letterSpacing = 0.22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val eyebrowText = (song.album?.name?.takeIf { it.isNotBlank() } ?: "SEDANG DIPUTAR").uppercase()
        val eyebrowClip = TextUtils.ellipsize(eyebrowText, TextPaint(eyebrowPaint), contentW * 0.72f, TextUtils.TruncateAt.END).toString()
        val headerY = topPad + 30f
        canvas.drawText(eyebrowClip, marginX, headerY, eyebrowPaint)

        val issuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryColor
            textSize = 22f
            letterSpacing = 0.16f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        val tagText = config.aspect.ratioText
        val tagW = issuePaint.measureText(tagText)
        canvas.drawText(tagText, w - marginX - tagW, headerY, issuePaint)

        val artMaxSize = when (config.aspect) {
            PosterAspect.SQUARE_1_1 -> min(contentW * 0.54f, h * 0.44f)
            PosterAspect.POSTER_4_5 -> min(contentW * 0.86f, h * 0.52f)
            PosterAspect.STORY_9_16 -> min(contentW * 0.92f, h * 0.45f)
        }
        val artLeft = (w - artMaxSize) / 2f
        val artTop = headerY + if (isStory) 68f else if (isSquare) 32f else 44f
        val artRect = RectF(artLeft, artTop, artLeft + artMaxSize, artTop + artMaxSize)

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isPaper) AColor.argb(38, 28, 22, 18) else AColor.argb(128, 0, 0, 0)
        }
        canvas.drawRoundRect(
            RectF(artRect.left + 12f, artRect.top + 24f, artRect.right + 12f, artRect.bottom + 24f),
            44f, 44f, shadowPaint,
        )

        val artFramePad = 14f
        val frameRect = RectF(
            artRect.left - artFramePad,
            artRect.top - artFramePad,
            artRect.right + artFramePad,
            artRect.bottom + artFramePad,
        )
        canvas.drawRoundRect(
            frameRect,
            46f,
            46f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isPaper) AColor.argb(190, 255, 252, 247) else AColor.argb(26, 255, 255, 255)
            },
        )
        canvas.drawRoundRect(
            frameRect,
            46f,
            46f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
                color = if (isPaper) AColor.argb(42, 30, 24, 18) else AColor.argb(64, 255, 255, 255)
            },
        )
        drawRoundedArtwork(canvas, cover, artRect, 34f, isPaper)

        var cursorY = frameRect.bottom + if (isSquare) 26f else 40f

        if (config.showPaletteBar) {
            val swatches = PosterPaletteSwatches.buildSwatches(palette, isPaper)
            val gap = 14f
            val swatchW = (contentW - gap * (swatches.size - 1)) / swatches.size
            val swatchH = if (isSquare) 16f else 22f
            swatches.forEachIndexed { i, color ->
                val sx = marginX + i * (swatchW + gap)
                canvas.drawRoundRect(
                    RectF(sx, cursorY, sx + swatchW, cursorY + swatchH),
                    11f, 11f,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb() },
                )
            }
            cursorY += swatchH + if (isSquare) 26f else 42f
        }

        val titleSize = when (config.aspect) {
            PosterAspect.SQUARE_1_1 -> 54f
            PosterAspect.POSTER_4_5 -> 62f
            PosterAspect.STORY_9_16 -> 70f
        }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            textSize = titleSize
            typeface = resolveTypeface(config.font, config.weight)
        }
        val titleLayout = buildStaticLayout(song.title, titlePaint, contentW.roundToInt(), config.align, maxLines = 2)
        canvas.save()
        canvas.translate(marginX, cursorY)
        titleLayout.draw(canvas)
        canvas.restore()
        cursorY += titleLayout.height + 14f

        val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryColor
            textSize = if (isSquare) 32f else 36f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val artistLayout = buildStaticLayout(
            song.artistsText.ifBlank { "VYORA" },
            artistPaint,
            contentW.roundToInt(),
            config.align,
            maxLines = 1,
        )
        canvas.save()
        canvas.translate(marginX, cursorY)
        artistLayout.draw(canvas)
        canvas.restore()
        cursorY += artistLayout.height + if (isStory) 36f else 22f

        val footerY = h - bottomPad - 22f
        val reservedBottom = if (config.showProgressBar) 96f else 44f
        if (!quoteLine.isNullOrBlank() && cursorY + 70f < footerY - reservedBottom) {
            val quotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = withAlpha(primaryColor, 224)
                textSize = if (isStory) 36f else 30f
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            }
            val quoteLayout = buildStaticLayout(
                "“$quoteLine”",
                quotePaint,
                contentW.roundToInt(),
                config.align,
                maxLines = if (isStory) 3 else 2,
            )
            canvas.save()
            canvas.translate(marginX, cursorY)
            quoteLayout.draw(canvas)
            canvas.restore()
        }

        if (config.showProgressBar) {
            val totalMs = (song.durationSec.takeIf { it > 0 } ?: 210) * 1000L
            val progress = if (positionMs > 0L && totalMs > 0L) {
                (positionMs.toFloat() / totalMs.toFloat()).coerceIn(0.06f, 0.94f)
            } else {
                0.38f
            }
            val barY = footerY - 68f
            val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isPaper) AColor.argb(38, 26, 22, 19) else AColor.argb(56, 255, 255, 255)
            }
            canvas.drawRoundRect(RectF(marginX, barY, w - marginX, barY + 8f), 4f, 4f, trackPaint)
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
            val fillX = marginX + contentW * progress
            canvas.drawRoundRect(RectF(marginX, barY, fillX, barY + 8f), 4f, 4f, fillPaint)
            canvas.drawCircle(fillX, barY + 4f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryColor })

            val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = secondaryColor
                textSize = 22f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            }
            val posText = formatMs(if (positionMs > 0L) positionMs else (totalMs * progress).toLong())
            val durText = formatMs(totalMs)
            canvas.drawText(posText, marginX, barY + 38f, timePaint)
            val durW = timePaint.measureText(durText)
            canvas.drawText(durText, w - marginX - durW, barY + 38f, timePaint)
        }

        if (config.showWatermark) {
            drawFooterBrand(canvas, marginX, footerY + 12f, contentW, primaryColor, secondaryColor, accentColor)
        }
    }

    private fun drawLyricPosterContent(
        canvas: Canvas,
        w: Int,
        h: Int,
        song: SongItem,
        cover: Bitmap?,
        palette: CoverPalette,
        config: PosterConfig,
        lines: List<String>,
    ) {
        val isPaper = config.background.isLightPaper
        val primaryColor = config.resolvePrimaryTextColor(palette).toArgb()
        val secondaryColor = config.resolveSecondaryTextColor(palette).toArgb()
        val accentColor = if (isPaper) blendColor(palette.accent.toArgb(), AColor.rgb(160, 67, 42), 0.35f) else palette.accent.toArgb()

        val marginX = w * 0.095f
        val topPad = if (config.aspect == PosterAspect.STORY_9_16) h * 0.085f else h * 0.072f
        val bottomPad = if (config.aspect == PosterAspect.STORY_9_16) h * 0.075f else h * 0.064f
        val contentW = w - marginX * 2

        if (config.background == PosterBackground.LIQUID_GLASS) {
            drawGlassPanel(canvas, RectF(marginX * 0.50f, topPad * 0.62f, w - marginX * 0.50f, h - bottomPad * 0.62f), 58f)
        } else if (isPaper) {
            canvas.drawRoundRect(
                RectF(marginX * 0.56f, topPad * 0.62f, w - marginX * 0.56f, h - bottomPad * 0.62f),
                36f, 36f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 2.5f
                    color = AColor.argb(44, 26, 22, 19)
                },
            )
        }

        val thumbSize = 116f
        val thumbRect = RectF(marginX, topPad, marginX + thumbSize, topPad + thumbSize)
        drawRoundedArtwork(canvas, cover, thumbRect, 24f, isPaper)

        val metaX = thumbRect.right + 28f
        val metaW = (w - marginX - metaX).roundToInt().coerceAtLeast(100)
        val songTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            textSize = 36f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val titleLayout = buildStaticLayout(song.title, songTitlePaint, metaW, PosterAlign.START, maxLines = 1)
        canvas.save()
        canvas.translate(metaX, topPad + 16f)
        titleLayout.draw(canvas)
        canvas.restore()

        val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryColor
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val artistLayout = buildStaticLayout(song.artistsText.ifBlank { "VYORA" }, artistPaint, metaW, PosterAlign.START, maxLines = 1)
        canvas.save()
        canvas.translate(metaX, topPad + 64f)
        artistLayout.draw(canvas)
        canvas.restore()

        val quoteGlyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(accentColor, 185)
            textSize = 128f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }

        val lyricBody = lines.filter { it.isNotBlank() }.ifEmpty {
            listOf(song.title, song.artistsText.ifBlank { "Sedang diputar di VYORA" })
        }.joinToString("\n")

        val bodyTop = thumbRect.bottom + 72f
        val bodyBottom = h - bottomPad - if (config.showPaletteBar) 136f else 84f
        val availH = (bodyBottom - bodyTop).coerceAtLeast(240f)

        var textSize = when {
            lyricBody.length < 48 -> 78f
            lyricBody.length < 95 -> 66f
            lyricBody.length < 155 -> 56f
            else -> 46f
        }
        val lyricPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            this.textSize = textSize
            typeface = resolveTypeface(config.font, config.weight)
        }
        var lyricLayout = buildStaticLayout(lyricBody, lyricPaint, contentW.roundToInt(), config.align, maxLines = 10)
        while (lyricLayout.height > availH - 72f && textSize > 30f) {
            textSize -= 4f
            lyricPaint.textSize = textSize
            lyricLayout = buildStaticLayout(lyricBody, lyricPaint, contentW.roundToInt(), config.align, maxLines = 12)
        }

        val blockY = bodyTop + ((availH - lyricLayout.height) / 2f).coerceAtLeast(28f)
        val quoteX = when (config.align) {
            PosterAlign.START -> marginX - 6f
            PosterAlign.CENTER -> w / 2f - 28f
            PosterAlign.END -> w - marginX - 58f
        }
        canvas.drawText("“", quoteX, blockY + 12f, quoteGlyphPaint)

        canvas.save()
        canvas.translate(marginX, blockY)
        lyricLayout.draw(canvas)
        canvas.restore()

        val footerY = h - bottomPad - 10f
        if (config.showPaletteBar) {
            val swatches = PosterPaletteSwatches.buildSwatches(palette, isPaper)
            val dotR = 12f
            val step = 36f
            swatches.forEachIndexed { idx, c ->
                canvas.drawCircle(
                    marginX + dotR + idx * step,
                    footerY - 56f,
                    dotR,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.toArgb() },
                )
            }
        }

        if (config.showWatermark) {
            drawFooterBrand(canvas, marginX, footerY, contentW, primaryColor, secondaryColor, accentColor)
        }
    }

    private fun drawGlassPanel(canvas: Canvas, rect: RectF, radius: Float) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                intArrayOf(AColor.argb(30, 255, 255, 255), AColor.argb(14, 255, 255, 255)),
                null,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(rect, radius, radius, fill)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            shader = LinearGradient(
                rect.left, rect.top, rect.left, rect.bottom,
                intArrayOf(AColor.argb(88, 255, 255, 255), AColor.argb(24, 255, 255, 255)),
                null,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(rect, radius, radius, border)
    }

    private fun drawFooterBrand(
        canvas: Canvas,
        marginX: Float,
        baselineY: Float,
        contentW: Float,
        primaryColor: Int,
        secondaryColor: Int,
        accentColor: Int,
    ) {
        val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(primaryColor, 34)
            strokeWidth = 2f
        }
        canvas.drawLine(marginX, baselineY - 34f, marginX + contentW, baselineY - 34f, divider)

        canvas.drawCircle(marginX + 9f, baselineY - 7f, 7f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor })
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            textSize = 24f
            letterSpacing = 0.20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("VYORA", marginX + 28f, baselineY, brandPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryColor
            textSize = 21f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }
        val tag = "Poster Lagu"
        val tw = subPaint.measureText(tag)
        canvas.drawText(tag, marginX + contentW - tw, baselineY, subPaint)
    }

    private fun drawRoundedArtwork(canvas: Canvas, cover: Bitmap?, rect: RectF, radius: Float, isLightPaper: Boolean) {
        canvas.save()
        val path = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }
        canvas.clipPath(path)
        if (cover != null) {
            drawCenterCrop(canvas, cover, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        } else {
            canvas.drawRect(
                rect,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isLightPaper) AColor.rgb(214, 203, 189) else AColor.rgb(36, 32, 39)
                },
            )
        }
        canvas.restore()
    }

    private fun drawCenterCrop(canvas: Canvas, bitmap: Bitmap, dst: RectF, paint: Paint) {
        val scale = max(dst.width() / bitmap.width, dst.height() / bitmap.height)
        val scaledW = bitmap.width * scale
        val scaledH = bitmap.height * scale
        val left = dst.left + (dst.width() - scaledW) / 2f
        val top = dst.top + (dst.height() - scaledH) / 2f
        canvas.save()
        canvas.clipRect(dst)
        canvas.drawBitmap(bitmap, null, RectF(left, top, left + scaledW, top + scaledH), paint)
        canvas.restore()
    }

    private fun resolveTypeface(font: PosterFont, weight: PosterWeight): Typeface {
        val base = when (font) {
            PosterFont.MODERN -> Typeface.SANS_SERIF
            PosterFont.EDITORIAL, PosterFont.ITALIC_SERIF -> Typeface.SERIF
            PosterFont.MONO -> Typeface.MONOSPACE
        }
        val style = when {
            font.italicByDefault && weight == PosterWeight.BOLD -> Typeface.BOLD_ITALIC
            font.italicByDefault -> Typeface.ITALIC
            weight == PosterWeight.BOLD || weight == PosterWeight.MEDIUM -> Typeface.BOLD
            else -> Typeface.NORMAL
        }
        return Typeface.create(base, style)
    }

    private fun buildStaticLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        align: PosterAlign,
        maxLines: Int,
    ): StaticLayout {
        val layoutAlign = when (align) {
            PosterAlign.START -> Layout.Alignment.ALIGN_NORMAL
            PosterAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
            PosterAlign.END -> Layout.Alignment.ALIGN_OPPOSITE
        }
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
            .setAlignment(layoutAlign)
            .setLineSpacing(8f, 1.16f)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setMaxLines(maxLines)
            .build()
    }

    /** Box-blur 2 pass cepat murni Kotlin untuk menghasilkan latar kaca buram & Cover Blur halus. */
    fun blurBitmap(source: Bitmap, scaleDown: Int = 8, radius: Int = 12): Bitmap {
        val sw = (source.width / scaleDown).coerceAtLeast(24)
        val sh = (source.height / scaleDown).coerceAtLeast(24)
        val scaled = Bitmap.createScaledBitmap(source, sw, sh, true)
        val pixels = IntArray(sw * sh)
        scaled.getPixels(pixels, 0, sw, 0, 0, sw, sh)
        val temp = IntArray(sw * sh)

        repeat(2) {
            for (y in 0 until sh) {
                val rowOffset = y * sw
                for (x in 0 until sw) {
                    var r = 0
                    var g = 0
                    var b = 0
                    var count = 0
                    val x0 = max(0, x - radius)
                    val x1 = min(sw - 1, x + radius)
                    for (ix in x0..x1) {
                        val p = pixels[rowOffset + ix]
                        r += (p shr 16) and 0xFF
                        g += (p shr 8) and 0xFF
                        b += p and 0xFF
                        count++
                    }
                    temp[rowOffset + x] = AColor.rgb(r / count, g / count, b / count)
                }
            }
            for (x in 0 until sw) {
                for (y in 0 until sh) {
                    var r = 0
                    var g = 0
                    var b = 0
                    var count = 0
                    val y0 = max(0, y - radius)
                    val y1 = min(sh - 1, y + radius)
                    for (iy in y0..y1) {
                        val p = temp[iy * sw + x]
                        r += (p shr 16) and 0xFF
                        g += (p shr 8) and 0xFF
                        b += p and 0xFF
                        count++
                    }
                    pixels[y * sw + x] = AColor.rgb(r / count, g / count, b / count)
                }
            }
        }
        val blurred = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
        blurred.setPixels(pixels, 0, sw, 0, 0, sw, sh)
        return blurred
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        AColor.argb(alpha.coerceIn(0, 255), AColor.red(color), AColor.green(color), AColor.blue(color))

    private fun blendColor(a: Int, b: Int, ratio: Float): Int {
        val inv = 1f - ratio
        return AColor.rgb(
            (AColor.red(a) * inv + AColor.red(b) * ratio).roundToInt(),
            (AColor.green(a) * inv + AColor.green(b) * ratio).roundToInt(),
            (AColor.blue(a) * inv + AColor.blue(b) * ratio).roundToInt(),
        )
    }

    // ---------------------------------------------------------------------------
    // Ekspor: Simpan ke Galeri (MediaStore) & Bagikan PNG (FileProvider)
    // ---------------------------------------------------------------------------

    /** Menyimpan poster PNG ke Galeri (`Pictures/VYORA`) melalui MediaStore (tanpa izin penyimpanan di Android 10+). */
    suspend fun saveToGallery(context: Context, bitmap: Bitmap, title: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val safeName = title.replace(Regex("[^a-zA-Z0-9]+"), "_").trim('_').ifBlank { "lagu" }.take(32)
            val fileName = "VYORA_${safeName}_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/VYORA")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Gagal membuat berkas gambar di Galeri")
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: error("Gagal menulis gambar ke Galeri")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            fileName
        }
    }

    /** Membagikan poster PNG ke Instagram Story, TikTok, WhatsApp, dll. lewat FileProvider. */
    suspend fun shareImage(context: Context, bitmap: Bitmap, song: SongItem): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
            val file = File(dir, "poster_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "${song.title} — ${song.artistsText} · VYORA")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            withContext(Dispatchers.Main) {
                context.startActivity(Intent.createChooser(intent, "Bagikan Kartu & Poster Lagu"))
            }
        }
    }

    /** Membagikan lagu + kutipan lirik sebagai teks biasa. */
    fun shareAsText(context: Context, song: SongItem, lines: List<String>) {
        val quote = lines.filter { it.isNotBlank() }.joinToString("\n").takeIf { it.isNotBlank() }
        val body = buildString {
            if (quote != null) {
                append("“$quote”\n\n")
            }
            append("🎵 ${song.title} — ${song.artistsText}\n")
            append("https://music.youtube.com/watch?v=${song.id}")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Lagu"))
    }
}
