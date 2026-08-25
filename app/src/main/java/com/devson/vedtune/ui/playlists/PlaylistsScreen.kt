package com.devson.vedtune.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.Playlist
import com.devson.vedtune.domain.model.ViewPreferences
import com.devson.vedtune.ui.components.VedTuneEmptyState
import com.devson.vedtune.ui.components.VedTuneIconButton
import com.devson.vedtune.ui.components.VedTunePlaylistCard
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.VedTuneTextStyles
import com.devson.vedtune.ui.theme.rememberVedTuneAdaptiveInfo
import com.devson.vedtune.ui.theme.spacing

@Composable
fun PlaylistsScreen(
    viewModel: PlaylistsViewModel,
    onPlaylistClick: (Long) -> Unit,
    viewPreferences: ViewPreferences,
    onLayoutToggleClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val playlists by viewModel.playlists.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    val isGridView = viewPreferences.isGridView
    val adaptiveInfo = rememberVedTuneAdaptiveInfo()

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()

    val gridSpanCount = when {
        adaptiveInfo.isTablet -> 4
        adaptiveInfo.isLandscape -> 3
        else -> viewPreferences.gridSpanCount
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (playlists.isEmpty()) {
            VedTuneEmptyState(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                title = if (searchQuery.isBlank()) "No Playlists" else "No Matching Playlists",
                description = "Create a custom playlist to organize your favorite music.",
                actionText = "Create Playlist",
                onActionClick = { showCreateDialog = true },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridSpanCount),
                    state = lazyGridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = MaterialTheme.spacing.s,
                        end = MaterialTheme.spacing.s,
                        top = MaterialTheme.spacing.s,
                        bottom = contentPadding.calculateBottomPadding() + 88.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
                ) {
                    items(
                        items = playlists,
                        key = { it.id },
                        contentType = { "playlist_grid_item" }
                    ) { playlist ->
                        VedTunePlaylistCard(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist.id) },
                            showArtwork = viewPreferences.showAlbumArt,
                            gridCount = gridSpanCount
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 0.dp,
                        end = 0.dp,
                        top = MaterialTheme.spacing.s,
                        bottom = contentPadding.calculateBottomPadding() + 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(
                        items = playlists,
                        key = { it.id },
                        contentType = { "playlist_list_item" }
                    ) { playlist ->
                        PlaylistItemRow(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist.id) },
                            onDeleteClick = { viewModel.deletePlaylist(playlist.id) }
                        )
                    }
                }
            }
        }

        // Floating Action Button to create playlist
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = VedTuneShapeTokens.Large,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = MaterialTheme.spacing.l,
                    bottom = contentPadding.calculateBottomPadding() + 16.dp
                )
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Create Playlist")
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.createPlaylist(name)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun PlaylistItemRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = "${playlist.songCount} ${if (playlist.songCount == 1) "track" else "tracks"}",
                style = VedTuneTextStyles.Metadata,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(VedTuneShapeTokens.Small)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(VedTuneIconSizes.Medium)
                )
            }
        },
        trailingContent = {
            Box {
                VedTuneIconButton(
                    icon = Icons.Default.MoreVert,
                    contentDescription = "Playlist Options",
                    onClick = { showMenu = true },
                    iconSize = VedTuneIconSizes.Medium,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete Playlist") },
                        onClick = {
                            onDeleteClick()
                            showMenu = false
                        }
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(VedTuneShapeTokens.Medium)
            .clickable(onClick = onClick)
    )
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var playlistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Playlist",
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
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    placeholder = { Text(text = "My Playlist") },
                    singleLine = true,
                    shape = VedTuneShapeTokens.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(playlistName) },
                enabled = playlistName.isNotBlank()
            ) {
                Text(text = "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}
