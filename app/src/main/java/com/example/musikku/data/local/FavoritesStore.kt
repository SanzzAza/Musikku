package com.example.musikku.data.local

import android.content.Context
import com.example.musikku.data.model.Track
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Menyimpan lagu yang disukai ke SharedPreferences (JSON). */
class FavoritesStore(context: Context) {

    private val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val listType = object : TypeToken<List<Track>>() {}.type

    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<Track>> = _items.asStateFlow()

    private fun load(): List<Track> = prefs.getString(KEY, null)
        ?.let { json -> runCatching { gson.fromJson<List<Track>>(json, listType) }.getOrNull() }
        .orEmpty()

    fun isFavorite(id: Long): Boolean = _items.value.any { it.id == id }

    fun toggle(track: Track) {
        val current = _items.value
        val updated = if (current.any { it.id == track.id }) {
            current.filterNot { it.id == track.id }
        } else {
            listOf(track.copy(album = track.album?.copy(tracks = null))) + current
        }
        _items.value = updated
        prefs.edit().putString(KEY, gson.toJson(updated)).apply()
    }

    private companion object {
        const val KEY = "liked_tracks"
    }
}
