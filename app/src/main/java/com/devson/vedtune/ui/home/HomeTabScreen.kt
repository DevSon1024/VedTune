package com.devson.vedtune.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.devson.vedtune.domain.model.Album
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.components.PlayingIndicator
import com.devson.vedtune.ui.components.SongArtwork
import com.devson.vedtune.ui.components.VedTuneListItem
import com.devson.vedtune.ui.components.VedTuneOverlapCarousel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTabScreen(
    viewModel: HomeViewModel,
    onNavigateToAlbum: (Long) -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToPlaylist: (Long) -> Unit = {},
    onNavigateToGenre: (String) -> Unit = {},
    onNavigateToLibraryTab: (Int) -> Unit = {},
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val recentlyAddedAlbums by viewModel.recentlyAddedAlbums.collectAsState()
    val jumpBackInSongs by viewModel.jumpBackInSongs.collectAsState()
    val latestSongs by viewModel.latestSongs.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val currentSongId by viewModel.currentSongId.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val showArtwork by viewModel.showArtwork.collectAsState()

    val totalSongs by viewModel.totalSongsCount.collectAsState()
    val totalAlbums by viewModel.totalAlbumsCount.collectAsState()
    val totalArtists by viewModel.totalArtistsCount.collectAsState()
    val totalPlaylists by viewModel.totalPlaylistsCount.collectAsState()

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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Permission Required",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Permission Required",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "VedTune needs access to your audio files to build your music library.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { launcher.launch(permission) }) {
                            Text("Grant Permission")
                        }
                    }
                }
            }
            recentlyAddedAlbums.isEmpty() && latestSongs.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "No Music Found",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No music files found", style = MaterialTheme.typography.titleMedium)
                        Text("Scanning device for music...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
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
                            top = 12.dp,
                            bottom = contentPadding.calculateBottomPadding() + 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // 1. Top Banner: VedTuneOverlapCarousel with Recently Added Albums
                        if (recentlyAddedAlbums.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    SectionHeader(
                                        title = "Recently Added Albums",
                                        subtitle = "Discover the latest additions to your collection",
                                        actionText = "See All",
                                        onActionClick = { onNavigateToLibraryTab(1) },
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    VedTuneOverlapCarousel(
                                        items = recentlyAddedAlbums,
                                        contentPadding = PaddingValues(horizontal = 44.dp),
                                        overlapOffset = 28.dp,
                                        key = { it.id },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { album, _, _ ->
                                        AlbumBannerCard(
                                            album = album,
                                            showArtwork = showArtwork,
                                            onClick = { onNavigateToAlbum(album.id) },
                                            onPlayClick = { viewModel.playAlbum(album) }
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Quick Picks: Jump Back In (LazyRow)
                        if (jumpBackInSongs.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    SectionHeader(
                                        title = "Jump Back In",
                                        subtitle = "Continue where you left off",
                                        actionText = "Play All",
                                        onActionClick = { viewModel.playJumpBackInSong(jumpBackInSongs.first()) },
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(
                                            items = jumpBackInSongs,
                                            key = { it.id }
                                        ) { song ->
                                            val isCurrentSong = song.id == currentSongId
                                            QuickPickSongCard(
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

                        // 3. Library Navigation: Sleek ElevatedCard Tiles
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                SectionHeader(
                                    title = "Explore Library",
                                    subtitle = "Browse your organized collection",
                                    actionText = null,
                                    onActionClick = null,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        LibraryTile(
                                            title = "Songs",
                                            countText = "$totalSongs Tracks",
                                            icon = Icons.AutoMirrored.Filled.List,
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                            iconColor = MaterialTheme.colorScheme.primary,
                                            onClick = { onNavigateToLibraryTab(0) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        LibraryTile(
                                            title = "Albums",
                                            countText = "$totalAlbums Albums",
                                            icon = Icons.Default.Album,
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                            iconColor = MaterialTheme.colorScheme.secondary,
                                            onClick = { onNavigateToLibraryTab(1) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        LibraryTile(
                                            title = "Artists",
                                            countText = "$totalArtists Artists",
                                            icon = Icons.Default.Person,
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                            iconColor = MaterialTheme.colorScheme.tertiary,
                                            onClick = { onNavigateToLibraryTab(2) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        LibraryTile(
                                            title = "Playlists",
                                            countText = "$totalPlaylists Playlists",
                                            icon = Icons.Default.Favorite,
                                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                            iconColor = MaterialTheme.colorScheme.error,
                                            onClick = { onNavigateToLibraryTab(4) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Fresh Tracks: Recent Song List
                        if (latestSongs.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    SectionHeader(
                                        title = "Fresh Tracks",
                                        subtitle = "Recently added to device",
                                        actionText = null,
                                        onActionClick = null,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { viewModel.playAll() },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Play All")
                                        }
                                        FilledTonalButton(
                                            onClick = { viewModel.shuffleAll() },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Shuffle,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Shuffle")
                                        }
                                    }
                                }
                            }

                            items(
                                items = latestSongs.take(8),
                                key = { it.id }
                            ) { song ->
                                val isCurrentSong = song.id == currentSongId
                                val containerColor = if (isCurrentSong) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }

                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    VedTuneListItem(
                                        primaryText = song.title,
                                        secondaryText = song.artist,
                                        containerColor = containerColor,
                                        onClick = { viewModel.playSong(song) },
                                        leadingContent = {
                                            Box(
                                                modifier = Modifier.size(48.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                SongArtwork(
                                                    albumId = song.albumId,
                                                    lastModified = song.dateModified,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(RoundedCornerShape(8.dp)),
                                                    showArtwork = showArtwork
                                                )
                                                if (isCurrentSong) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color.Black.copy(alpha = 0.4f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        PlayingIndicator(
                                                            isPlaying = isPlaying,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
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

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionText != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AlbumBannerCard(
    album: Album,
    showArtwork: Boolean,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SongArtwork(
                albumId = album.id,
                modifier = Modifier.fillMaxSize(),
                showArtwork = showArtwork
            )
            // Gradient scrim for text readability and sleek visual depth
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
            // Album details and quick play button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
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
                    Text(
                        text = "${album.artist} • ${album.songCount} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FilledIconButton(
                    onClick = onPlayClick,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Album",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickPickSongCard(
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    showArtwork: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isCurrentSong) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        modifier = modifier
            .width(136.dp)
            .wrapContentHeight()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .clip(RoundedCornerShape(12.dp)),
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
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
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

@Composable
private fun LibraryTile(
    title: String,
    countText: String,
    icon: ImageVector,
    containerColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        modifier = modifier.height(84.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.2f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = countText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
