package com.vyora.music.data.offline

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Pengelola cache stream audio ExoPlayer (`SimpleCache`).
 *
 * Lagu yang diputar lewat internet otomatis disimpan di cache (LRU) dengan kunci
 * tetap per `videoId` (`ytm_audio_{videoId}`). Saat diputar ulang:
 * - Tidak makan kuota internet sama sekali
 * - Langsung berbunyi instan meskipun sinyal hilang (offline)
 */
@OptIn(UnstableApi::class)
object StreamCacheHolder {

    private const val DIR_NAME = "vyora_stream_cache"

    @Volatile private var cache: SimpleCache? = null
    @Volatile private var configuredMaxBytes: Long = -1L

    @Synchronized
    fun get(context: Context, maxBytes: Long): SimpleCache? {
        if (maxBytes <= 0L) return null
        val existing = cache
        if (existing != null && configuredMaxBytes == maxBytes) return existing
        existing?.let { runCatching { it.release() } }
        return runCatching {
            val dir = File(context.applicationContext.cacheDir, DIR_NAME).apply { mkdirs() }
            val dbProvider = StandaloneDatabaseProvider(context.applicationContext)
            SimpleCache(dir, LeastRecentlyUsedCacheEvictor(maxBytes), dbProvider).also {
                cache = it
                configuredMaxBytes = maxBytes
            }
        }.getOrNull()
    }

    fun usedBytes(context: Context): Long {
        val active = cache?.cacheSpace
        if (active != null && active >= 0L) return active
        val dir = File(context.applicationContext.cacheDir, DIR_NAME)
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    @Synchronized
    fun clear(context: Context) {
        val prevLimit = configuredMaxBytes
        cache?.let { runCatching { it.release() } }
        cache = null
        configuredMaxBytes = -1L
        val dir = File(context.applicationContext.cacheDir, DIR_NAME)
        runCatching { dir.deleteRecursively() }
        if (prevLimit > 0L) {
            get(context, prevLimit)
        }
    }
}
