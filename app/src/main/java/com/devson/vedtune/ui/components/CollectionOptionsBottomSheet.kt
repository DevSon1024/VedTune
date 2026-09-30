package com.devson.vedtune.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.spacing

/**
 * Standard BottomSheet for collection options (Album, Artist, Genre, Folder, Playlist).
 * Allows Play, Play Next, Shuffle, Add to Queue, Add to Playlist, and Share.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VedTuneCollectionOptionsBottomSheet(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onPlay: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onPlayShuffle: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null
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
            VedTuneBottomSheetHeader(
                title = title,
                subtitle = subtitle,
                onCloseClick = onDismiss
            )

            if (onPlay != null) {
                SheetOptionItem(
                    title = "Play",
                    icon = Icons.Default.PlayArrow,
                    onClick = onPlay
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

            if (onAddToQueue != null) {
                SheetOptionItem(
                    title = "Add to Queue",
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    onClick = onAddToQueue
                )
            }

            if (onAddToPlaylist != null) {
                SheetOptionItem(
                    title = "Add to Playlist",
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    onClick = onAddToPlaylist
                )
            }

            if (onShare != null) {
                SheetOptionItem(
                    title = "Share",
                    icon = Icons.Default.Share,
                    onClick = onShare
                )
            }
        }
    }
}
