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

    /** Dipanggil setiap user mengetik. Pencarian jalan 500ms setelah berhenti mengetik. */
    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { SearchUiState(tab = it.tab) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(500)
            search(query.trim())
        }
    }

    fun searchNow(query: String = _state.value.query) {
        if (query.isBlank()) return
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch { search(query.trim()) }
    }

    fun onTabSelected(tab: SearchTab) = _state.update { it.copy(tab = tab) }

    private suspend fun search(q: String) {
        _state.update { it.copy(loading = true, error = null) }
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
                        songs = s.orEmpty().filterIsInstance<SongItem>(),
                        artists = a.orEmpty().filterIsInstance<ArtistItem>(),
                        albums = al.orEmpty().filterIsInstance<AlbumItem>(),
                        playlists = p.orEmpty().filterIsInstance<AlbumItem>(),
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
