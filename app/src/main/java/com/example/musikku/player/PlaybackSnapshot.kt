package com.example.musikku.player

import android.content.Context
import androidx.media3.common.Player
import com.example.musikku.data.ytmusic.SongItem
import com.google.gson.Gson

private data class Snapshot(val songs: List<SongItem>, val index: Int, val positionMs: Long, val shuffle: Boolean, val repeat: Int)

/** Snapshot berisi ID dan metadata saja, bukan URL stream yang bisa kedaluwarsa. */
class PlaybackSnapshot(context: Context) {
    private val prefs = context.getSharedPreferences("playback_snapshot", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun save(player: Player) {
        if (player.mediaItemCount == 0 || player.currentMediaItemIndex !in 0 until player.mediaItemCount) return
        // Jaga ukuran preferensi sambil memastikan lagu aktif selalu ikut tersimpan.
        val start = (player.currentMediaItemIndex - 250).coerceAtLeast(0)
        val end = (start + 500).coerceAtMost(player.mediaItemCount)
        val songs = (start until end).map { player.getMediaItemAt(it).toSong() }
        val snapshot = Snapshot(songs, player.currentMediaItemIndex - start, player.currentPosition.coerceAtLeast(0), player.shuffleModeEnabled, player.repeatMode)
        prefs.edit().putString("snapshot", gson.toJson(snapshot)).apply()
    }

    fun restore(player: Player) {
        val raw = prefs.getString("snapshot", null) ?: return
        runCatching {
            val snapshot = gson.fromJson(raw, Snapshot::class.java)
            require(snapshot.songs.isNotEmpty() && snapshot.songs.size <= 500 && snapshot.index in snapshot.songs.indices)
            require(snapshot.songs.all { it.id.isNotBlank() && it.title.isNotBlank() && it.artists != null })
            player.setMediaItems(snapshot.songs.map { it.toMediaItem() }, snapshot.index, snapshot.positionMs.coerceAtLeast(0))
            player.shuffleModeEnabled = snapshot.shuffle
            player.repeatMode = snapshot.repeat.takeIf { it in Player.REPEAT_MODE_OFF..Player.REPEAT_MODE_ALL } ?: Player.REPEAT_MODE_OFF
            // Tidak play/prepare otomatis; pengguna menentukan kapan ingin melanjutkan.
        }.onFailure { prefs.edit().remove("snapshot").apply() }
    }
}
