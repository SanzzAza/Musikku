package com.vyora.music.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vyora.music.AppModule
import com.vyora.music.data.ytmusic.AlbumItem
import com.vyora.music.data.ytmusic.ArtistItem
import com.vyora.music.data.ytmusic.SongItem
import com.vyora.music.data.ytmusic.YTItem
import com.vyora.music.player.PlayerViewModel
import com.vyora.music.player.SleepTimer
import com.vyora.music.ui.artist.ArtistScreen
import com.vyora.music.ui.collection.CollectionScreen
import com.vyora.music.ui.components.FloatingBottomChrome
import com.vyora.music.ui.components.LocalQueueActions
import com.vyora.music.ui.components.NavTab
import com.vyora.music.ui.components.QueueActions
import com.vyora.music.ui.discord.DiscordSettingsSheet
import com.vyora.music.ui.home.HistorySheet
import com.vyora.music.ui.home.HomeScreen
import com.vyora.music.ui.library.LibraryScreen
import com.vyora.music.ui.player.AudioFxSheet
import com.vyora.music.ui.player.PlayerScreen
import com.vyora.music.ui.player.rememberCoverPalette
import com.vyora.music.ui.playlist.AddToPlaylistHost
import com.vyora.music.ui.playlist.PlaylistPicker
import com.vyora.music.ui.playlist.PlaylistScreen
import com.vyora.music.ui.poster.SongPosterHost
import com.vyora.music.ui.search.SearchScreen
import com.vyora.music.ui.settings.AppearanceSheet
import com.vyora.music.ui.stats.StatsSheet
import com.vyora.music.ui.theme.AmbientBackdrop
import com.vyora.music.ui.theme.LocalBottomOverlayPadding
import com.vyora.music.ui.theme.LocalHazeState
import com.vyora.music.ui.theme.Motion
import com.vyora.music.ui.theme.VyoraTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

private object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val ARTIST = "artist/{id}"
    const val COLLECTION = "collection/{id}"
    const val PLAYER = "player"
    const val PLAYLIST = "playlist/{id}"
    fun playlist(id: String) = "playlist/$id"
    fun artist(id: String) = "artist/$id"
    fun collection(id: String) = "collection/$id"
}

private val navTabs = listOf(
    NavTab(Routes.HOME, "Beranda", Icons.Outlined.Home, Icons.Rounded.Home),
    NavTab(Routes.SEARCH, "Cari", Icons.Outlined.Search, Icons.Rounded.Search),
    NavTab(Routes.LIBRARY, "Pustaka", Icons.Outlined.LibraryMusic, Icons.Rounded.LibraryMusic),
)

@Composable
fun VyoraApp(playerVm: PlayerViewModel = viewModel()) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val now by playerVm.state.collectAsStateWithLifecycle()
    val liked by AppModule.favorites.items.collectAsStateWithLifecycle()
    val lyrics by playerVm.lyrics.collectAsStateWithLifecycle()
    val sleepRemaining by SleepTimer.remainingMs.collectAsStateWithLifecycle()
    val uiPrefs by AppModule.uiPrefs.state.collectAsStateWithLifecycle()
    val currentId = now.song?.id
    val isCurrentLiked = currentId != null && liked.any { it.id == currentId }

    val coverPalette = rememberCoverPalette(
        url = now.song?.thumbnail,
        enabled = uiPrefs.dynamicCoverTheme,
    )

    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showStats by rememberSaveable { mutableStateOf(false) }
    var showAudioFx by rememberSaveable { mutableStateOf(false) }
    var showDiscord by rememberSaveable { mutableStateOf(false) }
    var navCollapsed by rememberSaveable { mutableStateOf(false) }

    // Kembalikan navbar ke bentuk penuh saat berpindah layar
    LaunchedEffect(route) {
        navCollapsed = false
    }

    // Izin notifikasi (Android 13+) untuk notifikasi kontrol musik
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val play: (List<SongItem>, Int) -> Unit = playerVm::play
    val shuffle: (List<SongItem>) -> Unit = playerVm::shuffle
    val openArtist: (String) -> Unit = { nav.navigate(Routes.artist(it)) }
    val openCollection: (String) -> Unit = { nav.navigate(Routes.collection(it)) }
    val openPlayer: () -> Unit = { nav.navigate(Routes.PLAYER) { launchSingleTop = true } }
    val openItem: (YTItem) -> Unit = { item ->
        AppModule.recents.add(item)
        when (item) {
            is SongItem -> playerVm.playWithRadio(item)
            is ArtistItem -> openArtist(item.id)
            is AlbumItem -> openCollection(item.id)
        }
    }
    val selectTab: (String) -> Unit = { tabRoute ->
        navCollapsed = false
        nav.navigate(tabRoute) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Deteksi arah gulir untuk melipat navbar kaca menjadi tombol lingkaran + mini player di sebelahnya
    val collapseScrollConnection = remember(uiPrefs.isGlass, uiPrefs.collapseNavbarOnScroll) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (uiPrefs.isGlass && uiPrefs.collapseNavbarOnScroll) {
                    if (available.y < -18f && !navCollapsed) {
                        navCollapsed = true
                    } else if (available.y > 14f && navCollapsed) {
                        navCollapsed = false
                    }
                }
                return Offset.Zero
            }
        }
    }

    val context = LocalContext.current
    val hazeState = remember { HazeState() }
    val bottomOverlayPadding = when {
        route == Routes.PLAYER -> 0.dp
        uiPrefs.isGlass && now.hasMedia -> 162.dp
        uiPrefs.isGlass -> 96.dp
        now.hasMedia -> 144.dp
        else -> 82.dp
    }

    VyoraTheme(prefs = uiPrefs, palette = coverPalette) {
        CompositionLocalProvider(
            LocalHazeState provides hazeState,
            LocalBottomOverlayPadding provides bottomOverlayPadding,
            LocalQueueActions provides QueueActions { song, next ->
                val shuffleWasOn = now.shuffle && now.hasMedia && next
                playerVm.enqueue(song, next)
                val message = when {
                    shuffleWasOn -> "Ditambahkan berikutnya. Mode acak dimatikan."
                    !now.hasMedia -> "Memutar lagu"
                    next -> "Akan diputar berikutnya"
                    else -> "Ditambahkan ke antrean"
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
        ) {
            Box(Modifier.fillMaxSize()) {
                // Konten layar penuh menjadi sumber blur backdrop bagi Liquid Glass floating bars
                AmbientBackdrop(
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(state = hazeState)
                        .nestedScroll(collapseScrollConnection),
                ) {
                    NavHost(
                        navController = nav,
                        startDestination = Routes.HOME,
                        modifier = Modifier.fillMaxSize(),
                        // Gerak halaman: masuk dengan geser halus + fade, keluar cepat.
                        enterTransition = {
                            fadeIn(tween(Motion.NORMAL)) +
                                slideInHorizontally(tween(Motion.NORMAL)) { it / 14 }
                        },
                        exitTransition = { fadeOut(tween(Motion.FAST)) },
                        popEnterTransition = { fadeIn(tween(Motion.NORMAL)) },
                        popExitTransition = {
                            fadeOut(tween(Motion.FAST)) +
                                slideOutHorizontally(tween(Motion.NORMAL)) { it / 14 }
                        },
                    ) {
                        composable(Routes.HOME) {
                            HomeScreen(
                                currentSongId = currentId,
                                onPlaySongs = play,
                                onShuffleSongs = shuffle,
                                onOpenItem = openItem,
                                onOpenCollection = openCollection,
                                now = now,
                                onOpenPlayer = openPlayer,
                                onOpenHistory = { showHistory = true },
                                onOpenStats = { showStats = true },
                                onOpenSettings = { showSettings = true },
                            )
                        }
                        composable(Routes.SEARCH) {
                            SearchScreen(
                                currentSongId = currentId,
                                onPlaySong = playerVm::playWithRadio,
                                onArtistClick = openArtist,
                                onCollectionClick = openCollection,
                            )
                        }
                        composable(Routes.LIBRARY) {
                            LibraryScreen(
                                onOpenPlaylist = { nav.navigate(Routes.playlist(it)) },
                                onOpenSettings = { showSettings = true },
                                onOpenStats = { showStats = true },
                                onOpenDiscord = { showDiscord = true },
                                onPlaySongs = play,
                                onShuffle = shuffle,
                            )
                        }
                        composable(
                            Routes.PLAYLIST,
                            arguments = listOf(navArgument("id") { type = NavType.StringType }),
                        ) { entry ->
                            PlaylistScreen(
                                playlistId = entry.arguments?.getString("id").orEmpty(),
                                currentSongId = currentId,
                                onPlaySongs = play,
                                onShuffle = shuffle,
                                onFindSongs = { selectTab(Routes.SEARCH) },
                                onBack = { nav.popBackStack() },
                            )
                        }
                        composable(
                            Routes.ARTIST,
                            arguments = listOf(navArgument("id") { type = NavType.StringType }),
                        ) { entry ->
                            ArtistScreen(
                                artistId = entry.arguments?.getString("id").orEmpty(),
                                currentSongId = currentId,
                                onPlaySongs = play,
                                onShuffle = shuffle,
                                onOpenItem = openItem,
                                onOpenCollection = openCollection,
                                onBack = { nav.popBackStack() },
                            )
                        }
                        composable(
                            Routes.COLLECTION,
                            arguments = listOf(navArgument("id") { type = NavType.StringType }),
                        ) { entry ->
                            CollectionScreen(
                                browseId = entry.arguments?.getString("id").orEmpty(),
                                currentSongId = currentId,
                                onPlaySongs = play,
                                onShuffle = shuffle,
                                onArtistClick = openArtist,
                                onBack = { nav.popBackStack() },
                            )
                        }
                        composable(
                            Routes.PLAYER,
                            enterTransition = { slideInVertically(tween(320)) { it } },
                            exitTransition = { slideOutVertically(tween(280)) { it } },
                            popExitTransition = { slideOutVertically(tween(280)) { it } },
                        ) {
                            PlayerScreen(
                                state = now,
                                isFavorite = isCurrentLiked,
                                onCollapse = { nav.popBackStack() },
                                onPlayPause = playerVm::togglePlayPause,
                                onNext = playerVm::next,
                                onPrevious = playerVm::previous,
                                onSeek = playerVm::seekTo,
                                onToggleShuffle = playerVm::toggleShuffle,
                                onCycleRepeat = playerVm::cycleRepeat,
                                onToggleFavorite = playerVm::toggleFavorite,
                                onAddToPlaylist = { now.song?.let { PlaylistPicker.open(it) } },
                                onArtistClick = { id ->
                                    nav.popBackStack()
                                    openArtist(id)
                                },
                                lyrics = lyrics,
                                onRetryLyrics = playerVm::retryLyrics,
                                sleepRemainingMs = sleepRemaining,
                                onSetSleepTimer = SleepTimer::start,
                                onCancelSleepTimer = SleepTimer::cancel,
                                onPlayQueueItem = playerVm::playQueueItem,
                                onRemoveQueueItem = playerVm::removeQueueItem,
                                onMoveQueueItem = playerVm::moveQueueItem,
                                onRetryPlayback = playerVm::retryPlayback,
                                onToggleVideoMode = playerVm::toggleVideoMode,
                                onAttachVideoSurface = playerVm::attachVideoSurface,
                            )
                        }
                    }
                }

                // Floating Liquid Glass / Editorial MiniPlayer + Navbar di atas konten
                AnimatedVisibility(
                    visible = route != Routes.PLAYER,
                    enter = fadeIn(tween(200)) + slideInVertically(tween(240)) { it / 2 },
                    exit = fadeOut(tween(180)) + slideOutVertically(tween(220)) { it / 2 },
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    FloatingBottomChrome(
                        tabs = navTabs,
                        currentRoute = route,
                        now = now,
                        isFavorite = isCurrentLiked,
                        collapsed = navCollapsed,
                        onExpandNav = { navCollapsed = false },
                        onSelectTab = selectTab,
                        onOpenPlayer = openPlayer,
                        onPlayPause = playerVm::togglePlayPause,
                        onNext = playerVm::next,
                        onToggleFavorite = playerVm::toggleFavorite,
                    )
                }
            }

            if (showSettings) {
                AppearanceSheet(
                    prefs = uiPrefs,
                    onDismiss = { showSettings = false },
                    onOpenAudioFx = {
                        showSettings = false
                        showAudioFx = true
                    },
                    onOpenDiscord = {
                        showSettings = false
                        showDiscord = true
                    },
                )
            }
            if (showDiscord) {
                DiscordSettingsSheet(
                    now = now,
                    onDismiss = { showDiscord = false },
                )
            }
            if (showAudioFx) {
                AudioFxSheet(onDismiss = { showAudioFx = false })
            }
            if (showStats) {
                StatsSheet(
                    onDismiss = { showStats = false },
                    onPlaySong = playerVm::playWithRadio,
                    onOpenArtist = openArtist,
                )
            }
            if (showHistory) {
                HistorySheet(
                    onOpenItem = openItem,
                    onDismiss = { showHistory = false },
                )
            }
            AddToPlaylistHost()
            SongPosterHost()
        }
    }
}
