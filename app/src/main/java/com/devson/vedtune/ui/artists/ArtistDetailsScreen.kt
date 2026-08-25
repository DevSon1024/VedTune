package com.devson.vedtune.ui.artists

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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.vedtune.core.formatDuration
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.MainViewModel
import com.devson.vedtune.ui.components.MiniPlayer
import com.devson.vedtune.ui.components.PlayingIndicator
import com.devson.vedtune.ui.components.SongArtwork
import com.devson.vedtune.ui.components.VedTuneEmptyState
import com.devson.vedtune.ui.components.VedTunePrimaryButton
import com.devson.vedtune.ui.components.VedTuneSecondaryButton
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.spacing
import java.util.Locale

@Composable
fun ArtistDetailsScreen(
    viewModel: ArtistDetailsViewModel,
    mainViewModel: MainViewModel,
    onBackClick: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsState()
    val artistDetails by viewModel.artistDetails.collectAsState()
    val showArtwork by viewModel.showAlbumArt.collectAsState()

    val currentSongId by viewModel.currentSongId.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val currentSong by mainViewModel.currentSong.collectAsState()
    val mainIsPlaying by mainViewModel.isPlaying.collectAsState()
    val showArtworkFlow by mainViewModel.showAlbumArt.collectAsState()
    val showMiniPlayerProgress by mainViewModel.showMiniPlayerProgress.collectAsState()
    val isGestureMiniPlayerEnabled by mainViewModel.isGestureMiniPlayerEnabled.collectAsState()

    val progressProvider = remember(mainViewModel) {
        {
            val dur = mainViewModel.playbackDuration.value
            val pos = mainViewModel.playbackPosition.value
            if (dur > 0L) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
        }
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
                    .padding(horizontal = MaterialTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go Back"
                    )
                }
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.s))
                Text(
                    text = artistDetails?.name ?: "Artist Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (songs.isEmpty()) {
                VedTuneEmptyState(
                    icon = Icons.Default.Person,
                    title = "No Songs by Artist",
                    description = "This artist has no audio tracks in your library.",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = if (currentSong != null) 96.dp else MaterialTheme.spacing.l
                    )
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(MaterialTheme.spacing.l),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val distinctAlbums = remember(songs) {
                                songs.map { it.albumId }.distinct().take(4)
                            }

                            ElevatedCard(
                                modifier = Modifier.size(200.dp),
                                shape = VedTuneShapeTokens.Card
                            ) {
                                if (distinctAlbums.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "?")
                                    }
                                } else if (distinctAlbums.size == 1) {
                                    SongArtwork(
                                        albumId = distinctAlbums[0],
                                        modifier = Modifier.fillMaxSize(),
                                        showArtwork = showArtwork
                                    )
                                } else {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Row(modifier = Modifier.weight(1f)) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                SongArtwork(
                                                    albumId = distinctAlbums[0],
                                                    modifier = Modifier.fillMaxSize(),
                                                    showArtwork = showArtwork
                                                )
                                            }
                                            Box(modifier = Modifier.weight(1f)) {
                                                if (distinctAlbums.size > 1) {
                                                    SongArtwork(
                                                        albumId = distinctAlbums[1],
                                                        modifier = Modifier.fillMaxSize(),
                                                        showArtwork = showArtwork
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.fillMaxSize())
                                                }
                                            }
                                        }
                                        Row(modifier = Modifier.weight(1f)) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                if (distinctAlbums.size > 2) {
                                                    SongArtwork(
                                                        albumId = distinctAlbums[2],
                                                        modifier = Modifier.fillMaxSize(),
                                                        showArtwork = showArtwork
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.fillMaxSize())
                                                }
                                            }
                                            Box(modifier = Modifier.weight(1f)) {
                                                if (distinctAlbums.size > 3) {
                                                    SongArtwork(
                                                        albumId = distinctAlbums[3],
                                                        modifier = Modifier.fillMaxSize(),
                                                        showArtwork = showArtwork
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.fillMaxSize())
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.m))

                            Text(
                                text = artistDetails?.name ?: "Unknown Artist",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            val songsText = if (artistDetails?.songCount == 1) "1 Song" else "${artistDetails?.songCount ?: 0} Songs"
                            val albumsText = if (artistDetails?.albumCount == 1) "1 Album" else "${artistDetails?.albumCount ?: 0} Albums"
                            Text(
                                text = "$songsText • $albumsText",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.l))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VedTunePrimaryButton(
                                    text = "Play",
                                    icon = Icons.Default.PlayArrow,
                                    onClick = { viewModel.playArtist() },
                                    modifier = Modifier.weight(1f)
                                )

                                VedTuneSecondaryButton(
                                    text = "Shuffle",
                                    icon = Icons.Default.Shuffle,
                                    onClick = { viewModel.shuffleArtist() },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    itemsIndexed(
                        items = songs,
                        key = { _, song -> song.id }
                    ) { index, song ->
                        val isCurrentSong = song.id == currentSongId
                        ArtistTrackItem(
                            index = index + 1,
                            song = song,
                            isCurrentSong = isCurrentSong,
                            isPlaying = isPlaying,
                            onClick = { viewModel.playSong(song) }
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
                    progress = progressProvider,
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
fun ArtistTrackItem(
    index: Int,
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.spacing.l, vertical = MaterialTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCurrentSong) {
            Box(
                modifier = Modifier.width(36.dp),
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
                modifier = Modifier.width(36.dp)
            )
        }

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
                text = song.album,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.m))

        Text(
            text = formatDuration(song.duration),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
