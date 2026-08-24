package com.devson.vedtune.ui.genres

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import com.devson.vedtune.ui.songs.SortOrder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.vedtune.ui.components.FastScroller
import com.devson.vedtune.ui.components.buildAlphabeticalSectionIndices
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.remember

@Composable
fun GenresScreen(
    viewModel: GenresViewModel,
    onGenreClick: (String) -> Unit,
    viewPreferences: com.devson.vedtune.domain.model.ViewPreferences,
    onLayoutToggleClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val genres by viewModel.genres.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isGridView = viewPreferences.isGridView
    val sortOrder by viewModel.sortOrder.collectAsState()

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()

    val genreSectionIndices = remember(genres, sortOrder) {
        genres.buildAlphabeticalSectionIndices { it }
    }

    val currentSortLabel = "Name"
    val orderIcon = if (sortOrder == com.devson.vedtune.ui.songs.SortOrder.ASCENDING) "↑" else "↓"

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                com.devson.vedtune.ui.components.LibraryUtilityRow(
                    currentSortLabel = currentSortLabel,
                    sortOrderIcon = orderIcon,
                    onSortClick = { viewModel.toggleSortOrder() },
                    isGridView = isGridView,
                    onLayoutToggleClick = onLayoutToggleClick,
                    onShuffleClick = { viewModel.playShuffleAll() }
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (genres.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No genres found",
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
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 8.dp,
                                    bottom = contentPadding.calculateBottomPadding() + 88.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(
                                    items = genres,
                                    key = { it }
                                ) { genre ->
                                    GenreGridItem(
                                        genreName = genre,
                                        onClick = { onGenreClick(genre) },
                                        showArtwork = viewPreferences.showAlbumArt,
                                        gridCount = viewPreferences.gridSpanCount
                                    )
                                }
                            }

                            FastScroller(
                                gridState = lazyGridState,
                                sectionIndices = genreSectionIndices,
                                contentPadding = PaddingValues(
                                    top = 8.dp,
                                    bottom = contentPadding.calculateBottomPadding() + 88.dp
                                )
                            )
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            androidx.compose.foundation.lazy.LazyColumn(
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
                                    items = genres,
                                    key = { it }
                                ) { genre ->
                                    GenreListItem(
                                        genreName = genre,
                                        onClick = { onGenreClick(genre) },
                                        showArtwork = viewPreferences.showAlbumArt
                                    )
                                }
                            }

                            FastScroller(
                                listState = lazyListState,
                                sectionIndices = genreSectionIndices,
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
    }
}

@Composable
fun GenreGridItem(
    genreName: String,
    onClick: () -> Unit,
    showArtwork: Boolean = true,
    gridCount: Int = 2,
    modifier: Modifier = Modifier
) {
    com.devson.vedtune.ui.components.VedTuneGridCard(
        primaryText = genreName.ifEmpty { "Unknown" },
        secondaryText = "Genre",
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
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = genreName,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
fun GenreListItem(
    genreName: String,
    onClick: () -> Unit,
    showArtwork: Boolean = true,
    modifier: Modifier = Modifier
) {
    com.devson.vedtune.ui.components.VedTuneListItem(
        primaryText = genreName.ifEmpty { "Unknown" },
        secondaryText = "Genre",
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
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = genreName,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        } else null
    )
}

