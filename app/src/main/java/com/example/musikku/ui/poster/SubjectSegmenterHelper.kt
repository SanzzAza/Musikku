package com.example.musikku.ui.poster

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.roundToInt

/** Status pemrosesan segmentasi subjek foto (efek kartu melayang di belakang objek/orang). */
sealed interface SubjectMaskState {
    data object Idle : SubjectMaskState
    data object Processing : SubjectMaskState
    data class Ready(val maskBitmap: Bitmap) : SubjectMaskState
    data class Unavailable(val reason: String) : SubjectMaskState
}

/**
 * Helper segmentasi subjek on-device berbasis ML Kit Subject Segmentation.
 * Memisahkan orang/subjek utama di foto sehingga kartu pemutar Liquid Glass bisa dirender
 * persis di belakang subjek (efek kedalaman/depth).
 */
object SubjectSegmenterHelper {

    private val segmenter by lazy {
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build()
        SubjectSegmentation.getClient(options)
    }

    /** Memotong foto secara center-crop ke dimensi kanvas [targetW] × [targetH] secara presisi. */
    fun centerCropBitmap(source: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val out = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val scale = max(targetW.toFloat() / source.width, targetH.toFloat() / source.height)
        val scaledW = source.width * scale
        val scaledH = source.height * scale
        val left = ((targetW - scaledW) / 2f).roundToInt()
        val top = ((targetH - scaledH) / 2f).roundToInt()
        val dst = Rect(left, top, left + scaledW.roundToInt(), top + scaledH.roundToInt())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, null, dst, paint)
        return out
    }

    /**
     * Memisahkan subjek depan dari [croppedPhoto] (yang sudah dipotong sesuai rasio kanvas).
     * Mengembalikan potongan subjek transparan [Bitmap] atau error ramah pengguna bila gagal/model belum siap.
     */
    suspend fun segmentForeground(croppedPhoto: Bitmap): SubjectMaskState = withContext(Dispatchers.Default) {
        try {
            // Skala kerja max 960px agar proses ML Kit cepat (~200-400ms) dan hemat memori.
            val maxDim = max(croppedPhoto.width, croppedPhoto.height)
            val working = if (maxDim > 960) {
                val ratio = 960f / maxDim
                Bitmap.createScaledBitmap(
                    croppedPhoto,
                    (croppedPhoto.width * ratio).roundToInt().coerceAtLeast(1),
                    (croppedPhoto.height * ratio).roundToInt().coerceAtLeast(1),
                    true,
                )
            } else {
                croppedPhoto
            }

            val input = InputImage.fromBitmap(working, 0)
            val fg = suspendCancellableCoroutine<Result<Bitmap?>> { cont ->
                segmenter.process(input)
                    .addOnSuccessListener { result ->
                        if (cont.isActive) cont.resume(Result.success(result.foregroundBitmap))
                    }
                    .addOnFailureListener { error ->
                        if (cont.isActive) cont.resume(Result.failure(error))
                    }
            }

            val rawFg = fg.getOrElse { err ->
                val msg = err.localizedMessage.orEmpty()
                return@withContext SubjectMaskState.Unavailable(
                    if (msg.contains("waiting", ignoreCase = true) || msg.contains("download", ignoreCase = true)) {
                        "Model AI subjek sedang diunduh Google Play, coba beberapa detik lagi"
                    } else {
                        "Efek subjek belum tersedia di perangkat ini"
                    }
                )
            }

            if (rawFg == null) {
                return@withContext SubjectMaskState.Unavailable("Subjek utama tidak terdeteksi di foto ini")
            }

            // Pastikan hasil dikembalikan dalam ukuran penuh [croppedPhoto] agar presisi 1:1 saat dirender
            val aligned = if (rawFg.width != croppedPhoto.width || rawFg.height != croppedPhoto.height) {
                Bitmap.createScaledBitmap(rawFg, croppedPhoto.width, croppedPhoto.height, true)
            } else {
                rawFg.copy(Bitmap.Config.ARGB_8888, false)
            }
            SubjectMaskState.Ready(aligned)
        } catch (_: Throwable) {
            SubjectMaskState.Unavailable("Efek subjek membutuhkan layanan Google Play")
        }
    }
}
