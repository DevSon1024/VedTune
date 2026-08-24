package com.devson.vedtune.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.devson.vedtune.ui.MainViewModel
import com.devson.vedtune.ui.components.MiniPlayer
import com.devson.vedtune.ui.navigation.navigateSafe
import com.devson.vedtune.ui.settings.SettingsScreen
import com.devson.vedtune.ui.settings.SettingsViewModel
import com.devson.vedtune.ui.navigation.Screen
import com.devson.vedtune.ui.components.VedTuneBottomNavBar
import com.devson.vedtune.ui.search.SearchScreen
import com.devson.vedtune.ui.search.SearchViewModel
import com.devson.vedtune.ui.library.LibraryScreen
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    navController: NavHostController,
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToPlaylist: (Long) -> Unit,
    onNavigateToGenre: (String) -> Unit,
    onNavigateToFolderSettings: () -> Unit,
    onNavigateToAppearanceSettings: () -> Unit,
    onNavigateToPlayerInterfaceSettings: () -> Unit,
    onNavigateToPlaybackSettings: () -> Unit,
    onNavigateToLibrarySettings: () -> Unit,
    onNavigateToEditTags: (Long) -> Unit,
    onNavigateToLyricsConverter: () -> Unit,
    onNavigateToAbout: () -> Unit = {},
    defaultStartScreen: String,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val tabRoutes = listOf(
        Screen.HomeTab.route,
        Screen.SearchTab.route,
        Screen.LibraryTab.route,
        Screen.Settings.route
    )
    val initialPage = remember(defaultStartScreen) {
        val resolvedRoute = when (defaultStartScreen) {
            "songs", "albums", "artists", "playlists", "library_tab" -> Screen.LibraryTab.route
            "home_tab" -> Screen.HomeTab.route
            "search_tab" -> Screen.SearchTab.route
            "settings" -> Screen.Settings.route
            else -> Screen.HomeTab.route
        }
        val index = tabRoutes.indexOf(resolvedRoute)
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
                            navController.navigateSafe(Screen.Player.route)
                        },
                        showArtwork = showAlbumArt,
                        showProgress = showMiniPlayerProgress,
                        isGestureEnabled = isGestureMiniPlayerEnabled
                    )
                }

                VedTuneBottomNavBar(
                    currentRoute = tabRoutes.getOrNull(selectedIndex),
                    onNavigate = { route ->
                        val index = tabRoutes.indexOf(route)
                        if (index != -1) {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> {
                    val viewModel: HomeViewModel = hiltViewModel()
                    HomeTabScreen(
                        viewModel = viewModel,
                        onNavigateToAlbum = onNavigateToAlbum,
                        onNavigateToArtist = onNavigateToArtist,
                        onNavigateToPlaylist = onNavigateToPlaylist,
                        onNavigateToGenre = onNavigateToGenre,
                        onNavigateToLibraryTab = { tabIndex ->
                            scope.launch {
                                pagerState.animateScrollToPage(2)
                            }
                        },
                        contentPadding = innerPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                1 -> {
                    val viewModel: SearchViewModel = hiltViewModel()
                    SearchScreen(
                        viewModel = viewModel,
                        onNavigateToAlbum = onNavigateToAlbum,
                        onNavigateToArtist = onNavigateToArtist,
                        onNavigateToGenre = onNavigateToGenre,
                        contentPadding = innerPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                2 -> {
                    LibraryScreen(
                        onNavigateToAlbum = onNavigateToAlbum,
                        onNavigateToArtist = onNavigateToArtist,
                        onNavigateToPlaylist = onNavigateToPlaylist,
                        onNavigateToGenre = onNavigateToGenre,
                        onNavigateToEditTags = onNavigateToEditTags,
                        navigateToLocationEvent = mainViewModel.navigateToLocationEvent,
                        contentPadding = innerPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                3 -> {
                    val viewModel: SettingsViewModel = hiltViewModel()
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToAppearanceSettings = onNavigateToAppearanceSettings,
                        onNavigateToPlayerInterfaceSettings = onNavigateToPlayerInterfaceSettings,
                        onNavigateToPlaybackSettings = onNavigateToPlaybackSettings,
                        onNavigateToLibrarySettings = onNavigateToLibrarySettings,
                        onNavigateToLyricsConverter = onNavigateToLyricsConverter,
                        onNavigateToAbout = onNavigateToAbout,
                        contentPadding = innerPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
