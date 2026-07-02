package com.devson.vedtune.ui.player

import android.content.ContentUris
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.TextFormat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.domain.model.SeekBarStyle
import com.devson.vedtune.ui.components.AddToPlaylistDialog
import com.devson.vedtune.ui.components.SongArtwork
import com.devson.vedtune.ui.components.VedTuneSeekBar
import com.devson.vedtune.ui.songs.SongInfoBottomSheet
import java.util.Locale

// Unified sheet state — play queue removed
private sealed class PlayerSheetState {
    object Hidden : PlayerSheetState()
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
    onNavigateToLyricsEditor: (Long) -> Unit,
    onNavigateToLocation: (Long) -> Unit,
    modifier: Modifier = Modifier,
    showArtwork: Boolean = true,
    showRemainingTime: Boolean = false
) {
    val song by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val duration by viewModel.playbackDuration.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsStateWithLifecycle()
    val sleepTimerRemaining by viewModel.sleepTimerRemaining.collectAsStateWithLifecycle()
    val isFav by viewModel.isFavorite.collectAsStateWithLifecycle()
    val showArtwork by viewModel.showAlbumArt.collectAsStateWithLifecycle()
    val showRemainingTime by viewModel.showRemainingTime.collectAsStateWithLifecycle()
    val seekbarStyle by viewModel.seekbarStyle.collectAsStateWithLifecycle()

    val showForwardBackward by viewModel.showForwardBackward.collectAsStateWithLifecycle()
    val seekInterval by viewModel.seekInterval.collectAsStateWithLifecycle()

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPlayerSettingsDialog by remember { mutableStateOf(false) }
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

    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onDeletePermissionGranted()
        }
    }

    LaunchedEffect(song?.id) {
        song?.id?.let { songId ->
            viewModel.loadLyrics(songId)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is PlayerUiEvent.ShowToast -> {
                    android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_SHORT).show()
                }
                is PlayerUiEvent.LaunchIntentSender -> {
                    val request = IntentSenderRequest.Builder(event.intentSender).build()
                    intentSenderLauncher.launch(request)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // 1. Dynamic Blurred Background with smooth Crossfade transition
        Crossfade(targetState = song?.albumId, label = "BackgroundTransition") { albumId ->
            if (albumId != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SongArtwork(
                        albumId = albumId,
                        modifier = Modifier.fillMaxSize(),
                        showArtwork = showArtwork,
                        blurRadius = 25
                    )
                    val isDark = isSystemInDarkTheme()
                    val overlayBrush = remember(isDark) {
                        Brush.verticalGradient(
                            colors = if (isDark) {
                                listOf(
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = 0.5f),
                                    Color.White.copy(alpha = 0.7f),
                                    Color.White.copy(alpha = 0.85f)
                                )
                            }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(overlayBrush)
                    )
                }
            }
        }

        // 2. Main Contents (Non-scrolling viewing area)
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
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (Back button, Title, sleeps in label)
                PlayerHeader(
                    sleepTimerRemaining = sleepTimerRemaining,
                    onBackClick = onBackClick
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Smooth Crossfade transition for Artwork/Lyrics and Clickable Metadata
                Crossfade(
                    targetState = activeSong,
                    modifier = Modifier.weight(1f),
                    label = "CenterContentTransition"
                ) { currentActiveSong ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Artwork / Lyrics Container (Flexible viewport weight)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (showLyrics) {
                                LyricsPanel(
                                    viewModel = viewModel,
                                    activeSong = currentActiveSong,
                                    onToggleLyrics = { showLyrics = false }
                                )
                            } else {
                                ArtworkCard(
                                    song = currentActiveSong,
                                    showArtwork = showArtwork,
                                    artworkScale = artworkScale,
                                    onToggleLyrics = { showLyrics = true }
                                )
                            }
                        }

                        // Clickable Metadata (Hidden when lyrics panel is active)
                        if (!showLyrics) {
                            Spacer(modifier = Modifier.height(16.dp))
                            ClickableMetadata(
                                song = currentActiveSong,
                                onSongClick = { sheetState = PlayerSheetState.SongInfo },
                                onArtistClick = { onNavigateToArtist(currentActiveSong.artist) },
                                onAlbumClick = { onNavigateToAlbum(currentActiveSong.albumId) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Controls Strip
                ActionControlsStrip(
                    isFav = isFav,
                    sleepTimerRemaining = sleepTimerRemaining,
                    shuffleModeEnabled = shuffleModeEnabled,
                    repeatMode = repeatMode,
                    onSleepTimerClick = { showSleepTimerDialog = true },
                    onFavClick = { viewModel.toggleFavorite() },
                    onPlaylistClick = { sheetState = PlayerSheetState.AddToPlaylist },
                    onInfoClick = { sheetState = PlayerSheetState.SongInfo },
                    onOptionsClick = { sheetState = PlayerSheetState.Options },
                    onShuffleClick = { viewModel.setShuffleModeEnabled(!shuffleModeEnabled) },
                    onRepeatClick = {
                        val nextMode = when (repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                        viewModel.setRepeatMode(nextMode)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Seek Bar (Isolates high frequency position updates from parent)
                SeekBar(
                    positionState = viewModel.playbackPosition,
                    duration = duration,
                    showRemainingTime = showRemainingTime,
                    style = seekbarStyle,
                    onSeek = { viewModel.seekTo(it) },
                    onToggleRemainingTime = { viewModel.toggleRemainingTime() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom-most Playback Controls
                PlaybackControls(
                    isPlaying = isPlaying,
                    showForwardBackward = showForwardBackward,
                    onPreviousClick = { viewModel.skipToPrevious() },
                    onBackwardClick = { viewModel.skipBackward() },
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onForwardClick = { viewModel.skipForward() },
                    onNextClick = { viewModel.skipToNext() }
                )
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

    // Player Settings Dialog
    if (showPlayerSettingsDialog) {
        PlayerSettingsDialog(
            showSeekButtons = showForwardBackward,
            seekInterval = seekInterval,
            onToggleSeekButtons = { viewModel.setShowForwardBackward(it) },
            onSeekIntervalChange = { viewModel.setSeekInterval(it) },
            onDismiss = { showPlayerSettingsDialog = false }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && song != null) {
        val activeSong = song!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(text = "Delete Song Permanently") },
            text = { Text(text = "Are you sure you want to delete '${activeSong.title}' permanently from this device? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSongPermanently(context, activeSong)
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text(text = "Delete Permanently", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
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
                onEditTags = {
                    sheetState = PlayerSheetState.Hidden
                    onNavigateToEditTags(activeSong.id)
                },
                onEditLyrics = {
                    sheetState = PlayerSheetState.Hidden
                    onNavigateToLyricsEditor(activeSong.id)
                },
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
                },
                onDeletePermanently = {
                    sheetState = PlayerSheetState.Hidden
                    showDeleteConfirmDialog = true
                },
                onPlayerSettings = {
                    sheetState = PlayerSheetState.Hidden
                    showPlayerSettingsDialog = true
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

    // Song Info Bottom Sheet
    if (sheetState == PlayerSheetState.SongInfo && song != null) {
        val activeSong = song!!
        SongInfoBottomSheet(
            song = activeSong,
            onNavigateToAlbum = onNavigateToAlbum,
            onNavigateToArtist = onNavigateToArtist,
            onNavigateToLocation = onNavigateToLocation,
            onNavigateToEditTags = onNavigateToEditTags,
            onClearHistory = { songId ->
                viewModel.clearPlaybackHistory(songId)
            },
            onDismiss = { sheetState = PlayerSheetState.Hidden }
        )
    }
}

// Stateless sub-composables

@Composable
private fun PlayerHeader(
    sleepTimerRemaining: Long,
    onBackClick: () -> Unit
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
        // Balanced spacing to keep title perfectly centered
        Spacer(modifier = Modifier.width(48.dp))
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
private fun ArtworkCard(
    song: Song,
    showArtwork: Boolean,
    artworkScale: Float,
    onToggleLyrics: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .aspectRatio(1f)
            .scale(artworkScale)
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable { onToggleLyrics() }
    ) {
        SongArtwork(
            albumId = song.albumId,
            modifier = Modifier.fillMaxSize(),
            showArtwork = showArtwork
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClickableMetadata(
    song: Song,
    onSongClick: () -> Unit,
    onArtistClick: () -> Unit,
    onAlbumClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable { onSongClick() }
                .basicMarquee()
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable { onArtistClick() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = "Artist",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.basicMarquee()
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable { onAlbumClick() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Album,
                contentDescription = "Album",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = song.album.ifBlank { "Unknown Album" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.basicMarquee()
            )
        }
    }
}

@Composable
private fun ActionControlsStrip(
    isFav: Boolean,
    sleepTimerRemaining: Long,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    onSleepTimerClick: () -> Unit,
    onFavClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onInfoClick: () -> Unit,
    onOptionsClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onSleepTimerClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = "Sleep Timer",
                    tint = if (sleepTimerRemaining > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onFavClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFav) "Remove from Favorites" else "Add to Favorites",
                    tint = if (isFav) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onPlaylistClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = "Add to Playlist",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = "Song Info",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onOptionsClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More Options",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onShuffleClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (shuffleModeEnabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onRepeatClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne
                    else Icons.Rounded.Repeat,
                    contentDescription = "Repeat",
                    tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SeekBar(
    positionState: StateFlow<Long>,
    duration: Long,
    showRemainingTime: Boolean,
    style: SeekBarStyle,
    onSeek: (Long) -> Unit,
    onToggleRemainingTime: () -> Unit
) {
    val position by positionState.collectAsStateWithLifecycle()
    var isDragging by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(position.toFloat()) }

    LaunchedEffect(position) {
        if (!isDragging) {
            sliderValue = position.toFloat()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        VedTuneSeekBar(
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
            style = style,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(sliderValue.toLong()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.clickable { onToggleRemainingTime() }
            )
            val endLabel = if (showRemainingTime) {
                "-${formatTime((duration - sliderValue.toLong()).coerceAtLeast(0L))}"
            } else {
                formatTime(duration)
            }
            Text(
                text = endLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.clickable { onToggleRemainingTime() }
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    showForwardBackward: Boolean,
    onPreviousClick: () -> Unit,
    onBackwardClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onForwardClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        if (showForwardBackward) {
            IconButton(
                onClick = onBackwardClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FastRewind,
                    contentDescription = "Rewind",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
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
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        if (showForwardBackward) {
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(
                onClick = onForwardClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FastForward,
                    contentDescription = "Forward",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        IconButton(
            onClick = onNextClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun LyricsPanel(
    viewModel: PlayerViewModel,
    activeSong: Song,
    onToggleLyrics: () -> Unit
) {
    val lyricsText by viewModel.currentLyrics.collectAsStateWithLifecycle()
    val fontSizeState by viewModel.lyricsFontSize.collectAsStateWithLifecycle()
    val alignmentState by viewModel.lyricsAlignment.collectAsStateWithLifecycle()
    val positionState = viewModel.playbackPosition
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showFormattingDialog by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }

    val lrcPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importLrcFile(context, uri)
        }
    }

    val fontSize = when (fontSizeState) {
        "Small" -> MaterialTheme.typography.bodyMedium.fontSize
        "Big" -> MaterialTheme.typography.titleLarge.fontSize
        else -> MaterialTheme.typography.titleMedium.fontSize
    }

    val textAlign = when (alignmentState) {
        "Left" -> TextAlign.Start
        "Right" -> TextAlign.End
        else -> TextAlign.Center
    }

    val horizontalAlign = when (alignmentState) {
        "Left" -> Alignment.Start
        "Right" -> Alignment.End
        else -> Alignment.CenterHorizontally
    }

    val currentLyricsText = lyricsText
    val hasTimestamps = remember(currentLyricsText) {
        !currentLyricsText.isNullOrBlank() && currentLyricsText.contains(Regex("\\[\\d+:\\d+"))
    }
    var parsedLines by remember { mutableStateOf<List<LrcLine>>(emptyList()) }

    LaunchedEffect(lyricsText) {
        val text = lyricsText
        if (!text.isNullOrBlank()) {
            parsedLines = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                parseLrc(text)
            }
        } else {
            parsedLines = emptyList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f))
            .clickable { onToggleLyrics() }
    ) {
        // Floating options bar
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.medium
                )
                .clickable(enabled = false) {},
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { showFormattingDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.TextFormat,
                    contentDescription = "Format Lyrics",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { showCopyDialog = true },
                modifier = Modifier.size(32.dp),
                enabled = !lyricsText.isNullOrBlank()
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "Copy Lyrics",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { lrcPickerLauncher.launch("*/*") },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.UploadFile,
                    contentDescription = "Import LRC File",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                lyricsText.isNullOrBlank() -> {
                    Text(
                        text = "No lyrics available\nTap top-right to import .lrc",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center
                    )
                }

                hasTimestamps && parsedLines.isEmpty() -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                !hasTimestamps -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = horizontalAlign,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = lyricsText!!,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = fontSize,
                                textAlign = textAlign
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                else -> {
                    val listState = rememberLazyListState()
                    val position by positionState.collectAsStateWithLifecycle()

                    val activeLineIndex by remember(parsedLines) {
                        derivedStateOf { getActiveLyricsLineIndex(parsedLines, position) }
                    }

                    // Snap immediately on first composition or song change, animate subsequently
                    var hasSnappedToInitial by remember(activeSong.id) { mutableStateOf(false) }

                    LaunchedEffect(activeLineIndex) {
                        if (activeLineIndex >= 0) {
                            if (!hasSnappedToInitial) {
                                listState.scrollToItem(activeLineIndex, scrollOffset = -100)
                                hasSnappedToInitial = true
                            } else {
                                listState.animateScrollToItem(activeLineIndex, scrollOffset = -100)
                            }
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = horizontalAlign,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(100.dp)) }

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
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = fontSize,
                                    textAlign = textAlign
                                ),
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(lineScale)
                                    .graphicsLayer { this.alpha = alpha }
                                    .padding(horizontal = 8.dp)
                            )
                        }

                        item { Spacer(modifier = Modifier.height(100.dp)) }
                    }
                }
            }
        }
    }

    if (showFormattingDialog) {
        LyricsFormattingDialog(
            currentSize = fontSizeState,
            currentAlignment = alignmentState,
            onDismiss = { showFormattingDialog = false },
            onSelectSize = { viewModel.setLyricsFontSize(it) },
            onSelectAlignment = { viewModel.setLyricsAlignment(it) }
        )
    }

    if (showCopyDialog && !lyricsText.isNullOrBlank()) {
        LyricsCopyDialog(
            lyricsText = lyricsText!!,
            parsedLines = parsedLines,
            onDismiss = { showCopyDialog = false },
            onCopy = { textToCopy ->
                clipboardManager.setText(AnnotatedString(textToCopy))
                showCopyDialog = false
            }
        )
    }
}

@Composable
fun LyricsFormattingDialog(
    currentSize: String,
    currentAlignment: String,
    onDismiss: () -> Unit,
    onSelectSize: (String) -> Unit,
    onSelectAlignment: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lyrics Formatting") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text(
                        text = "Font Size",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Small", "Medium", "Big").forEach { size ->
                            val isSelected = size == currentSize
                            Button(
                                onClick = { onSelectSize(size) },
                                modifier = Modifier.weight(1f),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(size)
                            }
                        }
                    }
                }

                Column {
                    Text(
                        text = "Alignment",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Left", "Center", "Right").forEach { align ->
                            val isSelected = align == currentAlignment
                            val label = if (align == "Center") "Middle" else align
                            Button(
                                onClick = { onSelectAlignment(align) },
                                modifier = Modifier.weight(1f),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun LyricsCopyDialog(
    lyricsText: String,
    parsedLines: List<LrcLine>,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Copy Lyrics") },
        text = {
            Text("Select format to copy to clipboard:")
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (parsedLines.isNotEmpty()) {
                            val cleanLyrics = parsedLines.joinToString("\n") { it.text }
                            onCopy(cleanLyrics)
                        } else {
                            onCopy(lyricsText)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Without Timestamp")
                }

                if (parsedLines.isNotEmpty()) {
                    Button(
                        onClick = {
                            val formattedLyrics = parsedLines.joinToString("\n") { line ->
                                val min = line.timestamp / (60 * 1000)
                                val sec = (line.timestamp % (60 * 1000)) / 1000
                                val ms = (line.timestamp % 1000) / 10
                                val timeStr = String.format(Locale.getDefault(), "[%02d:%02d.%02d]", min, sec, ms)
                                "$timeStr ${line.text}"
                            }
                            onCopy(formattedLyrics)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("With Timestamp")
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun PlayerSettingsDialog(
    showSeekButtons: Boolean,
    seekInterval: Int,
    onToggleSeekButtons: (Boolean) -> Unit,
    onSeekIntervalChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Player Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Show Skip Buttons",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = showSeekButtons,
                        onCheckedChange = onToggleSeekButtons
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Skip Interval",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "${seekInterval}s",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Slider(
                        value = seekInterval.toFloat(),
                        onValueChange = { onSeekIntervalChange(it.toInt()) },
                        valueRange = 5f..60f,
                        steps = 55,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun OptionsSheetContent(
    song: Song,
    showArtwork: Boolean,
    onEditTags: () -> Unit,
    onEditLyrics: () -> Unit,
    onShare: () -> Unit,
    onDeletePermanently: () -> Unit,
    onPlayerSettings: () -> Unit
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

        BottomSheetOption(
            icon = Icons.Rounded.Edit,
            title = "Edit Tags",
            onClick = onEditTags
        )
        BottomSheetOption(
            icon = Icons.Rounded.TextFormat,
            title = "Edit Lyrics",
            onClick = onEditLyrics
        )
        BottomSheetOption(
            icon = Icons.Rounded.Share,
            title = "Share Song",
            onClick = onShare
        )
        BottomSheetOption(
            icon = Icons.Rounded.DeleteForever,
            title = "Delete Permanently",
            tint = MaterialTheme.colorScheme.error,
            onClick = onDeletePermanently
        )
        BottomSheetOption(
            icon = Icons.Rounded.Settings,
            title = "Player Settings",
            onClick = onPlayerSettings
        )
    }
}

// Shared helpers

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

// Synced lyrics helpers

data class LrcLine(val timestamp: Long, val text: String)

fun parseLrc(lrcText: String): List<LrcLine> {
    val timeRegex = Regex("\\[(\\d+):(\\d+)(?:\\.(\\d+))?]")
    val parsedLines = mutableListOf<LrcLine>()

    for (line in lrcText.lines()) {
        val cleanLine = line.trim()
        if (cleanLine.isEmpty()) continue

        // Skip metadata tags like [ti:Title]
        if (cleanLine.startsWith("[") && !cleanLine.startsWith("[0") && !cleanLine.startsWith("[1") && !cleanLine.startsWith("[2") && !cleanLine.startsWith("[3") && !cleanLine.startsWith("[4") && !cleanLine.startsWith("[5") && !cleanLine.startsWith("[6") && !cleanLine.startsWith("[7") && !cleanLine.startsWith("[8") && !cleanLine.startsWith("[9")) {
            continue
        }

        val matches = timeRegex.findAll(cleanLine).toList()
        if (matches.isNotEmpty()) {
            val text = cleanLine.replace(timeRegex, "").trim()
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
        } else {
            // Unsynced line gets -1L
            parsedLines.add(LrcLine(-1L, cleanLine))
        }
    }
    return parsedLines
}

fun getActiveLyricsLineIndex(lines: List<LrcLine>, currentPosition: Long): Int {
    if (lines.isEmpty()) return -1
    var activeIndex = -1
    for (i in lines.indices) {
        val timestamp = lines[i].timestamp
        if (timestamp in 0L..currentPosition) {
            activeIndex = i
        }
    }
    return activeIndex
}
