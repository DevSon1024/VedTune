package com.devson.vedtune.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TextFormat
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.components.VedTuneBottomSheetHeader
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.spacing

@Composable
fun OptionsSheetContent(
    song: Song,
    showArtwork: Boolean,
    onEditTags: () -> Unit,
    onEditLyrics: () -> Unit,
    onAudioDiagnostics: () -> Unit = {},
    onShare: () -> Unit,
    onDeletePermanently: () -> Unit,
    onPlayerSettings: () -> Unit,
    onCloseClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = MaterialTheme.spacing.xxl)
    ) {
        VedTuneBottomSheetHeader(
            title = song.title,
            subtitle = "${song.artist} • ${song.album.ifBlank { "Unknown Album" }}",
            onCloseClick = onCloseClick
        )

        BottomSheetOption(
            icon = Icons.Rounded.GraphicEq,
            title = "Audio Information & Diagnostics",
            onClick = onAudioDiagnostics
        )
        BottomSheetOption(
            icon = Icons.Rounded.Edit,
            title = "Edit Tags",
            onClick = onEditTags
        )
        BottomSheetOption(
            icon = Icons.Rounded.TextFormat,
            title = "Edit Lyrics",
            onClick = onEditLyrics
        )
        BottomSheetOption(
            icon = Icons.Rounded.Share,
            title = "Share Song",
            onClick = onShare
        )
        BottomSheetOption(
            icon = Icons.Rounded.DeleteForever,
            title = "Delete Permanently",
            tint = MaterialTheme.colorScheme.error,
            onClick = onDeletePermanently
        )
        BottomSheetOption(
            icon = Icons.Rounded.Settings,
            title = "Player Settings",
            onClick = onPlayerSettings
        )
    }
}

@Composable
fun BottomSheetOption(
    icon: ImageVector,
    title: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = tint
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(VedTuneIconSizes.Medium)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}
