package com.devson.vedtune.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.ReplayGainMode
import com.devson.vedtune.ui.components.AudioDiagnosticsDialog
import com.devson.vedtune.ui.components.AudioFeatureHelp
import com.devson.vedtune.ui.components.AudioFeatureInfoDialog
import com.devson.vedtune.ui.components.VedTuneConfirmDialog
import com.devson.vedtune.ui.components.VedTuneIconButton
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.VedTuneTextStyles
import com.devson.vedtune.ui.theme.spacing
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AudioSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val audioSettings by viewModel.audioSettings.collectAsState()
    val diagnostics by viewModel.audioDiagnostics.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    var activeHelpDialog by remember { mutableStateOf<AudioFeatureHelp?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top App Bar
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
                text = "Audio & Processing",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            VedTuneIconButton(
                icon = Icons.Default.RestartAlt,
                contentDescription = "Reset Audio Settings",
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
            // Master Processing Toggle Card
            item {
                AudioSettingSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Audio Processing Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (audioSettings.audioProcessingEnabled) "DSP effects and volume normalization are active"
                                else "Bit-Perfect Mode (all DSP bypassed for pristine transparency)",
                                style = VedTuneTextStyles.Metadata,
                                color = if (audioSettings.audioProcessingEnabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = audioSettings.audioProcessingEnabled,
                            onCheckedChange = { viewModel.setAudioProcessingEnabled(it) }
                        )
                    }
                }
            }

            // Equalizer Entry Card
            item {
                AudioSettingSectionCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToEqualizer)
                            .padding(vertical = 4.dp),
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
                                        imageVector = Icons.Default.Equalizer,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(VedTuneIconSizes.Medium)
                                    )
                                }
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                                ) {
                                    Text(
                                        text = "Equalizer",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = VedTuneShapeTokens.Pill,
                                        color = if (audioSettings.equalizerEnabled) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = if (audioSettings.equalizerEnabled) audioSettings.equalizerPreset ?: "Custom" else "Off",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (audioSettings.equalizerEnabled) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "10-band frequency shaper, custom curves, and acoustic presets",
                                    style = VedTuneTextStyles.Metadata,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { activeHelpDialog = AudioFeatureHelp.EQUALIZER }) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Equalizer Help",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(VedTuneIconSizes.Small)
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

            // ReplayGain Section Card
            item {
                AudioSettingSectionCard {
                    SectionHeaderWithHelp(
                        title = "ReplayGain Normalization",
                        icon = Icons.Default.GraphicEq,
                        onHelpClick = { activeHelpDialog = AudioFeatureHelp.REPLAY_GAIN }
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.s))

                    Text(
                        text = "Mode",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
                    ) {
                        ReplayGainMode.entries.forEach { mode ->
                            val isSelected = audioSettings.replayGainMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setReplayGainMode(mode) },
                                label = {
                                    Text(
                                        text = when (mode) {
                                            ReplayGainMode.OFF -> "Off"
                                            ReplayGainMode.TRACK -> "Track"
                                            ReplayGainMode.ALBUM -> "Album"
                                        }
                                    )
                                },
                                shape = VedTuneShapeTokens.Pill
                            )
                        }
                    }

                    if (audioSettings.replayGainMode != ReplayGainMode.OFF) {
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.s))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Preamp Gain",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%+.1f dB", audioSettings.replayGainPreampDb),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = audioSettings.replayGainPreampDb,
                            onValueChange = { viewModel.setReplayGainPreampDb(it) },
                            valueRange = -12.0f..12.0f,
                            steps = 47
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Prevent Clipping",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Automatically scale gain down if peaks exceed 0 dBFS",
                                    style = VedTuneTextStyles.Metadata,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = audioSettings.replayGainPreventClipping,
                                onCheckedChange = { viewModel.setReplayGainPreventClipping(it) }
                            )
                        }
                    }
                }
            }

            // Loudness Normalization Card
            item {
                AudioSettingSectionCard {
                    SectionHeaderWithHelp(
                        title = "Loudness Normalization (LUFS)",
                        icon = Icons.Default.Speed,
                        onHelpClick = { activeHelpDialog = AudioFeatureHelp.LOUDNESS_NORMALIZATION }
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.s))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable LUFS Normalization",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Standardizes volume to a perceived acoustic loudness target",
                                style = VedTuneTextStyles.Metadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = audioSettings.loudnessNormalizationEnabled,
                            onCheckedChange = { viewModel.setLoudnessNormalizationEnabled(it) }
                        )
                    }

                    if (audioSettings.loudnessNormalizationEnabled) {
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.s))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Target Loudness",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f LUFS", audioSettings.targetLufs),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = audioSettings.targetLufs,
                            onValueChange = { viewModel.setTargetLufs(it) },
                            valueRange = -23.0f..-9.0f,
                            steps = 27
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
                        ) {
                            FilterChip(
                                selected = (audioSettings.targetLufs == -14.0f),
                                onClick = { viewModel.setTargetLufs(-14.0f) },
                                label = { Text("-14 LUFS (Streaming)") },
                                shape = VedTuneShapeTokens.Pill
                            )
                            FilterChip(
                                selected = (audioSettings.targetLufs == -18.0f),
                                onClick = { viewModel.setTargetLufs(-18.0f) },
                                label = { Text("-18 LUFS (AES Hi-Fi)") },
                                shape = VedTuneShapeTokens.Pill
                            )
                        }
                    }
                }
            }

            // Bass Boost & Virtualizer Card
            item {
                AudioSettingSectionCard {
                    SectionHeaderWithHelp(
                        title = "Bass Boost & Virtualizer",
                        icon = Icons.Default.Tune,
                        onHelpClick = { activeHelpDialog = AudioFeatureHelp.BASS_BOOST }
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.s))

                    // Bass Boost
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Bass Boost",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = audioSettings.bassBoostEnabled,
                            onCheckedChange = { viewModel.setBassBoostEnabled(it) }
                        )
                    }

                    if (audioSettings.bassBoostEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Strength",
                                style = VedTuneTextStyles.Metadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${audioSettings.bassBoostStrength}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = audioSettings.bassBoostStrength.toFloat(),
                            onValueChange = { viewModel.setBassBoostStrength(it.roundToInt()) },
                            valueRange = 0f..100f
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    // Virtualizer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Virtualizer (Spatial Sound)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { activeHelpDialog = AudioFeatureHelp.VIRTUALIZER }) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Virtualizer Help",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(VedTuneIconSizes.Small)
                                )
                            }
                        }
                        Switch(
                            checked = audioSettings.virtualizerEnabled,
                            onCheckedChange = { viewModel.setVirtualizerEnabled(it) }
                        )
                    }

                    if (audioSettings.virtualizerEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Strength",
                                style = VedTuneTextStyles.Metadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${audioSettings.virtualizerStrength}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = audioSettings.virtualizerStrength.toFloat(),
                            onValueChange = { viewModel.setVirtualizerStrength(it.roundToInt()) },
                            valueRange = 0f..100f
                        )
                    }
                }
            }

            // Peak Limiter & Preamp Card
            item {
                AudioSettingSectionCard {
                    SectionHeaderWithHelp(
                        title = "Peak Limiter & Headroom",
                        icon = Icons.Default.Security,
                        onHelpClick = { activeHelpDialog = AudioFeatureHelp.LIMITER }
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.s))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Peak Limiter",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Prevents transient clipping distortion when boosting EQ or bass",
                                style = VedTuneTextStyles.Metadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = audioSettings.limiterEnabled,
                            onCheckedChange = { viewModel.setLimiterEnabled(it) }
                        )
                    }

                    if (audioSettings.limiterEnabled) {
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.s))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Limiter Threshold",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f dBFS", audioSettings.limiterThresholdDb),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = audioSettings.limiterThresholdDb,
                            onValueChange = { viewModel.setLimiterThresholdDb(it) },
                            valueRange = -6.0f..0.0f,
                            steps = 11
                        )
                    }
                }
            }

            // Audio Diagnostics Action Card
            item {
                AudioSettingSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Live Audio Diagnostics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Inspect current audio pipeline format, sample rate, and active DSP chain",
                                style = VedTuneTextStyles.Metadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.loadAudioDiagnostics()
                                showDiagnosticsDialog = true
                            },
                            shape = VedTuneShapeTokens.Pill
                        ) {
                            Text("Inspect")
                        }
                    }
                }
            }
        }
    }

    // Diagnostics Dialog
    if (showDiagnosticsDialog && diagnostics != null) {
        AudioDiagnosticsDialog(
            diagnostics = diagnostics!!,
            onDismiss = { showDiagnosticsDialog = false }
        )
    }

    // Audio Feature Explanation Dialog
    activeHelpDialog?.let { help ->
        AudioFeatureInfoDialog(
            help = help,
            onDismiss = { activeHelpDialog = null }
        )
    }

    // Reset Audio Settings Confirmation Dialog
    if (showResetDialog) {
        VedTuneConfirmDialog(
            title = "Reset Audio Settings?",
            message = "This will return all audio processing settings (Equalizer, ReplayGain, Loudness Normalization, Bass Boost, and Limiter) to factory default bit-perfect mode (0 dB unity gain). Playlists and library metadata will not be affected.",
            confirmText = "Reset Audio",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                viewModel.resetAudioSettings()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }
}

@Composable
private fun AudioSettingSectionCard(
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

@Composable
private fun SectionHeaderWithHelp(
    title: String,
    icon: ImageVector,
    onHelpClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
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
        IconButton(onClick = onHelpClick) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Feature Info",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(VedTuneIconSizes.Small)
            )
        }
    }
}
