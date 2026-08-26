package com.devson.vedtune.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devson.vedtune.domain.model.AudioDiagnostics
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.spacing

@Composable
fun AudioDiagnosticsDialog(
    diagnostics: AudioDiagnostics,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(MaterialTheme.spacing.l),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(min = 280.dp, max = 560.dp)
                    .heightIn(max = 680.dp),
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
                    // Header: Title + Close Icon
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
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(VedTuneIconSizes.Medium)
                                    )
                                }
                            }
                            Text(
                                text = "Audio Diagnostics",
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

                    // Scrollable Diagnostics Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)
                    ) {
                        // Source Section
                        DiagnosticsCategoryCard(
                            title = "Source Track",
                            icon = Icons.Default.MusicNote
                        ) {
                            DiagnosticRow("Container / Format", diagnostics.source.container)
                            DiagnosticRow("Codec", diagnostics.source.codec)
                            DiagnosticRow("Sample Rate", diagnostics.source.sampleRate)
                            DiagnosticRow("Bit Depth", diagnostics.source.bitDepth)
                            DiagnosticRow("Channels", diagnostics.source.channels)
                            DiagnosticRow("Bitrate", diagnostics.source.bitrate)
                            DiagnosticRow("File Size", diagnostics.source.fileSizeFormatted)
                        }

                        // Processing Section
                        DiagnosticsCategoryCard(
                            title = "Signal Processing (DSP)",
                            icon = Icons.Default.Tune
                        ) {
                            DiagnosticRow(
                                "Bit-Perfect Mode",
                                if (diagnostics.output.isBitPerfectTransparent) "ACTIVE (Untouched)" else "Inactive"
                            )
                            DiagnosticRow("ReplayGain Applied", diagnostics.dsp.replayGain)
                            DiagnosticRow("Loudness Normalization", diagnostics.dsp.loudnessNormalization)
                            DiagnosticRow("Equalizer Active", diagnostics.dsp.equalizer)
                            DiagnosticRow("Bass Boost Active", diagnostics.dsp.bassBoost)
                            DiagnosticRow("Virtualizer Active", diagnostics.dsp.virtualizer)
                            DiagnosticRow("Limiter Active", diagnostics.dsp.limiter)
                            DiagnosticRow("Active DSP Count", "${diagnostics.dsp.activeDspCount}")
                        }

                        // Output Section
                        DiagnosticsCategoryCard(
                            title = "Hardware Output",
                            icon = Icons.Default.Speaker
                        ) {
                            DiagnosticRow("Decoder", diagnostics.output.decoderName)
                            DiagnosticRow("Output Sample Rate", diagnostics.output.outputSampleRate)
                            DiagnosticRow("Output Channels", diagnostics.output.outputChannels)
                            DiagnosticRow("Master Volume", diagnostics.output.masterVolume)
                            DiagnosticRow("Audio Session ID", "${diagnostics.output.audioSessionId}")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Actions Row: Copy + Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val text = buildDiagnosticsReport(diagnostics)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("VedTune Diagnostics", text)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Diagnostics copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = VedTuneShapeTokens.Pill
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(VedTuneIconSizes.Small)
                            )
                            Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                            Text("Copy Report")
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = VedTuneShapeTokens.Pill
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsCategoryCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        shape = VedTuneShapeTokens.Medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.m),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(VedTuneIconSizes.Small)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            content()
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun buildDiagnosticsReport(d: AudioDiagnostics): String {
    return """
        === VedTune Audio Diagnostics ===
        [Source Track]
        • Title: ${d.source.title}
        • Artist: ${d.source.artist}
        • Container: ${d.source.container}
        • Codec: ${d.source.codec}
        • Sample Rate: ${d.source.sampleRate}
        • Bit Depth: ${d.source.bitDepth}
        • Channels: ${d.source.channels}
        • Bitrate: ${d.source.bitrate}
        • File Size: ${d.source.fileSizeFormatted}

        [Signal Processing (DSP)]
        • Bit-Perfect Mode: ${if (d.output.isBitPerfectTransparent) "YES (Untouched)" else "NO (DSP Active)"}
        • ReplayGain: ${d.dsp.replayGain}
        • Loudness Normalization: ${d.dsp.loudnessNormalization}
        • Equalizer: ${d.dsp.equalizer}
        • Bass Boost: ${d.dsp.bassBoost}
        • Virtualizer: ${d.dsp.virtualizer}
        • Limiter: ${d.dsp.limiter}
        • Active DSP Count: ${d.dsp.activeDspCount}

        [Hardware Output]
        • Decoder: ${d.output.decoderName}
        • Sample Rate: ${d.output.outputSampleRate}
        • Channels: ${d.output.outputChannels}
        • Master Volume: ${d.output.masterVolume}
        • Session ID: ${d.output.audioSessionId}
    """.trimIndent()
}
