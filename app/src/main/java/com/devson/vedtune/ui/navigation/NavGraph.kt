package com.devson.vedtune.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
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
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Songs.route,
        modifier = modifier
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
            PlaceholderScreen(title = "Artists Screen")
        }
        composable(Screen.Playlists.route) {
            PlaceholderScreen(title = "Playlists Screen")
        }
        composable(Screen.Settings.route) {
            PlaceholderScreen(title = "Settings Screen")
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
            )
        ) {
            val viewModel: AlbumDetailsViewModel = hiltViewModel()
            AlbumDetailsScreen(
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
