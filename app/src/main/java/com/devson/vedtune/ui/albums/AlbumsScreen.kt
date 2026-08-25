package com.devson.vedtune.ui.albums

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.ViewPreferences
import com.devson.vedtune.ui.components.SongArtwork
import com.devson.vedtune.ui.components.VedTuneAlbumCard
import com.devson.vedtune.ui.components.VedTuneEmptyState
import com.devson.vedtune.ui.components.VedTuneListItem
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.rememberVedTuneAdaptiveInfo
import com.devson.vedtune.ui.theme.spacing

@Composable
fun AlbumsScreen(
    viewModel: AlbumsViewModel,
    onAlbumClick: (Long) -> Unit,
    viewPreferences: ViewPreferences,
    onLayoutToggleClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val albums by viewModel.albums.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isGridView = viewPreferences.isGridView
    val showArtwork = viewPreferences.showAlbumArt
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
        if (albums.isEmpty()) {
            VedTuneEmptyState(
                icon = Icons.Default.Album,
                title = if (searchQuery.isBlank()) "No Albums Found" else "No Matching Albums",
                description = if (searchQuery.isBlank()) "Your synced audio albums will appear here." else "Try searching with a different keyword.",
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
                        items = albums,
                        key = { it.id },
                        contentType = { "album_grid_item" }
                    ) { album ->
                        VedTuneAlbumCard(
                            album = album,
                            onClick = { onAlbumClick(album.id) },
                            showArtwork = showArtwork,
                            showArtist = viewPreferences.showArtist,
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
                        items = albums,
                        key = { it.id },
                        contentType = { "album_list_item" }
                    ) { album ->
                        val artistPart = if (viewPreferences.showArtist) album.artist else ""
                        val songCountPart = "${album.songCount} ${if (album.songCount == 1) "song" else "songs"}"
                        val secondaryText = buildString {
                            if (artistPart.isNotEmpty()) {
                                append(artistPart)
                                append(" • ")
                            }
                            append(songCountPart)
                        }

                        VedTuneListItem(
                            primaryText = album.title,
                            secondaryText = secondaryText,
                            onClick = { onAlbumClick(album.id) },
                            leadingContent = if (showArtwork) {
                                {
                                    SongArtwork(
                                        albumId = album.id,
                                        fallbackIcon = Icons.Default.Album,
                                        showFallbackAnimation = false,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(VedTuneShapeTokens.Small),
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
