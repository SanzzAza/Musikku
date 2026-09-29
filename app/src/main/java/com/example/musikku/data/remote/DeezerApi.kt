package com.example.musikku.data.remote

import com.example.musikku.data.model.Album
import com.example.musikku.data.model.Artist
import com.example.musikku.data.model.DeezerList
import com.example.musikku.data.model.Track
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Deezer public API — gratis, tanpa API key.
 * Dokumentasi: https://developers.deezer.com/api
 */
interface DeezerApi {

    @GET("chart/0/tracks")
    suspend fun chartTracks(@Query("limit") limit: Int = 30): DeezerList<Track>

    @GET("chart/0/artists")
    suspend fun chartArtists(@Query("limit") limit: Int = 20): DeezerList<Artist>

    @GET("chart/0/albums")
    suspend fun chartAlbums(@Query("limit") limit: Int = 20): DeezerList<Album>

    @GET("search")
    suspend fun searchTracks(@Query("q") query: String, @Query("limit") limit: Int = 40): DeezerList<Track>

    @GET("search/artist")
    suspend fun searchArtists(@Query("q") query: String, @Query("limit") limit: Int = 30): DeezerList<Artist>

    @GET("search/album")
    suspend fun searchAlbums(@Query("q") query: String, @Query("limit") limit: Int = 30): DeezerList<Album>

    @GET("artist/{id}")
    suspend fun artist(@Path("id") id: Long): Artist

    @GET("artist/{id}/top")
    suspend fun artistTopTracks(@Path("id") id: Long, @Query("limit") limit: Int = 20): DeezerList<Track>

    @GET("artist/{id}/albums")
    suspend fun artistAlbums(@Path("id") id: Long, @Query("limit") limit: Int = 50): DeezerList<Album>

    @GET("artist/{id}/related")
    suspend fun relatedArtists(@Path("id") id: Long, @Query("limit") limit: Int = 20): DeezerList<Artist>

    @GET("album/{id}")
    suspend fun album(@Path("id") id: Long): Album

    /** Versi blocking — dipakai oleh player (thread loader ExoPlayer) untuk refresh URL preview. */
    @GET("track/{id}")
    fun trackBlocking(@Path("id") id: Long): Call<Track>

    companion object {
        const val BASE_URL = "https://api.deezer.com/"
    }
}
