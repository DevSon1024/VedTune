package com.devson.vedtune.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.VedTuneTextStyles
import com.devson.vedtune.ui.theme.spacing

@Composable
fun VedTuneListItem(
    primaryText: String,
    secondaryText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    ListItem(
        headlineContent = {
            Text(
                text = primaryText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
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
            .clip(VedTuneShapeTokens.Medium)
            .clickable(onClick = onClick)
    )
}

@Composable
fun VedTuneGridCard(
    primaryText: String,
    secondaryText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    gridCount: Int = 2,
    showArtwork: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null,
    artworkContent: @Composable () -> Unit
) {
    val padding = if (gridCount >= 4) MaterialTheme.spacing.xs else MaterialTheme.spacing.m
    val cardShape = if (gridCount >= 4) VedTuneShapeTokens.Small else VedTuneShapeTokens.Card
    val artworkShape = if (gridCount >= 4) VedTuneShapeTokens.ExtraSmall else VedTuneShapeTokens.Medium

    val titleStyle = when {
        gridCount >= 4 -> MaterialTheme.typography.labelSmall
        gridCount >= 3 -> MaterialTheme.typography.bodyMedium
        else -> MaterialTheme.typography.bodyLarge
    }
    val subtitleStyle = MaterialTheme.typography.bodyMedium

    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
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
