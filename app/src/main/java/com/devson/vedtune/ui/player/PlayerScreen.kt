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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.components.AddToPlaylistDialog
import com.devson.vedtune.ui.components.SongArtwork
import com.devson.vedtune.ui.songs.SongInfoDialog
import java.util.Locale
import kotlin.math.roundToInt

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
    val song by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.playbackPosition.collectAsState()
    val duration by viewModel.playbackDuration.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsState()
    val sleepTimerRemaining by viewModel.sleepTimerRemaining.collectAsState()
    val isFav by viewModel.isFavorite.collectAsState()

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showSongInfoDialog by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ArtworkScale"
    )

    val gradientColors = listOf(
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        MaterialTheme.colorScheme.background
    )

    val swipeUpModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures { _, dragAmount ->
            if (dragAmount < -15) {
                showQueueSheet = true
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors))
            .then(swipeUpModifier)
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
                    .statusBarsPadding()
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
                            imageVector = Icons.Rounded.Timer,
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

                // Artwork Cover Card or Synced Auto-Scroll Lyrics
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .then(
                            if (showLyrics) {
                                Modifier.height(380.dp)
                            } else {
                                Modifier.aspectRatio(1f)
                            }
                        )
                        .scale(artworkScale)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .clickable { showLyrics = !showLyrics }
                        .then(swipeUpModifier)
                ) {
                    Crossfade(targetState = showLyrics, label = "ArtworkLyricsCrossfade") { isLyrics ->
                        if (isLyrics) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f))
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val lyricsText by viewModel.currentLyrics.collectAsState()
                                val textVal = lyricsText
                                if (textVal.isNullOrBlank()) {
                                    Text(
                                        text = "No lyrics available",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    val parsedLines = remember(textVal) { parseLrc(textVal) }

                                    if (parsedLines.isEmpty()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                                .padding(horizontal = 24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = textVal,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    } else {
                                        val listState = rememberLazyListState()
                                        val activeLineIndex = remember(parsedLines, position) {
                                            getActiveLyricsLineIndex(parsedLines, position)
                                        }

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
                                                val scale by animateFloatAsState(
                                                    targetValue = if (isActive) 1.08f else 0.95f,
                                                    label = "LyricsScale"
                                                )
                                                val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                                                val textColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer

                                                Text(
                                                    text = line.text,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = fontWeight,
                                                    color = textColor,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .scale(scale)
                                                        .graphicsLayer { this.alpha = alpha }
                                                        .padding(horizontal = 16.dp)
                                                )
                                            }

                                            item { Spacer(modifier = Modifier.height(120.dp)) }
                                        }
                                    }
                                }
                            }
                        } else {
                            SongArtwork(
                                albumId = activeSong.albumId,
                                modifier = Modifier.fillMaxSize(),
                                showArtwork = showArtwork
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Song details (Title, Artist, Favorite & Options Menu)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(swipeUpModifier),
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = { showOptionsSheet = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(swipeUpModifier),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(onClick = { viewModel.setShuffleModeEnabled(!shuffleModeEnabled) }) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (shuffleModeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }

                    // Previous
                    IconButton(onClick = { viewModel.skipToPrevious() }) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
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
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Next
                    IconButton(onClick = { viewModel.skipToNext() }) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Repeat
                    val repeatIcon = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
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

                Spacer(modifier = Modifier.height(16.dp))

                // Queue Music Sheet toggle trigger button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                        .then(swipeUpModifier),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showQueueSheet = true }) {
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

    // Interactive Queue Bottom Sheet
    if (showQueueSheet && song != null) {
        val queueList by viewModel.queue.collectAsState()
        val activeSong = song!!
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            sheetState = sheetState
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

                if (queueList.isEmpty()) {
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
                        itemsIndexed(queueList, key = { _, s -> s.id }) { index, s ->
                            val isCurrent = s.id == activeSong.id
                            val currentIndex by rememberUpdatedState(index)
                            var itemOffsetY by remember { mutableStateOf(0f) }
                            var isDraggingItem by remember { mutableStateOf(false) }
                            val dragOffset = if (isDraggingItem) itemOffsetY else 0f

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .offset { IntOffset(0, dragOffset.roundToInt()) }
                                    .pointerInput(Unit) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                isDraggingItem = true
                                            },
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
                                                val density = this.density
                                                val swapThreshold = 64f * density
                                                val idx = currentIndex
                                                
                                                if (itemOffsetY > swapThreshold && idx < queueList.lastIndex) {
                                                    viewModel.moveQueueItem(idx, idx + 1)
                                                    itemOffsetY -= swapThreshold
                                                } else if (itemOffsetY < -swapThreshold && idx > 0) {
                                                    viewModel.moveQueueItem(idx, idx - 1)
                                                    itemOffsetY += swapThreshold
                                                }
                                            }
                                        )
                                    }
                                    .clickable {
                                        viewModel.skipToQueueItem(index)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrent) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
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
                                            text = s.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = s.artist,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
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
                                            text = formatTime(s.duration),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
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

    // Options Bottom Sheet
    if (showOptionsSheet && song != null) {
        val activeSong = song!!
        ModalBottomSheet(
            onDismissRequest = { showOptionsSheet = false }
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
                        albumId = activeSong.albumId,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeSong.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = activeSong.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                HorizontalDivider()

                BottomSheetOption(
                    icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    title = "Add to Playlist",
                    onClick = {
                        showOptionsSheet = false
                        showAddToPlaylistDialog = true
                    }
                )
                BottomSheetOption(
                    icon = Icons.Rounded.Person,
                    title = "Go to Artist",
                    onClick = {
                        showOptionsSheet = false
                        onNavigateToArtist(activeSong.artist)
                    }
                )
                BottomSheetOption(
                    icon = Icons.Rounded.Album,
                    title = "Go to Album",
                    onClick = {
                        showOptionsSheet = false
                        onNavigateToAlbum(activeSong.albumId)
                    }
                )
                BottomSheetOption(
                    icon = Icons.Rounded.Info,
                    title = "View Details",
                    onClick = {
                        showOptionsSheet = false
                        showSongInfoDialog = true
                    }
                )
                BottomSheetOption(
                    icon = Icons.Rounded.Share,
                    title = "Share",
                    onClick = {
                        showOptionsSheet = false
                        val songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, activeSong.id)
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
    }

    // Add to Playlist Dialog
    if (showAddToPlaylistDialog && song != null) {
        val playlists by viewModel.playlists.collectAsState()
        val activeSong = song!!
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { showAddToPlaylistDialog = false },
            onPlaylistSelected = { playlistId ->
                viewModel.addSongToPlaylist(playlistId, activeSong.id)
                showAddToPlaylistDialog = false
            },
            onCreateNewPlaylist = { playlistName ->
                viewModel.createPlaylistAndAddSong(playlistName, activeSong.id)
                showAddToPlaylistDialog = false
            }
        )
    }

    // View Song Details Info Dialog
    if (showSongInfoDialog && song != null) {
        val activeSong = song!!
        SongInfoDialog(
            song = activeSong,
            onEditTagsClick = {
                showSongInfoDialog = false
                onNavigateToEditTags(activeSong.id)
            },
            onDismiss = { showSongInfoDialog = false }
        )
    }
}

@Composable
fun BottomSheetOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

// Synced Lyrics Data Structures & Helpers

data class LrcLine(val timestamp: Long, val text: String)

fun parseLrc(lrcText: String): List<LrcLine> {
    val lines = lrcText.lines()
    val parsedLines = mutableListOf<LrcLine>()
    val timeRegex = Regex("\\[(\\d+):(\\d+)(?:\\.(\\d+))?]")

    var hasTimestamps = false
    for (line in lines) {
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
                } else {
                    0L
                }
                val timeMs = (min * 60 * 1000) + (sec * 1000) + ms
                parsedLines.add(LrcLine(timeMs, text))
            }
        }
    }

    return if (hasTimestamps) parsedLines.sortedBy { it.timestamp } else emptyList()
}

fun getActiveLyricsLineIndex(lines: List<LrcLine>, currentPosition: Long): Int {
    var activeIndex = -1
    for (i in lines.indices) {
        if (currentPosition >= lines[i].timestamp) {
            activeIndex = i
        } else {
            break
        }
    }
    return activeIndex
}
