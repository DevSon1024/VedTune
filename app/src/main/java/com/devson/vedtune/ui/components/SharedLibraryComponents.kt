package com.devson.vedtune.ui.components

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.VedTuneTextStyles
import com.devson.vedtune.ui.theme.spacing

/**
 * Standard Material 3 List Item for VedTune library views.
 * Supports leading artwork/icon, title, subtitle, and trailing options/actions.
 */
@Composable
fun VedTuneListItem(
    primaryText: String,
    secondaryText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    shape: Shape = VedTuneShapeTokens.Medium,
    isHighlighted: Boolean = false
) {
    ListItem(
        headlineContent = {
            Text(
                text = primaryText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = if (secondaryText.isNotBlank()) {
            {
                Text(
                    text = secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else null,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(
            containerColor = containerColor
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
    )
}

/**
 * Standard Material 3 Grid Card for VedTune library views.
 * Built with ElevatedCard, 1:1 aspect ratio artwork, tonal elevation, and clamped typography.
 */
@Composable
fun VedTuneGridCard(
    primaryText: String,
    secondaryText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    tonalElevation: Dp = 1.dp,
    gridCount: Int = 2,
    showArtwork: Boolean = true,
    shape: Shape = if (gridCount >= 4) VedTuneShapeTokens.Small else VedTuneShapeTokens.Medium,
    artworkShape: Shape = if (gridCount >= 4) VedTuneShapeTokens.ExtraSmall else VedTuneShapeTokens.Medium,
    trailingContent: @Composable (() -> Unit)? = null,
    artworkContent: @Composable () -> Unit
) {
    val padding = if (gridCount >= 4) MaterialTheme.spacing.xs else MaterialTheme.spacing.m

    val titleStyle = when {
        gridCount >= 4 -> MaterialTheme.typography.labelSmall
        gridCount >= 3 -> MaterialTheme.typography.bodyMedium
        else -> MaterialTheme.typography.bodyLarge
    }
    val subtitleStyle = MaterialTheme.typography.bodyMedium

    ElevatedCard(
        shape = shape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = tonalElevation
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(padding)
        ) {
            if (showArtwork) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(artworkShape)
                ) {
                    artworkContent()
                }
                Spacer(modifier = Modifier.height(if (gridCount >= 4) MaterialTheme.spacing.xs else MaterialTheme.spacing.s))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = primaryText,
                        style = titleStyle,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (gridCount < 4 && secondaryText.isNotBlank()) {
                        Text(
                            text = secondaryText,
                            style = subtitleStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (gridCount < 4 && trailingContent != null) {
                    trailingContent()
                }
            }
        }
    }
}

/**
 * High-performance library view switcher that seamlessly switches between
 * LazyColumn (List) and LazyVerticalGrid (Grid) while keeping state and content padding consistent.
 */
@Composable
fun <T : Any> VedTuneLibraryView(
    items: List<T>,
    isGridView: Boolean,
    gridSpanCount: Int,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    contentType: (T) -> Any? = { null },
    contentPadding: PaddingValues = PaddingValues(0.dp),
    lazyListState: LazyListState = rememberLazyListState(),
    lazyGridState: LazyGridState = rememberLazyGridState(),
    listItemContent: @Composable (T) -> Unit,
    gridItemContent: @Composable (T) -> Unit
) {
    if (isGridView) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridSpanCount),
            state = lazyGridState,
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s),
            contentPadding = contentPadding
        ) {
            items(
                count = items.size,
                key = { index -> key(items[index]) },
                contentType = { index -> contentType(items[index]) }
            ) { index ->
                gridItemContent(items[index])
            }
        }
    } else {
        LazyColumn(
            state = lazyListState,
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs),
            contentPadding = contentPadding
        ) {
            items(
                count = items.size,
                key = { index -> key(items[index]) },
                contentType = { index -> contentType(items[index]) }
            ) { index ->
                listItemContent(items[index])
            }
        }
    }
}

@Composable
fun LibraryUtilityRow(
    currentSortLabel: String,
    sortOrderIcon: String,
    onSortClick: () -> Unit,
    isGridView: Boolean,
    onLayoutToggleClick: () -> Unit,
    onShuffleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = MaterialTheme.spacing.l),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssistChip(
            onClick = onSortClick,
            label = {
                Text(
                    text = "$currentSortLabel $sortOrderIcon",
                    style = VedTuneTextStyles.Badge,
                    fontWeight = FontWeight.Medium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Sort",
                    modifier = Modifier.size(VedTuneIconSizes.Small)
                )
            },
            shape = VedTuneShapeTokens.Small,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.height(28.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VedTuneIconButton(
                icon = Icons.Default.Shuffle,
                contentDescription = "Shuffle All",
                onClick = onShuffleClick,
                iconSize = VedTuneIconSizes.Small,
                touchTargetSize = 36.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            VedTuneIconButton(
                icon = if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                contentDescription = "Toggle Layout",
                onClick = onLayoutToggleClick,
                iconSize = VedTuneIconSizes.Small,
                touchTargetSize = 36.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
