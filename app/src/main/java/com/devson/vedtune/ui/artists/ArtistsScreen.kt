package com.devson.vedtune.ui.artists

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.Artist
import com.devson.vedtune.domain.model.ViewPreferences
import com.devson.vedtune.ui.components.VedTuneArtistCard
import com.devson.vedtune.ui.components.VedTuneEmptyState
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.VedTuneTextStyles
import com.devson.vedtune.ui.theme.rememberVedTuneAdaptiveInfo
import com.devson.vedtune.ui.theme.spacing

@Composable
fun ArtistsScreen(
    viewModel: ArtistsViewModel,
    onArtistClick: (String) -> Unit,
    viewPreferences: ViewPreferences,
    onLayoutToggleClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val artists by viewModel.artists.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
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
        if (artists.isEmpty()) {
            VedTuneEmptyState(
                icon = Icons.Default.Person,
                title = if (searchQuery.isBlank()) "No Artists Found" else "No Matching Artists",
                description = if (searchQuery.isBlank()) "Your music library artists will be listed here." else "Try searching with a different artist name.",
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
                        items = artists,
                        key = { it.name },
                        contentType = { "artist_grid_item" }
                    ) { artist ->
                        VedTuneArtistCard(
                            artist = artist,
                            onClick = { onArtistClick(artist.name) },
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
                        items = artists,
                        key = { it.name },
                        contentType = { "artist_list_item" }
                    ) { artist ->
                        ArtistListItem(
                            artist = artist,
                            onClick = { onArtistClick(artist.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistListItem(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initial = remember(artist.name) {
        artist.name.trim().take(1).uppercase().ifBlank { "?" }
    }

    val subtitle = buildString {
        append("${artist.songCount} ${if (artist.songCount == 1) "song" else "songs"}")
        if (artist.albumCount > 0) {
            append(" • ${artist.albumCount} ${if (artist.albumCount == 1) "album" else "albums"}")
        }
    }

    ListItem(
        headlineContent = {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = VedTuneTextStyles.Metadata,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
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
