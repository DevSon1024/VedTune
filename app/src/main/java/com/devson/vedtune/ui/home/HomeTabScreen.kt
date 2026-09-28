package com.devson.vedtune.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devson.vedtune.domain.model.Album
import com.devson.vedtune.domain.model.Playlist
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.components.AddToPlaylistDialog
import com.devson.vedtune.ui.components.PlayingIndicator
import com.devson.vedtune.ui.components.SongArtwork
import com.devson.vedtune.ui.components.VedTuneBottomSheetHeader
import com.devson.vedtune.ui.components.VedTuneEmptyState
import com.devson.vedtune.ui.components.VedTuneOverlapCarousel
import com.devson.vedtune.ui.components.VedTunePrimaryButton
import com.devson.vedtune.ui.components.VedTuneSecondaryButton
import com.devson.vedtune.ui.components.VedTuneSectionHeader
import com.devson.vedtune.ui.components.VedTuneSongRow
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.rememberVedTuneAdaptiveInfo
import com.devson.vedtune.ui.theme.spacing
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTabScreen(
    viewModel: HomeViewModel,
    onNavigateToAlbum: (Long) -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToPlaylist: (Long) -> Unit = {},
    onNavigateToGenre: (String) -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToLibraryTab: (Int) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToFolderSettings: () -> Unit = {},
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val recentlyAddedAlbums by viewModel.recentlyAddedAlbums.collectAsStateWithLifecycle()
    val jumpBackInSongs by viewModel.jumpBackInSongs.collectAsStateWithLifecycle()
    val latestSongs by viewModel.latestSongs.collectAsStateWithLifecycle()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val currentSongId by viewModel.currentSongId.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val showArtwork by viewModel.showArtwork.collectAsStateWithLifecycle()

    val totalSongs by viewModel.totalSongsCount.collectAsStateWithLifecycle()
    val totalAlbums by viewModel.totalAlbumsCount.collectAsStateWithLifecycle()
    val totalArtists by viewModel.totalArtistsCount.collectAsStateWithLifecycle()
    val totalPlaylists by viewModel.totalPlaylistsCount.collectAsStateWithLifecycle()
    val favoriteSongsCount by viewModel.favoriteSongsCount.collectAsStateWithLifecycle()

    var selectedTrackTab by remember { mutableIntStateOf(0) } // 0: Fresh Tracks, 1: Most Played
    val currentTrackList = if (selectedTrackTab == 0) latestSongs else mostPlayedSongs
    val currentTrackDisplay = remember(currentTrackList) { currentTrackList.take(8) }

    val favoritesPlaylistId = remember(allPlaylists) {
        allPlaylists.firstOrNull { it.id == Playlist.FAVORITES_PLAYLIST_ID || it.name.equals(Playlist.FAVORITES_PLAYLIST_NAME, ignoreCase = true) }?.id ?: Playlist.FAVORITES_PLAYLIST_ID
    }

    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var songForPlaylistAdd by remember { mutableStateOf<Song?>(null) }

    val adaptiveInfo = rememberVedTuneAdaptiveInfo()
    val horizontalPadding = if (adaptiveInfo.isTablet) MaterialTheme.spacing.xxl else MaterialTheme.spacing.l

    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            viewModel.refresh()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        when {
            !hasPermission -> {
                VedTuneEmptyState(
                    title = "Permission Required",
                    description = "VedTune needs access to your audio files to build your music library.",
                    icon = Icons.Default.Lock,
                    actionText = "Grant Permission",
                    onActionClick = { launcher.launch(permission) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            recentlyAddedAlbums.isEmpty() && latestSongs.isEmpty() -> {
                VedTuneEmptyState(
                    title = "Your library is empty",
                    description = "Add music to your device and VedTune will find it here.",
                    icon = Icons.Default.MusicNote,
                    actionText = "Scan Library",
                    onActionClick = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = MaterialTheme.spacing.m,
                            bottom = contentPadding.calculateBottomPadding() + MaterialTheme.spacing.xxl
                        ),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.l)
                    ) {
                        // 1. Contextual Greeting Header with Stats & Actions
                        item {
                            HomeGreetingHeader(
                                totalSongs = totalSongs,
                                totalAlbums = totalAlbums,
                                totalArtists = totalArtists,
                                onSearchClick = onNavigateToSearch,
                                onSettingsClick = onNavigateToSettings,
                                modifier = Modifier.padding(horizontal = horizontalPadding)
                            )
                        }

                        // 2. Quick Access Shortcut Shelf
                        item {
                            QuickAccessRow(
                                favoriteCount = favoriteSongsCount,
                                albumCount = totalAlbums,
                                artistCount = totalArtists,
                                playlistCount = totalPlaylists,
                                onFavoritesClick = {
                                    onNavigateToPlaylist(favoritesPlaylistId)
                                },
                                onAlbumsClick = { onNavigateToLibraryTab(1) },
                                onArtistsClick = { onNavigateToLibraryTab(2) },
                                onPlaylistsClick = { onNavigateToLibraryTab(5) },
                                onFoldersClick = { onNavigateToLibraryTab(4) },
                                contentPadding = PaddingValues(horizontal = horizontalPadding)
                            )
                        }

                        // 3. Jump Back In / Recently Played
                        if (jumpBackInSongs.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    VedTuneSectionHeader(
                                        title = "Jump Back In",
                                        actionText = "See All",
                                        onActionClick = onNavigateToHistory,
                                        modifier = Modifier.padding(horizontal = horizontalPadding)
                                    )
                                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.s))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = horizontalPadding),
                                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(
                                            items = jumpBackInSongs,
                                            key = { it.id }
                                        ) { song ->
                                            val isCurrentSong = song.id == currentSongId
                                            JumpBackInSongCard(
                                                song = song,
                                                isCurrentSong = isCurrentSong,
                                                isPlaying = isPlaying && isCurrentSong,
                                                showArtwork = showArtwork,
                                                onClick = { viewModel.playJumpBackInSong(song) },
                                                onPlayClick = { viewModel.playJumpBackInSong(song) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Recently Added Albums Carousel
                        if (recentlyAddedAlbums.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    VedTuneSectionHeader(
                                        title = "Recently Added Albums",
                                        actionText = "See All",
                                        onActionClick = { onNavigateToLibraryTab(1) },
                                        modifier = Modifier.padding(horizontal = horizontalPadding)
                                    )
                                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.s))
                                    VedTuneOverlapCarousel(
                                        items = recentlyAddedAlbums,
                                        contentPadding = PaddingValues(horizontal = if (adaptiveInfo.isTablet) 80.dp else 36.dp),
                                        overlapOffset = 24.dp,
                                        key = { it.id },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { album, _, _ ->
                                        HomeAlbumBannerCard(
                                            album = album,
                                            showArtwork = showArtwork,
                                            onClick = { onNavigateToAlbum(album.id) },
                                            onPlayClick = { viewModel.playAlbum(album) }
                                        )
                                    }
                                }
                            }
                        }

                        // 5. Track Showcase (Tabs: Fresh Tracks & Most Played)
                        if (latestSongs.isNotEmpty() || mostPlayedSongs.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = horizontalPadding)
                                ) {
                                    VedTuneSectionHeader(
                                        title = "Music Showcase",
                                        count = if (selectedTrackTab == 0) totalSongs else mostPlayedSongs.size,
                                        actionText = "See All",
                                        onActionClick = { onNavigateToLibraryTab(0) }
                                    )
                                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.s))

                                    // Tab Pill Filter (Fresh Tracks vs Most Played)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        HomeTabFilterChip(
                                            label = "Fresh Tracks",
                                            icon = Icons.Default.MusicNote,
                                            selected = selectedTrackTab == 0,
                                            onClick = { selectedTrackTab = 0 }
                                        )
                                        HomeTabFilterChip(
                                            label = "Most Played",
                                            icon = Icons.Default.Star,
                                            selected = selectedTrackTab == 1,
                                            onClick = { selectedTrackTab = 1 }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.m))

                                    // Play All and Shuffle CTA Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        VedTunePrimaryButton(
                                            text = "Play All",
                                            icon = Icons.Default.PlayArrow,
                                            onClick = { viewModel.playAllFromList(currentTrackList) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        VedTuneSecondaryButton(
                                            text = "Shuffle",
                                            icon = Icons.Default.Shuffle,
                                            onClick = { viewModel.shuffleAllFromList(currentTrackList) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.m))

                                    // Grouped Card Container for Tracks
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        tonalElevation = 1.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = MaterialTheme.spacing.xs)
                                        ) {
                                            currentTrackDisplay.forEachIndexed { index, song ->
                                                val isCurrentSong = song.id == currentSongId
                                                VedTuneSongRow(
                                                    song = song,
                                                    isCurrentSong = isCurrentSong,
                                                    isPlaying = isPlaying && isCurrentSong,
                                                    showArtwork = showArtwork,
                                                    showDuration = true,
                                                    onClick = { viewModel.playSongFromList(song, currentTrackList) },
                                                    onOptionsClick = { selectedSongForOptions = song },
                                                    containerColor = if (isCurrentSong) {
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                                    } else {
                                                        Color.Transparent
                                                    }
                                                )
                                                if (index < currentTrackDisplay.lastIndex) {
                                                    HorizontalDivider(
                                                        thickness = 0.5.dp,
                                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                                                        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.m)
                                                    )
                                                }
                                            }

                                            // View all in library CTA button
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = MaterialTheme.spacing.xs),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                TextButton(
                                                    onClick = { onNavigateToLibraryTab(0) }
                                                ) {
                                                    Text(
                                                        text = "View all $totalSongs tracks in Library",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Song Options Bottom Sheet
        selectedSongForOptions?.let { song ->
            SongOptionsBottomSheet(
                song = song,
                onDismiss = { selectedSongForOptions = null },
                onToggleFavorite = {
                    viewModel.toggleFavorite(song)
                    selectedSongForOptions = null
                },
                onPlayNext = {
                    viewModel.playNext(song)
                    selectedSongForOptions = null
                },
                onShuffleThis = {
                    viewModel.playShuffle(song)
                    selectedSongForOptions = null
                },
                onAddToPlaylist = {
                    songForPlaylistAdd = song
                    selectedSongForOptions = null
                    showAddToPlaylistDialog = true
                },
                onGoToAlbum = {
                    selectedSongForOptions = null
                    onNavigateToAlbum(song.albumId)
                },
                onGoToArtist = {
                    selectedSongForOptions = null
                    onNavigateToArtist(song.artist)
                }
            )
        }

        // Add to Playlist Dialog
        if (showAddToPlaylistDialog && songForPlaylistAdd != null) {
            AddToPlaylistDialog(
                playlists = allPlaylists,
                onDismiss = {
                    showAddToPlaylistDialog = false
                    songForPlaylistAdd = null
                },
                onPlaylistSelected = { playlistId ->
                    songForPlaylistAdd?.let { s ->
                        viewModel.addSongToPlaylist(playlistId, s.id)
                    }
                    showAddToPlaylistDialog = false
                    songForPlaylistAdd = null
                },
                onCreateNewPlaylist = { name ->
                    songForPlaylistAdd?.let { s ->
                        viewModel.createPlaylistAndAddSong(name, s.id)
                    }
                    showAddToPlaylistDialog = false
                    songForPlaylistAdd = null
                }
            )
        }
    }
}

/**
 * Contextual Header with local time greeting and action buttons.
 */
@Composable
private fun HomeGreetingHeader(
    totalSongs: Int,
    totalAlbums: Int,
    totalArtists: Int,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    val subtitle = remember(totalSongs, totalAlbums, totalArtists) {
        if (totalSongs > 0) {
            "$totalSongs songs • $totalAlbums albums • $totalArtists artists"
        } else {
            "What would you like to listen to?"
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xxs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp,
                onClick = onSearchClick
            ) {
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search music",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(VedTuneIconSizes.Standard)
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp,
                onClick = onSettingsClick
            ) {
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(VedTuneIconSizes.Standard)
                    )
                }
            }
        }
    }
}

/**
 * Filter chip for switching track tabs (Fresh Tracks / Most Played).
 */
@Composable
private fun HomeTabFilterChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        shape = VedTuneShapeTokens.Pill,
        color = containerColor,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.m, vertical = MaterialTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

/**
 * Quick access horizontal chips for core destinations.
 */
@Composable
private fun QuickAccessRow(
    favoriteCount: Int,
    albumCount: Int,
    artistCount: Int,
    playlistCount: Int,
    onFavoritesClick: () -> Unit,
    onAlbumsClick: () -> Unit,
    onArtistsClick: () -> Unit,
    onPlaylistsClick: () -> Unit,
    onFoldersClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
    ) {
        item {
            QuickAccessChip(
                label = "Favorites",
                count = if (favoriteCount > 0) favoriteCount else null,
                icon = Icons.Default.Favorite,
                iconColor = Color(0xFFE53935),
                onClick = onFavoritesClick
            )
        }
        item {
            QuickAccessChip(
                label = "Albums",
                count = if (albumCount > 0) albumCount else null,
                icon = Icons.Default.Album,
                iconColor = MaterialTheme.colorScheme.secondary,
                onClick = onAlbumsClick
            )
        }
        item {
            QuickAccessChip(
                label = "Artists",
                count = if (artistCount > 0) artistCount else null,
                icon = Icons.Default.Person,
                iconColor = MaterialTheme.colorScheme.tertiary,
                onClick = onArtistsClick
            )
        }
        item {
            QuickAccessChip(
                label = "Playlists",
                count = if (playlistCount > 0) playlistCount else null,
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                iconColor = MaterialTheme.colorScheme.primary,
                onClick = onPlaylistsClick
            )
        }
        item {
            QuickAccessChip(
                label = "Folders",
                count = null,
                icon = Icons.Default.Folder,
                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onFoldersClick
            )
        }
    }
}

@Composable
private fun QuickAccessChip(
    label: String,
    count: Int?,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = VedTuneShapeTokens.Pill,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = modifier.height(42.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.m, vertical = MaterialTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (count != null) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Album-art-driven horizontal card for Recently Played / Jump Back In.
 */
@Composable
private fun JumpBackInSongCard(
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    showArtwork: Boolean,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentSong) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        border = if (isCurrentSong) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrentSong) 2.dp else 1.dp
        ),
        modifier = modifier
            .width(154.dp)
            .wrapContentHeight()
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.s)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                SongArtwork(
                    albumId = song.albumId,
                    lastModified = song.dateModified,
                    modifier = Modifier.fillMaxSize(),
                    showArtwork = showArtwork
                )
                if (isCurrentSong) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        PlayingIndicator(
                            isPlaying = isPlaying,
                            modifier = Modifier.size(VedTuneIconSizes.Standard)
                        )
                    }
                }

                // Quick Play FAB button overlay in bottom right
                FilledIconButton(
                    onClick = onPlayClick,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(MaterialTheme.spacing.xs)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isCurrentSong && isPlaying) Icons.Default.MusicNote else Icons.Default.PlayArrow,
                        contentDescription = "Play ${song.title}",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.s))
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xxs))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Album banner card for top carousel.
 */
@Composable
private fun HomeAlbumBannerCard(
    album: Album,
    showArtwork: Boolean,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(215.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SongArtwork(
                albumId = album.id,
                modifier = Modifier.fillMaxSize(),
                showArtwork = showArtwork
            )
            // Multi-stop gradient scrim for contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.50f),
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            )

            // Top-left "ALBUM" pill tag
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(MaterialTheme.spacing.m)
            ) {
                Text(
                    text = "ALBUM",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            // Bottom Details and Play CTA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(MaterialTheme.spacing.l),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xxs))
                    Text(
                        text = "${album.artist} • ${album.songCount} ${if (album.songCount == 1) "song" else "songs"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.m))
                FilledIconButton(
                    onClick = onPlayClick,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Album ${album.title}",
                        modifier = Modifier.size(VedTuneIconSizes.Large)
                    )
                }
            }
        }
    }
}

/**
 * Song options modal bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongOptionsBottomSheet(
    song: Song,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayNext: () -> Unit,
    onShuffleThis: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = VedTuneShapeTokens.BottomSheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MaterialTheme.spacing.xxl)
        ) {
            VedTuneBottomSheetHeader(
                title = song.title,
                subtitle = "${song.artist} • ${song.album}",
                onCloseClick = onDismiss
            )

            ListItem(
                headlineContent = { Text(if (song.isFavorite) "Remove from Favorites" else "Add to Favorites") },
                leadingContent = {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (song.isFavorite) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable(onClick = onToggleFavorite)
            )

            ListItem(
                headlineContent = { Text("Play Next") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable(onClick = onPlayNext)
            )
            ListItem(
                headlineContent = { Text("Shuffle") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable(onClick = onShuffleThis)
            )
            ListItem(
                headlineContent = { Text("Add to Playlist") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAddCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable(onClick = onAddToPlaylist)
            )
            if (song.albumId > 0) {
                ListItem(
                    headlineContent = { Text("Go to Album") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable(onClick = onGoToAlbum)
                )
            }
            if (song.artist.isNotBlank() && song.artist != "<unknown>") {
                ListItem(
                    headlineContent = { Text("Go to Artist") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable(onClick = onGoToArtist)
                )
            }
        }
    }
}
