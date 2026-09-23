package com.devson.vedtune.ui.albums

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
fun AlbumDetailsScreen(
    viewModel: AlbumDetailsViewModel,
    mainViewModel: MainViewModel,
    onBackClick: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val albumDetails by viewModel.albumDetails.collectAsStateWithLifecycle()
    val showArtwork by viewModel.showAlbumArt.collectAsStateWithLifecycle()

    val currentSongId by viewModel.currentSongId.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()

    val currentSong by mainViewModel.currentSong.collectAsStateWithLifecycle()
    val mainIsPlaying by mainViewModel.isPlaying.collectAsStateWithLifecycle()
    val showArtworkFlow by mainViewModel.showAlbumArt.collectAsStateWithLifecycle()
    val showMiniPlayerProgress by mainViewModel.showMiniPlayerProgress.collectAsStateWithLifecycle()
    val isGestureMiniPlayerEnabled by mainViewModel.isGestureMiniPlayerEnabled.collectAsStateWithLifecycle()

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
                    text = albumDetails?.title ?: "Album Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (songs.isEmpty()) {
                VedTuneEmptyState(
                    icon = Icons.Default.Album,
                    title = "No Songs in Album",
                    description = "This album contains no audio tracks.",
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
                                .padding(horizontal = MaterialTheme.spacing.l, vertical = MaterialTheme.spacing.m),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ElevatedCard(
                                modifier = Modifier
                                    .size(220.dp)
                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)), VedTuneShapeTokens.Large),
                                shape = VedTuneShapeTokens.Large,
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
                            ) {
                                SongArtwork(
                                    albumId = viewModel.albumId,
                                    modifier = Modifier.fillMaxSize(),
                                    showArtwork = showArtwork
                                )
                            }

                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.m))

                            Text(
                                text = albumDetails?.title ?: "Unknown Album",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = albumDetails?.artist ?: "Unknown Artist",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                val totalSeconds = remember(songs) { songs.sumOf { it.duration } }
                                val formattedTotalDuration = remember(totalSeconds) { formatDuration(totalSeconds) }
                                Text(
                                    text = "${songs.size} ${if (songs.size == 1) "track" else "tracks"} • $formattedTotalDuration",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.l))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VedTunePrimaryButton(
                                    text = "Play",
                                    icon = Icons.Default.PlayArrow,
                                    onClick = { viewModel.playAlbum() },
                                    modifier = Modifier.weight(1f)
                                )

                                VedTuneSecondaryButton(
                                    text = "Shuffle",
                                    icon = Icons.Default.Shuffle,
                                    onClick = { viewModel.shuffleAlbum() },
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
                        AlbumTrackItem(
                            trackNumber = index + 1,
                            song = song,
                            isCurrentSong = isCurrentSong,
                            isPlaying = isPlaying,
                            onClick = { viewModel.playSong(song) },
                            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.m, vertical = 2.dp)
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
fun AlbumTrackItem(
    trackNumber: Int,
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rowShape = RoundedCornerShape(12.dp)
    val backgroundColor = if (isCurrentSong) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.spacing.m, vertical = MaterialTheme.spacing.s),
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
                text = String.format(Locale.getDefault(), "%02d", trackNumber),
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
                fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = song.artist,
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

