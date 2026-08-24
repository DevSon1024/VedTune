package com.devson.vedtune.ui.playlists

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import com.devson.vedtune.ui.songs.SortOrder
import androidx.compose.ui.unit.sp
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
import com.devson.vedtune.ui.components.FastScroller
import com.devson.vedtune.ui.components.buildAlphabeticalSectionIndices
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState

@Composable
fun PlaylistsScreen(
    viewModel: PlaylistsViewModel,
    onPlaylistClick: (Long) -> Unit,
    viewPreferences: com.devson.vedtune.domain.model.ViewPreferences,
    onLayoutToggleClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val playlists by viewModel.playlists.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val totalItemCount by viewModel.totalItemCount.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()

    val sortBy by viewModel.sortBy.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    val playlistSectionIndices = remember(playlists, sortBy, sortOrder) {
        when (sortBy) {
            PlaylistSortBy.NAME -> playlists.buildAlphabeticalSectionIndices { it.name }
            PlaylistSortBy.SONG_COUNT -> {
                val map = linkedMapOf<String, Int>()
                playlists.forEachIndexed { index, playlist ->
                    val label = "${playlist.songCount} songs"
                    if (!map.containsKey(label)) map[label] = index
                }
                map
            }
            PlaylistSortBy.DATE_CREATED -> {
                val map = linkedMapOf<String, Int>()
                playlists.forEachIndexed { index, playlist ->
                    val label = if (playlist.createdAt > 0) {
                        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = playlist.createdAt }
                        calendar.get(java.util.Calendar.YEAR).toString()
                    } else "Unknown"
                    if (!map.containsKey(label)) map[label] = index
                }
                map
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            val isGridView = viewPreferences.isGridView
            var showSortMenu by remember { mutableStateOf(false) }

            val currentSortLabel = when (sortBy) {
                PlaylistSortBy.NAME -> "Name"
                PlaylistSortBy.SONG_COUNT -> "Song Count"
                PlaylistSortBy.DATE_CREATED -> "Date Created"
            }
            val orderIcon = if (sortOrder == com.devson.vedtune.ui.songs.SortOrder.ASCENDING) "↑" else "↓"

            Box(modifier = Modifier.fillMaxWidth()) {
                com.devson.vedtune.ui.components.LibraryUtilityRow(
                    currentSortLabel = currentSortLabel,
                    sortOrderIcon = orderIcon,
                    onSortClick = { showSortMenu = true },
                    isGridView = isGridView,
                    onLayoutToggleClick = onLayoutToggleClick,
                    onShuffleClick = { viewModel.playShuffleAll() }
                )

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    PlaylistSortBy.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = when (option) {
                                        PlaylistSortBy.NAME -> "Name"
                                        PlaylistSortBy.SONG_COUNT -> "Song Count"
                                        PlaylistSortBy.DATE_CREATED -> "Date Created"
                                    }
                                )
                            },
                            onClick = {
                                if (sortBy == option) {
                                    viewModel.toggleSortOrder()
                                } else {
                                    viewModel.setSortBy(option)
                                }
                                showSortMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "No playlists. Tap + to create one." else "No matching playlists",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                if (isGridView) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(viewPreferences.gridSpanCount),
                            state = lazyGridState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = contentPadding.calculateBottomPadding() + 88.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(
                                items = playlists,
                                key = { it.id },
                                contentType = { "playlist_grid_item" }
                            ) { playlist ->
                                PlaylistGridItem(
                                    playlist = playlist,
                                    onClick = { onPlaylistClick(playlist.id) },
                                    onDeleteClick = { viewModel.deletePlaylist(playlist.id) },
                                    showArtwork = viewPreferences.showAlbumArt,
                                    gridCount = viewPreferences.gridSpanCount
                                )
                            }
                        }

                        FastScroller(
                            gridState = lazyGridState,
                            sectionIndices = playlistSectionIndices,
                            contentPadding = PaddingValues(
                                top = 8.dp,
                                bottom = contentPadding.calculateBottomPadding() + 88.dp
                            )
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = contentPadding.calculateBottomPadding() + 88.dp
                            )
                        ) {
                            items(
                                items = playlists,
                                key = { it.id },
                                contentType = { "playlist_list_item" }
                            ) { playlist ->
                                PlaylistItemRow(
                                    playlist = playlist,
                                    onClick = { onPlaylistClick(playlist.id) },
                                    onDeleteClick = { viewModel.deletePlaylist(playlist.id) },
                                    showArtwork = viewPreferences.showAlbumArt
                                )
                            }
                        }

                        FastScroller(
                            listState = lazyListState,
                            sectionIndices = playlistSectionIndices,
                            contentPadding = PaddingValues(
                                top = 8.dp,
                                bottom = contentPadding.calculateBottomPadding() + 88.dp
                            )
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp)
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
    showArtwork: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    com.devson.vedtune.ui.components.VedTuneListItem(
        primaryText = playlist.name,
        secondaryText = if (playlist.songCount == 1) "1 song" else "${playlist.songCount} songs",
        onClick = onClick,
        modifier = modifier,
        leadingContent = if (showArtwork) {
            {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Playlist Placeholder",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        } else null,
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDeleteClick()
                            showMenu = false
                        }
                    )
                }
            }
        }
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
        title = { Text(text = "New Playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter a name for this playlist:",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    placeholder = { Text(text = "Playlist Name") },
                    singleLine = true,
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

@Composable
fun PlaylistGridItem(
    playlist: Playlist,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    showArtwork: Boolean = true,
    gridCount: Int = 2,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    com.devson.vedtune.ui.components.VedTuneGridCard(
        primaryText = playlist.name,
        secondaryText = "${playlist.songCount} ${if (playlist.songCount == 1) "song" else "songs"}",
        onClick = onClick,
        modifier = modifier,
        gridCount = gridCount,
        showArtwork = showArtwork
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(48.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDeleteClick()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}
