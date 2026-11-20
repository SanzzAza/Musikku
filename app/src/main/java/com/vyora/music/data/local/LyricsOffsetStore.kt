package com.vyora.music.data.local

import android.content.Context

/**
 * Aturan pergeseran waktu lirik, dipisah dari Android supaya bisa diuji unit test.
 *
 * Nilai positif = lirik ditampilkan lebih lambat (bagi lagu yang liriknya
 * muncul kecepatan), negatif = lebih awal.
 */
object LyricsOffset {
    const val MIN_MS = -5_000L
    const val MAX_MS = 5_000L
    const val STEP_MS = 250L

    /** Batasi pergeseran supaya tidak ekstrem (maksimal ±5 detik). */
    fun clamp(valueMs: Long): Long = valueMs.coerceIn(MIN_MS, MAX_MS)

    /** Hasil satu ketukan tombol geser. */
    fun step(currentMs: Long, deltaMs: Long): Long = clamp(currentMs + deltaMs)

    fun format(valueMs: Long): String = when {
        valueMs == 0L -> "Sinkron"
        valueMs > 0L -> "+${"%.2f".format(valueMs / 1000.0)} s"
        else -> "${"%.2f".format(valueMs / 1000.0)} s"
    }
}

/** Menyimpan pergeseran lirik per lagu (videoId). */
class LyricsOffsetStore(context: Context) {

    private val prefs = context.getSharedPreferences("lyrics_offset", Context.MODE_PRIVATE)

    fun offsetFor(songId: String): Long =
        runCatching { prefs.getLong(key(songId), 0L) }.getOrDefault(0L)

    /** Simpan; nilai 0 menghapus entri supaya tidak menumpuk. */
    fun setOffset(songId: String, offsetMs: Long) {
        val value = LyricsOffset.clamp(offsetMs)
        prefs.edit().apply {
            if (value == 0L) remove(key(songId)) else putLong(key(songId), value)
        }.apply()
    }

    private fun key(songId: String) = "off_$songId"
}
