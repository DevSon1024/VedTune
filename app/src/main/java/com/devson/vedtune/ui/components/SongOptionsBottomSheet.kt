package com.devson.vedtune.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.spacing

/**
 * Standard unified modal bottom sheet for song options across VedTune.
 * Implements Material 3 Expressive styling with consistent iconography and actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VedTuneSongOptionsBottomSheet(
    song: Song,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavorite: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onPlayShuffle: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onSongInfo: (() -> Unit)? = null,
    onPreviewSong: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onEditTags: (() -> Unit)? = null,
    onDeletePermanently: (() -> Unit)? = null,
    onRemoveFromHistory: (() -> Unit)? = null,
    onGoToAlbum: (() -> Unit)? = null,
    onGoToArtist: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = VedTuneShapeTokens.BottomSheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(bottom = MaterialTheme.spacing.xxl)
        ) {
            val subtitle = if (song.album.isNotBlank() && song.album != "Unknown Album") {
                "${song.artist} • ${song.album}"
            } else {
                song.artist
            }

            VedTuneBottomSheetHeader(
                title = song.title,
                subtitle = subtitle,
                onCloseClick = onDismiss
            )

            if (onToggleFavorite != null) {
                SheetOptionItem(
                    title = if (song.isFavorite) "Remove from Favorites" else "Add to Favorites",
                    icon = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    iconTint = if (song.isFavorite) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onToggleFavorite
                )
            }

            if (onPlayNext != null) {
                SheetOptionItem(
                    title = "Play Next",
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    onClick = onPlayNext
                )
            }

            if (onPlayShuffle != null) {
                SheetOptionItem(
                    title = "Shuffle",
                    icon = Icons.Default.Shuffle,
                    onClick = onPlayShuffle
                )
            }

            if (onAddToPlaylist != null) {
                SheetOptionItem(
                    title = "Add to Playlist",
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    onClick = onAddToPlaylist
                )
            }

            if (onPreviewSong != null) {
                SheetOptionItem(
                    title = "Preview Song",
                    icon = Icons.Default.PlayCircle,
                    onClick = onPreviewSong
                )
            }

            if (onSongInfo != null) {
                SheetOptionItem(
                    title = "Song Info",
                    icon = Icons.Default.Info,
                    onClick = onSongInfo
                )
            }

            if (onShare != null) {
                SheetOptionItem(
                    title = "Share Song",
                    icon = Icons.Default.Share,
                    onClick = onShare
                )
            }

            if (onEditTags != null) {
                SheetOptionItem(
                    title = "Edit Tags",
                    icon = Icons.Default.Edit,
                    onClick = onEditTags
                )
            }

            if (onRemoveFromHistory != null) {
                SheetOptionItem(
                    title = "Remove from History",
                    icon = Icons.Default.RemoveCircleOutline,
                    onClick = onRemoveFromHistory
                )
            }

            if (onGoToAlbum != null && song.albumId > 0) {
                SheetOptionItem(
                    title = "Go to Album",
                    icon = Icons.Default.Album,
                    onClick = onGoToAlbum
                )
            }

            if (onGoToArtist != null && song.artist.isNotBlank() && song.artist != "<unknown>") {
                SheetOptionItem(
                    title = "Go to Artist",
                    icon = Icons.Default.Person,
                    onClick = onGoToArtist
                )
            }

            if (onDeletePermanently != null) {
                SheetOptionItem(
                    title = "Delete Permanently",
                    icon = Icons.Default.DeleteForever,
                    iconTint = MaterialTheme.colorScheme.error,
                    textColor = MaterialTheme.colorScheme.error,
                    onClick = onDeletePermanently
                )
            }
        }
    }
}

@Composable
private fun SheetOptionItem(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(VedTuneIconSizes.Medium)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}
