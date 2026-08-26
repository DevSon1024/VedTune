package com.devson.vedtune.ui.settings

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devson.vedtune.ui.components.VedTuneConfirmDialog
import com.devson.vedtune.ui.components.VedTuneTopAppBar
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.VedTuneTextStyles
import com.devson.vedtune.ui.theme.spacing

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onNavigateToAppearanceSettings: () -> Unit = {},
    onNavigateToPlaybackSettings: () -> Unit = {},
    onNavigateToAudioSettings: () -> Unit = {},
    onNavigateToLibrarySettings: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {},
    onNavigateToStorageSettings: () -> Unit = {},
    onNavigateToPrivacySettings: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {}
) {
    val context = LocalContext.current
    var showResetAllDialog by remember { mutableStateOf(false) }

    val versionName = remember(context) {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName ?: "1.0.0"
        }.getOrDefault("1.0.0")
    }

    val isDebug = remember {
        runCatching {
            com.devson.vedtune.BuildConfig.DEBUG
        }.getOrDefault(false)
    }

    Column(modifier = modifier.fillMaxSize()) {
        VedTuneTopAppBar(
            title = "Settings",
            searchQuery = "",
            onQueryChange = {},
            showSearchAction = false,
            showSortAction = false,
            showLayoutToggleAction = false,
            modifier = Modifier.statusBarsPadding()
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.l,
                top = MaterialTheme.spacing.s,
                end = MaterialTheme.spacing.l,
                bottom = contentPadding.calculateBottomPadding() + MaterialTheme.spacing.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)
        ) {
            // App Identity Hero Card
            item {
                AppIdentityCard(
                    versionName = versionName,
                    isDebug = isDebug,
                    onClick = onNavigateToAbout
                )
            }

            // 1. Appearance Section
            item {
                SettingsSectionContainer {
                    SettingsNavigationTile(
                        icon = Icons.Default.Palette,
                        title = "Appearance",
                        subtitle = "Theme mode, AMOLED pure black, dynamic colors, artwork display",
                        onClick = onNavigateToAppearanceSettings
                    )
                }
            }

            // 2. Playback Section
            item {
                SettingsSectionContainer {
                    SettingsNavigationTile(
                        icon = Icons.Default.PlayArrow,
                        title = "Playback",
                        subtitle = "Queue, gapless playback, crossfade, and startup options",
                        onClick = onNavigateToPlaybackSettings
                    )
                }
            }

            // 3. Audio Section
            item {
                SettingsSectionContainer {
                    SettingsNavigationTile(
                        icon = Icons.Default.GraphicEq,
                        title = "Audio",
                        subtitle = "Equalizer, ReplayGain, volume normalization, and DSP processing",
                        onClick = onNavigateToAudioSettings
                    )
                }
            }

            // 4. Library Section
            item {
                SettingsSectionContainer {
                    SettingsNavigationTile(
                        icon = Icons.Default.LibraryMusic,
                        title = "Library",
                        subtitle = "Folder filters, auto-sync, scanner, and lyrics directory",
                        onClick = onNavigateToLibrarySettings
                    )
                }
            }

            // 5. Notifications Section
            item {
                SettingsSectionContainer {
                    SettingsNavigationTile(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        subtitle = "Media notification style, lockscreen playback controls",
                        onClick = onNavigateToNotificationSettings
                    )
                }
            }

            // 6. Storage Section
            item {
                SettingsSectionContainer {
                    SettingsNavigationTile(
                        icon = Icons.Default.SdCard,
                        title = "Storage",
                        subtitle = "Artwork cache, custom lyrics cache, and storage permissions",
                        onClick = onNavigateToStorageSettings
                    )
                }
            }

            // 7. Privacy Section
            item {
                SettingsSectionContainer {
                    SettingsNavigationTile(
                        icon = Icons.Default.Shield,
                        title = "Privacy",
                        subtitle = "Listening history logging, crash diagnostics, and data controls",
                        onClick = onNavigateToPrivacySettings
                    )
                }
            }

            // 8. About Section
            item {
                SettingsSectionContainer {
                    SettingsNavigationTile(
                        icon = Icons.Default.Info,
                        title = "About",
                        subtitle = "App version, developer info, open source licenses, credits",
                        onClick = onNavigateToAbout
                    )
                }
            }

            // Reset All Settings Action Card
            item {
                Card(
                    shape = VedTuneShapeTokens.Card,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.l),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reset All Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Restore all appearance, playback, audio, and library preferences to default",
                                style = VedTuneTextStyles.Metadata,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.m))
                        OutlinedButton(
                            onClick = { showResetAllDialog = true },
                            shape = VedTuneShapeTokens.Pill
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(VedTuneIconSizes.Small)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset")
                        }
                    }
                }
            }
        }
    }

    // Reset All Confirmation Dialog
    if (showResetAllDialog) {
        VedTuneConfirmDialog(
            title = "Reset All Settings?",
            message = "This will restore all application preferences back to defaults:\n\n• Appearance: System theme, dynamic colors, default start tab\n• Playback: Gapless enabled, crossfade off, standard seekbar\n• Audio: Bit-perfect mode (EQ, ReplayGain, LUFS, Limiter disabled)\n• Library: All folders included, auto-sync enabled\n\nYour actual music audio files and playlists will NOT be deleted.",
            confirmText = "Reset Everything",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                viewModel.resetAllSettings()
                showResetAllDialog = false
            },
            onDismiss = { showResetAllDialog = false }
        )
    }
}

@Composable
private fun SettingsSectionContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        shape = VedTuneShapeTokens.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        content()
    }
}

@Composable
private fun SettingsNavigationTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(MaterialTheme.spacing.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m),
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = VedTuneShapeTokens.Small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(VedTuneIconSizes.Medium)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = VedTuneTextStyles.Metadata,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AppIdentityCard(
    versionName: String,
    isDebug: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = VedTuneShapeTokens.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                ) {
                    Text(
                        text = "VedTune",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isDebug) {
                        Surface(
                            shape = VedTuneShapeTokens.Pill,
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "DEBUG",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Version $versionName • MediaStore-First Music Player",
                    style = VedTuneTextStyles.Metadata,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
