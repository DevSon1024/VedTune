package com.devson.vedtune.ui.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.QueueInfo
import com.devson.vedtune.ui.components.CreateQueueDialog
import com.devson.vedtune.ui.components.RenameQueueDialog
import com.devson.vedtune.ui.components.VedTuneConfirmDialog
import com.devson.vedtune.ui.components.VedTuneIconButton
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.VedTuneTextStyles
import com.devson.vedtune.ui.theme.spacing
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueuesManagementModal(
    queues: List<QueueInfo>,
    selectedQueueId: Long,
    activeQueueId: Long,
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    onSelectQueue: (Long) -> Unit,
    onSwitchActiveQueue: (Long) -> Unit,
    onCreateQueue: (String) -> Unit,
    onRenameQueue: (Long, String) -> Unit,
    onDeleteQueue: (Long) -> Unit,
    onRemoveAllOtherQueues: (Long) -> Unit,
    onReorderQueues: (List<Long>) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current

    var localQueues by remember(queues) { mutableStateOf(queues) }
    var queueToRename by remember { mutableStateOf<QueueInfo?>(null) }
    var queueToDelete by remember { mutableStateOf<QueueInfo?>(null) }
    var showRemoveAllOthersConfirm by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var dragEndIndex by remember { mutableIntStateOf(-1) }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (dragStartIndex == -1) {
            dragStartIndex = from.index
        }
        dragEndIndex = to.index
        localQueues = localQueues.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

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
                    Text(
                        text = "Queues",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                VedTuneIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Close",
                    onClick = onDismiss,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                thickness = 1.dp
            )

            // Queues List
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentPadding = PaddingValues(
                    horizontal = MaterialTheme.spacing.m,
                    vertical = MaterialTheme.spacing.s
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(
                    items = localQueues,
                    key = { _, q -> q.id }
                ) { index, queue ->
                    val isSelected = queue.id == selectedQueueId
                    val isActive = queue.id == activeQueueId

                    ReorderableItem(
                        state = reorderableLazyListState,
                        key = queue.id
                    ) { isDragging ->
                        val isDraggingPrev = remember { mutableStateOf(false) }
                        LaunchedEffect(isDragging) {
                            if (!isDragging && isDraggingPrev.value) {
                                if (dragStartIndex != -1 && dragEndIndex != -1 && dragStartIndex != dragEndIndex) {
                                    onReorderQueues(localQueues.map { it.id })
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                dragStartIndex = -1
                                dragEndIndex = -1
                            }
                            isDraggingPrev.value = isDragging
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectQueue(queue.id)
                                    onDismiss()
                                },
                            shape = VedTuneShapeTokens.Medium,
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isDragging -> MaterialTheme.colorScheme.surfaceContainerHighest
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = MaterialTheme.spacing.s, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Drag handle
                                Icon(
                                    imageVector = Icons.Rounded.DragHandle,
                                    contentDescription = "Drag to reorder queue",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .draggableHandle(
                                            onDragStarted = {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        )
                                        .padding(MaterialTheme.spacing.s)
                                        .size(VedTuneIconSizes.Medium)
                                )

                                // Radio Button indicating active playing queue
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onSelectQueue(queue.id)
                                        onDismiss()
                                    }
                                )

                                Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))

                                // Index Number
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.width(MaterialTheme.spacing.s))

                                // Play icon if active & playing
                                if (isActive) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Currently Playing Queue",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }

                                // Queue Name and Count
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = queue.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected || isActive) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${queue.songCount} ${if (queue.songCount == 1) "song" else "songs"}",
                                        style = VedTuneTextStyles.Metadata,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Edit / Rename button
                                IconButton(
                                    onClick = { queueToRename = queue }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Rename Queue",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Delete button
                                val canDelete = localQueues.size > 1 && queue.id != QueueInfo.DEFAULT_QUEUE_ID
                                IconButton(
                                    onClick = { queueToDelete = queue },
                                    enabled = canDelete
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete Queue",
                                        tint = if (canDelete) MaterialTheme.colorScheme.error.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.s))

            // Create New Queue button
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

            // REMOVE ALL OTHER QUEUES
            if (localQueues.size > 1) {
                TextButton(
                    onClick = { showRemoveAllOthersConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacing.l)
                ) {
                    Text(
                        text = "REMOVE ALL OTHER QUEUES",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    // Rename dialog
    queueToRename?.let { target ->
        RenameQueueDialog(
            currentName = target.name,
            onDismiss = { queueToRename = null },
            onRename = { newName ->
                onRenameQueue(target.id, newName)
                queueToRename = null
            }
        )
    }

    // Create new queue dialog
    if (showCreateDialog) {
        CreateQueueDialog(
            initialName = "",
            title = "Create New Queue",
            confirmText = "Create",
            onDismiss = { showCreateDialog = false },
            onCreate = { newName ->
                onCreateQueue(newName)
                showCreateDialog = false
            }
        )
    }

    // Delete queue confirmation
    queueToDelete?.let { target ->
        VedTuneConfirmDialog(
            title = "Delete Queue",
            message = "Are you sure you want to remove queue \"${target.name}\" and its songs?",
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onDeleteQueue(target.id)
                queueToDelete = null
            },
            onDismiss = { queueToDelete = null }
        )
    }

    // Remove all other queues confirmation
    if (showRemoveAllOthersConfirm) {
        VedTuneConfirmDialog(
            title = "Remove All Other Queues",
            message = "Are you sure you want to remove all queues except \"${queues.firstOrNull { it.id == selectedQueueId }?.name ?: "Current Queue"}\"?",
            confirmText = "Remove Others",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onRemoveAllOtherQueues(selectedQueueId)
                showRemoveAllOthersConfirm = false
            },
            onDismiss = { showRemoveAllOthersConfirm = false }
        )
    }
}
