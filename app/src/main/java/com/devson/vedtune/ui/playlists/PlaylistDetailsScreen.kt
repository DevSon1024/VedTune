package com.devson.vedtune.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.MainViewModel
import com.devson.vedtune.ui.components.MiniPlayer
import com.devson.vedtune.ui.components.PlayingIndicator
import com.devson.vedtune.ui.components.PlaylistArtworkCollage
import com.devson.vedtune.ui.components.SongArtwork
import com.devson.vedtune.ui.components.VedTuneEmptyState
import com.devson.vedtune.ui.components.VedTuneIconButton
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.VedTuneTextStyles
import com.devson.vedtune.ui.theme.spacing
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun PlaylistDetailsScreen(
    viewModel: PlaylistDetailsViewModel,
    mainViewModel: MainViewModel,
    onBackClick: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val previewAlbumIds by viewModel.previewAlbumIds.collectAsStateWithLifecycle()
    val totalDurationMs by viewModel.totalDurationMs.collectAsStateWithLifecycle()
    val playlistDetails by viewModel.playlistDetails.collectAsStateWithLifecycle()
    val showArtwork by viewModel.showAlbumArt.collectAsStateWithLifecycle()

    val currentSongId by viewModel.currentSongId.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()

    val currentSong by mainViewModel.currentSong.collectAsStateWithLifecycle()
    val mainIsPlaying by mainViewModel.isPlaying.collectAsStateWithLifecycle()
    val position by mainViewModel.playbackPosition.collectAsStateWithLifecycle()
    val duration by mainViewModel.playbackDuration.collectAsStateWithLifecycle()
    val showArtworkFlow by mainViewModel.showAlbumArt.collectAsStateWithLifecycle()
    val showMiniPlayerProgress by mainViewModel.showMiniPlayerProgress.collectAsStateWithLifecycle()
    val isGestureMiniPlayerEnabled by mainViewModel.isGestureMiniPlayerEnabled.collectAsStateWithLifecycle()

    val progress = remember(position, duration) {
        if (duration > 0) position.toFloat() / duration.toFloat() else 0f
    }

    val isFavorite = viewModel.isFavoritePlaylist
    val playlistTitle = if (isFavorite) "Favorites" else (playlistDetails?.name ?: "Playlist")

    val formattedTotalDuration = remember(totalDurationMs) {
        val hours = TimeUnit.MILLISECONDS.toHours(totalDurationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(totalDurationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(totalDurationMs) % 60
        when {
            hours > 0 -> String.format(Locale.getDefault(), "%d hr %d min", hours, minutes)
            minutes > 0 -> String.format(Locale.getDefault(), "%d min %d sec", minutes, seconds)
            else -> String.format(Locale.getDefault(), "%d sec", seconds)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Navigation Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = MaterialTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VedTuneIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go Back",
                    onClick = onBackClick,
                    iconSize = VedTuneIconSizes.Medium
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.s))
                Text(
                    text = playlistTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            if (songs.isEmpty()) {
                VedTuneEmptyState(
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.AutoMirrored.Filled.QueueMusic,
                    title = if (isFavorite) "No Favorite Songs" else "Playlist is Empty",
                    description = if (isFavorite) "Tap the heart icon on any track to add it to your favorites."
                    else "Add songs to this playlist from your library to start listening.",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = if (currentSong != null) 96.dp else 24.dp
                    )
                ) {
                    // Playlist Hero Header Section
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = MaterialTheme.spacing.l, vertical = MaterialTheme.spacing.m),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Collage / Hero Artwork
                            PlaylistArtworkCollage(
                                albumIds = previewAlbumIds,
                                isFavorite = isFavorite,
                                showArtwork = showArtwork,
                                modifier = Modifier
                                    .size(168.dp)
                                    .clip(VedTuneShapeTokens.Large)
                            )

                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.m))

                            // Playlist Title
                            Text(
                                text = playlistTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                            // Metadata (Count + Duration)
                            Text(
                                text = "${songs.size} ${if (songs.size == 1) "track" else "tracks"} • $formattedTotalDuration",
                                style = VedTuneTextStyles.Metadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.l))

                            // Play and Shuffle Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.playPlaylist() },
                                    shape = VedTuneShapeTokens.Pill,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(VedTuneIconSizes.Small)
                                    )
                                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                                    Text(
                                        text = "Play All",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                FilledTonalButton(
                                    onClick = { viewModel.shufflePlaylist() },
                                    shape = VedTuneShapeTokens.Pill,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = null,
                                        modifier = Modifier.size(VedTuneIconSizes.Small)
                                    )
                                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                                    Text(
                                        text = "Shuffle",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.m))
                        }
                    }

                    // Track List
                    itemsIndexed(
                        items = songs,
                        key = { _, song -> song.id }
                    ) { index, song ->
                        val isCurrentSong = song.id == currentSongId
                        PlaylistTrackRow(
                            index = index + 1,
                            song = song,
                            showArtwork = showArtwork,
                            isCurrentSong = isCurrentSong,
                            isPlaying = isPlaying,
                            onClick = { viewModel.playSong(song) },
                            onPlayNext = { viewModel.playNext(song) },
                            onRemoveClick = { viewModel.removeSongFromPlaylist(song.id) }
                        )
                    }
                }
            }
        }

        // MiniPlayer Overlay
        if (currentSong != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                MiniPlayer(
                    song = currentSong,
                    isPlaying = mainIsPlaying,
                    progress = progress,
                    onPlayPauseClick = {
                        if (mainIsPlaying) mainViewModel.pause() else mainViewModel.play()
                    },
                    onSkipNextClick = { mainViewModel.skipToNext() },
                    onSkipPreviousClick = { mainViewModel.skipToPrevious() },
                    onClick = onNavigateToPlayer,
                    showArtwork = showArtworkFlow,
                    showProgress = showMiniPlayerProgress,
                    isGestureEnabled = isGestureMiniPlayerEnabled
                )
            }
        }
    }
}

@Composable
private fun PlaylistTrackRow(
    index: Int,
    song: Song,
    showArtwork: Boolean,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    onRemoveClick: () -> Unit,
    isCurrentSong: Boolean = false,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = "${song.artist} • ${song.album}",
                style = VedTuneTextStyles.Metadata,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isCurrentSong) {
                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PlayingIndicator(
                            isPlaying = isPlaying,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Text(
                        text = String.format(Locale.getDefault(), "%02d", index),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.width(24.dp)
                    )
                }

                if (showArtwork) {
                    SongArtwork(
                        albumId = song.albumId,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(VedTuneShapeTokens.Small),
                        showArtwork = showArtwork
                    )
                }
            }
        },
        trailingContent = {
            Box {
                VedTuneIconButton(
                    icon = Icons.Default.MoreVert,
                    contentDescription = "Track Options",
                    onClick = { showMenu = true },
                    iconSize = VedTuneIconSizes.Medium,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Play Next") },
                        onClick = {
                            onPlayNext()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove from Playlist") },
                        onClick = {
                            onRemoveClick()
                            showMenu = false
                        }
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = if (isCurrentSong) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(VedTuneShapeTokens.Medium)
            .clickable(onClick = onClick)
    )
}
