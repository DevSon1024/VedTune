package com.devson.vedtune.ui.queue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.devson.vedtune.core.formatDuration
import com.devson.vedtune.domain.model.Playlist
import com.devson.vedtune.domain.model.QueueInfo
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.MainViewModel
import com.devson.vedtune.ui.components.AddToPlaylistDialog
import com.devson.vedtune.ui.components.AddToQueueModal
import com.devson.vedtune.ui.components.ArtworkThumbnailSize
import com.devson.vedtune.ui.components.MiniPlayer
import com.devson.vedtune.ui.components.PlayingIndicator
import com.devson.vedtune.ui.components.SongArtwork
import com.devson.vedtune.ui.components.VedTuneConfirmDialog
import com.devson.vedtune.ui.components.VedTuneEmptyState
import com.devson.vedtune.ui.components.VedTuneIconButton
import com.devson.vedtune.ui.songs.SongInfoBottomSheet
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.VedTuneTextStyles
import com.devson.vedtune.ui.theme.spacing
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private data class QueueSongEntry(
    val entryId: Long,
    val song: Song
)

@Composable
fun SuperQueueRoute(
    mainViewModel: MainViewModel,
    onBackClick: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToEditTags: (Long) -> Unit,
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SuperQueueViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SuperQueueScreen(
        uiState = uiState,
        mainViewModel = mainViewModel,
        onBackClick = onBackClick,
        onSelectQueue = viewModel::selectQueue,
        onSwitchActiveQueue = viewModel::switchActiveQueue,
        onPlayAll = viewModel::playAllInQueue,
        onPlaySongAt = viewModel::playSongAt,
        onPlayNext = viewModel::playNext,
        onReorderSongs = viewModel::reorderSongs,
        onRemoveSong = viewModel::removeSongFromQueue,
        onCreateQueue = viewModel::createNewQueue,
        onRenameQueue = viewModel::renameQueue,
        onDeleteQueue = viewModel::deleteQueue,
        onRemoveAllOtherQueues = viewModel::removeAllOtherQueues,
        onReorderQueues = viewModel::reorderQueues,
        onAddSongsToQueue = viewModel::addSongsToQueue,
        onSaveQueueAsPlaylist = viewModel::saveQueueAsPlaylist,
        onAddSongToPlaylist = viewModel::addSongToPlaylist,
        onCreatePlaylistAndAddSong = viewModel::createPlaylistAndAddSong,
        onSetSearchQuery = viewModel::setSearchQuery,
        onSetSearchActive = viewModel::setSearchActive,
        onSetSortMode = viewModel::setSortMode,
        onToggleSelectionMode = viewModel::toggleSelectionMode,
        onToggleSongSelection = viewModel::toggleSongSelection,
        onSelectAll = viewModel::selectAll,
        onClearSelection = viewModel::clearSelection,
        onRemoveSelectedSongs = viewModel::removeSelectedSongs,
        onExportM3u = { viewModel.exportQueueAsM3u(context) },
        onShareSongs = { songs -> viewModel.shareSongs(context, songs) },
        onNavigateToPlayer = onNavigateToPlayer,
        onNavigateToEditTags = onNavigateToEditTags,
        onNavigateToAlbum = onNavigateToAlbum,
        onNavigateToArtist = onNavigateToArtist,
        onCheckExistingSongIds = viewModel::getQueueSongIds,
        modifier = modifier
    )
}

@Composable
fun SuperQueueScreen(
    uiState: SuperQueueUiState,
    mainViewModel: MainViewModel,
    onBackClick: () -> Unit,
    onSelectQueue: (Long) -> Unit,
    onSwitchActiveQueue: (Long) -> Unit,
    onPlayAll: () -> Unit,
    onPlaySongAt: (Int) -> Unit,
    onPlayNext: (Song) -> Unit,
    onReorderSongs: (Int, Int) -> Unit,
    onRemoveSong: (Song, Int) -> Unit,
    onCreateQueue: (String) -> Unit,
    onRenameQueue: (Long, String) -> Unit,
    onDeleteQueue: (Long) -> Unit,
    onRemoveAllOtherQueues: (Long) -> Unit,
    onReorderQueues: (List<Long>) -> Unit,
    onAddSongsToQueue: (Long, List<Song>, Boolean) -> Unit,
    onSaveQueueAsPlaylist: (String) -> Unit,
    onAddSongToPlaylist: (Long, Long) -> Unit,
    onCreatePlaylistAndAddSong: (String, Long) -> Unit,
    onSetSearchQuery: (String) -> Unit,
    onSetSearchActive: (Boolean) -> Unit,
    onSetSortMode: (QueueSortMode) -> Unit,
    onToggleSelectionMode: () -> Unit,
    onToggleSongSelection: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onRemoveSelectedSongs: () -> Unit,
    onExportM3u: () -> Unit,
    onShareSongs: (List<Song>) -> Unit,
    onCheckExistingSongIds: suspend (Long) -> List<Long>,
    onNavigateToPlayer: () -> Unit,
    onNavigateToEditTags: (Long) -> Unit,
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentSong by mainViewModel.currentSong.collectAsStateWithLifecycle()
    val mainIsPlaying by mainViewModel.isPlaying.collectAsStateWithLifecycle()
    val showArtwork by mainViewModel.showAlbumArt.collectAsStateWithLifecycle()
    val showMiniPlayerProgress by mainViewModel.showMiniPlayerProgress.collectAsStateWithLifecycle()
    val isGestureMiniPlayerEnabled by mainViewModel.isGestureMiniPlayerEnabled.collectAsStateWithLifecycle()

    val progressProvider = remember(mainViewModel) {
        {
            val dur = mainViewModel.playbackDuration.value
            val pos = mainViewModel.playbackPosition.value
            if (dur > 0L) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
        }
    }

    var showQueuesModal by remember { mutableStateOf(false) }
    var showDeleteCurrentQueueConfirm by remember { mutableStateOf(false) }
    var showSaveQueueDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Dialog sheets
    var songForInfo by remember { mutableStateOf<Song?>(null) }
    var songForPlaylist by remember { mutableStateOf<Song?>(null) }
    var songForAnotherQueue by remember { mutableStateOf<Song?>(null) }

    // Local pool for drag reordering
    var nextEntryId by remember { mutableLongStateOf(1L) }
    var localQueueEntries by remember { mutableStateOf<List<QueueSongEntry>>(emptyList()) }
    var isDraggingAnyItem by remember { mutableStateOf(false) }
    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var dragEndIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(uiState.filteredSongs) {
        if (!isDraggingAnyItem) {
            val existingPool = localQueueEntries.toMutableList()
            val newEntries = uiState.filteredSongs.map { song ->
                val matchIndex = existingPool.indexOfFirst { it.song.id == song.id }
                if (matchIndex != -1) {
                    existingPool.removeAt(matchIndex)
                } else {
                    QueueSongEntry(entryId = nextEntryId++, song = song)
                }
            }
            localQueueEntries = newEntries
        }
    }

    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (dragStartIndex == -1) {
            dragStartIndex = from.index
        }
        dragEndIndex = to.index
        localQueueEntries = localQueueEntries.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    // Scroll to now playing item on start
    val currentSongIndex = remember(localQueueEntries, uiState.currentSong, uiState.selectedQueueId, uiState.activeQueueId) {
        if (uiState.selectedQueueId == uiState.activeQueueId) {
            localQueueEntries.indexOfFirst { it.song.id == uiState.currentSong?.id }
        } else {
            -1
        }
    }
    LaunchedEffect(currentSongIndex) {
        if (currentSongIndex > 0) {
            lazyListState.animateScrollToItem((currentSongIndex - 1).coerceAtLeast(0))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
        // TOP NAVIGATION & DROPDOWN BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.s, vertical = MaterialTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Top Dropdown Pill: [ 1. Devotional  v ]
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(VedTuneShapeTokens.Medium)
                    .clickable { showQueuesModal = true },
                shape = VedTuneShapeTokens.Medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacing.m, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val queueIndex = uiState.queues.indexOfFirst { it.id == uiState.selectedQueueId }
                    val displayIndex = if (queueIndex != -1) "${queueIndex + 1}. " else ""
                    Text(
                        text = "$displayIndex${uiState.selectedQueueName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Queue",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))

            // Search toggle icon
            IconButton(
                onClick = {
                    onSetSearchActive(!uiState.isSearchActive)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search in Queue",
                    tint = if (uiState.isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete queue icon with confirmation (All Songs queue cannot be deleted)
            val canDeleteCurrentQueue = uiState.queues.size > 1 && uiState.selectedQueueId != QueueInfo.DEFAULT_QUEUE_ID
            IconButton(
                onClick = { showDeleteCurrentQueueConfirm = true },
                enabled = canDeleteCurrentQueue
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete this queue",
                    tint = if (canDeleteCurrentQueue) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant
                )
            }
        }

        // EXPANDABLE SEARCH BAR
        AnimatedVisibility(
            visible = uiState.isSearchActive,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSetSearchQuery,
                placeholder = { Text("Search in this queue...") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSetSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                shape = VedTuneShapeTokens.Large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.m, vertical = MaterialTheme.spacing.xs)
            )
        }

        // QUEUE CONTROL STRIP
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.m, vertical = MaterialTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play All button
            IconButton(
                onClick = onPlayAll,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play all songs in queue",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Sorting button (disabled when shuffle is active)
            Box {
                IconButton(
                    onClick = {
                        if (uiState.isShuffleEnabled) {
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar("Sorting cannot be applied while shuffle is enabled")
                            }
                        } else {
                            showSortMenu = true
                        }
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort queue",
                        tint = if (uiState.isShuffleEnabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu && !uiState.isShuffleEnabled,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Custom / Drag Order") },
                        onClick = {
                            onSetSortMode(QueueSortMode.MANUAL)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Title (A-Z)") },
                        onClick = {
                            onSetSortMode(QueueSortMode.TITLE_ASC)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Title (Z-A)") },
                        onClick = {
                            onSetSortMode(QueueSortMode.TITLE_DESC)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Artist") },
                        onClick = {
                            onSetSortMode(QueueSortMode.ARTIST_ASC)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Album") },
                        onClick = {
                            onSetSortMode(QueueSortMode.ALBUM_ASC)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duration (Short to Long)") },
                        onClick = {
                            onSetSortMode(QueueSortMode.DURATION_ASC)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duration (Long to Short)") },
                        onClick = {
                            onSetSortMode(QueueSortMode.DURATION_DESC)
                            showSortMenu = false
                        }
                    )
                }
            }

            // Center Track Counter & Duration
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val currentTrackStr = if (uiState.currentSongTrackNumber != -1) "${uiState.currentSongTrackNumber}" else "-"
                Text(
                    text = "$currentTrackStr / ${uiState.totalTracksCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "🕒 ${uiState.totalDurationFormatted}",
                    style = VedTuneTextStyles.Metadata,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Add to Playlist (Save button)
            IconButton(
                onClick = { showSaveQueueDialog = true },
                enabled = uiState.songs.isNotEmpty(),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkAdd,
                    contentDescription = "Save queue as playlist",
                    tint = if (uiState.songs.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // More Options (Overflow 3-dots •••)
            Box {
                IconButton(
                    onClick = { showOverflowMenu = true },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false }
                ) {
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        text = { Text("Share songs") },
                        onClick = {
                            showOverflowMenu = false
                            onShareSongs(uiState.songs)
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        text = { Text("Export as .M3U file") },
                        onClick = {
                            showOverflowMenu = false
                            onExportM3u()
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        text = { Text(if (uiState.isSelectionMode) "Exit selection" else "Select multiple") },
                        onClick = {
                            showOverflowMenu = false
                            onToggleSelectionMode()
                        }
                    )
                }
            }
        }

        // MULTI-SELECTION ACTION BAR (When selection mode is active)
        AnimatedVisibility(
            visible = uiState.isSelectionMode,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacing.m, vertical = MaterialTheme.spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
                    ) {
                        IconButton(onClick = onClearSelection) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel selection",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "${uiState.selectedSongIds.size} selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                    ) {
                        IconButton(onClick = onSelectAll) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select All",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        if (uiState.selectedSongIds.isNotEmpty()) {
                            IconButton(onClick = onRemoveSelectedSongs) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove selected",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
            thickness = 1.dp
        )

        // SONGS LIST
        if (uiState.songs.isEmpty()) {
            VedTuneEmptyState(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                title = "Queue is Empty",
                description = "Add tracks from Songs, Albums, Artists, or Playlists.",
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
                    top = MaterialTheme.spacing.xs,
                    bottom = if (currentSong != null) 92.dp else MaterialTheme.spacing.m
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(
                    items = localQueueEntries,
                    key = { _, item -> item.entryId }
                ) { index, item ->
                    val song = item.song
                    val isNowPlaying = (song.id == uiState.currentSong?.id && uiState.selectedQueueId == uiState.activeQueueId)
                    val isSelected = uiState.selectedSongIds.contains(song.id)

                    ReorderableItem(
                        state = reorderableLazyListState,
                        key = item.entryId
                    ) { isItemDragging ->
                        val isDraggingPrev = remember { mutableStateOf(false) }
                        LaunchedEffect(isItemDragging) {
                            if (isItemDragging) {
                                isDraggingAnyItem = true
                            } else if (isDraggingPrev.value) {
                                isDraggingAnyItem = false
                                if (dragStartIndex != -1 && dragEndIndex != -1 && dragStartIndex != dragEndIndex) {
                                    onReorderSongs(dragStartIndex, dragEndIndex)
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                dragStartIndex = -1
                                dragEndIndex = -1
                            }
                            isDraggingPrev.value = isItemDragging
                        }

                        val scale by animateFloatAsState(
                            targetValue = if (isItemDragging) 1.02f else 1f,
                            label = "superQueueDragScale"
                        )
                        val elevation by animateDpAsState(
                            targetValue = if (isItemDragging) 8.dp else 0.dp,
                            label = "superQueueDragElevation"
                        )

                        SuperQueueTrackRow(
                            song = song,
                            isNowPlaying = isNowPlaying,
                            isPlaying = uiState.isPlaying,
                            isSelectionMode = uiState.isSelectionMode,
                            isSelected = isSelected,
                            elevation = elevation,
                            dragHandleModifier = Modifier
                                .draggableHandle(
                                    onDragStarted = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                )
                                .padding(MaterialTheme.spacing.s),
                            onClick = {
                                if (uiState.isSelectionMode) {
                                    onToggleSongSelection(song.id)
                                } else {
                                    onPlaySongAt(index)
                                    onNavigateToPlayer()
                                }
                            },
                            onLongClick = {
                                onToggleSongSelection(song.id)
                            },
                            onPlayNext = { onPlayNext(song) },
                            onAddToAnotherQueue = { songForAnotherQueue = song },
                            onAddToPlaylist = { songForPlaylist = song },
                            onRemoveFromQueue = { onRemoveSong(song, index) },
                            onSongInfo = { songForInfo = song },
                            onEditTags = { onNavigateToEditTags(song.id) },
                            onShare = { onShareSongs(listOf(song)) },
                            modifier = Modifier
                                .padding(horizontal = MaterialTheme.spacing.s, vertical = 2.dp)
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

    // Floating MiniPlayer overlay (transparent background, content visible underneath)
    if (currentSong != null) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
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
                showArtwork = showArtwork,
                showProgress = showMiniPlayerProgress,
                isGestureEnabled = isGestureMiniPlayerEnabled
            )
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = if (currentSong != null) 84.dp else 16.dp)
    )

    // QUEUES MANAGEMENT MODAL
    if (showQueuesModal) {
        QueuesManagementModal(
            queues = uiState.queues,
            selectedQueueId = uiState.selectedQueueId,
            activeQueueId = uiState.activeQueueId,
            isPlaying = uiState.isPlaying,
            onDismiss = { showQueuesModal = false },
            onSelectQueue = { qId ->
                onSelectQueue(qId)
                showQueuesModal = false
            },
            onSwitchActiveQueue = { qId ->
                onSwitchActiveQueue(qId)
                showQueuesModal = false
            },
            onCreateQueue = { name ->
                onCreateQueue(name)
                showQueuesModal = false
            },
            onRenameQueue = onRenameQueue,
            onDeleteQueue = onDeleteQueue,
            onRemoveAllOtherQueues = onRemoveAllOtherQueues,
            onReorderQueues = onReorderQueues
        )
    }

    // Delete Current Queue Confirmation
    if (showDeleteCurrentQueueConfirm) {
        VedTuneConfirmDialog(
            title = "Delete Queue",
            message = "Are you sure you want to delete \"${uiState.selectedQueueName}\" and all of its tracks?",
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onDeleteQueue(uiState.selectedQueueId)
                showDeleteCurrentQueueConfirm = false
            },
            onDismiss = { showDeleteCurrentQueueConfirm = false }
        )
    }

    // Save Queue as Playlist Dialog
    if (showSaveQueueDialog) {
        SaveQueueAsPlaylistDialog(
            defaultName = uiState.selectedQueueName,
            onDismiss = { showSaveQueueDialog = false },
            onSave = { name ->
                onSaveQueueAsPlaylist(name)
                showSaveQueueDialog = false
            }
        )
    }

    // Add to Playlist Dialog
    songForPlaylist?.let { targetSong ->
        AddToPlaylistDialog(
            playlists = uiState.playlists,
            onDismiss = { songForPlaylist = null },
            onPlaylistSelected = { playlistId ->
                onAddSongToPlaylist(playlistId, targetSong.id)
                songForPlaylist = null
            },
            onCreateNewPlaylist = { name ->
                onCreatePlaylistAndAddSong(name, targetSong.id)
                songForPlaylist = null
            }
        )
    }

    // Add to Another Queue Modal
    songForAnotherQueue?.let { targetSong ->
        AddToQueueModal(
            songs = listOf(targetSong),
            prefilledQueueName = targetSong.title,
            queues = uiState.queues,
            onDismiss = { songForAnotherQueue = null },
            onAddToQueue = { qId, songs, playNext ->
                onAddSongsToQueue(qId, songs, playNext)
                songForAnotherQueue = null
            },
            onCreateQueueAndAdd = { name, songs, playNext ->
                onCreateQueue(name)
                songForAnotherQueue = null
            },
            onCheckExistingSongIds = onCheckExistingSongIds
        )
    }

    // Song Info Bottom Sheet
    songForInfo?.let { targetSong ->
        SongInfoBottomSheet(
            song = targetSong,
            onNavigateToAlbum = onNavigateToAlbum,
            onNavigateToArtist = onNavigateToArtist,
            onNavigateToLocation = {},
            onNavigateToEditTags = onNavigateToEditTags,
            onClearHistory = {},
            onDismiss = { songForInfo = null }
        )
    }
}
}

@Composable
private fun SuperQueueTrackRow(
    song: Song,
    isNowPlaying: Boolean,
    isPlaying: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    elevation: Dp,
    dragHandleModifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToAnotherQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRemoveFromQueue: () -> Unit,
    onSongInfo: () -> Unit,
    onEditTags: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        isNowPlaying -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    }

    val borderStroke = when {
        isSelected -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        isNowPlaying -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
        else -> null
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = VedTuneShapeTokens.Medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = MaterialTheme.spacing.s, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle on left
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = dragHandleModifier.size(VedTuneIconSizes.Medium)
            )

            // Artwork
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(VedTuneShapeTokens.Small)
            ) {
                SongArtwork(
                    albumId = song.albumId,
                    lastModified = song.dateModified,
                    modifier = Modifier.fillMaxSize(),
                    thumbnailSize = ArtworkThumbnailSize.SMALL,
                    fallbackIcon = Icons.Default.MusicNote
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
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.m))

            // Metadata: Title, Artist, Album
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isNowPlaying) FontWeight.Bold else FontWeight.Medium,
                    color = if (isNowPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = if (song.album.isNotBlank() && song.album != "Unknown Album") {
                    "${song.artist} • ${song.album}"
                } else {
                    song.artist
                }
                Text(
                    text = subtitle,
                    style = VedTuneTextStyles.Metadata,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Duration
            Text(
                text = formatDuration(song.duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xs)
            )

            // Selection Checkbox or 3-dots Menu
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "Selected" else "Unselected",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(2.dp)
                )
            } else {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Song options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            text = { Text("Song Info") },
                            onClick = {
                                onSongInfo()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            text = { Text("Remove from this queue") },
                            onClick = {
                                onRemoveFromQueue()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            text = { Text("Add to another queue") },
                            onClick = {
                                onAddToAnotherQueue()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            text = { Text("Add to playlist") },
                            onClick = {
                                onAddToPlaylist()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            text = { Text("Edit tags") },
                            onClick = {
                                onEditTags()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            text = { Text("Share") },
                            onClick = {
                                onShare()
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveQueueAsPlaylistDialog(
    defaultName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(defaultName) }

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
                    text = "Enter a name for this playlist:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(text = "Playlist name") },
                    singleLine = true,
                    shape = VedTuneShapeTokens.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
