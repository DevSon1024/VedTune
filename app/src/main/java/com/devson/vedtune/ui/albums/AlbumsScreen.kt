package com.devson.vedtune.ui.albums

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.items
import com.devson.vedtune.ui.songs.SortOrder
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.Album
import com.devson.vedtune.ui.components.SongArtwork
import com.devson.vedtune.ui.components.VedTuneTopAppBar
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState

@Composable
fun AlbumsScreen(
    viewModel: AlbumsViewModel,
    onAlbumClick: (Long) -> Unit,
    viewPreferences: com.devson.vedtune.domain.model.ViewPreferences,
    onLayoutToggleClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val albums by viewModel.albums.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    val isGridView = viewPreferences.isGridView
    val showArtwork = viewPreferences.showAlbumArt
    val sortBy by viewModel.sortBy.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    var showSortMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()

    val currentSortLabel = when (sortBy) {
        AlbumSortBy.TITLE -> "Title"
        AlbumSortBy.ARTIST -> "Artist"
        AlbumSortBy.SONG_COUNT -> "Song Count"
    }
    val orderIcon = if (sortOrder == SortOrder.ASCENDING) "↑" else "↓"

    Column(
        modifier = modifier.fillMaxSize()
    ) {
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
                AlbumSortBy.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = when (option) {
                                    AlbumSortBy.TITLE -> "Title"
                                    AlbumSortBy.ARTIST -> "Artist"
                                    AlbumSortBy.SONG_COUNT -> "Song Count"
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

        if (albums.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "No albums found" else "No matching albums",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            if (isGridView) {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(viewPreferences.gridSpanCount),
                        state = lazyGridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 8.dp,
                            end = 8.dp,
                            top = 8.dp,
                            bottom = contentPadding.calculateBottomPadding() + 88.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = albums,
                            key = { it.id },
                            contentType = { "album_grid_item" }
                        ) { album ->
                            AlbumGridItem(
                                album = album,
                                onClick = { onAlbumClick(album.id) },
                                showArtwork = showArtwork,
                                showArtist = viewPreferences.showArtist,
                                gridCount = viewPreferences.gridSpanCount
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 0.dp,
                            end = 0.dp,
                            top = 8.dp,
                            bottom = contentPadding.calculateBottomPadding() + 88.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(
                            items = albums,
                            key = { it.id },
                            contentType = { "album_list_item" }
                        ) { album ->
                            com.devson.vedtune.ui.components.VedTuneListItem(
                                primaryText = album.title,
                                secondaryText = buildString {
                                    val artistPart = if (viewPreferences.showArtist) album.artist else ""
                                    val songCountPart = "${album.songCount} ${if (album.songCount == 1) "song" else "songs"}"
                                    if (artistPart.isNotEmpty()) {
                                        append(artistPart)
                                        append(" • ")
                                    }
                                    append(songCountPart)
                                },
                                onClick = { onAlbumClick(album.id) },
                                leadingContent = if (showArtwork) {
                                    {
                                        SongArtwork(
                                            albumId = album.id,
                                            fallbackIcon = Icons.Default.Album,
                                            showFallbackAnimation = false,
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                                            showArtwork = showArtwork
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumGridItem(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showArtwork: Boolean = true,
    showArtist: Boolean = true,
    gridCount: Int = 2
) {
    val artistPart = if (showArtist) album.artist else ""
    val songCountPart = "${album.songCount} ${if (album.songCount == 1) "song" else "songs"}"
    val secondaryText = buildString {
        if (artistPart.isNotEmpty()) {
            append(artistPart)
            append(" • ")
        }
        append(songCountPart)
    }
    com.devson.vedtune.ui.components.VedTuneGridCard(
        primaryText = album.title,
        secondaryText = secondaryText,
        onClick = onClick,
        modifier = modifier,
        gridCount = gridCount,
        showArtwork = showArtwork
    ) {
        SongArtwork(
            albumId = album.id,
            fallbackIcon = Icons.Default.Album,
            showFallbackAnimation = false,
            modifier = Modifier.fillMaxSize(),
            showArtwork = showArtwork
        )
    }
}
