package com.devson.vedtune.ui.library

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.vedtune.ui.songs.SongsScreen
import com.devson.vedtune.ui.songs.SongsViewModel
import com.devson.vedtune.ui.albums.AlbumsScreen
import com.devson.vedtune.ui.albums.AlbumsViewModel
import com.devson.vedtune.ui.artists.ArtistsScreen
import com.devson.vedtune.ui.artists.ArtistsViewModel
import com.devson.vedtune.ui.genres.GenresScreen
import com.devson.vedtune.ui.genres.GenresViewModel
import com.devson.vedtune.ui.playlists.PlaylistsScreen
import com.devson.vedtune.ui.playlists.PlaylistsViewModel
import kotlinx.coroutines.launch

data class LibraryTabItem(
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToPlaylist: (Long) -> Unit,
    onNavigateToGenre: (String) -> Unit,
    onNavigateToEditTags: (Long) -> Unit,
    navigateToLocationEvent: kotlinx.coroutines.flow.SharedFlow<Long>?,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val songsViewModel: SongsViewModel = hiltViewModel()
    val uiState by songsViewModel.uiState.collectAsState()
    var showViewSettings by remember { mutableStateOf(false) }

    val tabs = listOf(
        LibraryTabItem("Songs", Icons.AutoMirrored.Filled.List),
        LibraryTabItem("Albums", Icons.Default.Album),
        LibraryTabItem("Artists", Icons.Default.Person),
        LibraryTabItem("Genres", Icons.Default.MusicNote),
        LibraryTabItem("Playlists", Icons.Default.Favorite)
    )

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size }
    )
    val scope = rememberCoroutineScope()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val tabWidth = screenWidth / 4.5f

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Tab bar container — a soft tonal surface so the whole bar reads as one
        // cohesive control, with a hairline divider separating it from the pager.
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Scrollable Tab Row. Each tab is sized to its own content (icon +
                // label padding) instead of a fixed width, so long labels like
                // "Playlists" always render on a single line. Because the combined
                // content width naturally runs past the screen width, the last tab
                // is left peeking in partway — a built-in visual cue that the row
                // scrolls, with no manual math required.
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {},
                    indicator = { tabPositions ->
                        if (tabPositions.isNotEmpty()) {
                            val absolutePosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
                            val floorPage = kotlin.math.floor(absolutePosition).toInt().coerceIn(0, tabPositions.lastIndex)
                            val ceilPage = kotlin.math.ceil(absolutePosition).toInt().coerceIn(0, tabPositions.lastIndex)
                            val fraction = (absolutePosition - floorPage).coerceIn(0f, 1f)

                            val floorPosition = tabPositions[floorPage]
                            val ceilPosition = tabPositions[ceilPage]

                            val left = androidx.compose.ui.unit.lerp(floorPosition.left, ceilPosition.left, fraction)
                            val right = androidx.compose.ui.unit.lerp(floorPosition.right, ceilPosition.right, fraction)
                            val currentTabWidth = right - left
                            val indicatorWidth = 24.dp
                            val centerX = left + (currentTabWidth - indicatorWidth) / 2

                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentSize(Alignment.BottomStart)
                                    .offset(x = centerX)
                                    .width(indicatorWidth),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = pagerState.currentPage == index
                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = tween(200),
                            label = "tabContentColor"
                        )

                        Tab(
                            selected = isSelected,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            modifier = Modifier.width(tabWidth),
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            // icon → label → active indicator (drawn by the row above)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = contentColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Horizontal Pager for Library Pages
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            when (page) {
                0 -> {
                    SongsScreen(
                        viewModel = songsViewModel,
                        onNavigateToAlbum = onNavigateToAlbum,
                        onNavigateToArtist = onNavigateToArtist,
                        onNavigateToEditTags = onNavigateToEditTags,
                        navigateToLocationEvent = navigateToLocationEvent,
                        onLayoutToggleClick = { showViewSettings = true },
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                1 -> {
                    val viewModel: AlbumsViewModel = hiltViewModel()
                    AlbumsScreen(
                        viewModel = viewModel,
                        onAlbumClick = onNavigateToAlbum,
                        viewPreferences = uiState.viewPreferences,
                        onLayoutToggleClick = { showViewSettings = true },
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                2 -> {
                    val viewModel: ArtistsViewModel = hiltViewModel()
                    ArtistsScreen(
                        viewModel = viewModel,
                        onArtistClick = onNavigateToArtist,
                        viewPreferences = uiState.viewPreferences,
                        onLayoutToggleClick = { showViewSettings = true },
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                3 -> {
                    val viewModel: GenresViewModel = hiltViewModel()
                    GenresScreen(
                        viewModel = viewModel,
                        onGenreClick = onNavigateToGenre,
                        viewPreferences = uiState.viewPreferences,
                        onLayoutToggleClick = { showViewSettings = true },
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                4 -> {
                    val viewModel: PlaylistsViewModel = hiltViewModel()
                    PlaylistsScreen(
                        viewModel = viewModel,
                        onPlaylistClick = onNavigateToPlaylist,
                        viewPreferences = uiState.viewPreferences,
                        onLayoutToggleClick = { showViewSettings = true },
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    if (showViewSettings) {
        com.devson.vedtune.ui.components.ViewSettingsSheet(
            preferences = uiState.viewPreferences,
            onPreferencesChange = { songsViewModel.updateViewPreferences(it) },
            onDismiss = { showViewSettings = false }
        )
    }
}