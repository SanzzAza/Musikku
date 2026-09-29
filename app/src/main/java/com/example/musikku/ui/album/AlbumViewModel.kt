package com.example.musikku.ui.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musikku.AppModule
import com.example.musikku.data.model.Album
import com.example.musikku.ui.components.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlbumUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val album: Album? = null,
)

class AlbumViewModel : ViewModel() {
    private val repo = AppModule.repository
    private val _state = MutableStateFlow(AlbumUiState())
    val state: StateFlow<AlbumUiState> = _state.asStateFlow()
    private var loadedId: Long? = null

    fun load(id: Long, force: Boolean = false) {
        if (loadedId == id && !force) return
        loadedId = id
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                _state.value = AlbumUiState(loading = false, album = repo.album(id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.toUserMessage()) }
            }
        }
    }
}
