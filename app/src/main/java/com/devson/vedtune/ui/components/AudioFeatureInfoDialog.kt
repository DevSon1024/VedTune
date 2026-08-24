package com.devson.vedtune.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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
        whatItDoes = "ReplayGain adjusts playback gain so songs with different mastering levels sound consistent in volume. It does NOT improve original recording quality or restore lost details—it only changes playback gain. ReplayGain does not compress the music and preserves the original dynamic range.\n\n• Track Gain: Normalizes each song independently.\n• Album Gain: Normalizes an entire album while preserving the relative loudness differences between tracks.\n• Peak Info: Used to safely prevent clipping when gain is boosted.",
        whenToUse = "Enable Track Gain for mixed playlists. Enable Album Gain when listening to complete albums.",
        tradeOffs = "Requires audio files to have ReplayGain tags. If tags are missing, audio plays at standard unity gain (0 dB change).",
        recommended = "Enable with Album Gain for albums or Track Gain for playlists. Keep Prevent Clipping enabled."
    ),

    LOUDNESS_NORMALIZATION(
        title = "Loudness Normalization",
        icon = Icons.Default.Speed,
        whatItDoes = "Loudness normalization adjusts playback gain to bring songs to a standardized perceptual loudness target (measured in LUFS). Unlike dynamic range compressors, VedTune applies clean, non-destructive linear volume scaling without squashing transients or pumping dynamics.\n\nLUFS is an international standard for perceived loudness. For example, a track mastered at -8 LUFS sounds significantly louder than one at -18 LUFS when played at the same digital level.",
        whenToUse = "Enable when playing playlists containing songs from different eras or genres with widely varying mastering loudness.",
        tradeOffs = "Modulates volume output. Leave disabled if you prefer 100% bit-perfect, untouched source playback.",
        recommended = "Use -14.0 LUFS (Streaming Standard) or -18.0 LUFS (AES Hi-Fi Standard). Keep disabled for pure bit-perfect listening."
    ),

    EQUALIZER(
        title = "Equalizer",
        icon = Icons.Default.Equalizer,
        whatItDoes = "An equalizer adjusts the relative volume of specific frequency ranges (Bass: ~20–250 Hz, Midrange: ~250 Hz–4 kHz, Treble: ~4–20 kHz). EQ does NOT restore missing audio information—it shapes tonal balance to match your headphones, speakers, room acoustics, or personal preference.\n\n'Flat' represents completely neutral frequency response with zero alteration.",
        whenToUse = "Use to correct headphones with overly bright treble or weak bass, or to customize sound to your taste.",
        tradeOffs = "Large boosts can push signals above 0 dBFS and cause distortion or clipping unless headroom/preamp is adjusted.",
        recommended = "Make small adjustments (±1 to ±3 dB). Lower preamp when applying positive boosts to preserve headroom."
    ),

    BASS_BOOST(
        title = "Bass Boost",
        icon = Icons.Default.Tune,
        whatItDoes = "Bass Boost increases the energy of low-frequency audio content. It does not improve source recording fidelity, but modifies the sound signature to user preference.",
        whenToUse = "Useful when headphones or small speakers sound thin, or when you prefer a warmer, punchier low-end.",
        tradeOffs = "Excessive bass boost can reduce available headroom, cause clipping distortion, muddy the midrange, and overpower vocals.",
        recommended = "Start with low strength (10%–30%) and increase gradually. Keep OFF for accurate studio playback."
    ),

    VIRTUALIZER(
        title = "Virtualizer",
        icon = Icons.Default.SurroundSound,
        whatItDoes = "Virtualizer applies spatial acoustic processing to create a wider, more expansive soundstage presentation, especially when listening with headphones or stereo speakers.",
        whenToUse = "Enable if you enjoy an open, immersive 3D-like soundstage.",
        tradeOffs = "This is a synthetic sound effect, not a quality enhancement. It modifies original stereo imaging and phase relationships.",
        recommended = "Keep OFF for authentic, natural stereo imaging. Enable only if you prefer spatial expansion."
    ),

    LIMITER(
        title = "Peak Limiter",
        icon = Icons.Default.Security,
        whatItDoes = "A peak limiter enforces a hard digital ceiling threshold to prevent instantaneous audio peaks from exceeding maximum limits (0 dBFS). This protects against clipping distortion caused by active EQ boosts, Bass Boost, or positive ReplayGain.\n\nA limiter controls transient peaks, whereas normalization controls average perceived loudness.",
        whenToUse = "Enable when using aggressive EQ/Bass boosts or when you need guaranteed peak overload protection.",
        tradeOffs = "Heavy limiting can reduce dynamic punch and introduce compression artifacts on loud peaks.",
        recommended = "Keep disabled during normal transparent playback. Enable with a -0.5 dB threshold when heavy DSP boosts are active."
    ),

    PREAMP(
        title = "Preamp",
        icon = Icons.AutoMirrored.Filled.VolumeUp,
        whatItDoes = "Preamp modifies the overall signal gain before entering the equalizer and downstream effects. Positive values increase volume; negative values attenuate the signal to create digital headroom.\n\nFor example, if you boost bass frequencies by +5 dB, setting Preamp to -5 dB creates 5 dB of headroom, preventing digital clipping entirely.",
        whenToUse = "Adjust when applying positive EQ boosts or when matching gain between different audio sources.",
        tradeOffs = "Positive preamp can cause clipping if peaks exceed 0 dBFS. Negative preamp reduces overall playback loudness.",
        recommended = "Keep at 0.0 dB unless compensating for positive EQ boosts."
    ),

    PREVENT_CLIPPING(
        title = "Prevent Clipping",
        icon = Icons.Default.Security,
        whatItDoes = "Digital audio cannot represent signals above 0 dBFS without severe harshness, crackling, and distortion. Prevent Clipping is an automatic safety algorithm that calculates potential peak overloads across active EQ and ReplayGain boosts, applying exact pre-attenuation to maintain pristine, distortion-free playback without dynamic pumping.",
        whenToUse = "Keep enabled whenever using Equalizer, Bass Boost, or ReplayGain.",
        tradeOffs = "Slightly lowers master gain during high DSP boosts to protect audio clarity.",
        recommended = "Keep enabled at all times as your primary distortion safety net."
    ),

    GAPLESS_PLAYBACK(
        title = "Gapless Playback",
        icon = Icons.Default.Speed,
        whatItDoes = "Gapless playback eliminates silence or buffering delays between consecutive tracks, allowing seamless audio continuity. This is essential for live concerts, DJ continuous mixes, concept albums, and classical symphonies.",
        whenToUse = "Recommended for all music listening.",
        tradeOffs = "None. Gapless playback preserves authentic album sequencing without modifying the audio data.",
        recommended = "Keep enabled."
    ),

    CROSSFADE(
        title = "Crossfade",
        icon = Icons.Default.Tune,
        whatItDoes = "Crossfade smoothly fades out the ending track while simultaneously fading in the upcoming track over a chosen duration (1s to 20s), eliminating abrupt stops between unrelated songs.",
        whenToUse = "Ideal for party playlists, workouts, and continuous background music.",
        tradeOffs = "Intentionally overlaps songs. Not recommended for live albums, concept albums, or classical works where tracks transition naturally.",
        recommended = "Enable for mixed party playlists; disable for authentic album listening."
    )
}

@Composable
fun AudioFeatureInfoDialog(
    help: AudioFeatureHelp,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
                .heightIn(max = 680.dp)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header: Icon + Title + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = help.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
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
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Scrollable Body Content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Section 1: What it does
                    InfoSection(
                        title = "What it does",
                        body = help.whatItDoes
                    )

                    // Section 2: When to use it & Trade-offs
                    InfoSection(
                        title = "When to use it",
                        body = help.whenToUse
                    )

                    if (help.tradeOffs.isNotBlank()) {
                        InfoSection(
                            title = "Side effects & Trade-offs",
                            body = help.tradeOffs
                        )
                    }

                    // Section 3: Recommended
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Bottom Action
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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

@Composable
private fun InfoSection(
    title: String,
    body: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
