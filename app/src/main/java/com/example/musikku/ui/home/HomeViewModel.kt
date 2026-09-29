package com.example.musikku.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musikku.AppModule
import com.example.musikku.data.model.Album
import com.example.musikku.data.model.Artist
import com.example.musikku.data.model.Track
import com.example.musikku.ui.components.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val tracks: List<Track> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
)

class HomeViewModel : ViewModel() {
    private val repo = AppModule.repository
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                coroutineScope {
                    val tracks = async { repo.chartTracks() }
                    val artists = async { repo.chartArtists() }
                    val albums = async { repo.chartAlbums() }
                    _state.value = HomeUiState(
                        loading = false,
                        tracks = tracks.await(),
                        artists = artists.await(),
                        albums = albums.await(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.toUserMessage()) }
            }
        }
    }
}
