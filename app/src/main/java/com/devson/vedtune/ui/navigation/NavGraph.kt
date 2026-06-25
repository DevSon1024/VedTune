package com.devson.vedtune.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.devson.vedtune.ui.playlists.PlaylistDetailsScreen
import com.devson.vedtune.ui.playlists.PlaylistDetailsViewModel
import com.devson.vedtune.ui.songs.EditTagsScreen
import com.devson.vedtune.ui.songs.EditTagsViewModel

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Songs : Screen("songs")
    data object Albums : Screen("albums")
    data object Artists : Screen("artists")
    data object Playlists : Screen("playlists")
    data object Settings : Screen("settings")
    data object FolderSettings : Screen("folder_settings")
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
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) }
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
                onNavigateToEditTags = { songId ->
                    navController.navigateSafe(Screen.EditTags.createRoute(songId))
                },
                defaultStartScreen = defaultStartScreen,
                mainViewModel = mainViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
        composable(
            route = Screen.FolderSettings.route,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(350)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(350)
                )
            }
        ) {
            val viewModel: SettingsViewModel = hiltViewModel()
            FolderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.ArtistDetails.route,
            arguments = listOf(
                navArgument("artistName") { type = NavType.StringType }
            ),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(350)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(350)
                )
            }
        ) {
            val viewModel: ArtistDetailsViewModel = hiltViewModel()
            ArtistDetailsScreen(
                viewModel = viewModel,
                mainViewModel = mainViewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToPlayer = { navController.navigateSafe(Screen.Player.route) }
            )
        }
        composable(
            route = Screen.Player.route,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(400)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(400)
                )
            }
        ) {
            val viewModel: PlayerViewModel = hiltViewModel()
            PlayerScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.AlbumDetails.route,
            arguments = listOf(
                navArgument("albumId") { type = NavType.LongType }
            ),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(350)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(350)
                )
            }
        ) {
            val viewModel: AlbumDetailsViewModel = hiltViewModel()
            AlbumDetailsScreen(
                viewModel = viewModel,
                mainViewModel = mainViewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToPlayer = { navController.navigateSafe(Screen.Player.route) }
            )
        }
        composable(
            route = Screen.PlaylistDetails.route,
            arguments = listOf(
                navArgument("playlistId") { type = NavType.LongType }
            ),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(350)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(350)
                )
            }
        ) {
            val viewModel: PlaylistDetailsViewModel = hiltViewModel()
            PlaylistDetailsScreen(
                viewModel = viewModel,
                mainViewModel = mainViewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToPlayer = { navController.navigateSafe(Screen.Player.route) }
            )
        }
        composable(
            route = Screen.EditTags.route,
            arguments = listOf(
                navArgument("songId") { type = NavType.LongType }
            ),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(350)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(350)
                )
            }
        ) {
            val viewModel: EditTagsViewModel = hiltViewModel()
            EditTagsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
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
