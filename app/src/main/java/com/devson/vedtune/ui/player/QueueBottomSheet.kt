package com.devson.vedtune.ui.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.components.SongArtwork
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playlistQueue by viewModel.playlistQueue.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyListState()

    var localQueue by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isDragging by remember { mutableStateOf(false) }
    var isWaitingForSync by remember { mutableStateOf(false) }

    LaunchedEffect(playlistQueue) {
        if (playlistQueue.isEmpty()) {
            localQueue = emptyList()
            isDragging = false
            isWaitingForSync = false
        } else {
            if (!isDragging) {
                localQueue = playlistQueue
            } else if (isWaitingForSync) {
                if (playlistQueue.size == localQueue.size && playlistQueue == localQueue) {
                    isDragging = false
                    isWaitingForSync = false
                }
            }
        }
    }

    LaunchedEffect(isWaitingForSync) {
        if (isWaitingForSync) {
            kotlinx.coroutines.delay(1000)
            if (isWaitingForSync) {
                isDragging = false
                isWaitingForSync = false
                localQueue = playlistQueue
            }
        }
    }

    var dragStartIndex by remember { mutableStateOf<Int?>(null) }
    val haptics = LocalHapticFeedback.current

    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localQueue = localQueue.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Up Next",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (localQueue.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearQueue() }) {
                        Text(
                            text = "Clear Queue",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (localQueue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Queue is empty",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(localQueue, key = { _, song -> song.id }) { index, song ->
                        ReorderableItem(
                            state = reorderableLazyListState,
                            key = song.id
                        ) { isItemDragging ->
                            val scale by animateFloatAsState(
                                targetValue = if (isItemDragging) 1.03f else 1f,
                                label = "dragScale"
                            )
                            val elevation by animateDpAsState(
                                targetValue = if (isItemDragging) 8.dp else 0.dp,
                                label = "dragElevation"
                            )

                            val dragHandleModifier = Modifier
                                .draggableHandle(
                                    onDragStarted = {
                                        isDragging = true
                                        dragStartIndex = index
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragStopped = {
                                        val start = dragStartIndex
                                        dragStartIndex = null
                                        if (start != null && start != index) {
                                            isWaitingForSync = true
                                            viewModel.moveQueueItem(start, index)
                                        } else {
                                            isDragging = false
                                        }
                                    }
                                )
                                .padding(16.dp)

                            QueueItemRow(
                                song = song,
                                isNowPlaying = song.id == currentSong?.id,
                                isPlaying = isPlaying,
                                onPlay = { viewModel.playQueueItemById(song.id) },
                                dragHandleModifier = dragHandleModifier,
                                isDragging = isItemDragging,
                                elevation = elevation,
                                modifier = Modifier
                                    .padding(vertical = 4.dp, horizontal = 16.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QueueItemRow(
    song: Song,
    isNowPlaying: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    dragHandleModifier: Modifier,
    isDragging: Boolean = false,
    elevation: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else if (isNowPlaying) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPlay() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                SongArtwork(
                    albumId = song.albumId,
                    modifier = Modifier.fillMaxSize(),
                    showArtwork = true
                )
                if (isNowPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        PlayingVisualizer(
                            isPlaying = isPlaying,
                            barColor = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    color = if (isNowPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isNowPlaying) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = "Reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = dragHandleModifier
            )
        }
    }
}

@Composable
fun PlayingVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PlayingVisualizer")

    val scale1 by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 350, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar1"
        )
    } else {
        remember { mutableStateOf(0.3f) }
    }

    val scale2 by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 450, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar2"
        )
    } else {
        remember { mutableStateOf(0.2f) }
    }

    val scale3 by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar3"
        )
    } else {
        remember { mutableStateOf(0.4f) }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(scale1)
                .background(barColor, shape = CircleShape)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(scale2)
                .background(barColor, shape = CircleShape)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(scale3)
                .background(barColor, shape = CircleShape)
        )
    }
}