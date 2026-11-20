package com.vyora.music.data.lyrics

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.vyora.music.data.ytmusic.StreamResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Penerjemah Lirik Otomatis ke Bahasa Indonesia (100% Guest, tanpa API key)
 * lengkap dengan penyimpanan cache di disk agar terjemahan tetap tersedia saat offline.
 */
class LyricsTranslator(context: Context? = null) {

    private val cacheDir: File? = context?.applicationContext?.filesDir?.let {
        File(it, "lyrics_translations").apply { mkdirs() }
    }
    private val gson = Gson()
    private val memCache = ConcurrentHashMap<String, Map<Int, String>>()

    suspend fun translateLines(
        trackId: String,
        lines: List<String>,
        targetLang: String = "id",
    ): Map<Int, String> = withContext(Dispatchers.IO) {
        val cacheKey = "${trackId}_$targetLang"
        memCache[cacheKey]?.let { return@withContext it }
        loadFromDisk(cacheKey)?.let {
            memCache[cacheKey] = it
            return@withContext it
        }

        val result = LinkedHashMap<Int, String>()
        // Hanya terjemahkan baris yang berisi teks (bukan kosong / simbol musik)
        val nonBlank = lines.mapIndexedNotNull { idx, text ->
            val trimmed = text.trim()
            if (trimmed.isBlank() || trimmed == "♪" || trimmed == "...") null else idx to trimmed
        }
        if (nonBlank.isEmpty()) return@withContext emptyMap()

        // Kirim per batch (maks ~1800 karakter per request) memakai pemisah baris
        var cursor = 0
        while (cursor < nonBlank.size) {
            val batch = ArrayList<Pair<Int, String>>()
            var charCount = 0
            while (cursor < nonBlank.size) {
                val item = nonBlank[cursor]
                if (batch.isNotEmpty() && charCount + item.second.length > 1800) break
                batch.add(item)
                charCount += item.second.length + 3
                cursor++
            }
            val joined = batch.joinToString("\n") { it.second }
            val translatedBlock = requestGtxTranslate(joined, targetLang)
            if (translatedBlock != null) {
                val split = translatedBlock.split("\n")
                batch.forEachIndexed { i, (lineIndex, originalText) ->
                    val tr = split.getOrNull(i)?.trim().orEmpty()
                    if (tr.isNotBlank() && !tr.equals(originalText, ignoreCase = true)) {
                        result[lineIndex] = tr
                    }
                }
            }
        }

        if (result.isNotEmpty()) {
            memCache[cacheKey] = result
            saveToDisk(cacheKey, result)
        }
        result
    }

    private fun requestGtxTranslate(text: String, targetLang: String): String? = runCatching {
        val url = "https://translate.googleapis.com/translate_a/single".toHttpUrl().newBuilder()
            .addQueryParameter("client", "gtx")
            .addQueryParameter("sl", "auto")
            .addQueryParameter("tl", targetLang)
            .addQueryParameter("dt", "t")
            .addQueryParameter("q", text)
            .build()

        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        StreamResolver.httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            val root = JsonParser.parseString(body)
            if (!root.isJsonArray) return null
            val sentences = root.asJsonArray.firstOrNull()?.takeIf { it.isJsonArray }?.asJsonArray ?: return null
            val sb = StringBuilder()
            for (el in sentences) {
                if (el.isJsonArray && el.asJsonArray.size() > 0) {
                    val part = el.asJsonArray[0]
                    if (!part.isJsonNull) sb.append(part.asString)
                }
            }
            sb.toString().takeIf { it.isNotBlank() }
        }
    }.getOrNull()

    private fun safeFileName(key: String): String =
        key.replace(Regex("[^a-zA-Z0-9_-]"), "_") + ".json"

    private fun loadFromDisk(key: String): Map<Int, String>? {
        val dir = cacheDir ?: return null
        val file = File(dir, safeFileName(key))
        if (!file.exists()) return null
        return runCatching {
            val type = object : TypeToken<Map<Int, String>>() {}.type
            gson.fromJson<Map<Int, String>>(file.readText(), type)
        }.getOrNull()
    }

    private fun saveToDisk(key: String, data: Map<Int, String>) {
        val dir = cacheDir ?: return
        runCatching {
            File(dir, safeFileName(key)).writeText(gson.toJson(data))
        }
    }
}
