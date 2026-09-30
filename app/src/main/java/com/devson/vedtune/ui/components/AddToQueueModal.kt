package com.devson.vedtune.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.QueueInfo
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.VedTuneTextStyles
import com.devson.vedtune.ui.theme.spacing

/**
 * Bottom sheet modal allowing users to add songs to an existing queue or create a new queue.
 * The new queue name is pre-filled with the selected Album, Artist, Genre, Composer, or Song title.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToQueueModal(
    songs: List<Song>,
    prefilledQueueName: String,
    queues: List<QueueInfo>,
    onDismiss: () -> Unit,
    onAddToQueue: (queueId: Long, songs: List<Song>, playNext: Boolean) -> Unit,
    onCreateQueueAndAdd: (queueName: String, songs: List<Song>, playNext: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var playNext by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = VedTuneShapeTokens.BottomSheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = MaterialTheme.spacing.l)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.l, vertical = MaterialTheme.spacing.s),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(VedTuneIconSizes.Medium)
                    )
                    Column {
                        Text(
                            text = "Add to Queue",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${songs.size} ${if (songs.size == 1) "track" else "tracks"}",
                            style = VedTuneTextStyles.Metadata,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                VedTuneIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Close",
                    onClick = onDismiss,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Play Next / Add to End toggle chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.l, vertical = MaterialTheme.spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
            ) {
                FilterChip(
                    selected = !playNext,
                    onClick = { playNext = false },
                    label = { Text("Add to Bottom") }
                )
                FilterChip(
                    selected = playNext,
                    onClick = { playNext = true },
                    label = { Text("Play Next") }
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.s))

            // Create New Queue Action Button
            OutlinedButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.l),
                shape = VedTuneShapeTokens.Medium
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.s))
                Text(
                    text = "Create New Queue",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.s))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 1.dp
            )

            // Existing Queues List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentPadding = PaddingValues(
                    horizontal = MaterialTheme.spacing.l,
                    vertical = MaterialTheme.spacing.s
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
            ) {
                items(
                    items = queues,
                    key = { it.id }
                ) { queue ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAddToQueue(queue.id, songs, playNext)
                                onDismiss()
                            },
                        shape = VedTuneShapeTokens.Medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = MaterialTheme.spacing.m, vertical = MaterialTheme.spacing.s),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = VedTuneShapeTokens.Small,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(MaterialTheme.spacing.m))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = queue.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${queue.songCount} ${if (queue.songCount == 1) "track" else "tracks"}",
                                    style = VedTuneTextStyles.Metadata,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateQueueDialog(
            initialName = prefilledQueueName,
            onDismiss = { showCreateDialog = false },
            onCreate = { queueName ->
                showCreateDialog = false
                onCreateQueueAndAdd(queueName, songs, playNext)
                onDismiss()
            }
        )
    }
}
