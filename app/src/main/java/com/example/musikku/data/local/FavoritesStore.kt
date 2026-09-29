package com.example.musikku.data.local

import android.content.Context
import com.example.musikku.data.ytmusic.SongItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Menyimpan lagu yang disukai ke SharedPreferences (JSON). */
class FavoritesStore(context: Context) {

    private val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val listType = object : TypeToken<List<SongItem>>() {}.type

    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<SongItem>> = _items.asStateFlow()

    private fun load(): List<SongItem> = prefs.getString(KEY, null)
        ?.let { json -> runCatching { gson.fromJson<List<SongItem>>(json, listType) }.getOrNull() }
        .orEmpty()
        .filter { it.id.isNotBlank() }

    fun isFavorite(id: String): Boolean = _items.value.any { it.id == id }

    fun toggle(song: SongItem) {
        val current = _items.value
        val updated = if (current.any { it.id == song.id }) current.filterNot { it.id == song.id }
        else listOf(song) + current
        _items.value = updated
        prefs.edit().putString(KEY, gson.toJson(updated)).apply()
    }

    private companion object {
        const val KEY = "liked_songs_v2"
    }
}
