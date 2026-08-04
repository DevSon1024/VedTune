package com.devson.vedtune.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavController
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devson.vedtune.ui.MainViewModel
import com.devson.vedtune.ui.home.HomeScreen
import com.devson.vedtune.ui.player.PlayerScreen
import com.devson.vedtune.ui.player.PlayerViewModel
import com.devson.vedtune.ui.albums.AlbumDetailsScreen
import com.devson.vedtune.ui.albums.AlbumDetailsViewModel
import com.devson.vedtune.ui.artists.ArtistDetailsScreen
import com.devson.vedtune.ui.artists.ArtistDetailsViewModel
import com.devson.vedtune.ui.settings.SettingsViewModel
import com.devson.vedtune.ui.settings.FolderSettingsScreen
import com.devson.vedtune.ui.settings.AppearanceSettingsScreen
import com.devson.vedtune.ui.settings.PlayerInterfaceSettingScreen
import com.devson.vedtune.ui.settings.PlaybackSettingsScreen
import com.devson.vedtune.ui.settings.LibrarySettingsScreen
import com.devson.vedtune.ui.playlists.PlaylistDetailsScreen
import com.devson.vedtune.ui.playlists.PlaylistDetailsViewModel
import com.devson.vedtune.ui.songs.EditTagsScreen
import com.devson.vedtune.ui.songs.EditTagsViewModel
import com.devson.vedtune.ui.lyrics.LyricsEditorScreen
import com.devson.vedtune.ui.lyrics.LyricsEditorViewModel
import com.devson.vedtune.ui.settings.LyricsConverterScreen
import com.devson.vedtune.ui.settings.AboutScreen
import com.devson.vedtune.ui.settings.CreditsScreen
import com.devson.vedtune.ui.genres.GenreDetailsScreen
import com.devson.vedtune.ui.genres.GenreDetailsViewModel

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Songs : Screen("songs")
    data object Albums : Screen("albums")
    data object Artists : Screen("artists")
    data object Playlists : Screen("playlists")
    data object Settings : Screen("settings")
    data object FolderSettings : Screen("folder_settings")
    data object AppearanceSettings : Screen("appearance_settings")
    data object PlayerInterfaceSettings : Screen("player_interface_settings")
    data object PlaybackSettings : Screen("playback_settings")
    data object LibrarySettings : Screen("library_settings")
    data object Player : Screen("player")
    data object AlbumDetails : Screen("album_details/{albumId}") {
        fun createRoute(albumId: Long) = "album_details/$albumId"
    }
    data object ArtistDetails : Screen("artist_details/{artistName}") {
        fun createRoute(artistName: String) = "artist_details/$artistName"
    }
    data object PlaylistDetails : Screen("playlist_details/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist_details/$playlistId"
    }
    data object EditTags : Screen("edit_tags/{songId}") {
        fun createRoute(songId: Long) = "edit_tags/$songId"
    }
    data object LyricsEditor : Screen("lyrics_editor/{songId}") {
        fun createRoute(songId: Long) = "lyrics_editor/$songId"
    }
    data object LyricsConverter : Screen("lyrics_converter")
    data object About : Screen("about")
    data object Credits : Screen("credits")
    
    data object HomeTab : Screen("home_tab")
    data object SearchTab : Screen("search_tab")
    data object LibraryTab : Screen("library_tab")
    data object GenreDetails : Screen("genre_details/{genreName}") {
        fun createRoute(genreName: String) = "genre_details/${android.net.Uri.encode(genreName)}"
    }
}

private val horizontalEnterTransition: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.EnterTransition = {
    fadeIn(animationSpec = tween(200)) + slideInHorizontally(
        initialOffsetX = { it / 8 },
        animationSpec = tween(250, easing = FastOutSlowInEasing)
    )
}

private val horizontalExitTransition: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.ExitTransition = {
    fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
}

@Composable
fun NavGraph(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing)) },
        exitTransition = { fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing)) }
    ) {
        composable(Screen.Home.route) {
            val defaultStartScreen by mainViewModel.defaultStartScreen.collectAsState()
            HomeScreen(
                navController = navController,
                onNavigateToAlbum = { albumId ->
                    navController.navigateSafe(Screen.AlbumDetails.createRoute(albumId))
                },
                onNavigateToArtist = { artistName ->
                    navController.navigateSafe(Screen.ArtistDetails.createRoute(artistName))
                },
                onNavigateToPlaylist = { playlistId ->
                    navController.navigateSafe(Screen.PlaylistDetails.createRoute(playlistId))
                },
                onNavigateToFolderSettings = {
                    navController.navigateSafe(Screen.FolderSettings.route)
                },
                onNavigateToAppearanceSettings = {
                    navController.navigateSafe(Screen.AppearanceSettings.route)
                },
                onNavigateToPlayerInterfaceSettings = {
                    navController.navigateSafe(Screen.PlayerInterfaceSettings.route)
                },
                onNavigateToPlaybackSettings = {
                    navController.navigateSafe(Screen.PlaybackSettings.route)
                },
                onNavigateToLibrarySettings = {
                    navController.navigateSafe(Screen.LibrarySettings.route)
                },
                onNavigateToEditTags = { songId ->
                    navController.navigateSafe(Screen.EditTags.createRoute(songId))
                },
                onNavigateToLyricsConverter = {
                    navController.navigateSafe(Screen.LyricsConverter.route)
                },
                onNavigateToAbout = {
                    navController.navigateSafe(Screen.About.route)
                },
                onNavigateToGenre = { genreName ->
                    navController.navigateSafe(Screen.GenreDetails.createRoute(genreName))
                },
                defaultStartScreen = defaultStartScreen,
                mainViewModel = mainViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
        composable(
            route = Screen.FolderSettings.route,
            enterTransition = horizontalEnterTransition,
            exitTransition = horizontalExitTransition
        ) {
            val viewModel: SettingsViewModel = hiltViewModel()
            FolderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStackSafe() }
            )
        }
        composable(
            route = Screen.AppearanceSettings.route,
            enterTransition = horizontalEnterTransition,
            exitTransition = horizontalExitTransition
        ) {
            val viewModel: SettingsViewModel = hiltViewModel()
            AppearanceSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStackSafe() }
            )
        }
        composable(
            route = Screen.PlayerInterfaceSettings.route,
            enterTransition = horizontalEnterTransition,
            exitTransition = horizontalExitTransition
        ) {
            val viewModel: SettingsViewModel = hiltViewModel()
            PlayerInterfaceSettingScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStackSafe() }
            )
        }
        composable(
            route = Screen.PlaybackSettings.route,
            enterTransition = horizontalEnterTransition,
            exitTransition = horizontalExitTransition
        ) {
            val viewModel: SettingsViewModel = hiltViewModel()
            PlaybackSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStackSafe() }
            )
        }
        composable(
            route = Screen.LibrarySettings.route,
            enterTransition = horizontalEnterTransition,
            exitTransition = horizontalExitTransition
        ) {
            val viewModel: SettingsViewModel = hiltViewModel()
            LibrarySettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStackSafe() },
                onNavigateToFolderSettings = {
                    navController.navigateSafe(Screen.FolderSettings.route)
                }
            )
        }
        composable(
            route = Screen.ArtistDetails.route,
            arguments = listOf(
                navArgument("artistName") { type = NavType.StringType }
            ),
            enterTransition = horizontalEnterTransition,
            exitTransition = horizontalExitTransition
        ) {
            val viewModel: ArtistDetailsViewModel = hiltViewModel()
            ArtistDetailsScreen(
                viewModel = viewModel,
                mainViewModel = mainViewModel,
                onBackClick = { navController.popBackStackSafe() },
                onNavigateToPlayer = { navController.navigateSafe(Screen.Player.route) }
            )
        }
        composable(
            route = Screen.Player.route,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                )
            }
        ) {
            val viewModel: PlayerViewModel = hiltViewModel()
            PlayerScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStackSafe() },
                onNavigateToArtist = { artistName ->
                    navController.navigateSafe(Screen.ArtistDetails.createRoute(artistName))
                },
                onNavigateToAlbum = { albumId ->
                    navController.navigateSafe(Screen.AlbumDetails.createRoute(albumId))
                },
                onNavigateToEditTags = { songId ->
                    navController.navigateSafe(Screen.EditTags.createRoute(songId))
                },
                onNavigateToLyricsEditor = { songId ->
                    navController.navigateSafe(Screen.LyricsEditor.createRoute(songId))
                },
                onNavigateToLocation = { songId ->
                    navController.popBackStackSafe()
                    mainViewModel.navigateToLocation(songId)
                }
            )
        }
        composable(
            route = Screen.AlbumDetails.route,
            arguments = listOf(
                navArgument("albumId") { type = NavType.LongType }
            ),
            enterTransition = horizontalEnterTransition,
            exitTransition = horizontalExitTransition
        ) {
            val viewModel: AlbumDetailsViewModel = hiltViewModel()
            AlbumDetailsScreen(
                viewModel = viewModel,
                mainViewModel = mainViewModel,
                onBackClick = { navController.popBackStackSafe() },
                onNavigateToPlayer = { navController.navigateSafe(Screen.Player.route) }
            )
        }
        composable(
            route = Screen.PlaylistDetails.route,
            arguments = listOf(
                navArgument("playlistId") { type = NavType.LongType }
            ),
            enterTransition = horizontalEnterTransition,
            exitTransition = horizontalExitTransition
        ) {
            val viewModel: PlaylistDetailsViewModel = hiltViewModel()
            PlaylistDetailsScreen(
                viewModel = viewModel,
                mainViewModel = mainViewModel,
                onBackClick = { navController.popBackStackSafe() },
                onNavigateToPlayer = { navController.navigateSafe(Screen.Player.route) }
            )
        }
        composable(
            route = Screen.EditTags.route,
            arguments = listOf(
                navArgument("songId") { type = NavType.LongType }
            ),
            enterTransition = horizontalEnterTransition,
            exitTransition = horizontalExitTransition
        ) {
            val viewModel: EditTagsViewModel = hiltViewModel()
            EditTagsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStackSafe() },
                onNavigateToLyricsEditor = { songId ->
                    navController.navigateSafe(Screen.LyricsEditor.createRoute(songId))
                }
            )
        }
        composable(
            route = Screen.LyricsEditor.route,
            arguments = listOf(
                navArgument("songId") { type = NavType.LongType }
            ),
            enterTransition = horizontalEnterTransition,
            exitTransition = horizontalExitTransition
        ) {
            val viewModel: LyricsEditorViewModel = hiltViewModel()
            LyricsEditorScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStackSafe() }
            )
        }
        composable(
            route = Screen.LyricsConverter.route,
            enterTransition = horizontalEnterTransition,
            exitTransition = horizontalExitTransition
        ) {
            LyricsConverterScreen(
                onNavigateBack = { navController.popBackStackSafe() }
            )
        }
        composable(
            route = Screen.GenreDetails.route,
            arguments = listOf(
                navArgument("genreName") { type = NavType.StringType }
            ),
            enterTransition = horizontalEnterTransition,
            exitTransition = horizontalExitTransition
        ) {
            val viewModel: GenreDetailsViewModel = hiltViewModel()
            GenreDetailsScreen(
                viewModel = viewModel,
                mainViewModel = mainViewModel,
                onBackClick = { navController.popBackStackSafe() },
                onNavigateToPlayer = { navController.navigateSafe(Screen.Player.route) }
            )
        }
        composable(
            route = Screen.About.route,
            enterTransition = horizontalEnterTransition,
            exitTransition = horizontalExitTransition
        ) {
            AboutScreen(
                onBack = { navController.popBackStackSafe() },
                onNavigateToCredits = { navController.navigateSafe(Screen.Credits.route) }
            )
        }
        composable(
            route = Screen.Credits.route,
            enterTransition = horizontalEnterTransition,
            exitTransition = horizontalExitTransition
        ) {
            CreditsScreen(
                onBack = { navController.popBackStackSafe() }
            )
        }
    }
}

fun NavController.navigateSafe(route: String) {
    val currentEntry = currentBackStackEntry
    if (currentEntry == null || currentEntry.lifecycle.currentState == Lifecycle.State.RESUMED) {
        navigate(route)
    }
}

fun NavController.popBackStackSafe() {
    val currentEntry = currentBackStackEntry
    if (currentEntry != null && currentEntry.lifecycle.currentState == Lifecycle.State.RESUMED) {
        if (previousBackStackEntry != null) {
            popBackStack()
        }
    }
}
