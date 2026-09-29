package com.example.musikku.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musikku.AppModule
import com.example.musikku.data.model.Album
import com.example.musikku.data.model.Artist
import com.example.musikku.data.model.Track
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

enum class SearchTab(val label: String) { SONGS("Lagu"), ARTISTS("Artis"), ALBUMS("Album") }

data class SearchUiState(
    val query: String = "",
    val tab: SearchTab = SearchTab.SONGS,
    val loading: Boolean = false,
    val error: String? = null,
    val searched: Boolean = false,
    val tracks: List<Track> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
)

class SearchViewModel : ViewModel() {
    private val repo = AppModule.repository
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    /** Dipanggil setiap user mengetik. Pencarian dijalankan 400ms setelah berhenti mengetik (debounce). */
    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { SearchUiState(tab = it.tab) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            search(query.trim())
        }
    }

    /** Cari langsung (tombol search di keyboard / chip genre). */
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
                val tracks = async { repo.searchTracks(q) }
                val artists = async { repo.searchArtists(q) }
                val albums = async { repo.searchAlbums(q) }
                _state.update {
                    it.copy(
                        loading = false,
                        searched = true,
                        tracks = tracks.await(),
                        artists = artists.await(),
                        albums = albums.await(),
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
