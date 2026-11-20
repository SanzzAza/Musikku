package com.vyora.music.data.ytmusic

/**
 * Kandidat stream audio yang sudah dinormalisasi dari NewPipe.
 * Sengaja dipisah dari tipe NewPipe supaya logika pemilihan bisa diuji unit test
 * tanpa perlu Android runtime.
 */
data class AudioCandidate(
    val url: String,
    /** Bitrate rata-rata dalam kbps; 0 kalau tidak diketahui. */
    val bitrateKbps: Int,
    /** Contoh: "audio/webm", "audio/mp4", "audio/ogg". */
    val mimeType: String?,
    /** true kalau NewPipe menandainya sebagai stream HTTP progressive. */
    val isProgressive: Boolean,
)

/**
 * Tingkat kualitas audio yang bisa dipilih pengguna.
 * Nama enum disimpan sebagai String di SharedPreferences, jadi jangan diubah
 * tanpa migrasi (lihat keep-rule di proguard-rules.pro).
 */
enum class AudioQuality(val label: String, val hint: String) {
    HIGH("Tinggi", "Bitrate tertinggi yang tersedia"),
    NORMAL("Otomatis", "Sekitar 160 kbps, seimbang"),
    LOW("Hemat", "Bitrate terendah, hemat kuota"),
}

/**
 * Pemilihan stream audio terbaik dari daftar kandidat.
 *
 * Sebelumnya resolusi hanya menerima stream `PROGRESSIVE_HTTP` lalu mengambil
 * bitrate tertinggi. Akibatnya kandidat yang sebenarnya bisa diputar (misalnya
 * URL audio-only dari jalur lain) ikut terbuang, dan tidak ada cara menurunkan
 * kualitas saat kuota terbatas.
 */
object AudioStreamSelector {

    private fun isUsableUrl(url: String): Boolean =
        url.startsWith("https://") || url.startsWith("http://")

    /** Opus/WebM diutamakan: kualitas per bit lebih baik pada bitrate yang sama. */
    private fun codecScore(mimeType: String?): Int {
        val mime = mimeType?.lowercase() ?: return 0
        return when {
            mime.contains("opus") || mime.contains("webm") -> 3
            mime.contains("mp4") || mime.contains("aac") || mime.contains("m4a") -> 2
            mime.contains("ogg") || mime.contains("vorbis") -> 2
            mime.contains("mpeg") || mime.contains("mp3") -> 1
            else -> 0
        }
    }

    /**
     * @return kandidat terbaik, atau null kalau tidak ada URL yang bisa dipakai.
     */
    fun pick(
        candidates: List<AudioCandidate>,
        quality: AudioQuality = AudioQuality.HIGH,
    ): AudioCandidate? {
        val usable = candidates.filter { isUsableUrl(it.url) }
        if (usable.isEmpty()) return null

        val ranked = usable.sortedWith(
            compareByDescending<AudioCandidate> { it.bitrateKbps }
                .thenByDescending { codecScore(it.mimeType) }
                .thenByDescending { it.isProgressive }
        )

        return when (quality) {
            // Bitrate tertinggi yang tersedia.
            AudioQuality.HIGH -> ranked.first()
            // Sedekat mungkin dengan 160 kbps; kalau tidak ada, tetap pakai terbaik.
            AudioQuality.NORMAL -> ranked.firstOrNull { it.bitrateKbps in 1..160 } ?: ranked.first()
            // Paling hemat: bitrate terendah yang diketahui, jatuh ke terendah yang ada.
            AudioQuality.LOW -> ranked.lastOrNull { it.bitrateKbps in 1..96 } ?: ranked.last()
        }
    }
}
