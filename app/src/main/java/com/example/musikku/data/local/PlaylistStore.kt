package com.example.musikku.data.local

import android.content.Context
import com.example.musikku.data.ytmusic.SongItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/** Playlist buatan pengguna (disimpan lokal di HP). */
data class UserPlaylist(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val songs: List<SongItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
) {
    /** Cover: lagu pertama di playlist. */
    val cover: String? get() = songs.firstOrNull()?.thumbnail
    /** Sampai 4 cover berbeda untuk tampilan mozaik ala Spotify. */
    val mosaic: List<String> get() = songs.mapNotNull { it.thumbnail }.distinct().take(4)
}

class PlaylistStore(context: Context) {

    private val prefs = context.getSharedPreferences("playlists", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val type = object : TypeToken<List<UserPlaylist>>() {}.type

    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<UserPlaylist>> = _items.asStateFlow()

    private fun load(): List<UserPlaylist> = prefs.getString(KEY, null)
        ?.let { runCatching { gson.fromJson<List<UserPlaylist>>(it, type) }.getOrNull() }
        .orEmpty()
        // Gson bisa mengisi null pada field non-null kalau data lama rusak — rapikan.
        .mapNotNull { p ->
            @Suppress("SENSELESS_COMPARISON")
            if (p == null || p.id == null) null
            else p.copy(name = p.name ?: "Playlist", songs = (p.songs ?: emptyList()).filter { it != null && it.id.isNotBlank() })
        }

    fun get(id: String): UserPlaylist? = _items.value.firstOrNull { it.id == id }

    /** Buat playlist baru (opsional langsung berisi lagu). Mengembalikan playlist yang dibuat. */
    fun create(name: String, songs: List<SongItem> = emptyList()): UserPlaylist {
        val p = UserPlaylist(name = name.trim().ifBlank { defaultName() }, songs = songs.distinctBy { it.id })
        save(listOf(p) + _items.value)
        return p
    }

    fun defaultName(): String = "Playlist Saya #${_items.value.size + 1}"

    fun rename(id: String, name: String) = update(id) { it.copy(name = name.trim().ifBlank { it.name }) }

    fun delete(id: String) = save(_items.value.filterNot { it.id == id })

    /** Tambah lagu (yang sudah ada dilewati). Mengembalikan jumlah lagu yang benar-benar ditambahkan. */
    fun addSongs(id: String, songs: List<SongItem>): Int {
        var added = 0
        update(id) { p ->
            val existing = p.songs.map { it.id }.toSet()
            val fresh = songs.filter { it.id !in existing }.distinctBy { it.id }
            added = fresh.size
            p.copy(songs = p.songs + fresh)
        }
        return added
    }

    fun removeSong(id: String, songId: String) = update(id) { p -> p.copy(songs = p.songs.filterNot { it.id == songId }) }

    fun contains(id: String, songId: String): Boolean = get(id)?.songs?.any { it.id == songId } == true

    private fun update(id: String, transform: (UserPlaylist) -> UserPlaylist) =
        save(_items.value.map { if (it.id == id) transform(it) else it })

    private fun save(list: List<UserPlaylist>) {
        _items.value = list
        prefs.edit().putString(KEY, gson.toJson(list)).apply()
    }

    private companion object {
        const val KEY = "user_playlists"
    }
}
