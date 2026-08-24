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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devson.vedtune.domain.model.AudioDiagnostics

@Composable
fun AudioDiagnosticsDialog(
    diagnostics: AudioDiagnostics,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 560.dp)
                .heightIn(max = 700.dp)
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
                // Header: Title + Close Icon
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
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Text(
                            text = "Audio Information",
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

                // Scrollable Diagnostic Items
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Current Track Metadata
                    DiagnosticsGroupCard(
                        title = "Current Track",
                        icon = Icons.Default.MusicNote
                    ) {
                        DiagnosticItem("Title", diagnostics.source.title)
                        DiagnosticItem("Artist", diagnostics.source.artist)
                        DiagnosticItem("Album", diagnostics.source.album)
                    }

                    // 2. Source File Details
                    DiagnosticsGroupCard(
                        title = "Source File Format",
                        icon = Icons.Default.GraphicEq
                    ) {
                        DiagnosticItem("Format", diagnostics.source.container)
                        DiagnosticItem("Codec", diagnostics.source.codec)
                        DiagnosticItem("Bitrate", diagnostics.source.bitrate)
                        DiagnosticItem("Sample Rate", diagnostics.source.sampleRate)
                        DiagnosticItem("Bit Depth", diagnostics.source.bitDepth)
                        DiagnosticItem("Channels", diagnostics.source.channels)
                        DiagnosticItem("Duration", diagnostics.source.durationFormatted)
                        DiagnosticItem("File Size", diagnostics.source.fileSizeFormatted)
                    }

                    // 3. Playback / Decoder
                    DiagnosticsGroupCard(
                        title = "Playback Stream",
                        icon = Icons.Default.Speaker
                    ) {
                        DiagnosticItem("Decoder", diagnostics.output.decoderName)
                        DiagnosticItem("Audio Session ID", if (diagnostics.output.audioSessionId > 0) diagnostics.output.audioSessionId.toString() else "System Default")
                        DiagnosticItem("Output Sample Rate", diagnostics.output.outputSampleRate)
                        DiagnosticItem("Output Channels", diagnostics.output.outputChannels)
                        DiagnosticItem("Master Volume", diagnostics.output.masterVolume)
                        DiagnosticItem(
                            label = "Signal Integrity",
                            value = if (diagnostics.output.isBitPerfectTransparent) "Bit-Perfect Transparent" else "DSP Active / Modified",
                            isHighlighted = diagnostics.output.isBitPerfectTransparent
                        )
                    }

                    // 4. Processing / DSP State
                    DiagnosticsGroupCard(
                        title = "Processing (DSP)",
                        icon = Icons.Default.Tune
                    ) {
                        DiagnosticItem("ReplayGain", diagnostics.dsp.replayGain)
                        DiagnosticItem("Equalizer", diagnostics.dsp.equalizer)
                        DiagnosticItem("Bass Boost", diagnostics.dsp.bassBoost)
                        DiagnosticItem("Virtualizer", diagnostics.dsp.virtualizer)
                        DiagnosticItem("Normalizer (LUFS)", diagnostics.dsp.loudnessNormalization)
                        DiagnosticItem("Limiter & Safety", diagnostics.dsp.limiter)
                        DiagnosticItem(
                            label = "Active Modules",
                            value = if (diagnostics.dsp.activeDspCount == 0) "0 (Bit-Perfect Mode)" else "${diagnostics.dsp.activeDspCount} Active",
                            isHighlighted = diagnostics.dsp.activeDspCount == 0
                        )
                    }

                    // Transparency Notice
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Source format displays file tags. Output format represents the active AudioTrack stream. High source sample rates depend on hardware audio HAL support.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Bottom Buttons: Copy Report & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { copyDiagnosticsToClipboard(context, diagnostics) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Report")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsGroupCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            content()
        }
    }
}

@Composable
private fun DiagnosticItem(
    label: String,
    value: String,
    isHighlighted: Boolean = false
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.58f)
        )
    }
}

private fun copyDiagnosticsToClipboard(context: Context, diagnostics: AudioDiagnostics) {
    val report = buildString {
        appendLine("=== VedTune Audio Diagnostics ===")
        appendLine()
        appendLine("[CURRENT TRACK]")
        appendLine("Title:         ${diagnostics.source.title}")
        appendLine("Artist:        ${diagnostics.source.artist}")
        appendLine("Album:         ${diagnostics.source.album}")
        appendLine()
        appendLine("[SOURCE FORMAT]")
        appendLine("Container:     ${diagnostics.source.container}")
        appendLine("Codec:         ${diagnostics.source.codec}")
        appendLine("Bitrate:       ${diagnostics.source.bitrate}")
        appendLine("Sample Rate:   ${diagnostics.source.sampleRate}")
        appendLine("Bit Depth:     ${diagnostics.source.bitDepth}")
        appendLine("Channels:      ${diagnostics.source.channels}")
        appendLine("Duration:      ${diagnostics.source.durationFormatted}")
        appendLine("File Size:     ${diagnostics.source.fileSizeFormatted}")
        appendLine()
        appendLine("[PLAYBACK / DECODER]")
        appendLine("Decoder:       ${diagnostics.output.decoderName}")
        appendLine("Session ID:    ${diagnostics.output.audioSessionId}")
        appendLine("Output Rate:   ${diagnostics.output.outputSampleRate}")
        appendLine("Output Ch:     ${diagnostics.output.outputChannels}")
        appendLine("Master Volume: ${diagnostics.output.masterVolume}")
        appendLine("Bit-Perfect:   ${if (diagnostics.output.isBitPerfectTransparent) "YES" else "NO"}")
        appendLine()
        appendLine("[PROCESSING / DSP]")
        appendLine("ReplayGain:    ${diagnostics.dsp.replayGain}")
        appendLine("Equalizer:     ${diagnostics.dsp.equalizer}")
        appendLine("Bass Boost:    ${diagnostics.dsp.bassBoost}")
        appendLine("Virtualizer:   ${diagnostics.dsp.virtualizer}")
        appendLine("Normalizer:    ${diagnostics.dsp.loudnessNormalization}")
        appendLine("Limiter:       ${diagnostics.dsp.limiter}")
        appendLine("Active DSPs:   ${diagnostics.dsp.activeDspCount}")
    }

    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText("VedTune Audio Diagnostics", report)
    clipboard?.setPrimaryClip(clip)
    Toast.makeText(context, "Diagnostics report copied to clipboard", Toast.LENGTH_SHORT).show()
}
