package com.devson.vedtune.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.devson.vedtune.ui.MainViewModel
import com.devson.vedtune.ui.components.MiniPlayer
import com.devson.vedtune.ui.songs.SongsScreen
import com.devson.vedtune.ui.songs.SongsViewModel
import com.devson.vedtune.ui.albums.AlbumsScreen
import com.devson.vedtune.ui.albums.AlbumsViewModel
import com.devson.vedtune.ui.artists.ArtistsScreen
import com.devson.vedtune.ui.artists.ArtistsViewModel
import com.devson.vedtune.ui.playlists.PlaylistsScreen
import com.devson.vedtune.ui.playlists.PlaylistsViewModel
import com.devson.vedtune.ui.settings.SettingsScreen
import com.devson.vedtune.ui.settings.SettingsViewModel
import com.devson.vedtune.ui.navigation.Screen
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    navController: NavHostController,
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToPlaylist: (Long) -> Unit,
    onNavigateToFolderSettings: () -> Unit,
    onNavigateToEditTags: (Long) -> Unit,
    defaultStartScreen: String,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val tabRoutes = listOf(
        Screen.Songs.route,
        Screen.Albums.route,
        Screen.Artists.route,
        Screen.Playlists.route,
        Screen.Settings.route
    )
    val initialPage = remember(defaultStartScreen) {
        val index = tabRoutes.indexOf(defaultStartScreen)
        if (index != -1) index else 0
    }
    
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { tabRoutes.size }
    )
    val scope = rememberCoroutineScope()
    val selectedIndex = pagerState.currentPage

    val currentSong by mainViewModel.currentSong.collectAsState()
    val isPlaying by mainViewModel.isPlaying.collectAsState()
    val position by mainViewModel.playbackPosition.collectAsState()
    val duration by mainViewModel.playbackDuration.collectAsState()
    val showAlbumArt by mainViewModel.showAlbumArt.collectAsState()
    val showMiniPlayerProgress by mainViewModel.showMiniPlayerProgress.collectAsState()
    val isGestureMiniPlayerEnabled by mainViewModel.isGestureMiniPlayerEnabled.collectAsState()

    val progress = remember(position, duration) {
        if (duration > 0) position.toFloat() / duration.toFloat() else 0f
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Column {
                if (tabRoutes.getOrNull(selectedIndex) != Screen.Settings.route) {
                    MiniPlayer(
                        song = currentSong,
                        isPlaying = isPlaying,
                        progress = progress,
                        onPlayPauseClick = {
                            if (isPlaying) mainViewModel.pause() else mainViewModel.play()
                        },
                        onSkipNextClick = {
                            mainViewModel.skipToNext()
                        },
                        onSkipPreviousClick = {
                            mainViewModel.skipToPrevious()
                        },
                        onClick = {
                            navController.navigate(Screen.Player.route)
                        },
                        showArtwork = showAlbumArt,
                        showProgress = showMiniPlayerProgress,
                        isGestureEnabled = isGestureMiniPlayerEnabled
                    )
                }

                val items = listOf(
                    HomeNavigationItem("Songs", Screen.Songs.route, Icons.AutoMirrored.Filled.List),
                    HomeNavigationItem("Albums", Screen.Albums.route, Icons.Default.PlayArrow),
                    HomeNavigationItem("Artists", Screen.Artists.route, Icons.Default.Person),
                    HomeNavigationItem("Playlists", Screen.Playlists.route, Icons.Default.Favorite),
                    HomeNavigationItem("Settings", Screen.Settings.route, Icons.Default.Settings)
                )
                NavigationBar {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedIndex == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                            label = { Text(text = item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> {
                    val viewModel: SongsViewModel = hiltViewModel()
                    SongsScreen(
                        viewModel = viewModel,
                        onNavigateToAlbum = onNavigateToAlbum,
                        onNavigateToArtist = onNavigateToArtist,
                        onNavigateToEditTags = onNavigateToEditTags,
                        contentPadding = innerPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                1 -> {
                    val viewModel: AlbumsViewModel = hiltViewModel()
                    AlbumsScreen(
                        viewModel = viewModel,
                        onAlbumClick = onNavigateToAlbum,
                        contentPadding = innerPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                2 -> {
                    val viewModel: ArtistsViewModel = hiltViewModel()
                    ArtistsScreen(
                        viewModel = viewModel,
                        onArtistClick = onNavigateToArtist,
                        contentPadding = innerPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                3 -> {
                    val viewModel: PlaylistsViewModel = hiltViewModel()
                    PlaylistsScreen(
                        viewModel = viewModel,
                        onPlaylistClick = onNavigateToPlaylist,
                        contentPadding = innerPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                4 -> {
                    val viewModel: SettingsViewModel = hiltViewModel()
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToFolderSettings = onNavigateToFolderSettings,
                        contentPadding = innerPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

private data class HomeNavigationItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)
