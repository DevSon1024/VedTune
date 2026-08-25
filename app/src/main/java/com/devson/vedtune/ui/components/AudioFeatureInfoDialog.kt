package com.devson.vedtune.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.spacing

enum class AudioFeatureHelp(
    val title: String,
    val icon: ImageVector,
    val whatItDoes: String,
    val whenToUse: String,
    val tradeOffs: String,
    val recommended: String
) {
    REPLAY_GAIN(
        title = "ReplayGain",
        icon = Icons.Default.GraphicEq,
        whatItDoes = "ReplayGain adjusts playback gain so songs with different mastering levels sound consistent in volume. It does NOT compress audio or alter dynamic range—it calculates linear gain offsets based on perceived loudness.\n\n• Track Gain: Normalizes each song independently.\n• Album Gain: Normalizes an entire album while preserving original intra-album volume differences.\n• Peak Info: Used to safely prevent digital clipping when gain is boosted.",
        whenToUse = "Enable Track Gain for mixed playlists. Enable Album Gain when listening to complete concept or live albums.",
        tradeOffs = "Requires audio files to contain ReplayGain metadata tags. Untagged tracks play at 0.0 dB unity gain.",
        recommended = "Enable with Album Gain for albums or Track Gain for playlists. Keep Prevent Clipping enabled."
    ),

    LOUDNESS_NORMALIZATION(
        title = "Loudness Normalization",
        icon = Icons.Default.Speed,
        whatItDoes = "Loudness Normalization aligns track volume to an international standard loudness target (measured in LUFS). VedTune applies clean, non-destructive linear volume adjustments without dynamic compression or transient pumping.\n\nFor example, modern pop tracks mastered at -8 LUFS are leveled smoothly with acoustic recordings mastered at -18 LUFS.",
        whenToUse = "Enable when playing mixed collections spanning different eras and genres with large volume disparities.",
        tradeOffs = "Scales volume levels. If you prefer 100% untouched bit-perfect output, leave disabled.",
        recommended = "Use -14.0 LUFS (Streaming Standard) or -18.0 LUFS (AES Hi-Fi Standard)."
    ),

    EQUALIZER(
        title = "Equalizer",
        icon = Icons.Default.Equalizer,
        whatItDoes = "An equalizer adjusts the relative energy of specific audio frequency bands (Bass: ~20–250 Hz, Midrange: ~250 Hz–4 kHz, Treble: ~4–20 kHz). EQ shapes tonal balance to complement your headphones, speakers, or room acoustics.\n\n'Flat' represents a completely neutral frequency curve with zero coloration.",
        whenToUse = "Use to tune headphones with harsh treble or weak bass, or to customize sound to your preference.",
        tradeOffs = "Large positive boosts can exceed 0 dBFS and introduce clipping distortion unless headroom/preamp is compensated.",
        recommended = "Use subtle adjustments (±1 to ±3 dB). Lower preamp when applying positive boosts."
    ),

    BASS_BOOST(
        title = "Bass Boost",
        icon = Icons.Default.Tune,
        whatItDoes = "Bass Boost increases low-frequency acoustic energy to produce deeper, warmer bass response.",
        whenToUse = "Beneficial when listening with thin-sounding earphones or small portable speakers.",
        tradeOffs = "Excessive boost can muddy vocal midrange and cause digital clipping if output headroom is insufficient.",
        recommended = "Keep at low strength (10%–30%). Keep disabled for neutral studio monitoring."
    ),

    VIRTUALIZER(
        title = "Virtualizer",
        icon = Icons.Default.SurroundSound,
        whatItDoes = "Virtualizer applies spatial acoustic processing to simulate an expansive, three-dimensional soundstage presentation over headphones.",
        whenToUse = "Enable if you enjoy an open, immersive ambient soundfield.",
        tradeOffs = "Modifies original stereo panning and phase relationships.",
        recommended = "Keep disabled for authentic stereo mastering; enable for spatial expansion."
    ),

    LIMITER(
        title = "Peak Limiter",
        icon = Icons.Default.Security,
        whatItDoes = "A peak limiter establishes a digital ceiling threshold preventing transient audio peaks from exceeding 0 dBFS, shielding playback from harsh clipping distortion caused by active EQ or bass boosts.",
        whenToUse = "Enable when applying positive EQ boosts or heavy DSP processing.",
        tradeOffs = "Aggressive limiting can attenuate transient dynamic punch on loud peaks.",
        recommended = "Keep disabled during neutral playback; enable with -0.5 dB threshold when boosting frequencies."
    ),

    PREAMP(
        title = "Preamp",
        icon = Icons.AutoMirrored.Filled.VolumeUp,
        whatItDoes = "Preamp modifies the overall signal gain before entering the equalizer and downstream processing. Negative preamp creates digital headroom, preventing distortion when boosting EQ bands.",
        whenToUse = "Lower preamp when applying positive EQ boosts to ensure the total signal never exceeds 0 dBFS.",
        tradeOffs = "Negative preamp reduces overall volume; positive preamp can cause digital clipping.",
        recommended = "Keep at 0.0 dB unless compensating for active positive EQ boosts."
    ),

    PREVENT_CLIPPING(
        title = "Prevent Clipping",
        icon = Icons.Default.Security,
        whatItDoes = "Prevent Clipping automatically calculates potential peak overshoots across active ReplayGain and EQ settings, applying pre-attenuation to maintain pristine, distortion-free audio without pumping artifacts.",
        whenToUse = "Keep enabled whenever using Equalizer, Bass Boost, or ReplayGain.",
        tradeOffs = "Slightly reduces master gain during heavy boosts to protect clarity.",
        recommended = "Keep enabled at all times as your primary distortion safety net."
    ),

    GAPLESS_PLAYBACK(
        title = "Gapless Playback",
        icon = Icons.Default.Speed,
        whatItDoes = "Gapless playback eliminates silence or buffering pauses between consecutive tracks, allowing seamless continuity. This is essential for live concert recordings, DJ mixes, concept albums, and classical works.",
        whenToUse = "Recommended for all music listening.",
        tradeOffs = "None. Preserves original track sequencing without altering audio data.",
        recommended = "Keep enabled."
    ),

    CROSSFADE(
        title = "Crossfade",
        icon = Icons.Default.Tune,
        whatItDoes = "Crossfade smoothly fades out the ending song while fading in the next track over a chosen duration (1s to 20s), eliminating abrupt breaks between tracks.",
        whenToUse = "Great for workout sessions, parties, and casual playlist listening.",
        tradeOffs = "Overlaps tracks. Not recommended for live or concept albums where songs transition naturally.",
        recommended = "Enable for continuous party mixes; disable for album listening."
    ),

    SMOOTH_DIMMING(
        title = "Smooth Audio Dimming",
        icon = Icons.AutoMirrored.Filled.VolumeUp,
        whatItDoes = "Smooth Audio Dimming (Audio Fade-in/Fade-out) gently ramps playback volume during play, pause, and track skip actions instead of abruptly cutting the waveform, eliminating pops and clicks.",
        whenToUse = "Recommended for all audio output devices.",
        tradeOffs = "Adds a subtle micro-fade (approx. 50–100ms) on playback transitions.",
        recommended = "Keep enabled for smooth acoustic transitions."
    )
}

/**
 * Responsive, constraint-bounded Material 3 Information Dialog.
 * Solves intrinsic measurement collapse by using an explicit full-screen container
 * with bounded minWidth and maxWidth constraints.
 */
@Composable
fun AudioFeatureInfoDialog(
    help: AudioFeatureHelp,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        // Root container with explicit fill and center alignment to prevent 0-width measurement
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(MaterialTheme.spacing.l),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(min = 280.dp, max = 540.dp)
                    .heightIn(max = 660.dp),
                shape = VedTuneShapeTokens.Dialog,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)
                ) {
                    // Header: Icon + Title + Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = help.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(VedTuneIconSizes.Medium)
                                    )
                                }
                            }
                            Text(
                                text = help.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Scrollable Body Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)
                    ) {
                        // Section 1: What it does
                        InfoSection(
                            title = "What it does",
                            body = help.whatItDoes
                        )

                        // Section 2: When to use it
                        InfoSection(
                            title = "When to use it",
                            body = help.whenToUse
                        )

                        // Section 3: Trade-offs
                        if (help.tradeOffs.isNotBlank()) {
                            InfoSection(
                                title = "Side effects & Trade-offs",
                                body = help.tradeOffs
                            )
                        }

                        // Section 4: Recommendation Banner
                        Surface(
                            shape = VedTuneShapeTokens.Medium,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(MaterialTheme.spacing.m),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(VedTuneIconSizes.Small)
                                    )
                                    Text(
                                        text = "Recommendation",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = help.recommended,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Bottom Action Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = VedTuneShapeTokens.Pill,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Got it",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    body: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
