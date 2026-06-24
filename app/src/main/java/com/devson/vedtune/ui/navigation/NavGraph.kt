package com.devson.vedtune.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.devson.vedtune.ui.songs.SongsScreen
import com.devson.vedtune.ui.songs.SongsViewModel

sealed class Screen(val route: String) {
    data object Songs : Screen("songs")
    data object Albums : Screen("albums")
    data object Artists : Screen("artists")
    data object Playlists : Screen("playlists")
    data object Settings : Screen("settings")
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
            PlaceholderScreen(title = "Albums Screen")
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
