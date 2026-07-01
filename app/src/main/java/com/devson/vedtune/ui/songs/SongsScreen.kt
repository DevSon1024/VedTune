package com.devson.vedtune.ui.songs

import android.Manifest
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.launch
import androidx.media3.common.Player
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.ContextCompat
import androidx.compose.material3.Button
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.components.SongArtwork
import com.devson.vedtune.ui.components.AddToPlaylistDialog
import com.devson.vedtune.ui.components.VedTuneTopAppBar
import com.devson.vedtune.ui.components.PlayingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    viewModel: SongsViewModel,
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToEditTags: (Long) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    navigateToLocationEvent: kotlinx.coroutines.flow.SharedFlow<Long>? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentSongId by viewModel.currentSongId.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    val playlists by viewModel.playlists.collectAsState()
    var songForPlaylist by remember { mutableStateOf<Song?>(null) }

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var highlightedSongId by remember { mutableStateOf<Long?>(null) }
    val highlightAlpha = remember { Animatable(0f) }

    LaunchedEffect(navigateToLocationEvent) {
        navigateToLocationEvent?.collect { songId ->
            val index = uiState.songs.indexOfFirst { it.id == songId }
            if (index != -1) {
                if (uiState.isGridView) {
                    lazyGridState.animateScrollToItem(index)
                } else {
                    lazyListState.animateScrollToItem(index)
                }
                highlightedSongId = songId
                highlightAlpha.snapTo(0f)
                highlightAlpha.animateTo(
                    targetValue = 0.5f,
                    animationSpec = repeatable(
                        iterations = 3,
                        animation = tween(durationMillis = 300),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                highlightAlpha.animateTo(0f)
                highlightedSongId = null
            }
        }
    }

    // Dialog and bottom sheet states
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var showInfoDialogSong by remember { mutableStateOf<Song?>(null) }
    var showPreviewDialogSong by remember { mutableStateOf<Song?>(null) }
    var showDeleteConfirmDialogSong by remember { mutableStateOf<Song?>(null) }

    val context = LocalContext.current

    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onWritePermissionGranted(context)
            viewModel.onDeletePermissionGranted()
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is SongsUiEvent.ShowError -> {
                    android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_LONG).show()
                }
                is SongsUiEvent.LaunchIntentSender -> {
                    val request = IntentSenderRequest.Builder(event.intentSender).build()
                    intentSenderLauncher.launch(request)
                }
            }
        }
    }

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            viewModel.refresh()
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Box {
            VedTuneTopAppBar(
                title = "Songs",
                searchQuery = uiState.searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                searchPlaceholder = "Search songs, artists, albums...",
                showSortAction = true,
                onSortClick = { showSortMenu = true },
                showLayoutToggleAction = true,
                isGridView = uiState.isGridView,
                onLayoutToggleClick = { viewModel.toggleLayoutView() },
                totalItemCount = uiState.totalItemCount,
                totalDurationMs = uiState.totalDurationMs,
                onShuffleClick = { viewModel.playShuffleAll() },
                modifier = Modifier.statusBarsPadding()
            )

            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                DropdownMenuItem(
                    text = { Text("Sort by Title") },
                    onClick = {
                        viewModel.setSortBy(SortBy.TITLE)
                        showSortMenu = false
                    },
                    leadingIcon = {
                        if (uiState.sortBy == SortBy.TITLE) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Sort by Artist") },
                    onClick = {
                        viewModel.setSortBy(SortBy.ARTIST)
                        showSortMenu = false
                    },
                    leadingIcon = {
                        if (uiState.sortBy == SortBy.ARTIST) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Sort by Album") },
                    onClick = {
                        viewModel.setSortBy(SortBy.ALBUM)
                        showSortMenu = false
                    },
                    leadingIcon = {
                        if (uiState.sortBy == SortBy.ALBUM) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Sort by Date Added") },
                    onClick = {
                        viewModel.setSortBy(SortBy.DATE_ADDED)
                        showSortMenu = false
                    },
                    leadingIcon = {
                        if (uiState.sortBy == SortBy.DATE_ADDED) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
                        }
                    }
                )

                HorizontalDivider()

                DropdownMenuItem(
                    text = { Text("Ascending") },
                    onClick = {
                        viewModel.setSortOrder(SortOrder.ASCENDING)
                        showSortMenu = false
                    },
                    leadingIcon = {
                        if (uiState.sortOrder == SortOrder.ASCENDING) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Descending") },
                    onClick = {
                        viewModel.setSortOrder(SortOrder.DESCENDING)
                        showSortMenu = false
                    },
                    leadingIcon = {
                        if (uiState.sortOrder == SortOrder.DESCENDING) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
                        }
                    }
                )
            }
        }

        // 3. Songs List or Grid with Pull-to-refresh
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.weight(1f)
        ) {
            when {
                !hasPermission -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Permission Required",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Permission Required",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "VedTune needs access to your audio files to build your music library.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { launcher.launch(permission) }) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.songs.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "No Music Found",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No music files found", style = MaterialTheme.typography.titleMedium)
                            Text("Pull down to scan your library", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                uiState.isGridView -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = lazyGridState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = contentPadding.calculateBottomPadding() + 16.dp
                        )
                    ) {
                        items(uiState.songs, key = { it.id }) { song ->
                            val isCurrentSong = song.id == currentSongId
                            SongGridItem(
                                song = song,
                                onClick = { viewModel.playSong(song) },
                                showArtwork = uiState.showArtwork,
                                isCurrentSong = isCurrentSong,
                                isPlaying = isPlaying,
                                onOptionsClick = { selectedSongForOptions = song },
                                isHighlighted = song.id == highlightedSongId,
                                highlightAlpha = if (song.id == highlightedSongId) highlightAlpha.value else 0f
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = contentPadding.calculateBottomPadding() + 16.dp
                        )
                    ) {
                        items(uiState.songs, key = { it.id }) { song ->
                            val isCurrentSong = song.id == currentSongId
                            SongListItem(
                                song = song,
                                onClick = { viewModel.playSong(song) },
                                showArtwork = uiState.showArtwork,
                                isCurrentSong = isCurrentSong,
                                isPlaying = isPlaying,
                                onOptionsClick = { selectedSongForOptions = song },
                                isHighlighted = song.id == highlightedSongId,
                                highlightAlpha = if (song.id == highlightedSongId) highlightAlpha.value else 0f
                            )
                        }
                    }
                }
            }
        }
    }

    if (songForPlaylist != null) {
        val targetSong = songForPlaylist!!
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

    if (selectedSongForOptions != null) {
        val song = selectedSongForOptions!!
        ModalBottomSheet(
            onDismissRequest = { selectedSongForOptions = null }
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
                        lastModified = song.dateModified,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Artist",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Album,
                                contentDescription = "Album",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = song.album,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                HorizontalDivider()
                
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        BottomSheetOption(
                            icon = Icons.Default.Info,
                            title = "Song Info",
                            onClick = {
                                selectedSongForOptions = null
                                showInfoDialogSong = song
                            }
                        )
                    }
                    item {
                        BottomSheetOption(
                            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                            title = "Add to Playlist",
                            onClick = {
                                selectedSongForOptions = null
                                songForPlaylist = song
                            }
                        )
                    }
                    item {
                        BottomSheetOption(
                            icon = Icons.Default.PlayCircle,
                            title = "Preview Song",
                            onClick = {
                                selectedSongForOptions = null
                                showPreviewDialogSong = song
                            }
                        )
                    }
                    item {
                        BottomSheetOption(
                            icon = Icons.Default.Share,
                            title = "Share",
                            onClick = {
                                selectedSongForOptions = null
                                val songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "audio/*"
                                    putExtra(Intent.EXTRA_STREAM, songUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Song"))
                            }
                        )
                    }
                    item {
                        BottomSheetOption(
                            icon = Icons.Default.Edit,
                            title = "Edit Tags",
                            onClick = {
                                selectedSongForOptions = null
                                onNavigateToEditTags(song.id)
                            }
                        )
                    }
                    item {
                        BottomSheetOption(
                            icon = Icons.Default.DeleteForever,
                            title = "Delete Permanently",
                            tint = MaterialTheme.colorScheme.error,
                            onClick = {
                                selectedSongForOptions = null
                                showDeleteConfirmDialogSong = song
                            }
                        )
                    }
                    item {
                        BottomSheetOption(
                            icon = Icons.Default.Album,
                            title = "Go to Album",
                            onClick = {
                                selectedSongForOptions = null
                                onNavigateToAlbum(song.albumId)
                            }
                        )
                    }
                    item {
                        BottomSheetOption(
                            icon = Icons.Default.Person,
                            title = "Go to Artist",
                            onClick = {
                                selectedSongForOptions = null
                                onNavigateToArtist(song.artist)
                            }
                        )
                    }
                    item {
                        BottomSheetOption(
                            icon = Icons.Default.Shuffle,
                            title = "Shuffle",
                            onClick = {
                                selectedSongForOptions = null
                                viewModel.playShuffle(song)
                            }
                        )
                    }
                    item {
                        BottomSheetOption(
                            icon = Icons.Default.QueuePlayNext,
                            title = "Play Next",
                            onClick = {
                                selectedSongForOptions = null
                                viewModel.playNext(song)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showInfoDialogSong != null) {
        val song = showInfoDialogSong!!
        SongInfoBottomSheet(
            song = song,
            onNavigateToAlbum = onNavigateToAlbum,
            onNavigateToArtist = onNavigateToArtist,
            onNavigateToLocation = { songId ->
                val index = uiState.songs.indexOfFirst { it.id == songId }
                if (index != -1) {
                    scope.launch {
                        if (uiState.isGridView) {
                            lazyGridState.animateScrollToItem(index)
                        } else {
                            lazyListState.animateScrollToItem(index)
                        }
                        highlightedSongId = songId
                        highlightAlpha.snapTo(0f)
                        highlightAlpha.animateTo(
                            targetValue = 0.5f,
                            animationSpec = repeatable(
                                iterations = 3,
                                animation = tween(durationMillis = 300),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        highlightAlpha.animateTo(0f)
                        highlightedSongId = null
                    }
                }
            },
            onNavigateToEditTags = onNavigateToEditTags,
            onClearHistory = { songId ->
                viewModel.clearPlaybackHistory(songId)
            },
            onDismiss = { showInfoDialogSong = null }
        )
    }

    if (showPreviewDialogSong != null) {
        val song = showPreviewDialogSong!!
        SongPreviewDialog(
            song = song,
            viewModel = viewModel,
            onDismiss = { showPreviewDialogSong = null }
        )
    }

    if (showDeleteConfirmDialogSong != null) {
        val song = showDeleteConfirmDialogSong!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirmDialogSong = null },
            title = { Text("Delete Permanently") },
            text = { Text("Are you sure you want to permanently delete \"${song.title}\" from your device? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSongPermanently(context, song)
                        showDeleteConfirmDialogSong = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteConfirmDialogSong = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    showArtwork: Boolean = true,
    isCurrentSong: Boolean = false,
    isPlaying: Boolean = false,
    onOptionsClick: () -> Unit,
    isHighlighted: Boolean = false,
    highlightAlpha: Float = 0f
) {
    val containerColor = if (isHighlighted && highlightAlpha > 0f) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = highlightAlpha)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp)
            ) {
                SongArtwork(
                    albumId = song.albumId,
                    lastModified = song.dateModified,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    showArtwork = showArtwork
                )
                if (isCurrentSong) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.small)
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        PlayingIndicator(
                            isPlaying = isPlaying,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formatDuration(song.duration),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onOptionsClick) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
            }
        }
    }
}

@Composable
fun SongGridItem(
    song: Song,
    onClick: () -> Unit,
    showArtwork: Boolean = true,
    isCurrentSong: Boolean = false,
    isPlaying: Boolean = false,
    onOptionsClick: () -> Unit,
    isHighlighted: Boolean = false,
    highlightAlpha: Float = 0f
) {
    val containerColor = if (isHighlighted && highlightAlpha > 0f) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = highlightAlpha)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                SongArtwork(
                    albumId = song.albumId,
                    lastModified = song.dateModified,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    showArtwork = showArtwork
                )
                if (isCurrentSong) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.medium)
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        PlayingIndicator(
                            isPlaying = isPlaying,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onOptionsClick) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
                }
            }
        }
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
fun SongPreviewDialog(
    song: Song,
    viewModel: SongsViewModel,
    onDismiss: () -> Unit
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPositionLong by viewModel.playbackPosition.collectAsState()
    val durationLong by viewModel.playbackDuration.collectAsState()

    val currentPosition = currentPositionLong.toFloat()
    val duration = durationLong.toFloat()

    LaunchedEffect(song) {
        viewModel.playSong(song)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.pause()
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Preview Audio",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                SongArtwork(
                    albumId = song.albumId,
                    lastModified = song.dateModified,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = currentPosition,
                    onValueChange = {
                        viewModel.seekTo(it.toLong())
                    },
                    valueRange = 0f..duration.coerceAtLeast(1f),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentPosition.toLong()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDuration(duration.toLong()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                viewModel.pause()
                            } else {
                                viewModel.play()
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
