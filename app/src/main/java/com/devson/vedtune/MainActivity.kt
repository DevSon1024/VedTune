package com.devson.vedtune

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.devson.vedtune.ui.MainViewModel
import com.devson.vedtune.ui.components.MiniPlayer
import com.devson.vedtune.ui.navigation.NavGraph
import com.devson.vedtune.ui.navigation.Screen
import com.devson.vedtune.ui.theme.vedtuneTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(this, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            lifecycleScope.launch {
                val autoSync = viewModel.autoSyncOnStartup.value
                if (autoSync) {
                    viewModel.syncLibrary()
                }
            }
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val dynamicColorsEnabled by viewModel.dynamicColorsEnabled.collectAsState()
            val defaultStartScreen by viewModel.defaultStartScreen.collectAsState()

            vedtuneTheme(themeMode = themeMode, dynamicColor = dynamicColorsEnabled) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val currentSong by viewModel.currentSong.collectAsState()
                val isPlaying by viewModel.isPlaying.collectAsState()
                val position by viewModel.playbackPosition.collectAsState()
                val duration by viewModel.playbackDuration.collectAsState()
                val showAlbumArt by viewModel.showAlbumArt.collectAsState()
                val showMiniPlayerProgress by viewModel.showMiniPlayerProgress.collectAsState()

                val progress = remember(position, duration) {
                    if (duration > 0) position.toFloat() / duration.toFloat() else 0f
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentRoute != Screen.Player.route) {
                            Column {
                                MiniPlayer(
                                    song = currentSong,
                                    isPlaying = isPlaying,
                                    progress = progress,
                                    onPlayPauseClick = {
                                        if (isPlaying) viewModel.pause() else viewModel.play()
                                    },
                                    onSkipNextClick = {
                                        viewModel.skipToNext()
                                    },
                                    onClick = {
                                        navController.navigate(Screen.Player.route)
                                    },
                                    showArtwork = showAlbumArt,
                                    showProgress = showMiniPlayerProgress
                                )

                                val items = listOf(
                                    NavigationItem("Songs", Screen.Songs.route, Icons.AutoMirrored.Filled.List),
                                    NavigationItem("Albums", Screen.Albums.route, Icons.Default.PlayArrow),
                                    NavigationItem("Artists", Screen.Artists.route, Icons.Default.Person),
                                    NavigationItem("Playlists", Screen.Playlists.route, Icons.Default.Favorite),
                                    NavigationItem("Settings", Screen.Settings.route, Icons.Default.Settings)
                                )
                                NavigationBar {
                                    items.forEach { item ->
                                        NavigationBarItem(
                                            selected = currentRoute == item.route,
                                            onClick = {
                                                if (currentRoute != item.route) {
                                                    navController.navigate(item.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                            icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                                            label = { Text(text = item.label) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavGraph(
                        navController = navController,
                        startDestination = defaultStartScreen,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

private data class NavigationItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)