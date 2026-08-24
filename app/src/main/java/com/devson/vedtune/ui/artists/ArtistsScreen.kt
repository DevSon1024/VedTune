package com.devson.vedtune.ui.artists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.Artist
import com.devson.vedtune.ui.songs.SortOrder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.devson.vedtune.ui.components.FastScroller
import com.devson.vedtune.ui.components.buildAlphabeticalSectionIndices
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState

@Composable
fun ArtistsScreen(
    viewModel: ArtistsViewModel,
    onArtistClick: (String) -> Unit,
    viewPreferences: com.devson.vedtune.domain.model.ViewPreferences,
    onLayoutToggleClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val artists by viewModel.artists.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    val isGridView = viewPreferences.isGridView
    val sortBy by viewModel.sortBy.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    var showSortMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()

    val artistSectionIndices by viewModel.scrollIndices.collectAsState()

    val currentSortLabel = when (sortBy) {
        ArtistSortBy.NAME -> "Name"
        ArtistSortBy.SONG_COUNT -> "Song Count"
        ArtistSortBy.ALBUM_COUNT -> "Album Count"
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
                ArtistSortBy.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = when (option) {
                                    ArtistSortBy.NAME -> "Name"
                                    ArtistSortBy.SONG_COUNT -> "Song Count"
                                    ArtistSortBy.ALBUM_COUNT -> "Album Count"
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

        if (artists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "No artists found" else "No matching artists",
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
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = artists,
                            key = { it.name },
                            contentType = { "artist_grid_item" }
                        ) { artist ->
                            ArtistGridItem(
                                artist = artist,
                                onClick = { onArtistClick(artist.name) },
                                showArtwork = viewPreferences.showAlbumArt,
                                gridCount = viewPreferences.gridSpanCount
                            )
                        }
                    }

                    FastScroller(
                        gridState = lazyGridState,
                        sectionIndices = artistSectionIndices,
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
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = contentPadding.calculateBottomPadding() + 88.dp
                        )
                    ) {
                        items(
                            items = artists,
                            key = { it.name },
                            contentType = { "artist_list_item" }
                        ) { artist ->
                            ArtistListItem(
                                artist = artist,
                                onClick = { onArtistClick(artist.name) },
                                showArtwork = viewPreferences.showAlbumArt
                            )
                        }
                    }

                    FastScroller(
                        listState = lazyListState,
                        sectionIndices = artistSectionIndices,
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = contentPadding.calculateBottomPadding() + 88.dp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ArtistListItem(
    artist: Artist,
    onClick: () -> Unit,
    showArtwork: Boolean = true,
    modifier: Modifier = Modifier
) {
    val firstLetter = remember(artist.name) {
        if (artist.name.isNotBlank()) artist.name.take(1).uppercase() else "?"
    }
    com.devson.vedtune.ui.components.VedTuneListItem(
        primaryText = artist.name,
        secondaryText = "${if (artist.songCount == 1) "1 song" else "${artist.songCount} songs"} • ${if (artist.albumCount == 1) "1 album" else "${artist.albumCount} albums"}",
        onClick = onClick,
        modifier = modifier,
        leadingContent = if (showArtwork) {
            {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = firstLetter,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        } else null
    )
}

@Composable
fun ArtistGridItem(
    artist: Artist,
    onClick: () -> Unit,
    showArtwork: Boolean = true,
    gridCount: Int = 2,
    modifier: Modifier = Modifier
) {
    val firstLetter = remember(artist.name) {
        if (artist.name.isNotBlank()) artist.name.take(1).uppercase() else "?"
    }
    com.devson.vedtune.ui.components.VedTuneGridCard(
        primaryText = artist.name,
        secondaryText = "${artist.songCount} ${if (artist.songCount == 1) "song" else "songs"} • ${artist.albumCount} ${if (artist.albumCount == 1) "album" else "albums"}",
        onClick = onClick,
        modifier = modifier,
        gridCount = gridCount,
        showArtwork = showArtwork
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = firstLetter,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
