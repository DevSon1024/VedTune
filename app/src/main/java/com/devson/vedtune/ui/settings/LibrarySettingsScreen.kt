package com.devson.vedtune.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.FolderFilterMode
import com.devson.vedtune.ui.components.VedTuneConfirmDialog
import com.devson.vedtune.ui.components.VedTuneIconButton
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.VedTuneTextStyles
import com.devson.vedtune.ui.theme.spacing

@Composable
fun LibrarySettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToFolderSettings: () -> Unit,
    onNavigateToLyricsConverter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val autoSyncOnStartup by viewModel.autoSyncOnStartup.collectAsStateWithLifecycle()
    val folderFilterMode by viewModel.folderFilterMode.collectAsStateWithLifecycle()

    var showResetDialog by remember { mutableStateOf(false) }

    val folderModeLabel = when (folderFilterMode) {
        FolderFilterMode.NONE -> "All folders included"
        FolderFilterMode.WHITELIST -> "Whitelist active"
        FolderFilterMode.BLACKLIST -> "Blacklist active"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = MaterialTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VedTuneIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Go Back",
                onClick = onNavigateBack,
                iconSize = VedTuneIconSizes.Medium
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.s))
            Text(
                text = "Library & Folders",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            VedTuneIconButton(
                icon = Icons.Default.RestartAlt,
                contentDescription = "Reset Library Settings",
                onClick = { showResetDialog = true },
                iconSize = VedTuneIconSizes.Medium,
                tint = MaterialTheme.colorScheme.error
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.l,
                end = MaterialTheme.spacing.l,
                top = MaterialTheme.spacing.s,
                bottom = MaterialTheme.spacing.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)
        ) {
            // MediaStore Sync Card
            item {
                LibrarySectionCard(
                    title = "Media Synchronization",
                    icon = Icons.Default.Sync
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-sync on Startup",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Scan and sync device MediaStore audio on app launch",
                                style = VedTuneTextStyles.Metadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoSyncOnStartup,
                            onCheckedChange = { viewModel.setAutoSyncOnStartup(it) }
                        )
                    }
                }
            }

            // Folder Filtering Card
            item {
                LibrarySectionCard(
                    title = "Folders & Scanning",
                    icon = Icons.Default.Folder
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToFolderSettings)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Folder Visibility & Filters",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = folderModeLabel,
                                style = VedTuneTextStyles.Metadata,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Lyrics Tools Card
            item {
                LibrarySectionCard(
                    title = "Lyrics Management",
                    icon = Icons.Default.Lyrics
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToLyricsConverter)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LRC Lyrics Batch Converter",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Scan and convert external .lrc files into synchronized tags",
                                style = VedTuneTextStyles.Metadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Reset Library Dialog
    if (showResetDialog) {
        VedTuneConfirmDialog(
            title = "Reset Library Settings?",
            message = "This will enable auto-sync on startup, clear all folder whitelist and blacklist rules, and include all folders in library scanning.",
            confirmText = "Reset",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                viewModel.resetLibrarySettings()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }
}

@Composable
private fun LibrarySectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = VedTuneShapeTokens.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(VedTuneIconSizes.Medium)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            content()
        }
    }
}
