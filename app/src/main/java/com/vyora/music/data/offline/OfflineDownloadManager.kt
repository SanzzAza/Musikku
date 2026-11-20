package com.vyora.music.data.offline

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vyora.music.AppModule
import com.vyora.music.data.ytmusic.AudioQuality
import com.vyora.music.data.ytmusic.SongItem
import com.vyora.music.data.ytmusic.StreamResolver
import com.vyora.music.data.ytmusic.YTMusic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

data class DownloadedSong(
    val song: SongItem,
    val audioFileName: String,
    val coverFileName: String? = null,
    val sizeBytes: Long = 0L,
    val downloadedAt: Long = System.currentTimeMillis(),
)

/**
 * Pengelola Unduhan Lagu Offline Permanen VYORA.
 *
 * Berbeda dari auto-cache sementara (LRU), lagu yang diunduh di sini:
 * - Disimpan permanen di penyimpanan aplikasi (`filesDir/offline_audio`)
 * - Sampul album & lirik lagu ikut disimpan lokal agar muncul penuh saat mode pesawat
 * - Bisa dikelola lewat menu Pustaka -> "Unduhan Offline"
 */
class OfflineDownloadManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("vyora_offline_downloads", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val audioDir = File(appContext.filesDir, "offline_audio").apply { mkdirs() }
    private val coverDir = File(appContext.filesDir, "offline_covers").apply { mkdirs() }

    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<DownloadedSong>> = _items.asStateFlow()

    /** Progress unduhan aktif: videoId -> 0f..1f */
    private val _progress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val progress: StateFlow<Map<String, Float>> = _progress.asStateFlow()

    /** Pesan status / error terakhir (opsional untuk Toast/UI). */
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val activeJobs = ConcurrentHashMap<String, Job>()

    private fun load(): List<DownloadedSong> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<DownloadedSong>>() {}.type
            val parsed: List<DownloadedSong> = gson.fromJson(raw, type) ?: emptyList()
            // Hanya pertahankan entri yang file audionya masih utuh di disk
            parsed.filter { entry ->
                val f = File(audioDir, entry.audioFileName)
                f.exists() && f.length() > 1024L
            }
        }.getOrDefault(emptyList())
    }

    private fun persist(list: List<DownloadedSong>) {
        _items.value = list
        prefs.edit().putString(KEY_ITEMS, gson.toJson(list)).apply()
    }

    fun isDownloaded(videoId: String): Boolean =
        _items.value.any { it.song.id == videoId }

    fun isDownloading(videoId: String): Boolean =
        _progress.value.containsKey(videoId)

    fun localAudioFile(videoId: String): File? {
        val entry = _items.value.firstOrNull { it.song.id == videoId } ?: return null
        val f = File(audioDir, entry.audioFileName)
        return f.takeIf { it.exists() && it.length() > 1024L }
    }

    fun localCoverUri(videoId: String): String? {
        val entry = _items.value.firstOrNull { it.song.id == videoId } ?: return null
        val name = entry.coverFileName ?: return null
        val f = File(coverDir, name)
        return if (f.exists() && f.length() > 128L) Uri.fromFile(f).toString() else null
    }

    fun totalBytes(): Long = _items.value.sumOf { it.sizeBytes }

    fun toggle(song: SongItem) {
        if (isDownloaded(song.id) || isDownloading(song.id)) {
            remove(song.id)
        } else {
            download(song)
        }
    }

    fun download(song: SongItem) {
        if (song.localUri != null) return // Lagu lokal sudah di perangkat
        if (isDownloaded(song.id) || activeJobs.containsKey(song.id)) return

        _progress.update { it + (song.id to 0.01f) }
        val job = scope.launch {
            val safeId = song.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val tempFile = File(audioDir, "$safeId.part")
            val finalName = "$safeId.audio"
            val finalFile = File(audioDir, finalName)

            try {
                val streamUrl = StreamResolver.audioUrl(song.id, AudioQuality.HIGH)
                val req = Request.Builder()
                    .url(streamUrl)
                    .header("User-Agent", YTMusic.USER_AGENT)
                    .build()

                StreamResolver.httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw IllegalStateException("HTTP ${resp.code}")
                    }
                    val body = resp.body ?: throw IllegalStateException("Stream kosong")
                    val total = body.contentLength()
                    var downloaded = 0L
                    var lastReportedPct = 0

                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buf = ByteArray(16 * 1024)
                            while (isActive) {
                                val read = input.read(buf)
                                if (read < 0) break
                                output.write(buf, 0, read)
                                downloaded += read
                                if (total > 0L) {
                                    val pct = ((downloaded * 100L) / total).toInt().coerceIn(1, 99)
                                    if (pct >= lastReportedPct + 2) {
                                        lastReportedPct = pct
                                        _progress.update { it + (song.id to (pct / 100f)) }
                                    }
                                }
                            }
                            output.flush()
                        }
                    }
                }

                if (!isActive || tempFile.length() <= 1024L) {
                    tempFile.delete()
                    return@launch
                }

                if (finalFile.exists()) finalFile.delete()
                if (!tempFile.renameTo(finalFile)) {
                    tempFile.copyTo(finalFile, overwrite = true)
                    tempFile.delete()
                }

                // Unduh juga sampul album untuk tampilan offline
                val coverName = downloadCoverIfAvailable(safeId, song.thumbnail)
                val offlineSong = if (coverName != null) {
                    song.copy(thumbnail = Uri.fromFile(File(coverDir, coverName)).toString())
                } else {
                    song
                }

                // Simpan juga lirik ke cache lokal supaya lirik muncul saat mode pesawat
                runCatching {
                    AppModule.lyrics.getLyrics(
                        trackId = song.id,
                        title = song.title,
                        artist = song.artists.firstOrNull()?.name.orEmpty(),
                        album = song.album?.name,
                        durationSec = song.durationSec,
                    )
                }

                val newEntry = DownloadedSong(
                    song = offlineSong,
                    audioFileName = finalName,
                    coverFileName = coverName,
                    sizeBytes = finalFile.length(),
                    downloadedAt = System.currentTimeMillis(),
                )
                synchronized(this@OfflineDownloadManager) {
                    val updated = listOf(newEntry) + _items.value.filterNot { it.song.id == song.id }
                    persist(updated)
                }
            } catch (e: CancellationException) {
                tempFile.delete()
                throw e
            } catch (e: Exception) {
                tempFile.delete()
                _lastError.value = "Gagal mengunduh \"${song.title}\": ${e.message ?: "kesalahan jaringan"}"
            } finally {
                activeJobs.remove(song.id)
                _progress.update { it - song.id }
            }
        }
        activeJobs[song.id] = job
    }

    private fun downloadCoverIfAvailable(safeId: String, url: String?): String? {
        if (url.isNullOrBlank() || !url.startsWith("http")) return null
        val name = "$safeId.jpg"
        val dest = File(coverDir, name)
        return runCatching {
            val req = Request.Builder().url(url).build()
            StreamResolver.httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val bytes = resp.body?.bytes() ?: return null
                if (bytes.size < 128) return null
                dest.writeBytes(bytes)
                name
            }
        }.getOrNull()
    }

    fun remove(videoId: String) {
        activeJobs.remove(videoId)?.cancel()
        _progress.update { it - videoId }
        synchronized(this) {
            val target = _items.value.firstOrNull { it.song.id == videoId }
            if (target != null) {
                runCatching { File(audioDir, target.audioFileName).delete() }
                target.coverFileName?.let { runCatching { File(coverDir, it).delete() } }
            }
            persist(_items.value.filterNot { it.song.id == videoId })
        }
    }

    fun clearAll() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        _progress.value = emptyMap()
        synchronized(this) {
            runCatching { audioDir.listFiles()?.forEach { it.delete() } }
            runCatching { coverDir.listFiles()?.forEach { it.delete() } }
            persist(emptyList())
        }
    }

    private companion object {
        const val KEY_ITEMS = "downloaded_songs_v1"
    }
}
