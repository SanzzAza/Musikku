package com.example.musikku.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.musikku.AppModule
import com.example.musikku.data.model.Track
import com.example.musikku.player.PlayerViewModel
import com.example.musikku.ui.album.AlbumScreen
import com.example.musikku.ui.artist.ArtistScreen
import com.example.musikku.ui.home.HomeScreen
import com.example.musikku.ui.library.LibraryScreen
import com.example.musikku.ui.player.MiniPlayer
import com.example.musikku.ui.player.PlayerScreen
import com.example.musikku.ui.search.SearchScreen

private object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val ARTIST = "artist/{id}"
    const val ALBUM = "album/{id}"
    const val PLAYER = "player"
    fun artist(id: Long) = "artist/$id"
    fun album(id: Long) = "album/$id"
}

private data class TabItem(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.HOME, "Beranda", Icons.Outlined.Home, Icons.Filled.Home),
    TabItem(Routes.SEARCH, "Cari", Icons.Outlined.Search, Icons.Filled.Search),
    TabItem(Routes.LIBRARY, "Koleksi", Icons.Outlined.LibraryMusic, Icons.Filled.LibraryMusic),
)

@Composable
fun MusikkuApp(playerVm: PlayerViewModel = viewModel()) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val now by playerVm.state.collectAsStateWithLifecycle()
    val liked by AppModule.favorites.items.collectAsStateWithLifecycle()
    val lyrics by playerVm.lyrics.collectAsStateWithLifecycle()
    val currentId = now.track?.id

    // Izin notifikasi (Android 13+) untuk notifikasi kontrol musik
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val play: (List<Track>, Int) -> Unit = playerVm::play
    val shuffle: (List<Track>) -> Unit = playerVm::shuffle
    val openArtist: (Long) -> Unit = { nav.navigate(Routes.artist(it)) }
    val openAlbum: (Long) -> Unit = { nav.navigate(Routes.album(it)) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (route != Routes.PLAYER) {
                Column {
                    if (now.hasMedia) {
                        MiniPlayer(
                            state = now,
                            onClick = { nav.navigate(Routes.PLAYER) { launchSingleTop = true } },
                            onPlayPause = playerVm::togglePlayPause,
                            onNext = playerVm::next,
                        )
                    }
                    BottomBar(nav, route)
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(currentId, play, openArtist, openAlbum)
            }
            composable(Routes.SEARCH) {
                SearchScreen(currentId, play, openArtist, openAlbum)
            }
            composable(Routes.LIBRARY) {
                LibraryScreen(currentId, play, shuffle)
            }
            composable(Routes.ARTIST, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                ArtistScreen(
                    artistId = entry.arguments?.getLong("id") ?: 0L,
                    currentTrackId = currentId,
                    onPlay = play,
                    onShuffle = shuffle,
                    onAlbumClick = openAlbum,
                    onArtistClick = openArtist,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.ALBUM, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                AlbumScreen(
                    albumId = entry.arguments?.getLong("id") ?: 0L,
                    currentTrackId = currentId,
                    onPlay = play,
                    onShuffle = shuffle,
                    onArtistClick = openArtist,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                Routes.PLAYER,
                enterTransition = { slideInVertically(tween(300)) { it } },
                exitTransition = { slideOutVertically(tween(300)) { it } },
                popExitTransition = { slideOutVertically(tween(300)) { it } },
            ) {
                PlayerScreen(
                    state = now,
                    isFavorite = liked.any { it.id == currentId },
                    onCollapse = { nav.popBackStack() },
                    onPlayPause = playerVm::togglePlayPause,
                    onNext = playerVm::next,
                    onPrevious = playerVm::previous,
                    onSeek = playerVm::seekTo,
                    onToggleShuffle = playerVm::toggleShuffle,
                    onCycleRepeat = playerVm::cycleRepeat,
                    onToggleFavorite = playerVm::toggleFavorite,
                    onArtistClick = { id ->
                        nav.popBackStack()
                        openArtist(id)
                    },
                    lyrics = lyrics,
                    onRetryLyrics = playerVm::retryLyrics,
                )
            }
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController, route: String?) {
    NavigationBar(containerColor = Color(0xF20E0E10)) {
        tabs.forEach { tab ->
            val selected = route == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    nav.navigate(tab.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(if (selected) tab.selectedIcon else tab.icon, tab.label) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent,
                )
            )
        }
    }
}
