package com.devson.vedtune.ui.folders

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.components.VedTuneEmptyState
import com.devson.vedtune.ui.components.VedTuneIconButton
import com.devson.vedtune.ui.components.VedTunePrimaryButton
import com.devson.vedtune.ui.components.VedTuneSecondaryButton
import com.devson.vedtune.ui.components.VedTuneSongRow
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.VedTuneTextStyles
import com.devson.vedtune.ui.theme.spacing

@Composable
fun FoldersScreen(
    viewModel: FoldersViewModel,
    onSongOptionsClick: (Song) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentSongId by viewModel.currentSongId.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState.selectedFolder != null) {
        viewModel.selectFolder(null)
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            AnimatedContent(
                targetState = uiState.selectedFolder,
                transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                label = "FolderNavigationTransition"
            ) { selectedFolder ->
                if (selectedFolder == null) {
                    // Top-Level: Folder List
                    if (uiState.folders.isEmpty()) {
                        VedTuneEmptyState(
                            icon = Icons.Rounded.Folder,
                            title = "No Audio Folders Found",
                            description = "Make sure audio files are placed in accessible storage folders.",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val listState = rememberLazyListState()
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs),
                            contentPadding = PaddingValues(
                                start = MaterialTheme.spacing.m,
                                end = MaterialTheme.spacing.m,
                                top = MaterialTheme.spacing.s,
                                bottom = contentPadding.calculateBottomPadding() + 88.dp
                            )
                        ) {
                            items(
                                items = uiState.folders,
                                key = { it.path }
                            ) { folder ->
                                FolderListItem(
                                    folder = folder,
                                    onClick = { viewModel.selectFolder(folder.path) }
                                )
                            }
                        }
                    }
                } else {
                    // Drill-Down: Folder Songs View
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Folder Header with Back button and Actions
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = MaterialTheme.spacing.l,
                                        vertical = MaterialTheme.spacing.m
                                    )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    VedTuneIconButton(
                                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Back to Folders",
                                        onClick = { viewModel.selectFolder(null) },
                                        iconSize = VedTuneIconSizes.Medium,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.s))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedFolder.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = selectedFolder.path,
                                            style = VedTuneTextStyles.Metadata,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(MaterialTheme.spacing.m))

                                // Play All & Shuffle Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)
                                ) {
                                    VedTunePrimaryButton(
                                        text = "Play All (${selectedFolder.songCount})",
                                        icon = Icons.Rounded.PlayArrow,
                                        onClick = { viewModel.playAllInFolder(selectedFolder) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    VedTuneSecondaryButton(
                                        text = "Shuffle",
                                        icon = Icons.Rounded.Shuffle,
                                        onClick = { viewModel.shuffleAllInFolder(selectedFolder) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Song List in Folder
                        val songListState = rememberLazyListState()
                        LazyColumn(
                            state = songListState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs),
                            contentPadding = PaddingValues(
                                start = MaterialTheme.spacing.m,
                                end = MaterialTheme.spacing.m,
                                top = MaterialTheme.spacing.s,
                                bottom = contentPadding.calculateBottomPadding() + 88.dp
                            )
                        ) {
                            items(
                                items = selectedFolder.songs,
                                key = { it.id }
                            ) { song ->
                                VedTuneSongRow(
                                    song = song,
                                    isCurrentSong = song.id == currentSongId,
                                    isPlaying = isPlaying,
                                    onClick = { viewModel.playSongInFolder(song, selectedFolder) },
                                    onOptionsClick = { onSongOptionsClick(song) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderListItem(
    folder: FolderItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = folder.path,
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
                    .clip(VedTuneShapeTokens.Medium)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(VedTuneIconSizes.Medium)
                )
            }
        },
        trailingContent = {
            Surface(
                shape = VedTuneShapeTokens.Pill,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = "${folder.songCount} ${if (folder.songCount == 1) "track" else "tracks"}",
                    style = VedTuneTextStyles.Badge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.s, vertical = 2.dp)
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
