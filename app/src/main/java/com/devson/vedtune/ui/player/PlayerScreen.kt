package com.devson.vedtune.ui.player

import android.content.ContentUris
import android.content.Intent
import android.provider.MediaStore
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.components.AddToPlaylistDialog
import com.devson.vedtune.ui.components.SongArtwork
import com.devson.vedtune.ui.songs.SongInfoDialog
import java.util.Locale
import kotlin.math.roundToInt

// Unified sheet state — only one sheet active at a time
private sealed class PlayerSheetState {
    object Hidden : PlayerSheetState()
    object Queue : PlayerSheetState()
    object Options : PlayerSheetState()
    object AddToPlaylist : PlayerSheetState()
    object SongInfo : PlayerSheetState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToEditTags: (Long) -> Unit,
    modifier: Modifier = Modifier,
    showArtwork: Boolean = true,
    showRemainingTime: Boolean = false
) {
    val song by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val position by viewModel.playbackPosition.collectAsStateWithLifecycle()
    val duration by viewModel.playbackDuration.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsStateWithLifecycle()
    val sleepTimerRemaining by viewModel.sleepTimerRemaining.collectAsStateWithLifecycle()
    val isFav by viewModel.isFavorite.collectAsStateWithLifecycle()

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var sheetState: PlayerSheetState by remember { mutableStateOf(PlayerSheetState.Hidden) }
    var showLyrics by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Artwork scale animation — stable, driven only by isPlaying
    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ArtworkScale"
    )

    // Swipe-up gesture to open queue — defined once and shared
    val swipeUpToQueue = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures { _, dragAmount ->
            if (dragAmount < -20) {
                sheetState = PlayerSheetState.Queue
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .then(swipeUpToQueue)
    ) {
        if (song == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                PlayerHeader(
                    sleepTimerRemaining = sleepTimerRemaining,
                    onBackClick = onBackClick,
                    onTimerClick = { showSleepTimerDialog = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Album Art / Lyrics crossfade area
                ArtworkLyricsCard(
                    song = activeSong,
                    showArtwork = showArtwork,
                    showLyrics = showLyrics,
                    artworkScale = artworkScale,
                    position = position,
                    viewModel = viewModel,
                    swipeUpModifier = swipeUpToQueue,
                    onToggleLyrics = { showLyrics = !showLyrics }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Song title, artist, favorite + options
                SongInfoRow(
                    song = activeSong,
                    isFav = isFav,
                    swipeUpModifier = swipeUpToQueue,
                    onFavClick = { viewModel.toggleFavorite() },
                    onOptionsClick = { sheetState = PlayerSheetState.Options }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Seek slider — isolated to prevent parent recomposition on every tick
                SeekBar(
                    position = position,
                    duration = duration,
                    showRemainingTime = showRemainingTime,
                    swipeUpModifier = swipeUpToQueue,
                    onSeek = { viewModel.seekTo(it) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Playback controls
                PlaybackControls(
                    isPlaying = isPlaying,
                    repeatMode = repeatMode,
                    shuffleModeEnabled = shuffleModeEnabled,
                    swipeUpModifier = swipeUpToQueue,
                    onShuffleClick = { viewModel.setShuffleModeEnabled(!shuffleModeEnabled) },
                    onPreviousClick = { viewModel.skipToPrevious() },
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onNextClick = { viewModel.skipToNext() },
                    onRepeatClick = {
                        val nextMode = when (repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                        viewModel.setRepeatMode(nextMode)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Queue open trigger
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                        .then(swipeUpToQueue),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { sheetState = PlayerSheetState.Queue }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = "Playback Queue",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
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

    // Queue Bottom Sheet
    if (sheetState == PlayerSheetState.Queue && song != null) {
        val queueList by viewModel.queue.collectAsStateWithLifecycle()
        val activeSong = song!!
        val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { sheetState = PlayerSheetState.Hidden },
            sheetState = queueSheetState
        ) {
            QueueSheetContent(
                queue = queueList,
                currentSong = activeSong,
                isPlaying = isPlaying,
                onItemClick = { index -> viewModel.skipToQueueItem(index) },
                onMoveItem = { from, to -> viewModel.moveQueueItem(from, to) }
            )
        }
    }

    // Options Bottom Sheet
    if (sheetState == PlayerSheetState.Options && song != null) {
        val activeSong = song!!
        ModalBottomSheet(
            onDismissRequest = { sheetState = PlayerSheetState.Hidden }
        ) {
            OptionsSheetContent(
                song = activeSong,
                showArtwork = showArtwork,
                onAddToPlaylist = { sheetState = PlayerSheetState.AddToPlaylist },
                onGoToArtist = {
                    sheetState = PlayerSheetState.Hidden
                    onNavigateToArtist(activeSong.artist)
                },
                onGoToAlbum = {
                    sheetState = PlayerSheetState.Hidden
                    onNavigateToAlbum(activeSong.albumId)
                },
                onViewDetails = { sheetState = PlayerSheetState.SongInfo },
                onShare = {
                    sheetState = PlayerSheetState.Hidden
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
                }
            )
        }
    }

    // Add to Playlist Dialog
    if (sheetState == PlayerSheetState.AddToPlaylist && song != null) {
        val playlists by viewModel.playlists.collectAsStateWithLifecycle()
        val activeSong = song!!
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { sheetState = PlayerSheetState.Hidden },
            onPlaylistSelected = { playlistId ->
                viewModel.addSongToPlaylist(playlistId, activeSong.id)
                sheetState = PlayerSheetState.Hidden
            },
            onCreateNewPlaylist = { playlistName ->
                viewModel.createPlaylistAndAddSong(playlistName, activeSong.id)
                sheetState = PlayerSheetState.Hidden
            }
        )
    }

    // Song Info Dialog
    if (sheetState == PlayerSheetState.SongInfo && song != null) {
        val activeSong = song!!
        SongInfoDialog(
            song = activeSong,
            onEditTagsClick = {
                sheetState = PlayerSheetState.Hidden
                onNavigateToEditTags(activeSong.id)
            },
            onDismiss = { sheetState = PlayerSheetState.Hidden }
        )
    }
}

// ---------------------------------------------------------------------------
// Stateless sub-composables
// ---------------------------------------------------------------------------

@Composable
private fun PlayerHeader(
    sleepTimerRemaining: Long,
    onBackClick: () -> Unit,
    onTimerClick: () -> Unit
) {
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
        IconButton(onClick = onTimerClick) {
            Icon(
                imageVector = Icons.Rounded.Timer,
                contentDescription = "Sleep Timer",
                tint = if (sleepTimerRemaining > 0) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground
            )
        }
    }

    if (sleepTimerRemaining > 0) {
        Text(
            text = "Sleeps in ${formatTime(sleepTimerRemaining)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    } else {
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ArtworkLyricsCard(
    song: Song,
    showArtwork: Boolean,
    showLyrics: Boolean,
    artworkScale: Float,
    position: Long,
    viewModel: PlayerViewModel,
    swipeUpModifier: Modifier,
    onToggleLyrics: () -> Unit
) {
    val lyricsText by viewModel.currentLyrics.collectAsStateWithLifecycle()

    // Parse LRC lines only when lyrics text changes — not on every position tick
    val parsedLines = remember(lyricsText) {
        val text = lyricsText
        if (!text.isNullOrBlank()) parseLrc(text) else emptyList()
    }

    // derivedStateOf: activeLineIndex recalculates only when position or parsedLines change,
    // and only triggers recomposition when the resulting index value actually changes.
    val activeLineIndex by remember(parsedLines) {
        derivedStateOf { getActiveLyricsLineIndex(parsedLines, position) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .then(
                if (showLyrics) Modifier.height(380.dp)
                else Modifier.aspectRatio(1f)
            )
            .scale(artworkScale)
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable { onToggleLyrics() }
            .then(swipeUpModifier)
    ) {
        Crossfade(targetState = showLyrics, label = "ArtworkLyricsCrossfade") { isLyrics ->
            if (isLyrics) {
                LyricsPanel(
                    lyricsText = lyricsText,
                    parsedLines = parsedLines,
                    activeLineIndex = activeLineIndex
                )
            } else {
                SongArtwork(
                    albumId = song.albumId,
                    modifier = Modifier.fillMaxSize(),
                    showArtwork = showArtwork
                )
            }
        }
    }
}

@Composable
private fun LyricsPanel(
    lyricsText: String?,
    parsedLines: List<LrcLine>,
    activeLineIndex: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f))
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            lyricsText.isNullOrBlank() -> {
                Text(
                    text = "No lyrics available",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center
                )
            }

            parsedLines.isEmpty() -> {
                // Plain text lyrics (no timestamps)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = lyricsText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }

            else -> {
                // Synced LRC lyrics
                val listState = rememberLazyListState()

                LaunchedEffect(activeLineIndex) {
                    if (activeLineIndex >= 0) {
                        listState.animateScrollToItem(
                            index = activeLineIndex,
                            scrollOffset = -150
                        )
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(120.dp)) }

                    itemsIndexed(parsedLines) { index, line ->
                        val isActive = index == activeLineIndex
                        val alpha by animateFloatAsState(
                            targetValue = if (isActive) 1f else 0.4f,
                            label = "LyricsAlpha"
                        )
                        val lineScale by animateFloatAsState(
                            targetValue = if (isActive) 1.08f else 0.95f,
                            label = "LyricsScale"
                        )
                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(lineScale)
                                .graphicsLayer { this.alpha = alpha }
                                .padding(horizontal = 16.dp)
                        )
                    }

                    item { Spacer(modifier = Modifier.height(120.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SongInfoRow(
    song: Song,
    isFav: Boolean,
    swipeUpModifier: Modifier,
    onFavClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(swipeUpModifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onFavClick) {
                Icon(
                    imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFav) "Remove from Favorites" else "Add to Favorites",
                    tint = if (isFav) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = onOptionsClick) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun SeekBar(
    position: Long,
    duration: Long,
    showRemainingTime: Boolean,
    swipeUpModifier: Modifier,
    onSeek: (Long) -> Unit
) {
    // Slider drag state: isDragging guards against position ticks overwriting sliderValue
    var isDragging by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(position.toFloat()) }

    // Sync slider to position only when not dragging
    LaunchedEffect(position) {
        if (!isDragging) {
            sliderValue = position.toFloat()
        }
    }

    Slider(
        value = sliderValue,
        onValueChange = { value ->
            isDragging = true
            sliderValue = value
        },
        onValueChangeFinished = {
            isDragging = false
            onSeek(sliderValue.toLong())
        },
        valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
        modifier = Modifier
            .fillMaxWidth()
            .then(swipeUpModifier)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(swipeUpModifier),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formatTime(sliderValue.toLong()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        val endLabel = if (showRemainingTime) {
            "-${formatTime((duration - sliderValue.toLong()).coerceAtLeast(0L))}"
        } else {
            formatTime(duration)
        }
        Text(
            text = endLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    repeatMode: Int,
    shuffleModeEnabled: Boolean,
    swipeUpModifier: Modifier,
    onShuffleClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onRepeatClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(swipeUpModifier),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onShuffleClick) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = "Shuffle",
                tint = if (shuffleModeEnabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
        IconButton(onClick = onPreviousClick) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .size(72.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        IconButton(onClick = onNextClick) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        IconButton(onClick = onRepeatClick) {
            Icon(
                imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne
                else Icons.Rounded.Repeat,
                contentDescription = "Repeat",
                tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Queue sheet
// ---------------------------------------------------------------------------

@Composable
private fun QueueSheetContent(
    queue: List<Song>,
    currentSong: Song,
    isPlaying: Boolean,
    onItemClick: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Play Queue",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        if (queue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Queue is empty",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = 16.dp)
            ) {
                itemsIndexed(queue, key = { _, s -> s.id }) { index, s ->
                    QueueItem(
                        song = s,
                        index = index,
                        isCurrent = s.id == currentSong.id,
                        isPlaying = isPlaying,
                        onClick = { onItemClick(index) },
                        onMove = onMoveItem
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueItem(
    song: Song,
    index: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMove: (Int, Int) -> Unit
) {
    var itemOffsetY by remember { mutableFloatStateOf(0f) }
    var isDraggingItem by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .offset { IntOffset(0, if (isDraggingItem) itemOffsetY.roundToInt() else 0) }
            .pointerInput(index) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { isDraggingItem = true },
                    onDragEnd = {
                        isDraggingItem = false
                        itemOffsetY = 0f
                    },
                    onDragCancel = {
                        isDraggingItem = false
                        itemOffsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        itemOffsetY += dragAmount.y
                        val swapThreshold = 64f * density
                        val currentIdx = index
                        if (itemOffsetY > swapThreshold) {
                            onMove(currentIdx, currentIdx + 1)
                            itemOffsetY -= swapThreshold
                        } else if (itemOffsetY < -swapThreshold) {
                            onMove(currentIdx, currentIdx - 1)
                            itemOffsetY += swapThreshold
                        }
                    }
                )
            }
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (isCurrent && isPlaying) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Playing",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = formatTime(song.duration),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Options sheet
// ---------------------------------------------------------------------------

@Composable
private fun OptionsSheetContent(
    song: Song,
    showArtwork: Boolean,
    onAddToPlaylist: () -> Unit,
    onGoToArtist: () -> Unit,
    onGoToAlbum: () -> Unit,
    onViewDetails: () -> Unit,
    onShare: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongArtwork(
                albumId = song.albumId,
                modifier = Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                showArtwork = showArtwork
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        HorizontalDivider()

        BottomSheetOption(icon = Icons.AutoMirrored.Rounded.PlaylistAdd, title = "Add to Playlist", onClick = onAddToPlaylist)
        BottomSheetOption(icon = Icons.Rounded.Person, title = "Go to Artist", onClick = onGoToArtist)
        BottomSheetOption(icon = Icons.Rounded.Album, title = "Go to Album", onClick = onGoToAlbum)
        BottomSheetOption(icon = Icons.Rounded.Info, title = "View Details", onClick = onViewDetails)
        BottomSheetOption(icon = Icons.Rounded.Share, title = "Share", onClick = onShare)
    }
}

// ---------------------------------------------------------------------------
// Shared helpers
// ---------------------------------------------------------------------------

@Composable
fun BottomSheetOption(
    icon: ImageVector,
    title: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = tint
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
                TextButton(onClick = onCancelTimer) { Text(text = "Turn Off Timer") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Dismiss") }
        }
    )
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

// ---------------------------------------------------------------------------
// Synced lyrics helpers
// ---------------------------------------------------------------------------

data class LrcLine(val timestamp: Long, val text: String)

fun parseLrc(lrcText: String): List<LrcLine> {
    val timeRegex = Regex("\\[(\\d+):(\\d+)(?:\\.(\\d+))?]")
    val parsedLines = mutableListOf<LrcLine>()
    var hasTimestamps = false

    for (line in lrcText.lines()) {
        val matches = timeRegex.findAll(line).toList()
        if (matches.isNotEmpty()) {
            hasTimestamps = true
            val text = line.replace(timeRegex, "").trim()
            for (match in matches) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val msPart = match.groupValues.getOrNull(3)
                val ms = if (!msPart.isNullOrEmpty()) {
                    when (msPart.length) {
                        1 -> msPart.toLong() * 100
                        2 -> msPart.toLong() * 10
                        else -> msPart.substring(0, 3).toLong()
                    }
                } else 0L
                parsedLines.add(LrcLine((min * 60 * 1000) + (sec * 1000) + ms, text))
            }
        }
    }
    return if (hasTimestamps) parsedLines.sortedBy { it.timestamp } else emptyList()
}

fun getActiveLyricsLineIndex(lines: List<LrcLine>, currentPosition: Long): Int {
    var activeIndex = -1
    for (i in lines.indices) {
        if (currentPosition >= lines[i].timestamp) activeIndex = i else break
    }
    return activeIndex
}
