package com.vyora.music.data.local

import android.content.Context
import com.google.gson.Gson
import com.vyora.music.data.ytmusic.SongItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

enum class StatsPeriod(val label: String, val windowDays: Int?) {
    WEEK("7 Hari", 7),
    MONTH("30 Hari", 30),
    ALL_TIME("Semua Waktu", null),
}

enum class StreakTier(val title: String, val minDays: Int, val badge: String) {
    NEWCOMER("Pendengar Baru", 0, "✦"),
    GROOVE("Penikmat Nada", 3, "🔥"),
    DEVOTED("Pecinta Musik", 7, "⚡"),
    MAESTRO("Maestro VYORA", 14, "👑"),
    ;

    companion object {
        fun fromDays(days: Int): StreakTier = entries.lastOrNull { days >= it.minDays } ?: NEWCOMER
    }
}

data class PlayEventRecord(
    val song: SongItem,
    val timestampMs: Long,
    val listenedSec: Int,
)

data class ListeningStatsData(
    val totalListenSec: Long = 0L,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val lastListenEpochDay: Long = -1L,
    val events: List<PlayEventRecord> = emptyList(),
)

data class RankedSongStat(
    val song: SongItem,
    val playCount: Int,
    val totalSec: Int,
)

data class RankedArtistStat(
    val artistName: String,
    val artistId: String?,
    val thumbnail: String?,
    val playCount: Int,
    val totalSec: Int,
)

data class StatsSummary(
    val period: StatsPeriod,
    val totalMinutes: Long,
    val totalPlays: Int,
    val uniqueSongs: Int,
    val uniqueArtists: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val streakTier: StreakTier,
    val topSongs: List<RankedSongStat>,
    val topArtists: List<RankedArtistStat>,
)

/**
 * Pencatat Statistik Mendengarkan, Listening Streak Harian, & VYORA Recap (Wrapped).
 */
class ListeningStatsStore(context: Context) {

    private val prefs = context.getSharedPreferences("vyora_listening_stats", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _data = MutableStateFlow(load())
    val data: StateFlow<ListeningStatsData> = _data.asStateFlow()

    private fun load(): ListeningStatsData {
        val raw = prefs.getString(KEY_STATS, null) ?: return ListeningStatsData()
        return runCatching {
            gson.fromJson(raw, ListeningStatsData::class.java) ?: ListeningStatsData()
        }.getOrDefault(ListeningStatsData())
    }

    private fun save(next: ListeningStatsData) {
        _data.value = next
        prefs.edit().putString(KEY_STATS, gson.toJson(next)).apply()
    }

    private fun currentEpochDay(nowMs: Long = System.currentTimeMillis()): Long =
        TimeUnit.MILLISECONDS.toDays(nowMs)

    /** Dipanggil berkala (mis. tiap 5 detik) saat pengguna benar-benar mendengarkan lagu. */
    @Synchronized
    fun addListenSeconds(song: SongItem, deltaSec: Int, nowMs: Long = System.currentTimeMillis()) {
        if (deltaSec <= 0) return
        val cur = _data.value
        val today = currentEpochDay(nowMs)

        val (nextStreak, nextLongest) = when {
            cur.lastListenEpochDay < 0L -> 1 to maxOf(1, cur.longestStreakDays)
            today == cur.lastListenEpochDay -> cur.currentStreakDays.coerceAtLeast(1) to cur.longestStreakDays
            today == cur.lastListenEpochDay + 1L -> {
                val s = cur.currentStreakDays + 1
                s to maxOf(s, cur.longestStreakDays)
            }
            today > cur.lastListenEpochDay + 1L -> 1 to maxOf(1, cur.longestStreakDays)
            else -> cur.currentStreakDays to cur.longestStreakDays
        }

        // Gabungkan ke event terakhir bila masih lagu yang sama dalam 10 menit terakhir,
        // atau buat event putar baru bila lagu berganti.
        val events = cur.events.toMutableList()
        val last = events.firstOrNull()
        if (last != null && last.song.id == song.id && (nowMs - last.timestampMs) < 10 * 60_000L) {
            events[0] = last.copy(listenedSec = last.listenedSec + deltaSec)
        } else {
            events.add(0, PlayEventRecord(song = song, timestampMs = nowMs, listenedSec = deltaSec))
            if (events.size > MAX_EVENTS) {
                events.subList(MAX_EVENTS, events.size).clear()
            }
        }

        save(
            cur.copy(
                totalListenSec = cur.totalListenSec + deltaSec,
                currentStreakDays = nextStreak,
                longestStreakDays = nextLongest,
                lastListenEpochDay = maxOf(today, cur.lastListenEpochDay),
                events = events,
            )
        )
    }

    fun summarize(period: StatsPeriod, nowMs: Long = System.currentTimeMillis()): StatsSummary {
        val snap = _data.value
        val filtered = if (period.windowDays == null) {
            snap.events
        } else {
            val cutoff = nowMs - TimeUnit.DAYS.toMillis(period.windowDays.toLong())
            snap.events.filter { it.timestampMs >= cutoff }
        }

        // Hanya hitung putaran yang didengar minimal 10 detik sebagai "play" sah
        val valid = filtered.filter { it.listenedSec >= 10 }
        val periodSec = if (period == StatsPeriod.ALL_TIME) {
            maxOf(snap.totalListenSec, filtered.sumOf { it.listenedSec.toLong() })
        } else {
            filtered.sumOf { it.listenedSec.toLong() }
        }

        val topSongs = valid
            .groupBy { it.song.id }
            .map { (_, plays) ->
                RankedSongStat(
                    song = plays.first().song,
                    playCount = plays.size,
                    totalSec = plays.sumOf { it.listenedSec },
                )
            }
            .sortedWith(compareByDescending<RankedSongStat> { it.playCount }.thenByDescending { it.totalSec })
            .take(15)

        val topArtists = valid
            .mapNotNull { rec ->
                val primary = rec.song.artists.firstOrNull()
                val name = (primary?.name ?: rec.song.artistsText).trim()
                if (name.isBlank()) null else Triple(name, primary?.id, rec)
            }
            .groupBy { it.first.lowercase() }
            .map { (_, entries) ->
                val sample = entries.first()
                RankedArtistStat(
                    artistName = sample.first,
                    artistId = sample.second,
                    thumbnail = entries.firstNotNullOfOrNull { it.third.song.thumbnail },
                    playCount = entries.size,
                    totalSec = entries.sumOf { it.third.listenedSec },
                )
            }
            .sortedWith(compareByDescending<RankedArtistStat> { it.playCount }.thenByDescending { it.totalSec })
            .take(10)

        val today = currentEpochDay(nowMs)
        val activeStreak = if (snap.lastListenEpochDay >= 0 && today - snap.lastListenEpochDay <= 1L) {
            snap.currentStreakDays
        } else {
            0
        }

        return StatsSummary(
            period = period,
            totalMinutes = (periodSec / 60L),
            totalPlays = valid.size,
            uniqueSongs = valid.map { it.song.id }.distinct().size,
            uniqueArtists = topArtists.size,
            currentStreakDays = activeStreak,
            longestStreakDays = snap.longestStreakDays,
            streakTier = StreakTier.fromDays(activeStreak),
            topSongs = topSongs,
            topArtists = topArtists,
        )
    }

    fun clearAll() = save(ListeningStatsData())

    private companion object {
        const val KEY_STATS = "listening_stats_v1"
        const val MAX_EVENTS = 2500
    }
}
