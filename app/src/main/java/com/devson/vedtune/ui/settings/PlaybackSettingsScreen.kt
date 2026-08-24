package com.devson.vedtune.ui.settings

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devson.vedtune.player.engine.loudness.LoudnessTarget
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEqualizer: () -> Unit = {}
) {
    val audioSettings by viewModel.audioSettings.collectAsState()
    val autoplayOnStartup by viewModel.autoplayOnStartup.collectAsState()
    val audioFadeInEnabled by viewModel.audioFadeInEnabled.collectAsState()
    val defaultStartScreen by viewModel.defaultStartScreen.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Playback Preferences",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsCard(
                title = "Playback Preferences",
                icon = Icons.Default.PlayArrow
            ) {
                SettingSwitchRow(
                    title = "Auto-resume on Startup",
                    description = "Automatically resume playback when app is restarted.",
                    checked = autoplayOnStartup,
                    onCheckedChange = { viewModel.setAutoplayOnStartup(it) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                SettingSwitchRow(
                    title = "Audio Fade-in on Play/Resume",
                    description = "Gradually fade in sound when starting or resuming playback.",
                    checked = audioFadeInEnabled,
                    onCheckedChange = { viewModel.setAudioFadeInEnabled(it) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                DefaultStartScreenSelector(
                    currentScreen = defaultStartScreen,
                    onScreenSelected = { viewModel.setDefaultStartScreen(it) }
                )
            }

            // 10-Band Graphic Equalizer
            SettingsCard(
                title = "Equalizer",
                icon = Icons.Default.Tune
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (audioSettings.equalizerEnabled) {
                            "Status: Active (${audioSettings.equalizerPreset ?: "Custom"})"
                        } else {
                            "Status: Disabled (100% Transparent Bit-Perfect Output)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (audioSettings.equalizerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Customize 10-band frequency gains (-12 dB to +12 dB), user preamp, and frequency presets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onNavigateToEqualizer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Open 10-Band Equalizer")
                    }
                }
            }

            // Loudness Normalization (LUFS) Card
            SettingsCard(
                title = "Loudness Normalization",
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (audioSettings.loudnessNormalizationEnabled) {
                                    "Target: ${audioSettings.targetLufs} LUFS"
                                } else {
                                    "Loudness Normalization Disabled"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (audioSettings.loudnessNormalizationEnabled) {
                                    "Linear volume matching via ReplayGain/LUFS tags"
                                } else {
                                    "100% Bit-Perfect Transparent Volume (Zero Compression)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = audioSettings.loudnessNormalizationEnabled,
                            onCheckedChange = { viewModel.setLoudnessNormalizationEnabled(it) }
                        )
                    }

                    Text(
                        text = "LUFS normalization applies clean linear volume scaling without altering dynamics. Dynamic limiting and compression are separate operations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    AnimatedVisibility(
                        visible = audioSettings.loudnessNormalizationEnabled,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Target Presets Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (target in LoudnessTarget.ALL_TARGETS) {
                                    val isSelected = audioSettings.targetLufs == target.lufs
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setTargetLufs(target.lufs) },
                                        label = { Text(target.title) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }

                            // Fine Adjustment Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Target Loudness",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${audioSettings.targetLufs} LUFS",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Slider(
                                value = audioSettings.targetLufs,
                                onValueChange = {
                                    val snapped = ((it * 2).roundToInt() / 2f).coerceIn(-24f, -8f)
                                    viewModel.setTargetLufs(snapped)
                                },
                                valueRange = -24.0f..-8.0f,
                                steps = 31,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedButton(
                                onClick = { viewModel.resetLoudnessNormalization() },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.height(16.dp).width(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Loudness")
                            }
                        }
                    }
                }
            }

            // Safety Limiter & Headroom Protection Card
            SettingsCard(
                title = "Safety Limiter & Headroom",
                icon = Icons.Default.Tune
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Stage 1: Technical Anti-Clipping Prevention (Pre-attenuation)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Automatic Anti-Clipping Protection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Mathematically prevents clipping across active EQ/Bass Boost/ReplayGain without dynamic pumping or audible compression.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = audioSettings.preventClipping,
                            onCheckedChange = { viewModel.setPreventClipping(it) }
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Stage 2: User Peak Brickwall Limiter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (audioSettings.limiterEnabled) "Peak Limiter Active" else "Peak Limiter Disabled",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Enforces a hard ceiling limiter on dynamic audio peaks (Android 9.0+).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = audioSettings.limiterEnabled,
                            onCheckedChange = { viewModel.setLimiterEnabled(it) }
                        )
                    }

                    AnimatedVisibility(
                        visible = audioSettings.limiterEnabled,
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
                                    text = "Limiter Ceiling Threshold",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f dB", audioSettings.limiterThresholdDb),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Slider(
                                value = audioSettings.limiterThresholdDb,
                                onValueChange = {
                                    val snapped = ((it * 2).roundToInt() / 2f).coerceIn(-12f, 0f)
                                    viewModel.setLimiterThresholdDb(snapped)
                                },
                                valueRange = -12.0f..0.0f,
                                steps = 23,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.resetLimiter() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.height(16.dp).width(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Limiter")
                    }
                }
            }

            // System Equalizer
            SettingsCard(
                title = "System Equalizer",
                icon = Icons.Default.Tune
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Launch external OEM/system equalizer panel if installed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                                    putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                                    putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "System Equalizer not supported on this device.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Launch System Control Panel")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
