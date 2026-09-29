package com.example.musikku.data.model

import com.google.gson.annotations.SerializedName

/** Respons list standar dari Deezer API: { "data": [...], "total": n, "next": "..." } */
data class DeezerList<T>(
    val data: List<T>? = null,
    val total: Int = 0,
    val next: String? = null,
)

data class Artist(
    val id: Long = 0,
    val name: String? = null,
    @SerializedName("picture_medium") val pictureMedium: String? = null,
    @SerializedName("picture_big") val pictureBig: String? = null,
    @SerializedName("picture_xl") val pictureXl: String? = null,
    @SerializedName("nb_fan") val nbFan: Long = 0,
    @SerializedName("nb_album") val nbAlbum: Int = 0,
)

data class Album(
    val id: Long = 0,
    val title: String? = null,
    @SerializedName("cover_medium") val coverMedium: String? = null,
    @SerializedName("cover_big") val coverBig: String? = null,
    @SerializedName("cover_xl") val coverXl: String? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("record_type") val recordType: String? = null,
    @SerializedName("nb_tracks") val nbTracks: Int = 0,
    val artist: Artist? = null,
    val tracks: DeezerList<Track>? = null,
)

data class Track(
    val id: Long = 0,
    val title: String? = null,
    val duration: Int = 0,
    val preview: String? = null,
    @SerializedName("explicit_lyrics") val explicitLyrics: Boolean = false,
    val artist: Artist? = null,
    val album: Album? = null,
) {
    val isPlayable: Boolean get() = !preview.isNullOrBlank()
}
