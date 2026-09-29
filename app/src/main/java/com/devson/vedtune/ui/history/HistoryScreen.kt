package com.devson.vedtune.ui.history

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.MainViewModel
import com.devson.vedtune.ui.components.AddToPlaylistDialog
import com.devson.vedtune.ui.components.MiniPlayer
import com.devson.vedtune.ui.components.VedTuneConfirmDialog
import com.devson.vedtune.ui.components.VedTuneEmptyState
import com.devson.vedtune.ui.components.VedTuneSongOptionsBottomSheet
import com.devson.vedtune.ui.components.VedTuneSongRow
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    mainViewModel: MainViewModel,
    onBackClick: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.historySongs.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val showArtwork by viewModel.showAlbumArt.collectAsStateWithLifecycle()

    val currentSongId by viewModel.currentSongId.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()

    val currentSong by mainViewModel.currentSong.collectAsStateWithLifecycle()
    val mainIsPlaying by mainViewModel.isPlaying.collectAsStateWithLifecycle()
    val showMiniPlayerProgress by mainViewModel.showMiniPlayerProgress.collectAsStateWithLifecycle()
    val isGestureMiniPlayerEnabled by mainViewModel.isGestureMiniPlayerEnabled.collectAsStateWithLifecycle()

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var songForPlaylistAdd by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    val progressProvider = remember(mainViewModel) {
        {
            val dur = mainViewModel.playbackDuration.value
            val pos = mainViewModel.playbackPosition.value
            if (dur > 0L) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Recently Played",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (songs.isNotEmpty()) {
                            Text(
                                text = "${songs.size} ${if (songs.size == 1) "track" else "tracks"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (songs.isNotEmpty()) {
                        IconButton(onClick = { showClearHistoryDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear History",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
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
    ) { innerPadding ->
        if (songs.isEmpty()) {
            VedTuneEmptyState(
                title = "No Playback History",
                description = "Songs you listen to will appear here so you can easily jump back in.",
                icon = Icons.Default.History,
                actionText = null,
                onActionClick = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = MaterialTheme.spacing.xxl)
            ) {
                // Header with Action Buttons
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MaterialTheme.spacing.l, vertical = MaterialTheme.spacing.m)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    viewModel.playAll()
                                    onNavigateToPlayer()
                                },
                                shape = VedTuneShapeTokens.Pill,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(VedTuneIconSizes.Small)
                                )
                                Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                                Text(
                                    text = "Play All",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            FilledTonalButton(
                                onClick = {
                                    viewModel.shuffleAll()
                                    onNavigateToPlayer()
                                },
                                shape = VedTuneShapeTokens.Pill,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = null,
                                    modifier = Modifier.size(VedTuneIconSizes.Small)
                                )
                                Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                                Text(
                                    text = "Shuffle",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.m))

                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }

                // Song List
                itemsIndexed(
                    items = songs,
                    key = { _, song -> song.id }
                ) { index, song ->
                    val isCurrent = song.id == currentSongId
                    VedTuneSongRow(
                        song = song,
                        isCurrentSong = isCurrent,
                        isPlaying = isPlaying && isCurrent,
                        showArtwork = showArtwork,
                        showDuration = true,
                        onClick = {
                            viewModel.playSong(song)
                            onNavigateToPlayer()
                        },
                        onOptionsClick = { selectedSongForOptions = song },
                        containerColor = if (isCurrent) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        } else {
                            Color.Transparent
                        }
                    )
                    if (index < songs.lastIndex) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.l)
                        )
                    }
                }
            }
        }
    }

    // Clear History Dialog
    if (showClearHistoryDialog) {
        VedTuneConfirmDialog(
            title = "Clear Playback History?",
            message = "This will clear all tracks from your playback history. Your songs will remain intact in your library.",
            confirmText = "Clear",
            dismissText = "Cancel",
            isDestructive = true,
            icon = Icons.Default.DeleteOutline,
            onConfirm = {
                viewModel.clearHistory()
                showClearHistoryDialog = false
            },
            onDismiss = { showClearHistoryDialog = false }
        )
    }

    // Song Options Bottom Sheet
    selectedSongForOptions?.let { song ->
        VedTuneSongOptionsBottomSheet(
            song = song,
            onDismiss = { selectedSongForOptions = null },
            onToggleFavorite = {
                viewModel.toggleFavorite(song)
                selectedSongForOptions = null
            },
            onPlayNext = {
                viewModel.playNext(song)
                selectedSongForOptions = null
            },
            onAddToPlaylist = {
                songForPlaylistAdd = song
                selectedSongForOptions = null
                showAddToPlaylistDialog = true
            },
            onRemoveFromHistory = {
                viewModel.removeSongFromHistory(song.id)
                selectedSongForOptions = null
            },
            onGoToAlbum = {
                selectedSongForOptions = null
                onNavigateToAlbum(song.albumId)
            },
            onGoToArtist = {
                selectedSongForOptions = null
                onNavigateToArtist(song.artist)
            }
        )
    }

    // Add To Playlist Dialog
    if (showAddToPlaylistDialog && songForPlaylistAdd != null) {
        AddToPlaylistDialog(
            playlists = allPlaylists,
            onDismiss = {
                showAddToPlaylistDialog = false
                songForPlaylistAdd = null
            },
            onPlaylistSelected = { playlistId ->
                songForPlaylistAdd?.let { s ->
                    viewModel.addSongToPlaylist(playlistId, s.id)
                }
                showAddToPlaylistDialog = false
                songForPlaylistAdd = null
            },
            onCreateNewPlaylist = { name ->
                songForPlaylistAdd?.let { s ->
                    viewModel.createPlaylistAndAddSong(name, s.id)
                }
                showAddToPlaylistDialog = false
                songForPlaylistAdd = null
            }
        )
    }
}
