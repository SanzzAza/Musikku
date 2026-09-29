package com.example.musikku.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musikku.AppModule
import com.example.musikku.data.ytmusic.AlbumItem
import com.example.musikku.data.ytmusic.ArtistItem
import com.example.musikku.data.ytmusic.SearchFilter
import com.example.musikku.data.ytmusic.SongItem
import com.example.musikku.ui.components.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SearchTab(val label: String) { SONGS("Lagu"), ARTISTS("Artis"), ALBUMS("Album"), PLAYLISTS("Playlist") }

data class SearchUiState(
    val query: String = "",
    val tab: SearchTab = SearchTab.SONGS,
    val loading: Boolean = false,
    val error: String? = null,
    val searched: Boolean = false,
    /** Kata kunci dari hasil yang sedang ditampilkan (null = belum pernah mencari). */
    val searchedQuery: String? = null,
    /** Saran kata kunci saat mengetik. */
    val suggestions: List<String> = emptyList(),
    val songs: List<SongItem> = emptyList(),
    val artists: List<ArtistItem> = emptyList(),
    val albums: List<AlbumItem> = emptyList(),
    val playlists: List<AlbumItem> = emptyList(),
)

class SearchViewModel : ViewModel() {
    private val yt = AppModule.ytMusic
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var suggestionsJob: Job? = null

    /** Dipanggil setiap user mengetik. Saran muncul cepat, pencarian jalan 600ms setelah berhenti mengetik. */
    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        suggestionsJob?.cancel()
        if (query.isBlank()) {
            _state.update { SearchUiState(tab = it.tab) }
            return
        }
        // Saran kata kunci dimuat lebih dulu supaya responsif…
        suggestionsJob = viewModelScope.launch {
            delay(250)
            val suggestions = runCatching { yt.suggestions(query.trim()) }.getOrDefault(emptyList())
            // Tampilkan hanya kalau user masih di kata kunci yang sama dan hasil pencarian belum menggantikan
            if (_state.value.query.trim() == query.trim() && _state.value.searchedQuery != query.trim()) {
                _state.update { it.copy(suggestions = suggestions) }
            }
        }
        // …hasil pencarian menyusul setelah user berhenti mengetik
        searchJob = viewModelScope.launch {
            delay(600)
            search(query.trim())
        }
    }

    fun searchNow(query: String = _state.value.query) {
        if (query.isBlank()) return
        _state.update { it.copy(query = query) }
        // Rekam ke riwayat: hanya pencarian eksplisit (submit keyboard / tap saran / tap
        // kategori), bukan pencarian otomatis sambil mengetik, supaya riwayat tidak
        // penuh kata kunci setengah ketik seperti "tul", "tulus…
        AppModule.searchHistory.add(query)
        searchJob?.cancel()
        suggestionsJob?.cancel()
        searchJob = viewModelScope.launch { search(query.trim()) }
    }

    fun onTabSelected(tab: SearchTab) = _state.update { it.copy(tab = tab) }

    private suspend fun search(q: String) {
        _state.update { it.copy(loading = true, error = null, suggestions = emptyList()) }
        try {
            coroutineScope {
                val songs = async { runCatching { yt.search(q, SearchFilter.SONGS) }.getOrNull() }
                val artists = async { runCatching { yt.search(q, SearchFilter.ARTISTS) }.getOrNull() }
                val albums = async { runCatching { yt.search(q, SearchFilter.ALBUMS) }.getOrNull() }
                val playlists = async { runCatching { yt.search(q, SearchFilter.PLAYLISTS) }.getOrNull() }
                val s = songs.await(); val a = artists.await(); val al = albums.await(); val p = playlists.await()
                if (s == null && a == null && al == null && p == null) {
                    // semua gagal → kemungkinan tidak ada internet
                    yt.search(q, SearchFilter.SONGS)
                }
                _state.update {
                    it.copy(
                        loading = false,
                        searched = true,
                        searchedQuery = q,
                        // distinctBy: hasil pencarian bisa memuat videoId yang sama dua kali
                        // (mis. lagu muncul di album dan single) → key duplikat bikin crash di LazyColumn
                        songs = s.orEmpty().filterIsInstance<SongItem>().distinctBy { it.id },
                        artists = a.orEmpty().filterIsInstance<ArtistItem>().distinctBy { it.id },
                        albums = al.orEmpty().filterIsInstance<AlbumItem>().distinctBy { it.id },
                        playlists = p.orEmpty().filterIsInstance<AlbumItem>().distinctBy { it.id },
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = e.toUserMessage()) }
        }
    }
}
