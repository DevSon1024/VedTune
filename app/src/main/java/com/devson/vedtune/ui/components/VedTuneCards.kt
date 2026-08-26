package com.devson.vedtune.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.devson.vedtune.core.toFormattedSongDuration
import com.devson.vedtune.domain.model.Album
import com.devson.vedtune.domain.model.Artist
import com.devson.vedtune.domain.model.Playlist
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.spacing

/**
 * Standard Song list row for VedTune with high performance and accessibility.
 * Powered by VedTuneListItem.
 */
@Composable
fun VedTuneSongRow(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrentSong: Boolean = false,
    isPlaying: Boolean = false,
    showArtwork: Boolean = true,
    showDuration: Boolean = true,
    onOptionsClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    val subtitle = if (song.album.isNotBlank() && song.album != "Unknown Album") {
        "${song.artist} • ${song.album}"
    } else {
        song.artist
    }

    VedTuneListItem(
        primaryText = song.title,
        secondaryText = subtitle,
        onClick = onClick,
        modifier = modifier,
        containerColor = containerColor,
        isHighlighted = isCurrentSong,
        leadingContent = if (showArtwork) {
            {
                Box(
                    modifier = Modifier.size(48.dp)
                ) {
                    SongArtwork(
                        albumId = song.albumId,
                        lastModified = song.dateModified,
                        fallbackIcon = Icons.Default.MusicNote,
                        showFallbackAnimation = false,
                        thumbnailSize = ArtworkThumbnailSize.SMALL,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(VedTuneShapeTokens.Small)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        showArtwork = showArtwork
                    )
                    if (isCurrentSong) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(VedTuneShapeTokens.Small)
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            PlayingIndicator(
                                isPlaying = isPlaying,
                                modifier = Modifier.size(VedTuneIconSizes.Standard)
                            )
                        }
                    }
                }
            }
        } else null,
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
            ) {
                if (showDuration && song.duration > 0) {
                    Text(
                        text = song.duration.toFormattedSongDuration(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
                if (onOptionsClick != null) {
                    VedTuneIconButton(
                        icon = Icons.Default.MoreVert,
                        contentDescription = "Song options for ${song.title}",
                        onClick = onOptionsClick,
                        iconSize = VedTuneIconSizes.Medium,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

/**
 * Standard Album Grid Card.
 */
@Composable
fun VedTuneAlbumCard(
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

    VedTuneGridCard(
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
            thumbnailSize = ArtworkThumbnailSize.MEDIUM,
            modifier = Modifier.fillMaxSize(),
            showArtwork = showArtwork
        )
    }
}

/**
 * Standard Artist Grid Card with circular artwork.
 */
@Composable
fun VedTuneArtistCard(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showArtwork: Boolean = true,
    gridCount: Int = 2
) {
    val subtitle = "${artist.songCount} ${if (artist.songCount == 1) "song" else "songs"}"

    Card(
        shape = VedTuneShapeTokens.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(VedTuneShapeTokens.Card)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(if (gridCount >= 4) MaterialTheme.spacing.xs else MaterialTheme.spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showArtwork) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                ) {
                    SongArtwork(
                        albumId = -1L,
                        fallbackIcon = Icons.Default.Person,
                        showFallbackAnimation = false,
                        modifier = Modifier.fillMaxSize(),
                        showArtwork = showArtwork
                    )
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.s))
            }
            Text(
                text = artist.name,
                style = if (gridCount >= 4) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (gridCount < 4) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Standard Playlist Grid Card.
 */
@Composable
fun VedTunePlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    previewAlbumIds: List<Long> = emptyList(),
    showArtwork: Boolean = true,
    gridCount: Int = 2
) {
    val isFavorite = playlist.id == Playlist.FAVORITES_PLAYLIST_ID
    val subtitle = "${playlist.songCount} ${if (playlist.songCount == 1) "track" else "tracks"}"

    VedTuneGridCard(
        primaryText = playlist.name,
        secondaryText = subtitle,
        onClick = onClick,
        modifier = modifier,
        gridCount = gridCount,
        showArtwork = showArtwork
    ) {
        PlaylistArtworkCollage(
            albumIds = previewAlbumIds,
            isFavorite = isFavorite,
            showArtwork = showArtwork,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Standard Section Header with optional action button or counter.
 */
@Composable
fun VedTuneSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.l, vertical = MaterialTheme.spacing.s),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (count != null) {
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.s))
                Text(
                    text = "($count)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!actionText.isNullOrBlank() && onActionClick != null) {
            VedTuneTextButton(
                text = actionText,
                onClick = onActionClick
            )
        }
    }
}
