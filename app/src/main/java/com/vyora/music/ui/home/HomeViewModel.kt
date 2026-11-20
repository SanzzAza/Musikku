package com.vyora.music.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyora.music.AppModule
import com.vyora.music.data.ytmusic.SearchFilter
import com.vyora.music.data.ytmusic.Section
import com.vyora.music.data.ytmusic.SongItem
import com.vyora.music.ui.components.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeMood(
    val id: String,
    val label: String,
    val query: String,
)

val HOME_MOODS = listOf(
    HomeMood("relax", "Bersantai", "lagu santai akustik indonesia chill"),
    HomeMood("happy", "Senang", "lagu pop ceria feel good indonesia"),
    HomeMood("party", "Pesta", "lagu upbeat party dance hits"),
    HomeMood("commute", "Perjalanan", "lagu enak di jalan road trip indonesia"),
    HomeMood("focus", "Fokus", "lofi chill instrumental study beats"),
    HomeMood("sad", "Galau", "lagu galau sedih indonesia populer"),
)

data class HomeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val sections: List<Section> = emptyList(),
    val selectedMoodId: String? = null,
    val moodLoading: Boolean = false,
    val moodSongs: List<SongItem> = emptyList(),
    val moodSections: List<Section> = emptyList(),
)

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val moodCache = mutableMapOf<String, Pair<List<SongItem>, List<Section>>>()
    private var moodJob: Job? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val loaded = AppModule.ytMusic.home()
                _state.update { it.copy(loading = false, sections = loaded, error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.toUserMessage()) }
            }
        }
    }

    fun selectMood(mood: HomeMood?) {
        // Ketuk mood yang sudah aktif -> kembali ke beranda utama
        val target = if (mood != null && _state.value.selectedMoodId == mood.id) null else mood
        moodJob?.cancel()
        if (target == null) {
            _state.update {
                it.copy(
                    selectedMoodId = null,
                    moodLoading = false,
                    moodSongs = emptyList(),
                    moodSections = emptyList(),
                )
            }
            return
        }
        moodCache[target.id]?.let { (songs, sections) ->
            _state.update {
                it.copy(
                    selectedMoodId = target.id,
                    moodLoading = false,
                    moodSongs = songs,
                    moodSections = sections,
                )
            }
            return
        }
        _state.update {
            it.copy(
                selectedMoodId = target.id,
                moodLoading = true,
                moodSongs = emptyList(),
                moodSections = emptyList(),
            )
        }
        moodJob = viewModelScope.launch {
            try {
                val (songs, playlists) = coroutineScope {
                    val s = async {
                        runCatching {
                            AppModule.ytMusic.search(target.query, SearchFilter.SONGS)
                                .filterIsInstance<SongItem>()
                                .take(20)
                        }.getOrDefault(emptyList())
                    }
                    val p = async {
                        runCatching {
                            AppModule.ytMusic.search(target.query, SearchFilter.PLAYLISTS)
                                .take(10)
                        }.getOrDefault(emptyList())
                    }
                    s.await() to p.await()
                }
                val sections = buildList {
                    if (playlists.isNotEmpty()) {
                        add(Section("Playlist ${target.label}", playlists, moreBrowseId = null))
                    }
                }
                moodCache[target.id] = songs to sections
                _state.update {
                    it.copy(
                        moodLoading = false,
                        moodSongs = songs,
                        moodSections = sections,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(moodLoading = false) }
            }
        }
    }
}
