package com.example.musikku.data.ytmusic

/** Referensi singkat ke artis / album (nama + id halaman YouTube Music). */
data class ArtistRef(val id: String? = null, val name: String = "")
data class AlbumRef(val id: String? = null, val name: String = "")

/** Semua item yang bisa muncul di YouTube Music. */
sealed interface YTItem {
    val id: String
    val title: String
    val thumbnail: String?
}

data class SongItem(
    override val id: String,                 // videoId
    override val title: String,
    val artists: List<ArtistRef> = emptyList(),
    val album: AlbumRef? = null,
    val durationSec: Int = 0,
    override val thumbnail: String? = null,
) : YTItem {
    val artistsText: String get() = artists.joinToString(", ") { it.name }
}

data class AlbumItem(
    override val id: String,                 // browseId: MPREb_... (album) atau VL... (playlist)
    override val title: String,
    val subtitle: String = "",
    override val thumbnail: String? = null,
    val isPlaylist: Boolean = false,
) : YTItem

data class ArtistItem(
    override val id: String,                 // browseId channel: UC...
    override val title: String,
    val subtitle: String = "",
    override val thumbnail: String? = null,
) : YTItem

/** Satu baris/bagian di beranda atau halaman artis. */
data class Section(
    val title: String,
    val items: List<YTItem>,
    val moreBrowseId: String? = null,
)

data class ArtistPage(
    val id: String,
    val name: String,
    val thumbnail: String?,
    val subscribers: String?,
    val description: String?,
    val sections: List<Section>,
) {
    /** Bagian "Lagu teratas" (daftar lagu). */
    val topSongs: Section? get() = sections.firstOrNull { s -> s.items.isNotEmpty() && s.items.all { it is SongItem } }
}

data class CollectionPage(
    val id: String,
    val title: String,
    val subtitle: String,
    val secondSubtitle: String,
    val thumbnail: String?,
    val author: ArtistRef?,
    val songs: List<SongItem>,
)

enum class SearchFilter(val params: String) {
    SONGS("EgWKAQIIAWoMEA4QChADEAQQCRAF"),
    ARTISTS("EgWKAQIgAWoMEA4QChADEAQQCRAF"),
    ALBUMS("EgWKAQIYAWoMEA4QChADEAQQCRAF"),
    PLAYLISTS("EgeKAQQoAEABagwQDhAKEAMQBBAJEAU="),
}
