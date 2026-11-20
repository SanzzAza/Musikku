package com.vyora.music

import android.content.Context
import com.vyora.music.data.importer.PlaylistImporter
import com.vyora.music.data.local.AudioFxPreferencesStore
import com.vyora.music.data.local.FavoritesStore
import com.vyora.music.data.local.ListeningStatsStore
import com.vyora.music.data.local.LocalMusicRepository
import com.vyora.music.data.local.LyricsOffsetStore
import com.vyora.music.data.local.PlaylistStore
import com.vyora.music.data.local.RecentStore
import com.vyora.music.data.local.SearchHistoryStore
import com.vyora.music.data.local.UiPreferencesStore
import com.vyora.music.data.lyrics.LyricsCacheStore
import com.vyora.music.data.lyrics.LyricsRepository
import com.vyora.music.data.lyrics.LyricsTranslator
import com.vyora.music.data.offline.OfflineDownloadManager
import com.vyora.music.data.ytmusic.YTMusic

/** Service locator global VYORA (100% Guest, ringan tanpa Hilt). */
object AppModule {

    lateinit var favorites: FavoritesStore
        private set

    lateinit var searchHistory: SearchHistoryStore
        private set

    lateinit var recents: RecentStore
        private set

    lateinit var playlists: PlaylistStore
        private set

    lateinit var uiPrefs: UiPreferencesStore
        private set

    lateinit var audioFx: AudioFxPreferencesStore
        private set

    lateinit var stats: ListeningStatsStore
        private set

    lateinit var downloads: OfflineDownloadManager
        private set

    lateinit var lyricsOffset: LyricsOffsetStore
        private set

    lateinit var localMusic: LocalMusicRepository
        private set

    lateinit var lyricsCache: LyricsCacheStore
        private set

    lateinit var lyricsTranslator: LyricsTranslator
        private set

    lateinit var lyrics: LyricsRepository
        private set

    fun init(context: Context) {
        val app = context.applicationContext
        favorites = FavoritesStore(app)
        searchHistory = SearchHistoryStore(app)
        recents = RecentStore(app)
        playlists = PlaylistStore(app)
        uiPrefs = UiPreferencesStore(app)
        audioFx = AudioFxPreferencesStore(app)
        stats = ListeningStatsStore(app)
        downloads = OfflineDownloadManager(app)
        lyricsOffset = LyricsOffsetStore(app)
        localMusic = LocalMusicRepository(app)
        lyricsCache = LyricsCacheStore(app)
        lyricsTranslator = LyricsTranslator(app)
        lyrics = LyricsRepository(lyricsCache)
    }

    /** Katalog musik: YouTube Music Guest API (cari, beranda, artis, album, playlist, radio). */
    val ytMusic: YTMusic by lazy { YTMusic() }

    /** Impor playlist dari link Spotify / Apple Music / YouTube Music. */
    val playlistImporter: PlaylistImporter by lazy { PlaylistImporter(ytMusic, playlists) }
}
