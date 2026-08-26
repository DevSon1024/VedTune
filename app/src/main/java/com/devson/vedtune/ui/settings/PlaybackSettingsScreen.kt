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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.SeekBarStyle
import com.devson.vedtune.ui.components.AudioFeatureHelp
import com.devson.vedtune.ui.components.AudioFeatureInfoDialog
import com.devson.vedtune.ui.components.VedTuneConfirmDialog
import com.devson.vedtune.ui.components.VedTuneIconButton
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.VedTuneTextStyles
import com.devson.vedtune.ui.theme.spacing
import kotlin.math.roundToInt

@Composable
fun PlaybackSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val audioSettings by viewModel.audioSettings.collectAsState()
    val autoplayOnStartup by viewModel.autoplayOnStartup.collectAsState()
    val audioFadeInEnabled by viewModel.audioFadeInEnabled.collectAsState()
    val enableSwipeToSkip by viewModel.enableSwipeToSkip.collectAsState()
    val seekbarStyle by viewModel.seekbarStyle.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }
    var showClearQueueDialog by remember { mutableStateOf(false) }
    var activeHelpDialog by remember { mutableStateOf<AudioFeatureHelp?>(null) }

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
                text = "Playback",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            VedTuneIconButton(
                icon = Icons.Default.RestartAlt,
                contentDescription = "Reset Playback Settings",
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
            // Gapless Playback Card
            item {
                PlaybackSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(VedTuneIconSizes.Medium)
                            )
                            Column {
                                Text(
                                    text = "Gapless Playback",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Eliminates silence between consecutive album tracks",
                                    style = VedTuneTextStyles.Metadata,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { activeHelpDialog = AudioFeatureHelp.GAPLESS_PLAYBACK }) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Help",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(VedTuneIconSizes.Small)
                                )
                            }
                            Switch(
                                checked = audioSettings.gaplessPlaybackEnabled,
                                onCheckedChange = { viewModel.updateAudioSettings { s -> s.copy(gaplessPlaybackEnabled = it) } }
                            )
                        }
                    }
                }
            }

            // Crossfade Card
            item {
                PlaybackSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(VedTuneIconSizes.Medium)
                            )
                            Column {
                                Text(
                                    text = "Crossfade",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Seamlessly blend ending and upcoming songs",
                                    style = VedTuneTextStyles.Metadata,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { activeHelpDialog = AudioFeatureHelp.CROSSFADE }) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Help",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(VedTuneIconSizes.Small)
                                )
                            }
                            Switch(
                                checked = audioSettings.crossfadeEnabled,
                                onCheckedChange = { viewModel.updateAudioSettings { s -> s.copy(crossfadeEnabled = it) } }
                            )
                        }
                    }

                    if (audioSettings.crossfadeEnabled) {
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.s))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Crossfade Duration",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${audioSettings.crossfadeDurationMs / 1000}s",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = (audioSettings.crossfadeDurationMs / 1000).toFloat(),
                            onValueChange = {
                                val ms = (it.roundToInt() * 1000).coerceIn(1000, 20000)
                                viewModel.updateAudioSettings { s -> s.copy(crossfadeDurationMs = ms) }
                            },
                            valueRange = 1f..20f,
                            steps = 18
                        )
                    }
                }
            }

            // Smooth Audio Dimming Card
            item {
                PlaybackSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(VedTuneIconSizes.Medium)
                            )
                            Column {
                                Text(
                                    text = "Smooth Audio Dimming",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Gently fade audio in/out on play, pause, and track skip",
                                    style = VedTuneTextStyles.Metadata,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { activeHelpDialog = AudioFeatureHelp.SMOOTH_DIMMING }) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Help",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(VedTuneIconSizes.Small)
                                )
                            }
                            Switch(
                                checked = audioFadeInEnabled,
                                onCheckedChange = { viewModel.setAudioFadeInEnabled(it) }
                            )
                        }
                    }
                }
            }

            // Startup & Navigation Card
            item {
                PlaybackSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(VedTuneIconSizes.Medium)
                            )
                            Column {
                                Text(
                                    text = "Autoplay on Startup",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Automatically resume playback when app launches",
                                    style = VedTuneTextStyles.Metadata,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = autoplayOnStartup,
                            onCheckedChange = { viewModel.setAutoplayOnStartup(it) }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    // Swipe to skip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Swipe,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(VedTuneIconSizes.Medium)
                            )
                            Column {
                                Text(
                                    text = "Swipe to Skip",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Swipe left/right on artwork in player to skip songs",
                                    style = VedTuneTextStyles.Metadata,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = enableSwipeToSkip,
                            onCheckedChange = { viewModel.setEnableSwipeToSkip(it) }
                        )
                    }
                }
            }

            // Seekbar Style Card
            item {
                PlaybackSectionCard {
                    Text(
                        text = "Seekbar Style",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Visual presentation of player progress bar",
                        style = VedTuneTextStyles.Metadata,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
                    ) {
                        SeekBarStyle.entries.forEach { style ->
                            FilterChip(
                                selected = (seekbarStyle == style),
                                onClick = { viewModel.setSeekBarStyle(style) },
                                label = {
                                    Text(
                                        text = when (style) {
                                            SeekBarStyle.DEFAULT -> "Standard"
                                            SeekBarStyle.SLIM -> "Slim"
                                            SeekBarStyle.WAVY -> "Waveform"
                                        }
                                    )
                                },
                                shape = VedTuneShapeTokens.Pill
                            )
                        }
                    }
                }
            }

            // Clear Playback Queue Action Card
            item {
                PlaybackSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Clear Playback Queue",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Remove all upcoming tracks and stop active playback",
                                style = VedTuneTextStyles.Metadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { showClearQueueDialog = true },
                            shape = VedTuneShapeTokens.Pill
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }
        }
    }

    // Help Dialog
    activeHelpDialog?.let { help ->
        AudioFeatureInfoDialog(
            help = help,
            onDismiss = { activeHelpDialog = null }
        )
    }

    // Clear Queue Confirmation Dialog
    if (showClearQueueDialog) {
        VedTuneConfirmDialog(
            title = "Clear Playback Queue?",
            message = "This will stop the active playback and empty all tracks in the current queue.",
            confirmText = "Clear Queue",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                viewModel.clearPlaybackQueue()
                showClearQueueDialog = false
            },
            onDismiss = { showClearQueueDialog = false }
        )
    }

    // Reset Playback Confirmation Dialog
    if (showResetDialog) {
        VedTuneConfirmDialog(
            title = "Reset Playback Settings?",
            message = "This will restore default playback options (Gapless enabled, Crossfade 0s, Smooth Dimming enabled, Autoplay off, Standard seekbar). Your music library and playlists will not be changed.",
            confirmText = "Reset",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                viewModel.resetPlaybackSettings()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }
}

@Composable
private fun PlaybackSectionCard(
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
        ) {
            content()
        }
    }
}
