package com.devson.vedtune.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devson.vedtune.ui.songs.SongsScreen
import com.devson.vedtune.ui.songs.SongsViewModel
import com.devson.vedtune.ui.player.PlayerScreen
import com.devson.vedtune.ui.player.PlayerViewModel
import com.devson.vedtune.ui.albums.AlbumsScreen
import com.devson.vedtune.ui.albums.AlbumsViewModel
import com.devson.vedtune.ui.albums.AlbumDetailsScreen
import com.devson.vedtune.ui.albums.AlbumDetailsViewModel
import com.devson.vedtune.ui.artists.ArtistsScreen
import com.devson.vedtune.ui.artists.ArtistsViewModel
import com.devson.vedtune.ui.artists.ArtistDetailsScreen
import com.devson.vedtune.ui.artists.ArtistDetailsViewModel
import com.devson.vedtune.ui.settings.SettingsScreen
import com.devson.vedtune.ui.settings.SettingsViewModel
import com.devson.vedtune.ui.playlists.PlaylistsScreen
import com.devson.vedtune.ui.playlists.PlaylistsViewModel
import com.devson.vedtune.ui.playlists.PlaylistDetailsScreen
import com.devson.vedtune.ui.playlists.PlaylistDetailsViewModel

sealed class Screen(val route: String) {
    data object Songs : Screen("songs")
    data object Albums : Screen("albums")
    data object Artists : Screen("artists")
    data object Playlists : Screen("playlists")
    data object Settings : Screen("settings")
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
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Songs.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        composable(Screen.Songs.route) {
            val viewModel: SongsViewModel = hiltViewModel()
            SongsScreen(viewModel = viewModel)
        }
        composable(Screen.Albums.route) {
            val viewModel: AlbumsViewModel = hiltViewModel()
            AlbumsScreen(
                viewModel = viewModel,
                onAlbumClick = { albumId ->
                    navController.navigate(Screen.AlbumDetails.createRoute(albumId))
                }
            )
        }
        composable(Screen.Artists.route) {
            val viewModel: ArtistsViewModel = hiltViewModel()
            ArtistsScreen(
                viewModel = viewModel,
                onArtistClick = { artistName ->
                    navController.navigate(Screen.ArtistDetails.createRoute(artistName))
                }
            )
        }
        composable(Screen.Playlists.route) {
            val viewModel: PlaylistsViewModel = hiltViewModel()
            PlaylistsScreen(
                viewModel = viewModel,
                onPlaylistClick = { playlistId ->
                    navController.navigate(Screen.PlaylistDetails.createRoute(playlistId))
                }
            )
        }
        composable(
            route = Screen.Settings.route,
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
            SettingsScreen(viewModel = viewModel)
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
                onBackClick = { navController.popBackStack() }
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
                onBackClick = { navController.popBackStack() }
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
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title)
    }
}
