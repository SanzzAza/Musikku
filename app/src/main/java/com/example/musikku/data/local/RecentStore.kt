package com.example.musikku.data.local

import android.content.Context
import com.example.musikku.data.ytmusic.AlbumItem
import com.example.musikku.data.ytmusic.ArtistItem
import com.example.musikku.data.ytmusic.SongItem
import com.example.musikku.data.ytmusic.YTItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Riwayat "Baru Diputar" di halaman Cari (artis, lagu, album, playlist). */
class RecentStore(context: Context) {

    /** Gson tidak bisa langsung menyimpan sealed interface, jadi dibungkus. */
    private data class Entry(
        val song: SongItem? = null,
        val album: AlbumItem? = null,
        val artist: ArtistItem? = null,
    ) {
        fun toItem(): YTItem? = song ?: album ?: artist
    }

    private val prefs = context.getSharedPreferences("recent", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val type = object : TypeToken<List<Entry>>() {}.type

    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<YTItem>> = _items.asStateFlow()

    private fun load(): List<YTItem> = prefs.getString(KEY, null)
        ?.let { runCatching { gson.fromJson<List<Entry>>(it, type) }.getOrNull() }
        .orEmpty()
        .mapNotNull { it.toItem() }
        .filter { it.id.isNotBlank() }

    /** Tambahkan ke paling atas (kalau sudah ada, dipindah ke atas). */
    fun add(item: YTItem) = save(listOf(item) + _items.value.filterNot { it.id == item.id })

    fun remove(item: YTItem) = save(_items.value.filterNot { it.id == item.id })

    fun clear() = save(emptyList())

    private fun save(list: List<YTItem>) {
        val trimmed = list.take(MAX)
        _items.value = trimmed
        val entries = trimmed.map {
            when (it) {
                is SongItem -> Entry(song = it)
                is AlbumItem -> Entry(album = it)
                is ArtistItem -> Entry(artist = it)
            }
        }
        prefs.edit().putString(KEY, gson.toJson(entries)).apply()
    }

    private companion object {
        const val KEY = "recent_items"
        const val MAX = 30
    }
}
