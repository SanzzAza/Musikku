package com.example.musikku

import android.content.Context
import com.example.musikku.data.local.FavoritesStore
import com.example.musikku.data.local.SearchHistoryStore
import com.example.musikku.data.local.RecentStore
import com.example.musikku.data.local.PlaylistStore
import com.example.musikku.data.lyrics.LyricsRepository
import com.example.musikku.data.ytmusic.YTMusic

/** Service locator sederhana (tanpa Hilt supaya mudah dipahami). */
object AppModule {

    lateinit var favorites: FavoritesStore
        private set

    lateinit var searchHistory: SearchHistoryStore
        private set

    lateinit var recents: RecentStore
        private set

    lateinit var playlists: PlaylistStore
        private set

    fun init(context: Context) {
        favorites = FavoritesStore(context.applicationContext)
        searchHistory = SearchHistoryStore(context.applicationContext)
        recents = RecentStore(context.applicationContext)
        playlists = PlaylistStore(context.applicationContext)
    }

    /** Katalog musik: YouTube Music (cari, beranda, artis, album, playlist, radio). */
    val ytMusic: YTMusic by lazy { YTMusic() }

    val lyrics: LyricsRepository by lazy { LyricsRepository() }
}
