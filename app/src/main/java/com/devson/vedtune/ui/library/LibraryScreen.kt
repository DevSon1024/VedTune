package com.devson.vedtune.ui.library

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.albums.AlbumSortBy
import com.devson.vedtune.ui.albums.AlbumsScreen
import com.devson.vedtune.ui.albums.AlbumsViewModel
import com.devson.vedtune.ui.artists.ArtistSortBy
import com.devson.vedtune.ui.artists.ArtistsScreen
import com.devson.vedtune.ui.artists.ArtistsViewModel
import com.devson.vedtune.ui.components.AddToPlaylistDialog
import com.devson.vedtune.ui.components.LibraryUtilityRow
import com.devson.vedtune.ui.components.VedTuneConfirmDialog
import com.devson.vedtune.ui.components.VedTuneIconButton
import com.devson.vedtune.ui.folders.FolderSortBy
import com.devson.vedtune.ui.folders.FolderSortOrder
import com.devson.vedtune.ui.folders.FoldersScreen
import com.devson.vedtune.ui.folders.FoldersViewModel
import com.devson.vedtune.ui.genres.GenresScreen
import com.devson.vedtune.ui.genres.GenresViewModel
import com.devson.vedtune.ui.player.components.OptionsSheetContent
import com.devson.vedtune.ui.playlists.PlaylistSortBy
import com.devson.vedtune.ui.playlists.PlaylistsScreen
import com.devson.vedtune.ui.playlists.PlaylistsViewModel
import com.devson.vedtune.ui.songs.SongsScreen
import com.devson.vedtune.ui.songs.SongsViewModel
import com.devson.vedtune.ui.songs.SortBy
import com.devson.vedtune.ui.songs.SortOrder
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.spacing
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
    val albumsViewModel: AlbumsViewModel = hiltViewModel()
    val artistsViewModel: ArtistsViewModel = hiltViewModel()
    val genresViewModel: GenresViewModel = hiltViewModel()
    val foldersViewModel: FoldersViewModel = hiltViewModel()
    val playlistsViewModel: PlaylistsViewModel = hiltViewModel()

    val uiState by songsViewModel.uiState.collectAsState()
    val albumSortBy by albumsViewModel.sortBy.collectAsState()
    val albumSortOrder by albumsViewModel.sortOrder.collectAsState()
    val artistSortBy by artistsViewModel.sortBy.collectAsState()
    val artistSortOrder by artistsViewModel.sortOrder.collectAsState()
    val genreSortOrder by genresViewModel.sortOrder.collectAsState()
    val folderSortBy by foldersViewModel.sortBy.collectAsState()
    val folderSortOrder by foldersViewModel.sortOrder.collectAsState()
    val playlistSortBy by playlistsViewModel.sortBy.collectAsState()
    val playlistSortOrder by playlistsViewModel.sortOrder.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var songForOptions by remember { mutableStateOf<Song?>(null) }
    var songForPlaylist by remember { mutableStateOf<Song?>(null) }
    var songToDeletePermanently by remember { mutableStateOf<Song?>(null) }
    val playlists by playlistsViewModel.playlists.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val tabs = listOf(
        LibraryTabItem("Songs", Icons.AutoMirrored.Filled.List),
        LibraryTabItem("Albums", Icons.Default.Album),
        LibraryTabItem("Artists", Icons.Default.Person),
        LibraryTabItem("Genres", Icons.Default.MusicNote),
        LibraryTabItem("Folders", Icons.Default.Folder),
        LibraryTabItem("Playlists", Icons.AutoMirrored.Filled.QueueMusic)
    )

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size }
    )
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Collapsible header height calculation
    val fullHeaderHeight = 120.dp
    val fullHeaderHeightPx = with(density) { fullHeaderHeight.toPx() }
    var headerOffsetPx by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember(fullHeaderHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = (headerOffsetPx + delta).coerceIn(-fullHeaderHeightPx, 0f)
                val consumedY = newOffset - headerOffsetPx
                headerOffsetPx = newOffset
                return Offset(0f, consumedY)
            }
        }
    }

    val headerProgress = ((fullHeaderHeightPx + headerOffsetPx) / fullHeaderHeightPx).coerceIn(0f, 1f)
    val currentHeaderHeight = fullHeaderHeight * headerProgress

    val currentSortLabel = when (pagerState.currentPage) {
        0 -> when (uiState.sortBy) {
            SortBy.TITLE -> "Title"
            SortBy.ARTIST -> "Artist"
            SortBy.ALBUM -> "Album"
            SortBy.DATE_ADDED -> "Date Added"
            SortBy.DURATION -> "Duration"
        }
        1 -> when (albumSortBy) {
            AlbumSortBy.TITLE -> "Title"
            AlbumSortBy.ARTIST -> "Artist"
            AlbumSortBy.SONG_COUNT -> "Song Count"
        }
        2 -> when (artistSortBy) {
            ArtistSortBy.NAME -> "Name"
            ArtistSortBy.SONG_COUNT -> "Song Count"
            ArtistSortBy.ALBUM_COUNT -> "Album Count"
        }
        3 -> "Name"
        4 -> when (folderSortBy) {
            FolderSortBy.NAME -> "Name"
            FolderSortBy.TRACK_COUNT -> "Track Count"
            FolderSortBy.PATH -> "Path"
        }
        5 -> when (playlistSortBy) {
            PlaylistSortBy.NAME -> "Name"
            PlaylistSortBy.SONG_COUNT -> "Song Count"
            PlaylistSortBy.DATE_CREATED -> "Date Created"
        }
        else -> "Title"
    }

    val currentSortOrderIcon = when (pagerState.currentPage) {
        0 -> if (uiState.sortOrder == SortOrder.ASCENDING) "↑" else "↓"
        1 -> if (albumSortOrder == SortOrder.ASCENDING) "↑" else "↓"
        2 -> if (artistSortOrder == SortOrder.ASCENDING) "↑" else "↓"
        3 -> if (genreSortOrder == SortOrder.ASCENDING) "↑" else "↓"
        4 -> if (folderSortOrder == FolderSortOrder.ASCENDING) "↑" else "↓"
        5 -> if (playlistSortOrder == SortOrder.ASCENDING) "↑" else "↓"
        else -> "↑"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .nestedScroll(nestedScrollConnection)
    ) {
        // Collapsible Header Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(currentHeaderHeight)
                .clipToBounds()
                .graphicsLayer {
                    alpha = headerProgress
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = MaterialTheme.spacing.l, vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Title & Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Library",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    VedTuneIconButton(
                        icon = Icons.Default.Tune,
                        contentDescription = "View Settings",
                        onClick = { songsViewModel.toggleLayoutView() },
                        iconSize = VedTuneIconSizes.Medium,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quick Shortcut Cards Row (Favorites, Recent, Folders, Playlists)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LibraryQuickCard(
                        title = "Favorites",
                        icon = Icons.Default.Favorite,
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        iconColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            scope.launch { pagerState.scrollToPage(5) }
                        }
                    )
                    LibraryQuickCard(
                        title = "Recent",
                        icon = Icons.Default.History,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        iconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            songsViewModel.setSortBy(SortBy.DATE_ADDED)
                            scope.launch { pagerState.scrollToPage(0) }
                        }
                    )
                    LibraryQuickCard(
                        title = "Folders",
                        icon = Icons.Default.Folder,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        iconColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            scope.launch { pagerState.scrollToPage(4) }
                        }
                    )
                    LibraryQuickCard(
                        title = "Playlists",
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        iconColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            scope.launch { pagerState.scrollToPage(5) }
                        }
                    )
                }
            }
        }

        // Tab bar container - Sleek modern pill design
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = MaterialTheme.spacing.l,
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

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentSize(Alignment.CenterStart)
                                    .offset(x = left + 4.dp)
                                    .width((currentTabWidth - 8.dp).coerceAtLeast(0.dp))
                                    .height(38.dp)
                                    .clip(VedTuneShapeTokens.Pill)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                                    .border(
                                        width = 1.5.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = VedTuneShapeTokens.Pill
                                    )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
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
                                    pagerState.scrollToPage(index)
                                }
                            },
                            modifier = Modifier
                                .clip(VedTuneShapeTokens.Pill)
                                .height(40.dp)
                                .padding(horizontal = 2.dp),
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = contentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = contentColor,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
            }
        }

        // Sticky Utility Row (Sort, Shuffle, Layout Toggle)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        ) {
            LibraryUtilityRow(
                currentSortLabel = currentSortLabel,
                sortOrderIcon = currentSortOrderIcon,
                onSortClick = {
                    if (pagerState.currentPage == 3) {
                        genresViewModel.toggleSortOrder()
                    } else {
                        showSortMenu = true
                    }
                },
                isGridView = uiState.isGridView,
                onLayoutToggleClick = { songsViewModel.toggleLayoutView() },
                onShuffleClick = {
                    when (pagerState.currentPage) {
                        0 -> songsViewModel.playShuffleAll()
                        1 -> albumsViewModel.playShuffleAll()
                        2 -> artistsViewModel.playShuffleAll()
                        3 -> genresViewModel.playShuffleAll()
                        4 -> foldersViewModel.playShuffleAll()
                        5 -> playlistsViewModel.playShuffleAll()
                    }
                }
            )

            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                when (pagerState.currentPage) {
                    0 -> {
                        SortBy.entries.forEach { option ->
                            val isSelected = uiState.sortBy == option
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = when (option) {
                                            SortBy.TITLE -> "Sort by Title"
                                            SortBy.ARTIST -> "Sort by Artist"
                                            SortBy.ALBUM -> "Sort by Album"
                                            SortBy.DATE_ADDED -> "Sort by Date Added"
                                            SortBy.DURATION -> "Sort by Duration"
                                        }
                                    )
                                },
                                onClick = {
                                    songsViewModel.setSortBy(option)
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
                                    }
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Ascending") },
                            onClick = {
                                songsViewModel.setSortOrder(SortOrder.ASCENDING)
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (uiState.sortOrder == SortOrder.ASCENDING) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Descending") },
                            onClick = {
                                songsViewModel.setSortOrder(SortOrder.DESCENDING)
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (uiState.sortOrder == SortOrder.DESCENDING) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
                                }
                            }
                        )
                    }
                    1 -> {
                        AlbumSortBy.entries.forEach { option ->
                            val isSelected = albumSortBy == option
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = when (option) {
                                            AlbumSortBy.TITLE -> "Sort by Title"
                                            AlbumSortBy.ARTIST -> "Sort by Artist"
                                            AlbumSortBy.SONG_COUNT -> "Sort by Song Count"
                                        }
                                    )
                                },
                                onClick = {
                                    if (albumSortBy == option) {
                                        albumsViewModel.toggleSortOrder()
                                    } else {
                                        albumsViewModel.setSortBy(option)
                                    }
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
                                    }
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Ascending") },
                            onClick = {
                                if (albumSortOrder != SortOrder.ASCENDING) albumsViewModel.toggleSortOrder()
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (albumSortOrder == SortOrder.ASCENDING) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Descending") },
                            onClick = {
                                if (albumSortOrder != SortOrder.DESCENDING) albumsViewModel.toggleSortOrder()
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (albumSortOrder == SortOrder.DESCENDING) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
                                }
                            }
                        )
                    }
                    2 -> {
                        ArtistSortBy.entries.forEach { option ->
                            val isSelected = artistSortBy == option
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = when (option) {
                                            ArtistSortBy.NAME -> "Sort by Name"
                                            ArtistSortBy.SONG_COUNT -> "Sort by Song Count"
                                            ArtistSortBy.ALBUM_COUNT -> "Sort by Album Count"
                                        }
                                    )
                                },
                                onClick = {
                                    if (artistSortBy == option) {
                                        artistsViewModel.toggleSortOrder()
                                    } else {
                                        artistsViewModel.setSortBy(option)
                                    }
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
                                    }
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Ascending") },
                            onClick = {
                                if (artistSortOrder != SortOrder.ASCENDING) artistsViewModel.toggleSortOrder()
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (artistSortOrder == SortOrder.ASCENDING) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Descending") },
                            onClick = {
                                if (artistSortOrder != SortOrder.DESCENDING) artistsViewModel.toggleSortOrder()
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (artistSortOrder == SortOrder.DESCENDING) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
                                }
                            }
                        )
                    }
                    4 -> {
                        FolderSortBy.entries.forEach { option ->
                            val isSelected = folderSortBy == option
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = when (option) {
                                            FolderSortBy.NAME -> "Sort by Name"
                                            FolderSortBy.TRACK_COUNT -> "Sort by Track Count"
                                            FolderSortBy.PATH -> "Sort by Path"
                                        }
                                    )
                                },
                                onClick = {
                                    if (folderSortBy == option) {
                                        foldersViewModel.toggleSortOrder()
                                    } else {
                                        foldersViewModel.setSortBy(option)
                                    }
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
                                    }
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Ascending") },
                            onClick = {
                                if (folderSortOrder != FolderSortOrder.ASCENDING) foldersViewModel.toggleSortOrder()
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (folderSortOrder == FolderSortOrder.ASCENDING) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Descending") },
                            onClick = {
                                if (folderSortOrder != FolderSortOrder.DESCENDING) foldersViewModel.toggleSortOrder()
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (folderSortOrder == FolderSortOrder.DESCENDING) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
                                }
                            }
                        )
                    }
                    5 -> {
                        PlaylistSortBy.entries.forEach { option ->
                            val isSelected = playlistSortBy == option
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = when (option) {
                                            PlaylistSortBy.NAME -> "Sort by Name"
                                            PlaylistSortBy.SONG_COUNT -> "Sort by Song Count"
                                            PlaylistSortBy.DATE_CREATED -> "Sort by Date Created"
                                        }
                                    )
                                },
                                onClick = {
                                    if (playlistSortBy == option) {
                                        playlistsViewModel.toggleSortOrder()
                                    } else {
                                        playlistsViewModel.setSortBy(option)
                                    }
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
                                    }
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Ascending") },
                            onClick = {
                                if (playlistSortOrder != SortOrder.ASCENDING) playlistsViewModel.toggleSortOrder()
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (playlistSortOrder == SortOrder.ASCENDING) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Descending") },
                            onClick = {
                                if (playlistSortOrder != SortOrder.DESCENDING) playlistsViewModel.toggleSortOrder()
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (playlistSortOrder == SortOrder.DESCENDING) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
                                }
                            }
                        )
                    }
                }
            }
        }

        // Horizontal Pager for 6 Library Categories
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
                        onLayoutToggleClick = { songsViewModel.toggleLayoutView() },
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                1 -> {
                    AlbumsScreen(
                        viewModel = albumsViewModel,
                        onAlbumClick = onNavigateToAlbum,
                        viewPreferences = uiState.viewPreferences,
                        onLayoutToggleClick = { songsViewModel.toggleLayoutView() },
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                2 -> {
                    ArtistsScreen(
                        viewModel = artistsViewModel,
                        onArtistClick = onNavigateToArtist,
                        viewPreferences = uiState.viewPreferences,
                        onLayoutToggleClick = { songsViewModel.toggleLayoutView() },
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                3 -> {
                    GenresScreen(
                        viewModel = genresViewModel,
                        onGenreClick = onNavigateToGenre,
                        viewPreferences = uiState.viewPreferences,
                        onLayoutToggleClick = { songsViewModel.toggleLayoutView() },
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                4 -> {
                    FoldersScreen(
                        viewModel = foldersViewModel,
                        onSongOptionsClick = { song -> songForOptions = song },
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                5 -> {
                    PlaylistsScreen(
                        viewModel = playlistsViewModel,
                        onPlaylistClick = onNavigateToPlaylist,
                        viewPreferences = uiState.viewPreferences,
                        onLayoutToggleClick = { songsViewModel.toggleLayoutView() },
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Song Options Bottom Sheet for Library
    songForOptions?.let { activeSong ->
        ModalBottomSheet(
            onDismissRequest = { songForOptions = null },
            shape = VedTuneShapeTokens.BottomSheet,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            OptionsSheetContent(
                song = activeSong,
                showArtwork = uiState.showArtwork,
                onEditTags = {
                    songForOptions = null
                    onNavigateToEditTags(activeSong.id)
                },
                onEditLyrics = {
                    songForOptions = null
                },
                onShare = {
                    songForOptions = null
                    val songUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        activeSong.id
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "audio/*"
                        putExtra(Intent.EXTRA_STREAM, songUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Song"))
                },
                onDeletePermanently = {
                    songForOptions = null
                    songToDeletePermanently = activeSong
                },
                onPlayerSettings = {
                    songForOptions = null
                },
                onCloseClick = { songForOptions = null }
            )
        }
    }

    // Add to Playlist Dialog
    songForPlaylist?.let { targetSong ->
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { songForPlaylist = null },
            onPlaylistSelected = { playlistId ->
                songsViewModel.addSongToPlaylist(playlistId, targetSong.id)
                songForPlaylist = null
            },
            onCreateNewPlaylist = { playlistName ->
                songsViewModel.createPlaylistAndAddSong(playlistName, targetSong.id)
                songForPlaylist = null
            }
        )
    }

    // Permanent Delete Confirmation Dialog
    songToDeletePermanently?.let { targetSong ->
        VedTuneConfirmDialog(
            title = "Delete Song Permanently",
            message = "Are you sure you want to delete '${targetSong.title}' permanently from this device? This action cannot be undone.",
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                songsViewModel.deleteSongPermanently(context, targetSong)
                songToDeletePermanently = null
            },
            onDismiss = { songToDeletePermanently = null }
        )
    }
}

@Composable
private fun LibraryQuickCard(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = VedTuneShapeTokens.Card,
        color = containerColor,
        modifier = modifier.height(58.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}