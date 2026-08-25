package com.devson.vedtune.ui.settings

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.AudioSettings
import com.devson.vedtune.domain.model.ReplayGainMode
import com.devson.vedtune.player.engine.equalizer.EqualizerPresets
import com.devson.vedtune.player.engine.loudness.LoudnessTarget
import com.devson.vedtune.ui.components.AudioDiagnosticsDialog
import com.devson.vedtune.ui.components.AudioFeatureHelp
import com.devson.vedtune.ui.components.AudioFeatureInfoDialog
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private val EQ_FREQUENCIES = listOf(
    "31 Hz", "62 Hz", "125 Hz", "250 Hz", "500 Hz",
    "1 kHz", "2 kHz", "4 kHz", "8 kHz", "16 kHz"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEqualizer: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val audioSettings by viewModel.audioSettings.collectAsState()
    val diagnostics by viewModel.audioDiagnostics.collectAsState()
    var showResetAllDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    var activeHelpDialog by remember { mutableStateOf<AudioFeatureHelp?>(null) }

    fun showFeedback(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    activeHelpDialog?.let { help ->
        AudioFeatureInfoDialog(
            help = help,
            onDismiss = { activeHelpDialog = null }
        )
    }

    if (showDiagnosticsDialog) {
        diagnostics?.let { diag ->
            AudioDiagnosticsDialog(
                diagnostics = diag,
                onDismiss = { showDiagnosticsDialog = false }
            )
        }
    }

    if (showResetAllDialog) {
        AlertDialog(
            onDismissRequest = { showResetAllDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Reset all audio settings?") },
            text = { Text("All custom audio processing settings will return to factory default (100% transparent bit-perfect mode). Queue, library, and favorites will not be affected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAudioSettings()
                        showResetAllDialog = false
                        showFeedback("All audio settings reset to defaults")
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Audio & Playback",
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
                actions = {
                    IconButton(onClick = { showResetAllDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset All Audio Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Engine Status Banner
            AudioEngineStatusCard(audioSettings = audioSettings)

            // 1. Playback Section
            PlaybackSectionCard(
                audioSettings = audioSettings,
                viewModel = viewModel,
                onShowHelp = { activeHelpDialog = it },
                onReset = {
                    viewModel.resetPlaybackSettings()
                    showFeedback("Playback settings reset to default")
                }
            )

            // 2. Loudness Section (ReplayGain & LUFS Normalization)
            LoudnessSectionCard(
                audioSettings = audioSettings,
                viewModel = viewModel,
                onShowHelp = { activeHelpDialog = it },
                onResetReplayGain = {
                    viewModel.resetReplayGain()
                    showFeedback("ReplayGain reset to default")
                },
                onResetLoudness = {
                    viewModel.resetLoudnessNormalization()
                    showFeedback("Loudness normalization reset to default")
                }
            )

            // 3. Equalizer Section
            EqualizerSectionCard(
                audioSettings = audioSettings,
                viewModel = viewModel,
                onShowHelp = { activeHelpDialog = it },
                onNavigateToEqualizer = onNavigateToEqualizer,
                onReset = {
                    viewModel.resetEqualizer()
                    showFeedback("Equalizer reset to default (flat)")
                }
            )

            // 4. Bass & Effects Section
            BassAndEffectsSectionCard(
                audioSettings = audioSettings,
                viewModel = viewModel,
                onShowHelp = { activeHelpDialog = it },
                onReset = {
                    viewModel.resetBassAndEffects()
                    showFeedback("Bass Boost & Virtualizer reset to default")
                }
            )

            // 5. Safety & Limiter Section
            SafetySectionCard(
                audioSettings = audioSettings,
                viewModel = viewModel,
                onShowHelp = { activeHelpDialog = it },
                onReset = {
                    viewModel.resetLimiter()
                    showFeedback("Safety & Limiter reset to default")
                }
            )

            // 6. Advanced & System Control Panel Section
            AdvancedSectionCard(
                onViewDiagnostics = {
                    viewModel.loadAudioDiagnostics()
                    showDiagnosticsDialog = true
                },
                onLaunchSystemPanel = {
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
                onResetAll = { showResetAllDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ENGINE STATUS BANNER

@Composable
private fun AudioEngineStatusCard(audioSettings: AudioSettings) {
    val activeDSPs = buildList {
        if (audioSettings.equalizerEnabled) add("10-Band EQ (${audioSettings.equalizerPreset ?: "Custom"})")
        if (audioSettings.bassBoostEnabled && audioSettings.bassBoostStrength > 0) add("Bass Boost (${audioSettings.bassBoostStrength / 10}%)")
        if (audioSettings.virtualizerEnabled && audioSettings.virtualizerStrength > 0) add("Virtualizer (${audioSettings.virtualizerStrength / 10}%)")
        if (audioSettings.replayGainEnabled && audioSettings.replayGainMode != ReplayGainMode.OFF) add("ReplayGain (${audioSettings.replayGainMode.name})")
        if (audioSettings.loudnessNormalizationEnabled) add("LUFS Normalization (${audioSettings.targetLufs} LUFS)")
        if (audioSettings.limiterEnabled) add("Peak Limiter (${audioSettings.limiterThresholdDb} dB)")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (activeDSPs.isEmpty()) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (activeDSPs.isEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (activeDSPs.isEmpty()) Icons.Default.GraphicEq else Icons.Default.Tune,
                    contentDescription = null,
                    tint = if (activeDSPs.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (activeDSPs.isEmpty()) "Bit-Perfect Transparent Mode" else "${activeDSPs.size} DSP Modules Active",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (activeDSPs.isEmpty()) {
                        "Zero audio alteration. Studio-pure playback directly from MediaStore source."
                    } else {
                        activeDSPs.joinToString(", ")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// 1. PLAYBACK SECTION

@Composable
private fun PlaybackSectionCard(
    audioSettings: AudioSettings,
    viewModel: SettingsViewModel,
    onShowHelp: (AudioFeatureHelp) -> Unit,
    onReset: () -> Unit
) {
    SettingsCard(
        title = "Playback",
        icon = Icons.Default.PlayArrow
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Master Volume
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Master Volume",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "${(audioSettings.masterVolume * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = audioSettings.masterVolume,
                    onValueChange = { viewModel.setMasterVolume(it) },
                    valueRange = 0.0f..1.0f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Gapless Playback
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
                            text = "Gapless Playback",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Eliminates silence between consecutive tracks for seamless albums and live recordings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onShowHelp(AudioFeatureHelp.GAPLESS_PLAYBACK) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Learn about Gapless Playback",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = audioSettings.gaplessPlaybackEnabled,
                    onCheckedChange = { viewModel.setGaplessPlaybackEnabled(it) }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Crossfade
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
                            text = if (audioSettings.crossfadeEnabled) "Crossfade Active" else "Crossfade Disabled",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Smoothly blends the ending track into the beginning of the next track.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onShowHelp(AudioFeatureHelp.CROSSFADE) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Learn about Crossfade",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = audioSettings.crossfadeEnabled,
                    onCheckedChange = { viewModel.setCrossfadeEnabled(it) }
                )
            }

            AnimatedVisibility(
                visible = audioSettings.crossfadeEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Crossfade Duration",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${audioSettings.crossfadeDurationMs / 1000} seconds",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = (audioSettings.crossfadeDurationMs / 1000).toFloat(),
                        onValueChange = { viewModel.setCrossfadeDurationMs((it * 1000).toInt()) },
                        valueRange = 1.0f..20.0f,
                        steps = 18,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Smooth Audio Dimming (Audio Fade In / Fade Out)
            val audioFadeInEnabled by viewModel.audioFadeInEnabled.collectAsState()
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
                            text = if (audioFadeInEnabled) "Smooth Audio Dimming Active" else "Smooth Audio Dimming Disabled",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Gradually ramps volume in and out when playing, pausing, or skipping tracks to eliminate abrupt cuts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onShowHelp(AudioFeatureHelp.SMOOTH_DIMMING) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Learn about Smooth Audio Dimming",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = audioFadeInEnabled,
                    onCheckedChange = { viewModel.setAudioFadeInEnabled(it) }
                )
            }

            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset Playback")
            }
        }
    }
}

// 2. LOUDNESS SECTION

@Composable
private fun LoudnessSectionCard(
    audioSettings: AudioSettings,
    viewModel: SettingsViewModel,
    onShowHelp: (AudioFeatureHelp) -> Unit,
    onResetReplayGain: () -> Unit,
    onResetLoudness: () -> Unit
) {
    SettingsCard(
        title = "Loudness & Normalization",
        icon = Icons.Default.GraphicEq
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ReplayGain Subsystem
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
                            text = if (audioSettings.replayGainEnabled) "ReplayGain Active" else "ReplayGain Disabled",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Applies loudness tags (track/album) without dynamic compression.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onShowHelp(AudioFeatureHelp.REPLAY_GAIN) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Learn about ReplayGain",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = audioSettings.replayGainEnabled,
                    onCheckedChange = { viewModel.setReplayGainEnabled(it) }
                )
            }

            AnimatedVisibility(
                visible = audioSettings.replayGainEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ReplayGain Mode",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = audioSettings.replayGainMode == ReplayGainMode.TRACK,
                            onClick = { viewModel.setReplayGainMode(ReplayGainMode.TRACK) },
                            label = { Text("Track Mode") },
                            leadingIcon = if (audioSettings.replayGainMode == ReplayGainMode.TRACK) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                        FilterChip(
                            selected = audioSettings.replayGainMode == ReplayGainMode.ALBUM,
                            onClick = { viewModel.setReplayGainMode(ReplayGainMode.ALBUM) },
                            label = { Text("Album Mode") },
                            leadingIcon = if (audioSettings.replayGainMode == ReplayGainMode.ALBUM) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("ReplayGain Preamp", style = MaterialTheme.typography.bodySmall)
                                IconButton(
                                    onClick = { onShowHelp(AudioFeatureHelp.PREAMP) },
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
                                text = String.format(Locale.US, "%+.1f dB", audioSettings.replayGainPreampDb),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = audioSettings.replayGainPreampDb,
                            onValueChange = { viewModel.setReplayGainPreampDb(((it * 2).roundToInt() / 2f).coerceIn(-12f, 12f)) },
                            valueRange = -12.0f..12.0f,
                            steps = 47,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onResetReplayGain) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reset ReplayGain", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // LUFS Loudness Normalization Subsystem
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
                            text = if (audioSettings.loudnessNormalizationEnabled) "LUFS Normalization Active" else "LUFS Normalization Disabled",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Pure linear volume scaling matching your perceptual loudness target.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onShowHelp(AudioFeatureHelp.LOUDNESS_NORMALIZATION) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Learn about Loudness Normalization",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = audioSettings.loudnessNormalizationEnabled,
                    onCheckedChange = { viewModel.setLoudnessNormalizationEnabled(it) }
                )
            }

            AnimatedVisibility(
                visible = audioSettings.loudnessNormalizationEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Loudness Target Preset",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LoudnessTarget.values().forEach { target ->
                            val isSelected = target.lufs == audioSettings.targetLufs
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setTargetLufs(target.lufs) },
                                label = { Text(target.title) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Target Level", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = String.format(Locale.US, "%.1f LUFS", audioSettings.targetLufs),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = audioSettings.targetLufs,
                            onValueChange = { viewModel.setTargetLufs(((it * 2).roundToInt() / 2f).coerceIn(-24f, -8f)) },
                            valueRange = -24.0f..-8.0f,
                            steps = 31,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onResetLoudness) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reset Loudness", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

// 3. EQUALIZER SECTION

@Composable
private fun EqualizerSectionCard(
    audioSettings: AudioSettings,
    viewModel: SettingsViewModel,
    onShowHelp: (AudioFeatureHelp) -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onReset: () -> Unit
) {
    SettingsCard(
        title = "10-Band Graphic Equalizer",
        icon = Icons.Default.Equalizer
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Master EQ Switch
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
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (audioSettings.equalizerEnabled) {
                                "Preset: ${audioSettings.equalizerPreset ?: "Custom"}"
                            } else {
                                "100% Bit-Perfect Transparent (Zero DSP active)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onShowHelp(AudioFeatureHelp.EQUALIZER) },
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

            AnimatedVisibility(
                visible = audioSettings.equalizerEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Presets Selection Chips
                    Text(
                        text = "Presets",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        EqualizerPresets.ALL_PRESETS.forEach { preset ->
                            val isSelected = audioSettings.equalizerPreset == preset.name
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectEqualizerPreset(preset.name) },
                                label = { Text(preset.name) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }

                    // Preamp Slider
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("EQ Preamp", style = MaterialTheme.typography.bodySmall)
                                IconButton(
                                    onClick = { onShowHelp(AudioFeatureHelp.PREAMP) },
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
                                text = String.format(Locale.US, "%+.1f dB", audioSettings.equalizerPreampDb),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = audioSettings.equalizerPreampDb,
                            onValueChange = { viewModel.setEqualizerPreampDb(((it * 2).roundToInt() / 2f).coerceIn(-12f, 12f)) },
                            valueRange = -12.0f..12.0f,
                            steps = 47,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 10 Frequency Band Sliders
                    Text(
                        text = "Frequency Bands",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )

                    val bandGains = audioSettings.equalizerBandGains.ifEmpty { EqualizerPresets.defaultBandGains() }
                    bandGains.take(10).forEachIndexed { index, gain ->
                        val freqLabel = EQ_FREQUENCIES.getOrElse(index) { "Band ${index + 1}" }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = freqLabel,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(54.dp)
                            )
                            Slider(
                                value = gain,
                                onValueChange = {
                                    val snapped = ((it * 2).roundToInt() / 2f).coerceIn(-12f, 12f)
                                    viewModel.updateEqualizerBand(index, snapped)
                                },
                                valueRange = -12.0f..12.0f,
                                steps = 47,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = String.format(Locale.US, "%+.1f dB", gain),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(54.dp),
                                color = if (gain != 0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onNavigateToEqualizer) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fullscreen EQ")
                }

                OutlinedButton(onClick = onReset) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset EQ")
                }
            }
        }
    }
}

// 4. BASS & EFFECTS SECTION

@Composable
private fun BassAndEffectsSectionCard(
    audioSettings: AudioSettings,
    viewModel: SettingsViewModel,
    onShowHelp: (AudioFeatureHelp) -> Unit,
    onReset: () -> Unit
) {
    SettingsCard(
        title = "Bass & Audio Effects",
        icon = Icons.Default.SurroundSound
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Bass Boost
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
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Hardware-accelerated low frequency amplification.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onShowHelp(AudioFeatureHelp.BASS_BOOST) },
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
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Bass Strength", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${audioSettings.bassBoostStrength / 10}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = audioSettings.bassBoostStrength.toFloat(),
                        onValueChange = { viewModel.setBassBoostStrength(it.roundToInt()) },
                        valueRange = 0f..1000f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Virtualizer
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
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Spatial soundstage widening for headphones and stereo speakers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onShowHelp(AudioFeatureHelp.VIRTUALIZER) },
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
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Spatial Strength", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${audioSettings.virtualizerStrength / 10}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = audioSettings.virtualizerStrength.toFloat(),
                        onValueChange = { viewModel.setVirtualizerStrength(it.roundToInt()) },
                        valueRange = 0f..1000f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset Effects")
            }
        }
    }
}

// 5. SAFETY & LIMITER SECTION

@Composable
private fun SafetySectionCard(
    audioSettings: AudioSettings,
    viewModel: SettingsViewModel,
    onShowHelp: (AudioFeatureHelp) -> Unit,
    onReset: () -> Unit
) {
    SettingsCard(
        title = "Safety & Headroom Protection",
        icon = Icons.Default.Security
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Stage 1: Technical Anti-Clipping Headroom Pre-attenuation
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
                            text = "Automatic Anti-Clipping Protection",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Mathematically prevents clipping across active EQ/Bass Boost without dynamic pumping or distortion.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onShowHelp(AudioFeatureHelp.PREVENT_CLIPPING) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Learn about Prevent Clipping",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = audioSettings.preventClipping,
                    onCheckedChange = { viewModel.setPreventClipping(it) }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Stage 2: Peak Brickwall Limiter
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
                            text = if (audioSettings.limiterEnabled) "Peak Limiter Active" else "Peak Limiter Disabled",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Enforces a hard ceiling limiter on dynamic audio peaks (Android 9.0+).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onShowHelp(AudioFeatureHelp.LIMITER) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Learn about Limiter",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
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
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Limiter Ceiling Threshold", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = String.format(Locale.US, "%.1f dB", audioSettings.limiterThresholdDb),
                            style = MaterialTheme.typography.bodySmall,
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
                onClick = onReset,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset Safety")
            }
        }
    }
}

// 6. ADVANCED & SYSTEM CONTROL PANEL SECTION

@Composable
private fun AdvancedSectionCard(
    onViewDiagnostics: () -> Unit,
    onLaunchSystemPanel: () -> Unit,
    onResetAll: () -> Unit
) {
    SettingsCard(
        title = "Advanced & Diagnostics",
        icon = Icons.Default.Settings
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Audio Diagnostics / Info Button
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Audio Stream Diagnostics",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Inspect exact source file tags (codec, bitrate, sample rate, bit depth) vs. output AudioTrack pipeline and active DSP modules.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onViewDiagnostics,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Audio Information & Diagnostics")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Text(
                text = "Audio Pipeline Architecture",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "MediaStore Source (content://) → ExoPlayer (Media3) → VedTune AudioEngine → Android AudioTrack Output",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "VedTune uses non-destructive linear processing with zero multi-band compression or fake volume boosters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "System Equalizer",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Launch external OEM/system audio control panel if installed by your device manufacturer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onLaunchSystemPanel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Launch System Control Panel")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Master Reset All Audio Settings Button
            FilledTonalButton(
                onClick = onResetAll,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset All Audio Settings")
            }
        }
    }
}
