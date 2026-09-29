package com.example.musikku.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Riwayat kata kunci pencarian — terbaru di depan, maksimal [MAX] entri. */
class SearchHistoryStore(context: Context) {

    private val prefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val listType = object : TypeToken<List<String>>() {}.type

    private val _queries = MutableStateFlow(load())
    val queries: StateFlow<List<String>> = _queries.asStateFlow()

    /** Tambahkan kata kunci (yang sama dipindah ke paling depan, tidak dobel). */
    fun add(query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        _queries.value = (listOf(q) + _queries.value.filter { it != q }).take(MAX)
        persist()
    }

    fun remove(query: String) {
        _queries.value = _queries.value.filterNot { it == query }
        persist()
    }

    fun clear() {
        _queries.value = emptyList()
        persist()
    }

    private fun load(): List<String> =
        prefs.getString(KEY, null)
            ?.let { json -> runCatching { gson.fromJson<List<String>>(json, listType) }.getOrNull() }
            .orEmpty()

    private fun persist() = prefs.edit().putString(KEY, gson.toJson(_queries.value)).apply()

    private companion object {
        const val KEY = "queries_v1"
        const val MAX = 10
    }
}
