package com.devson.vedtune.ui.genres

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.ViewPreferences
import com.devson.vedtune.ui.components.VedTuneEmptyState
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.VedTuneTextStyles
import com.devson.vedtune.ui.theme.rememberVedTuneAdaptiveInfo
import com.devson.vedtune.ui.theme.spacing

@Composable
fun GenresScreen(
    viewModel: GenresViewModel,
    onGenreClick: (String) -> Unit,
    viewPreferences: ViewPreferences,
    onLayoutToggleClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val genres by viewModel.genres.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
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
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        } else if (genres.isEmpty()) {
            VedTuneEmptyState(
                icon = Icons.Default.MusicNote,
                title = "No Genres Found",
                description = "Genre tags from your audio files will appear here.",
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
                        items = genres,
                        key = { it }
                    ) { genre ->
                        GenreGridCard(
                            genreName = genre,
                            onClick = { onGenreClick(genre) }
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
                        items = genres,
                        key = { it }
                    ) { genre ->
                        GenreListRow(
                            genreName = genre,
                            onClick = { onGenreClick(genre) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreGridCard(
    genreName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = VedTuneShapeTokens.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(VedTuneShapeTokens.Card)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(VedTuneIconSizes.Large)
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.s))
            Text(
                text = genreName.ifBlank { "Unknown Genre" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Genre",
                style = VedTuneTextStyles.Metadata,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GenreListRow(
    genreName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = {
            Text(
                text = genreName.ifBlank { "Unknown Genre" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = "Genre",
                style = VedTuneTextStyles.Metadata,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(VedTuneShapeTokens.Small)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(VedTuneIconSizes.Medium)
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
