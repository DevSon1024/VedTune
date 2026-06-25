package com.devson.vedtune.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.components.SongArtwork
import com.devson.vedtune.ui.components.PlayingIndicator
import com.devson.vedtune.ui.components.MiniPlayer
import com.devson.vedtune.ui.MainViewModel
import androidx.compose.foundation.layout.PaddingValues
import java.util.Locale

@Composable
fun PlaylistDetailsScreen(
    viewModel: PlaylistDetailsViewModel,
    mainViewModel: MainViewModel,
    onBackClick: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsState()
    val playlistDetails by viewModel.playlistDetails.collectAsState()
    val showArtwork by viewModel.showAlbumArt.collectAsState()

    val currentSongId by viewModel.currentSongId.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val currentSong by mainViewModel.currentSong.collectAsState()
    val mainIsPlaying by mainViewModel.isPlaying.collectAsState()
    val position by mainViewModel.playbackPosition.collectAsState()
    val duration by mainViewModel.playbackDuration.collectAsState()
    val showArtworkFlow by mainViewModel.showAlbumArt.collectAsState()
    val showMiniPlayerProgress by mainViewModel.showMiniPlayerProgress.collectAsState()
    val isGestureMiniPlayerEnabled by mainViewModel.isGestureMiniPlayerEnabled.collectAsState()

    val progress = remember(position, duration) {
        if (duration > 0) position.toFloat() / duration.toFloat() else 0f
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go Back"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = playlistDetails?.name ?: "Playlist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (songs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No songs in this playlist")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = if (currentSong != null) 96.dp else 16.dp
                    )
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Card(
                                modifier = Modifier.size(160.dp),
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Playlist cover",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(64.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = playlistDetails?.name ?: "Playlist",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = if (songs.size == 1) "1 Song" else "${songs.size} Songs",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.playPlaylist() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "Play")
                                }

                                FilledTonalButton(
                                    onClick = { viewModel.shufflePlaylist() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "Shuffle")
                                }
                            }
                        }
                    }

                    itemsIndexed(
                        items = songs,
                        key = { _, song -> song.id }
                    ) { index, song ->
                        val isCurrentSong = song.id == currentSongId
                        PlaylistTrackItem(
                            index = index + 1,
                            song = song,
                            showArtwork = showArtwork,
                            isCurrentSong = isCurrentSong,
                            isPlaying = isPlaying,
                            onClick = { viewModel.playSong(song) },
                            onRemoveClick = { viewModel.removeSongFromPlaylist(song.id) }
                        )
                    }
                }
            }
        }

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
fun PlaylistTrackItem(
    index: Int,
    song: Song,
    showArtwork: Boolean,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    isCurrentSong: Boolean = false,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCurrentSong) {
            Box(
                modifier = Modifier.width(32.dp),
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
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.width(32.dp)
            )
        }

        SongArtwork(
            albumId = song.albumId,
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.small),
            showArtwork = showArtwork
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Remove from Playlist") },
                    onClick = {
                        onRemoveClick()
                        showMenu = false
                    }
                )
            }
        }
    }
}
