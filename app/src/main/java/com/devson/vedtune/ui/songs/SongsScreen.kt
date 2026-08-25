package com.devson.vedtune.ui.songs

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.devson.vedtune.core.formatDuration
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.components.AddToPlaylistDialog
import com.devson.vedtune.ui.components.ArtworkThumbnailSize
import com.devson.vedtune.ui.components.PlayingIndicator
import com.devson.vedtune.ui.components.SongArtwork
import com.devson.vedtune.ui.components.VedTuneConfirmDialog
import com.devson.vedtune.ui.components.VedTuneEmptyState
import com.devson.vedtune.ui.components.VedTuneGridCard
import com.devson.vedtune.ui.components.VedTuneLibraryView
import com.devson.vedtune.ui.components.VedTuneSongRow
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneMotion
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.spacing
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    viewModel: SongsViewModel,
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToEditTags: (Long) -> Unit,
    onLayoutToggleClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    navigateToLocationEvent: SharedFlow<Long>? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentSongId by viewModel.currentSongId.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    var highlightedSongId by remember { mutableStateOf<Long?>(null) }
    val highlightAlpha = remember { Animatable(0f) }
    val highlightColor = MaterialTheme.colorScheme.primaryContainer

    // Dialog and bottom sheet states
    var songForPlaylist by remember { mutableStateOf<Song?>(null) }
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var showInfoDialogSong by remember { mutableStateOf<Song?>(null) }
    var showPreviewDialogSong by remember { mutableStateOf<Song?>(null) }
    var showDeleteConfirmDialogSong by remember { mutableStateOf<Song?>(null) }

    val context = LocalContext.current

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

    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onWritePermissionGranted(context)
            viewModel.onDeletePermissionGranted()
        }
    }

    LaunchedEffect(Unit) {
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
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.weight(1f)
        ) {
            val contentState = when {
                !hasPermission -> "PERMISSION_DENIED"
                uiState.isLoading -> "LOADING"
                uiState.songs.isEmpty() -> "EMPTY"
                else -> "CONTENT"
            }

            Crossfade(
                targetState = contentState,
                animationSpec = VedTuneMotion.standardTween(VedTuneMotion.DurationMedium),
                label = "SongsContentStateTransition",
                modifier = Modifier.fillMaxSize()
            ) { state ->
                when (state) {
                    "PERMISSION_DENIED" -> {
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
                    "LOADING" -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    "EMPTY" -> {
                        VedTuneEmptyState(
                            icon = Icons.Default.MusicNote,
                            title = "No Music Files Found",
                            description = "Pull down to scan your storage or add audio files to your device.",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    "CONTENT" -> {
                        VedTuneLibraryView(
                            items = uiState.songs,
                            isGridView = uiState.isGridView,
                            gridSpanCount = uiState.viewPreferences.gridSpanCount,
                            key = { it.id },
                            contentType = { "song_item" },
                            lazyListState = lazyListState,
                            lazyGridState = lazyGridState,
                            contentPadding = PaddingValues(
                                start = if (uiState.isGridView) MaterialTheme.spacing.s else 0.dp,
                                end = if (uiState.isGridView) MaterialTheme.spacing.s else 0.dp,
                                top = MaterialTheme.spacing.s,
                                bottom = contentPadding.calculateBottomPadding() + 88.dp
                            ),
                            listItemContent = { song ->
                                val isCurrent = song.id == currentSongId
                                VedTuneSongRow(
                                    song = song,
                                    isCurrentSong = isCurrent,
                                    isPlaying = isPlaying && isCurrent,
                                    showArtwork = uiState.viewPreferences.showAlbumArt,
                                    onClick = { viewModel.playSong(song) },
                                    onOptionsClick = { selectedSongForOptions = song },
                                    modifier = Modifier.drawBehind {
                                        if (highlightedSongId == song.id) {
                                            drawRect(
                                                color = highlightColor,
                                                alpha = highlightAlpha.value
                                            )
                                        }
                                    }
                                )
                            },
                            gridItemContent = { song ->
                                val isCurrentSong = song.id == currentSongId
                                val subtitle = if (song.album.isNotBlank() && song.album != "Unknown Album") {
                                    "${song.artist} • ${song.album}"
                                } else {
                                    song.artist
                                }
                                VedTuneGridCard(
                                    primaryText = song.title,
                                    secondaryText = subtitle,
                                    onClick = { viewModel.playSong(song) },
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    gridCount = uiState.viewPreferences.gridSpanCount,
                                    showArtwork = uiState.viewPreferences.showAlbumArt,
                                    trailingContent = {
                                        IconButton(onClick = { selectedSongForOptions = song }) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Options"
                                            )
                                        }
                                    },
                                    modifier = Modifier.drawBehind {
                                        if (highlightedSongId == song.id) {
                                            drawRect(
                                                color = highlightColor,
                                                alpha = highlightAlpha.value
                                            )
                                        }
                                    }
                                ) {
                                    if (uiState.viewPreferences.showAlbumArt) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            SongArtwork(
                                                albumId = song.albumId,
                                                lastModified = song.dateModified,
                                                fallbackIcon = Icons.Default.MusicNote,
                                                showFallbackAnimation = false,
                                                thumbnailSize = ArtworkThumbnailSize.MEDIUM,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(VedTuneShapeTokens.Medium)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                                showArtwork = uiState.viewPreferences.showAlbumArt
                                            )
                                            if (isCurrentSong) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(VedTuneShapeTokens.Medium)
                                                        .background(Color.Black.copy(alpha = 0.45f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    PlayingIndicator(
                                                        isPlaying = isPlaying,
                                                        modifier = Modifier.size(VedTuneIconSizes.ExtraLarge)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

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

    selectedSongForOptions?.let { song ->
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
                        fallbackIcon = Icons.Default.MusicNote,
                        showFallbackAnimation = false,
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

    showInfoDialogSong?.let { song ->
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

    showPreviewDialogSong?.let { song ->
        SongPreviewDialog(
            song = song,
            viewModel = viewModel,
            onDismiss = { showPreviewDialogSong = null }
        )
    }

    showDeleteConfirmDialogSong?.let { song ->
        VedTuneConfirmDialog(
            title = "Delete Permanently",
            message = "Are you sure you want to permanently delete \"${song.title}\" from your device? This action cannot be undone.",
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteSongPermanently(context, song)
                showDeleteConfirmDialogSong = null
            },
            onDismiss = { showDeleteConfirmDialogSong = null }
        )
    }
}

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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = VedTuneShapeTokens.Dialog,
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
                        .clip(VedTuneShapeTokens.Card)
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

                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}
