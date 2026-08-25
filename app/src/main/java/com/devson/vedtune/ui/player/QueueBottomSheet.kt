package com.devson.vedtune.ui.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.devson.vedtune.ui.components.AddToPlaylistDialog
import com.devson.vedtune.ui.components.PlayingIndicator
import com.devson.vedtune.ui.components.SongArtwork
import com.devson.vedtune.ui.components.VedTuneConfirmDialog
import com.devson.vedtune.ui.components.VedTuneEmptyState
import com.devson.vedtune.ui.components.VedTuneIconButton
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.VedTuneTextStyles
import com.devson.vedtune.ui.theme.spacing
import kotlinx.coroutines.delay
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
    val showArtwork by viewModel.showAlbumArt.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    val lazyListState = rememberLazyListState()

    var localQueue by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isDragging by remember { mutableStateOf(false) }
    var isWaitingForSync by remember { mutableStateOf(false) }
    var dragStartIndex by remember { mutableStateOf<Int?>(null) }

    var showSaveQueueDialog by remember { mutableStateOf(false) }
    var showClearQueueConfirm by remember { mutableStateOf(false) }
    var songForPlaylist by remember { mutableStateOf<Song?>(null) }

    val haptics = LocalHapticFeedback.current

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
            delay(1000)
            if (isWaitingForSync) {
                isDragging = false
                isWaitingForSync = false
                localQueue = playlistQueue
            }
        }
    }

    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localQueue = localQueue.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = VedTuneShapeTokens.BottomSheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Queue Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.l, vertical = MaterialTheme.spacing.s),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(VedTuneIconSizes.Medium)
                    )
                    Column {
                        Text(
                            text = "Playback Queue",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (localQueue.isNotEmpty()) {
                            Text(
                                text = "${localQueue.size} ${if (localQueue.size == 1) "track" else "tracks"}",
                                style = VedTuneTextStyles.Metadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                ) {
                    if (localQueue.isNotEmpty()) {
                        VedTuneIconButton(
                            icon = Icons.Default.BookmarkAdd,
                            contentDescription = "Save Queue as Playlist",
                            onClick = { showSaveQueueDialog = true },
                            tint = MaterialTheme.colorScheme.primary
                        )
                        VedTuneIconButton(
                            icon = Icons.Default.ClearAll,
                            contentDescription = "Clear Queue",
                            onClick = { showClearQueueConfirm = true },
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    VedTuneIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close Queue",
                        onClick = onDismiss,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            )

            if (localQueue.isEmpty()) {
                VedTuneEmptyState(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    title = "Queue is Empty",
                    description = "Play any song, album, or playlist to start listening.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        top = MaterialTheme.spacing.m,
                        bottom = MaterialTheme.spacing.xxl
                    )
                ) {
                    itemsIndexed(
                        items = localQueue,
                        key = { _, song -> song.id }
                    ) { index, song ->
                        val isNowPlaying = (song.id == currentSong?.id)

                        ReorderableItem(
                            state = reorderableLazyListState,
                            key = song.id
                        ) { isItemDragging ->
                            val scale by animateFloatAsState(
                                targetValue = if (isItemDragging) 1.02f else 1f,
                                label = "queueDragScale"
                            )
                            val elevation by animateDpAsState(
                                targetValue = if (isItemDragging) 6.dp else 0.dp,
                                label = "queueDragElevation"
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
                                .padding(MaterialTheme.spacing.s)

                            QueueTrackRow(
                                song = song,
                                isNowPlaying = isNowPlaying,
                                isPlaying = isPlaying,
                                showArtwork = showArtwork,
                                isDragging = isItemDragging,
                                elevation = elevation,
                                dragHandleModifier = dragHandleModifier,
                                onPlay = { viewModel.playQueueItemById(song.id) },
                                onPlayNext = { viewModel.playNext(song) },
                                onAddToPlaylist = { songForPlaylist = song },
                                onRemoveFromQueue = { viewModel.removeQueueItem(index) },
                                modifier = Modifier
                                    .padding(horizontal = MaterialTheme.spacing.m, vertical = 2.dp)
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

    // Save Queue Dialog
    if (showSaveQueueDialog) {
        SaveQueueDialog(
            onDismiss = { showSaveQueueDialog = false },
            onSave = { name ->
                viewModel.saveQueueAsPlaylist(name)
                showSaveQueueDialog = false
            }
        )
    }

    // Clear Queue Confirmation Dialog
    if (showClearQueueConfirm) {
        VedTuneConfirmDialog(
            title = "Clear Queue",
            message = "Are you sure you want to clear all songs from the current queue?",
            confirmText = "Clear",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                viewModel.clearQueue()
                showClearQueueConfirm = false
            },
            onDismiss = { showClearQueueConfirm = false }
        )
    }

    // Add to Playlist Dialog for Queue Item
    songForPlaylist?.let { targetSong ->
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { songForPlaylist = null },
            onPlaylistSelected = { playlistId ->
                viewModel.addSongToPlaylist(playlistId, targetSong.id)
                songForPlaylist = null
            },
            onCreateNewPlaylist = { playlistName ->
                viewModel.createPlaylistAndAddSong(playlistName, targetSong.id)
                songForPlaylist = null
            }
        )
    }
}

@Composable
private fun QueueTrackRow(
    song: Song,
    isNowPlaying: Boolean,
    isPlaying: Boolean,
    showArtwork: Boolean,
    isDragging: Boolean,
    elevation: Dp,
    dragHandleModifier: Modifier,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRemoveFromQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = VedTuneShapeTokens.Medium,
        colors = CardDefaults.cardColors(
            containerColor = when {
                isDragging -> MaterialTheme.colorScheme.surfaceContainerHighest
                isNowPlaying -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPlay)
                .padding(horizontal = MaterialTheme.spacing.s, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = dragHandleModifier.size(VedTuneIconSizes.Medium)
            )

            // Song Artwork with Now Playing indicator
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(VedTuneShapeTokens.Small)
            ) {
                SongArtwork(
                    albumId = song.albumId,
                    modifier = Modifier.fillMaxSize(),
                    showArtwork = showArtwork
                )
                if (isNowPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        PlayingIndicator(
                            isPlaying = isPlaying,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.m))

            // Metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isNowPlaying) FontWeight.Bold else FontWeight.Medium,
                    color = if (isNowPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${song.album}",
                    style = VedTuneTextStyles.Metadata,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Options overflow
            Box {
                VedTuneIconButton(
                    icon = Icons.Default.MoreVert,
                    contentDescription = "Options",
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
                        text = { Text("Add to Playlist") },
                        onClick = {
                            onAddToPlaylist()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove from Queue") },
                        onClick = {
                            onRemoveFromQueue()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveQueueDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var playlistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Save Queue as Playlist",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)) {
                Text(
                    text = "Enter a name for this saved queue:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    placeholder = { Text(text = "My Saved Queue") },
                    singleLine = true,
                    shape = VedTuneShapeTokens.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(playlistName.trim()) },
                enabled = playlistName.isNotBlank()
            ) {
                Text(text = "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}