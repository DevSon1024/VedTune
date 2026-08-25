package com.devson.vedtune.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens

/**
 * High-performance, memory-cached artwork composable for playlists.
 * - Empty: Displays category icon with styled tinted container
 * - 1-3 songs: Displays single hero artwork
 * - 4+ songs: Composes a 2x2 grid of album artwork cached via Coil
 */
@Composable
fun PlaylistArtworkCollage(
    albumIds: List<Long>,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    showArtwork: Boolean = true,
    fallbackIcon: ImageVector = if (isFavorite) Icons.Default.Favorite else Icons.AutoMirrored.Filled.QueueMusic
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(VedTuneShapeTokens.ArtworkCard),
        contentAlignment = Alignment.Center
    ) {
        if (!showArtwork || albumIds.isEmpty()) {
            // Placeholder container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isFavorite) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.secondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = null,
                    tint = if (isFavorite) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(VedTuneIconSizes.Large)
                )
            }
        } else if (albumIds.size < 4) {
            // Single hero artwork
            SongArtwork(
                albumId = albumIds.first(),
                fallbackIcon = fallbackIcon,
                showFallbackAnimation = false,
                modifier = Modifier.fillMaxSize(),
                showArtwork = showArtwork
            )
        } else {
            // 2x2 Collage of cached artwork
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    SongArtwork(
                        albumId = albumIds[0],
                        fallbackIcon = fallbackIcon,
                        showFallbackAnimation = false,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        showArtwork = showArtwork
                    )
                    SongArtwork(
                        albumId = albumIds[1],
                        fallbackIcon = fallbackIcon,
                        showFallbackAnimation = false,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        showArtwork = showArtwork
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    SongArtwork(
                        albumId = albumIds[2],
                        fallbackIcon = fallbackIcon,
                        showFallbackAnimation = false,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        showArtwork = showArtwork
                    )
                    SongArtwork(
                        albumId = albumIds[3],
                        fallbackIcon = fallbackIcon,
                        showFallbackAnimation = false,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        showArtwork = showArtwork
                    )
                }
            }
        }
    }
}
