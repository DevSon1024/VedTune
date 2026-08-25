package com.devson.vedtune.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.vector.ImageVector
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.spacing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devson.vedtune.player.engine.equalizer.EqualizerPresets
import com.devson.vedtune.ui.components.AudioFeatureHelp
import com.devson.vedtune.ui.components.AudioFeatureInfoDialog
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val audioSettings by viewModel.audioSettings.collectAsState()
    var activeHelpDialog by remember { mutableStateOf<AudioFeatureHelp?>(null) }

    activeHelpDialog?.let { help ->
        AudioFeatureInfoDialog(
            help = help,
            onDismiss = { activeHelpDialog = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "10-Band Equalizer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { activeHelpDialog = AudioFeatureHelp.EQUALIZER }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Learn about Equalizer"
                        )
                    }
                    IconButton(onClick = { viewModel.resetEqualizer() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Equalizer"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master Toggle Card
            item {
                SettingsCard(
                    title = "Equalizer Master",
                    icon = Icons.Default.GraphicEq
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (audioSettings.equalizerEnabled) "Equalizer Active" else "Equalizer Disabled",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (audioSettings.equalizerEnabled) "Applying customized 10-band profile" else "100% Transparent (Zero DSP active)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { activeHelpDialog = AudioFeatureHelp.EQUALIZER },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Learn about Equalizer",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = audioSettings.equalizerEnabled,
                            onCheckedChange = { viewModel.setEqualizerEnabled(it) }
                        )
                    }
                }
            }

            // Presets Selection Card
            item {
                AnimatedVisibility(
                    visible = audioSettings.equalizerEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    SettingsCard(
                        title = "Presets",
                        icon = Icons.Default.Tune
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Select a balanced frequency preset or fine-tune bands manually.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (preset in EqualizerPresets.ALL_PRESETS) {
                                    val isSelected = audioSettings.equalizerPreset.equals(preset.name, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.selectEqualizerPreset(preset.name) },
                                        label = { Text(preset.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }

                                val isCustom = audioSettings.equalizerPreset == EqualizerPresets.PRESET_CUSTOM
                                FilterChip(
                                    selected = isCustom,
                                    onClick = { /* Custom is auto-selected when editing bands */ },
                                    label = { Text("Custom") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Preamp Gain Card
            item {
                AnimatedVisibility(
                    visible = audioSettings.equalizerEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    SettingsCard(
                        title = "Preamp Gain",
                        icon = Icons.Default.Tune
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Overall Equalizer Level",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    IconButton(
                                        onClick = { activeHelpDialog = AudioFeatureHelp.PREAMP },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Learn about Preamp",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = formatGainDb(audioSettings.equalizerPreampDb),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Slider(
                                value = audioSettings.equalizerPreampDb,
                                onValueChange = { viewModel.setEqualizerPreampDb(((it * 2).roundToInt() / 2f).coerceIn(-12f, 12f)) },
                                valueRange = -12.0f..12.0f,
                                steps = 47, // 0.5 dB steps
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 10-Band Graphic Equalizer Bands
            item {
                AnimatedVisibility(
                    visible = audioSettings.equalizerEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    SettingsCard(
                        title = "Graphic Equalizer Bands",
                        icon = Icons.Default.GraphicEq
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val userBands = audioSettings.equalizerBandGains.ifEmpty { EqualizerPresets.defaultBandGains() }

                            EqualizerPresets.FREQUENCIES_HZ.forEachIndexed { index, freqHz ->
                                val label = EqualizerPresets.BAND_LABELS.getOrNull(index) ?: "${freqHz}Hz"
                                val currentGain = userBands.getOrNull(index) ?: 0.0f

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = formatGainDb(currentGain),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (currentGain != 0.0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Slider(
                                        value = currentGain,
                                        onValueChange = { newGain ->
                                            val snapped = ((newGain * 2).roundToInt() / 2f).coerceIn(-12f, 12f)
                                            viewModel.updateEqualizerBand(index, snapped)
                                        },
                                        valueRange = -12.0f..12.0f,
                                        steps = 47,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bass Boost Card
            item {
                SettingsCard(
                    title = "Bass Boost",
                    icon = Icons.Default.Tune
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (audioSettings.bassBoostEnabled) "Bass Boost Active" else "Bass Boost Disabled",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Low-frequency sub-bass reinforcement",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { activeHelpDialog = AudioFeatureHelp.BASS_BOOST },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Learn about Bass Boost",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = audioSettings.bassBoostEnabled,
                                onCheckedChange = { viewModel.setBassBoostEnabled(it) }
                            )
                        }

                        AnimatedVisibility(
                            visible = audioSettings.bassBoostEnabled,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Bass Strength",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${(audioSettings.bassBoostStrength / 10f).roundToInt()}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Slider(
                                    value = audioSettings.bassBoostStrength.toFloat(),
                                    onValueChange = { viewModel.setBassBoostStrength(it.roundToInt()) },
                                    valueRange = 0f..1000f,
                                    steps = 19, // 5% increments
                                    modifier = Modifier.fillMaxWidth()
                                )

                                androidx.compose.material3.OutlinedButton(
                                    onClick = { viewModel.resetBassBoost() },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.height(16.dp).width(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset Bass Boost")
                                }
                            }
                        }
                    }
                }
            }

            // Virtualizer Card
            item {
                SettingsCard(
                    title = "Virtualizer (3D Surround)",
                    icon = Icons.Default.Tune
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (audioSettings.virtualizerEnabled) "Virtualizer Active" else "Virtualizer Disabled",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Spatial soundstage widening and 3D acoustic simulation",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { activeHelpDialog = AudioFeatureHelp.VIRTUALIZER },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Learn about Virtualizer",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = audioSettings.virtualizerEnabled,
                                onCheckedChange = { viewModel.setVirtualizerEnabled(it) }
                            )
                        }

                        AnimatedVisibility(
                            visible = audioSettings.virtualizerEnabled,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Surround Effect Strength",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${(audioSettings.virtualizerStrength / 10f).roundToInt()}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Slider(
                                    value = audioSettings.virtualizerStrength.toFloat(),
                                    onValueChange = { viewModel.setVirtualizerStrength(it.roundToInt()) },
                                    valueRange = 0f..1000f,
                                    steps = 19, // 5% increments
                                    modifier = Modifier.fillMaxWidth()
                                )

                                androidx.compose.material3.OutlinedButton(
                                    onClick = { viewModel.resetVirtualizer() },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.height(16.dp).width(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset Virtualizer")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun formatGainDb(gainDb: Float): String {
    return if (gainDb > 0.0f) {
        String.format(Locale.US, "+%.1f dB", gainDb)
    } else {
        String.format(Locale.US, "%.1f dB", gainDb)
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
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
