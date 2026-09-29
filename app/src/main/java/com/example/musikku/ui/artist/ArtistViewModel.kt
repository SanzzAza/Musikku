package com.example.musikku.ui.artist

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

data class ArtistUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val artist: Artist? = null,
    val topTracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val related: List<Artist> = emptyList(),
)

class ArtistViewModel : ViewModel() {
    private val repo = AppModule.repository
    private val _state = MutableStateFlow(ArtistUiState())
    val state: StateFlow<ArtistUiState> = _state.asStateFlow()
    private var loadedId: Long? = null

    fun load(id: Long, force: Boolean = false) {
        if (loadedId == id && !force) return
        loadedId = id
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                coroutineScope {
                    val artist = async { repo.artist(id) }
                    val top = async { repo.artistTopTracks(id) }
                    val albums = async { repo.artistAlbums(id) }
                    val related = async { runCatching { repo.relatedArtists(id) }.getOrDefault(emptyList()) }
                    _state.value = ArtistUiState(
                        loading = false,
                        artist = artist.await(),
                        topTracks = top.await(),
                        albums = albums.await().sortedByDescending { it.releaseDate },
                        related = related.await(),
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
