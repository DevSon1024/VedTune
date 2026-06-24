package com.devson.vedtune.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.media3.common.Player
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.components.PlayerIcons
import com.devson.vedtune.ui.components.SongArtwork
import java.util.Locale

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    showArtwork: Boolean = true,
    showRemainingTime: Boolean = false
) {
    val song by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.playbackPosition.collectAsState()
    val duration by viewModel.playbackDuration.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsState()
    val sleepTimerRemaining by viewModel.sleepTimerRemaining.collectAsState()

    var showSleepTimerDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        if (song == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No song selected",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        } else {
            val activeSong = song!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Text(
                        text = "Now Playing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Sleep Timer Info or Button
                    IconButton(onClick = { showSleepTimerDialog = true }) {
                        Icon(
                            imageVector = PlayerIcons.Timer,
                            contentDescription = "Sleep Timer",
                            tint = if (sleepTimerRemaining > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // Sleep Timer countdown indicator if active
                if (sleepTimerRemaining > 0) {
                    Text(
                        text = "Sleeps in ${formatTime(sleepTimerRemaining)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Artwork Cover Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.extraLarge)
                ) {
                    SongArtwork(
                        albumId = activeSong.albumId,
                        modifier = Modifier.fillMaxSize(),
                        showArtwork = showArtwork
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Song details (Title, Artist, Favorite)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeSong.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = activeSong.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (activeSong.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (activeSong.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Progress slider
                var sliderValue by remember(position) { mutableStateOf(position.toFloat()) }
                var isDragging by remember { mutableStateOf(false) }

                Slider(
                    value = if (isDragging) sliderValue else position.toFloat(),
                    onValueChange = {
                        isDragging = true
                        sliderValue = it
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        viewModel.seekTo(sliderValue.toLong())
                    },
                    valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(if (isDragging) sliderValue.toLong() else position),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    val remainingOrDurationMs = if (showRemainingTime) {
                        val currentPos = if (isDragging) sliderValue.toLong() else position
                        (duration - currentPos).coerceAtLeast(0L)
                    } else {
                        duration
                    }
                    Text(
                        text = if (showRemainingTime) "-${formatTime(remainingOrDurationMs)}" else formatTime(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Playing Control Buttons (Shuffle, Prev, Play/Pause, Next, Repeat)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(onClick = { viewModel.setShuffleModeEnabled(!shuffleModeEnabled) }) {
                        Icon(
                            imageVector = PlayerIcons.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (shuffleModeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }

                    // Previous
                    IconButton(onClick = { viewModel.skipToPrevious() }) {
                        Icon(
                            imageVector = PlayerIcons.Previous,
                            contentDescription = "Previous",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Play/Pause circular button
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        if (isPlaying) {
                            // Render custom vector pause bars (scaled up)
                            Row(
                                modifier = Modifier.size(24.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val tint = MaterialTheme.colorScheme.onPrimaryContainer
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .fillMaxHeight()
                                        .background(tint, MaterialTheme.shapes.small)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .fillMaxHeight()
                                        .background(tint, MaterialTheme.shapes.small)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Next
                    IconButton(onClick = { viewModel.skipToNext() }) {
                        Icon(
                            imageVector = PlayerIcons.Next,
                            contentDescription = "Next",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Repeat
                    val repeatIcon = if (repeatMode == Player.REPEAT_MODE_ONE) PlayerIcons.RepeatOne else PlayerIcons.Repeat
                    IconButton(
                        onClick = {
                            val nextMode = when (repeatMode) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                else -> Player.REPEAT_MODE_OFF
                            }
                            viewModel.setRepeatMode(nextMode)
                        }
                    ) {
                        Icon(
                            imageVector = repeatIcon,
                            contentDescription = "Repeat",
                            tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Lyrics Placeholder Segment
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 40.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Lyrics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Lyrics placeholder - Synchronization and retrieval will be implemented in a future phase.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        SleepTimerDialog(
            currentRemainingMs = sleepTimerRemaining,
            onSelect = { minutes ->
                viewModel.startSleepTimer(minutes)
                showSleepTimerDialog = false
            },
            onCancelTimer = {
                viewModel.cancelSleepTimer()
                showSleepTimerDialog = false
            },
            onDismiss = { showSleepTimerDialog = false }
        )
    }
}

@Composable
fun SleepTimerDialog(
    currentRemainingMs: Long,
    onSelect: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Sleep Timer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentRemainingMs > 0) {
                    Text(
                        text = "Active: Timer expires in ${formatTime(currentRemainingMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                listOf(5, 15, 30, 45, 60).forEach { minutes ->
                    Button(
                        onClick = { onSelect(minutes) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "$minutes Minutes")
                    }
                }
            }
        },
        confirmButton = {
            if (currentRemainingMs > 0) {
                TextButton(onClick = onCancelTimer) {
                    Text(text = "Turn Off Timer")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Dismiss")
            }
        }
    )
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
