package com.vyora.music.data.lyrics

import android.content.Context
import com.google.gson.Gson
import java.io.File

data class CachedLyricsPayload(
    val type: String, // "SYNCED" | "PLAIN" | "INSTRUMENTAL"
    val source: String = "Cache",
    val lines: List<LyricLine> = emptyList(),
    val plainText: String? = null,
    val savedAt: Long = System.currentTimeMillis(),
)

/**
 * Penyimpanan cache lirik di disk (`filesDir/lyrics_cache`) agar lirik
 * (termasuk timing karaoke per-kata) langsung terbuka 0ms dan jalan saat offline.
 */
class LyricsCacheStore(context: Context) {

    private val dir = File(context.applicationContext.filesDir, "lyrics_cache").apply { mkdirs() }
    private val gson = Gson()

    private fun fileFor(trackId: String): File {
        val safe = trackId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(dir, "$safe.json")
    }

    fun get(trackId: String): Lyrics? {
        val f = fileFor(trackId)
        if (!f.exists()) return null
        return runCatching {
            val p = gson.fromJson(f.readText(), CachedLyricsPayload::class.java) ?: return null
            when (p.type) {
                "SYNCED" -> if (p.lines.isNotEmpty()) Lyrics.Synced(p.lines, p.source) else null
                "PLAIN" -> p.plainText?.takeIf { it.isNotBlank() }?.let { Lyrics.Plain(it, p.source) }
                "INSTRUMENTAL" -> Lyrics.Instrumental
                else -> null
            }
        }.getOrNull()
    }

    fun put(trackId: String, lyrics: Lyrics) {
        val payload = when (lyrics) {
            is Lyrics.Synced -> CachedLyricsPayload(
                type = "SYNCED",
                source = lyrics.source,
                lines = lyrics.lines,
            )
            is Lyrics.Plain -> CachedLyricsPayload(
                type = "PLAIN",
                source = lyrics.source,
                plainText = lyrics.text,
            )
            Lyrics.Instrumental -> CachedLyricsPayload(type = "INSTRUMENTAL", source = "LRCLIB")
            Lyrics.NotFound -> return
        }
        runCatching {
            fileFor(trackId).writeText(gson.toJson(payload))
        }
    }
}
