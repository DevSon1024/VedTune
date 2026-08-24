package com.devson.vedtune.ui.player

import android.app.Activity
import android.content.ContentUris
import android.content.ContextWrapper
import android.content.Intent
import android.provider.MediaStore
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.devson.vedtune.ui.components.AddToPlaylistDialog
import com.devson.vedtune.ui.components.SongArtwork
import com.devson.vedtune.ui.player.components.ActionControlsStrip
import com.devson.vedtune.ui.player.components.ArtworkCard
import com.devson.vedtune.ui.player.components.PlayerArtworkPager
import com.devson.vedtune.ui.player.components.ClickableMetadata
import com.devson.vedtune.ui.player.components.LyricsPanel
import com.devson.vedtune.ui.player.components.OptionsSheetContent
import com.devson.vedtune.ui.player.components.PlaybackControls
import com.devson.vedtune.ui.player.components.PlayerHeader
import com.devson.vedtune.ui.player.components.PlayerSeekBar
import com.devson.vedtune.ui.player.components.PlayerSettingsDialog
import com.devson.vedtune.ui.player.components.SleepTimerDialog
import com.devson.vedtune.ui.player.components.ViewAlbumArtOverlay
import com.devson.vedtune.ui.songs.SongInfoBottomSheet

// Unified sheet state
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
    val playlistQueue by viewModel.playlistQueue.collectAsStateWithLifecycle()
    val currentQueueIndex by viewModel.currentQueueIndex.collectAsStateWithLifecycle()
    val showArtworkState by viewModel.showAlbumArt.collectAsStateWithLifecycle()
    val showRemainingTimeState by viewModel.showRemainingTime.collectAsStateWithLifecycle()
    val seekbarStyle by viewModel.seekbarStyle.collectAsStateWithLifecycle()

    val showLyricsButton by viewModel.showLyricsButton.collectAsStateWithLifecycle()
    val showSleepTimerButton by viewModel.showSleepTimerButton.collectAsStateWithLifecycle()
    val showShuffleRepeatButtons by viewModel.showShuffleRepeatButtons.collectAsStateWithLifecycle()

    val showForwardBackward by viewModel.showForwardBackward.collectAsStateWithLifecycle()
    val seekInterval by viewModel.seekInterval.collectAsStateWithLifecycle()

    val enableSwipeToSkip by viewModel.enableSwipeToSkip.collectAsStateWithLifecycle()
    val keepScreenOnWithLyrics by viewModel.keepScreenOnWithLyrics.collectAsStateWithLifecycle()
    val albumArtClickAction by viewModel.albumArtClickAction.collectAsStateWithLifecycle()
    val playerBackgroundBlurRadius by viewModel.playerBackgroundBlurRadius.collectAsStateWithLifecycle()

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPlayerSettingsDialog by remember { mutableStateOf(false) }
    var showViewAlbumArtOverlay by remember { mutableStateOf(false) }
    var sheetState: PlayerSheetState by remember { mutableStateOf(PlayerSheetState.Hidden) }
    var showLyrics by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Artwork scale animation - driven smoothly by isPlaying state
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
        if (result.resultCode == Activity.RESULT_OK) {
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
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is PlayerUiEvent.LaunchIntentSender -> {
                    val request = IntentSenderRequest.Builder(event.intentSender).build()
                    intentSenderLauncher.launch(request)
                }
            }
        }
    }

    // Keep Screen On logic for Lyrics
    if (showLyrics && keepScreenOnWithLyrics) {
        DisposableEffect(context) {
            var ctx = context
            while (ctx is ContextWrapper) {
                if (ctx is Activity) break
                ctx = ctx.baseContext
            }
            val activity = ctx as? Activity
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Dynamic Blurred Background with Crossfade transition
        Crossfade(targetState = song?.albumId, label = "BackgroundTransition") { albumId ->
            if (albumId != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SongArtwork(
                        albumId = albumId,
                        modifier = Modifier.fillMaxSize(),
                        showArtwork = showArtworkState,
                        blurRadius = playerBackgroundBlurRadius.toInt(),
                        isPlaying = isPlaying,
                        showFallbackAnimation = false
                    )
                    val background = MaterialTheme.colorScheme.background
                    val isDark = (background.red + background.green + background.blue) / 3f < 0.5f
                    val overlayBrush = remember(isDark) {
                        Brush.verticalGradient(
                            colors = if (isDark) {
                                listOf(
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Black.copy(alpha = 0.95f)
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

        // 2. Main Contents
        val currentActiveSong = song
        if (currentActiveSong == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No song selected",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        } else {
            val progressProvider = remember(duration) {
                {
                    if (duration > 0) {
                        (viewModel.playbackPosition.value.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                PlayerHeader(
                    sleepTimerRemaining = sleepTimerRemaining,
                    onBackClick = onBackClick,
                    onQueueClick = { showQueueSheet = true }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Smooth Crossfade transition for Artwork/Lyrics and Clickable Metadata
                Crossfade(
                    targetState = currentActiveSong,
                    modifier = Modifier.weight(1f),
                    label = "CenterContentTransition"
                ) { displayedSong ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Artwork / Lyrics Container
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (showLyrics) {
                                LyricsPanel(
                                    viewModel = viewModel,
                                    activeSong = displayedSong,
                                    onToggleLyrics = { showLyrics = false },
                                    onEditLyricsClick = { onNavigateToLyricsEditor(displayedSong.id) }
                                )
                            } else {
                                if (playlistQueue.isNotEmpty()) {
                                    PlayerArtworkPager(
                                        queue = playlistQueue,
                                        currentQueueIndex = currentQueueIndex,
                                        isPlaying = isPlaying,
                                        artworkScale = artworkScale,
                                        showArtwork = showArtworkState,
                                        albumArtClickAction = albumArtClickAction,
                                        playbackProgress = progressProvider,
                                        onSkipToQueueItem = { newIndex -> viewModel.skipToQueueItem(newIndex) },
                                        onToggleLyrics = { showLyrics = true },
                                        onPlayPause = { viewModel.togglePlayPause() },
                                        onViewAlbumArt = { showViewAlbumArtOverlay = true }
                                    )
                                } else {
                                    ArtworkCard(
                                        song = displayedSong,
                                        showArtwork = showArtworkState,
                                        isPlaying = isPlaying,
                                        artworkScale = artworkScale,
                                        enableSwipeToSkip = enableSwipeToSkip,
                                        albumArtClickAction = albumArtClickAction,
                                        playbackProgress = progressProvider,
                                        onToggleLyrics = { showLyrics = true },
                                        onPlayPause = { viewModel.togglePlayPause() },
                                        onViewAlbumArt = { showViewAlbumArtOverlay = true },
                                        onSwipeNext = { viewModel.skipToNext() },
                                        onSwipePrevious = { viewModel.skipToPrevious() }
                                    )
                                }
                            }
                        }

                        // Clickable Metadata
                        if (!showLyrics) {
                            Spacer(modifier = Modifier.height(16.dp))
                            ClickableMetadata(
                                song = displayedSong,
                                onSongClick = { sheetState = PlayerSheetState.SongInfo },
                                onArtistClick = { onNavigateToArtist(displayedSong.artist) },
                                onAlbumClick = { onNavigateToAlbum(displayedSong.albumId) }
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
                    showLyricsButton = showLyricsButton,
                    showSleepTimerButton = showSleepTimerButton,
                    showShuffleRepeatButtons = showShuffleRepeatButtons,
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
                    },
                    onToggleLyrics = { showLyrics = !showLyrics }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Seek Bar
                PlayerSeekBar(
                    positionState = viewModel.playbackPosition,
                    duration = duration,
                    showRemainingTime = showRemainingTimeState,
                    style = seekbarStyle,
                    isPlaying = isPlaying,
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
    if (showDeleteConfirmDialog) {
        song?.let { activeSong ->
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
    }

    // Options Bottom Sheet
    if (sheetState == PlayerSheetState.Options) {
        song?.let { activeSong ->
            ModalBottomSheet(
                onDismissRequest = { sheetState = PlayerSheetState.Hidden }
            ) {
                OptionsSheetContent(
                    song = activeSong,
                    showArtwork = showArtworkState,
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
    }

    // Add to Playlist Dialog
    if (sheetState == PlayerSheetState.AddToPlaylist) {
        song?.let { activeSong ->
            val playlists by viewModel.playlists.collectAsStateWithLifecycle()
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
    }

    // Song Info Bottom Sheet
    if (sheetState == PlayerSheetState.SongInfo) {
        song?.let { activeSong ->
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

    // Full Screen Album Art Overlay
    if (showViewAlbumArtOverlay) {
        song?.let { activeSong ->
            ViewAlbumArtOverlay(
                albumId = activeSong.albumId,
                showArtwork = showArtworkState,
                onDismiss = { showViewAlbumArtOverlay = false },
                onSaveToGallery = {
                    viewModel.saveAlbumArtToGallery(activeSong.albumId)
                }
            )
        }
    }

    // Queue Bottom Sheet
    if (showQueueSheet) {
        QueueBottomSheet(
            viewModel = viewModel,
            onDismiss = { showQueueSheet = false }
        )
    }
}
