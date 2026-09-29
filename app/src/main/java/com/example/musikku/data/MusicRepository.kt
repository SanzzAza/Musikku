package com.example.musikku.data

import com.example.musikku.data.model.Album
import com.example.musikku.data.model.Artist
import com.example.musikku.data.model.Track
import com.example.musikku.data.remote.DeezerApi

class MusicRepository(private val api: DeezerApi) {

    suspend fun chartTracks(): List<Track> = api.chartTracks().data.orEmpty()
    suspend fun chartArtists(): List<Artist> = api.chartArtists().data.orEmpty()
    suspend fun chartAlbums(): List<Album> = api.chartAlbums().data.orEmpty()

    suspend fun searchTracks(q: String): List<Track> = api.searchTracks(q).data.orEmpty()
    suspend fun searchArtists(q: String): List<Artist> = api.searchArtists(q).data.orEmpty()
    suspend fun searchAlbums(q: String): List<Album> = api.searchAlbums(q).data.orEmpty()

    suspend fun artist(id: Long): Artist = api.artist(id)
    suspend fun artistTopTracks(id: Long): List<Track> = api.artistTopTracks(id).data.orEmpty()
    suspend fun artistAlbums(id: Long): List<Album> = api.artistAlbums(id).data.orEmpty()
    suspend fun relatedArtists(id: Long): List<Artist> = api.relatedArtists(id).data.orEmpty()

    /** Detail album. Track di dalam album tidak membawa info album, jadi kita isi manual. */
    suspend fun album(id: Long): Album {
        val album = api.album(id)
        val bare = album.copy(tracks = null)
        val tracks = album.tracks?.data.orEmpty().map { it.copy(album = bare) }
        return album.copy(tracks = album.tracks?.copy(data = tracks))
    }

    /** Ambil URL preview terbaru (URL dari Deezer punya masa berlaku ± 1 jam). BLOCKING. */
    fun freshPreviewUrlBlocking(trackId: Long): String? =
        runCatching { api.trackBlocking(trackId).execute().body()?.preview }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
}
