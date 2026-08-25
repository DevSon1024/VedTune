package com.devson.vedtune.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.core.content.ContextCompat
import com.devson.vedtune.domain.model.Album
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.components.AddToPlaylistDialog
import com.devson.vedtune.ui.components.PlayingIndicator
import com.devson.vedtune.ui.components.SongArtwork
import com.devson.vedtune.ui.components.VedTuneBottomSheetHeader
import com.devson.vedtune.ui.components.VedTuneEmptyState
import com.devson.vedtune.ui.components.VedTuneIconButton
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
    onNavigateToLibraryTab: (Int) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToFolderSettings: () -> Unit = {},
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val recentlyAddedAlbums by viewModel.recentlyAddedAlbums.collectAsState()
    val jumpBackInSongs by viewModel.jumpBackInSongs.collectAsState()
    val latestSongs by viewModel.latestSongs.collectAsState()
    val allPlaylists by viewModel.allPlaylists.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val currentSongId by viewModel.currentSongId.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val showArtwork by viewModel.showArtwork.collectAsState()

    val totalSongs by viewModel.totalSongsCount.collectAsState()
    val totalAlbums by viewModel.totalAlbumsCount.collectAsState()
    val totalArtists by viewModel.totalArtistsCount.collectAsState()
    val totalPlaylists by viewModel.totalPlaylistsCount.collectAsState()
    val favoriteSongsCount by viewModel.favoriteSongsCount.collectAsState()

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
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxl)
                    ) {
                        // 1. Header with contextual greeting, brand title, and action icons
                        item {
                            HomeGreetingHeader(
                                onSearchClick = onNavigateToSearch,
                                onSettingsClick = onNavigateToSettings,
                                modifier = Modifier.padding(horizontal = horizontalPadding)
                            )
                        }

                        // 2. Quick Access destinations
                        item {
                            QuickAccessRow(
                                favoriteCount = favoriteSongsCount,
                                albumCount = totalAlbums,
                                artistCount = totalArtists,
                                playlistCount = totalPlaylists,
                                onFavoritesClick = { onNavigateToLibraryTab(4) },
                                onAlbumsClick = { onNavigateToLibraryTab(1) },
                                onArtistsClick = { onNavigateToLibraryTab(2) },
                                onPlaylistsClick = { onNavigateToLibraryTab(4) },
                                onFoldersClick = onNavigateToFolderSettings,
                                contentPadding = PaddingValues(horizontal = horizontalPadding)
                            )
                        }

                        // 3. Jump Back In / Recently Played
                        if (jumpBackInSongs.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    VedTuneSectionHeader(
                                        title = "Jump Back In",
                                        actionText = "Play All",
                                        onActionClick = { viewModel.playJumpBackInSong(jumpBackInSongs.first()) },
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
                                                onClick = { viewModel.playJumpBackInSong(song) }
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

                        // 5. Fresh Tracks / Recently Added Songs
                        if (latestSongs.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = horizontalPadding)
                                ) {
                                    VedTuneSectionHeader(
                                        title = "Fresh Tracks",
                                        count = totalSongs,
                                        actionText = "See All",
                                        onActionClick = { onNavigateToLibraryTab(0) }
                                    )
                                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.s))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        VedTunePrimaryButton(
                                            text = "Play All",
                                            icon = Icons.Default.PlayArrow,
                                            onClick = { viewModel.playAll() },
                                            modifier = Modifier.weight(1f)
                                        )
                                        VedTuneSecondaryButton(
                                            text = "Shuffle",
                                            icon = Icons.Default.Shuffle,
                                            onClick = { viewModel.shuffleAll() },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            items(
                                items = latestSongs.take(8),
                                key = { it.id }
                            ) { song ->
                                val isCurrentSong = song.id == currentSongId
                                Box(modifier = Modifier.padding(horizontal = horizontalPadding)) {
                                    VedTuneSongRow(
                                        song = song,
                                        isCurrentSong = isCurrentSong,
                                        isPlaying = isPlaying && isCurrentSong,
                                        showArtwork = showArtwork,
                                        showDuration = true,
                                        onClick = { viewModel.playSong(song) },
                                        onOptionsClick = { selectedSongForOptions = song },
                                        containerColor = if (isCurrentSong) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerLow
                                        }
                                    )
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
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
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
                text = "What would you like to listen to?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VedTuneIconButton(
                icon = Icons.Default.Search,
                contentDescription = "Search music",
                onClick = onSearchClick,
                iconSize = VedTuneIconSizes.Standard,
                tint = MaterialTheme.colorScheme.onSurface
            )
            VedTuneIconButton(
                icon = Icons.Default.Settings,
                contentDescription = "Settings",
                onClick = onSettingsClick,
                iconSize = VedTuneIconSizes.Standard,
                tint = MaterialTheme.colorScheme.onSurface
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
                iconColor = MaterialTheme.colorScheme.error,
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
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.height(38.dp)
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
                modifier = Modifier.size(VedTuneIconSizes.Small)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (count != null) {
                Text(
                    text = "($count)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = VedTuneShapeTokens.Medium,
        color = if (isCurrentSong) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        modifier = modifier
            .width(132.dp)
            .wrapContentHeight()
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.s)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(VedTuneShapeTokens.Small),
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
        shape = VedTuneShapeTokens.ExtraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SongArtwork(
                albumId = album.id,
                modifier = Modifier.fillMaxSize(),
                showArtwork = showArtwork
            )
            // Gradient scrim for contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.85f)
                            ),
                            startY = 60f
                        )
                    )
            )
            // Details and Play CTA
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
                        color = Color.White.copy(alpha = 0.8f),
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
                        contentDescription = "Play Album",
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
