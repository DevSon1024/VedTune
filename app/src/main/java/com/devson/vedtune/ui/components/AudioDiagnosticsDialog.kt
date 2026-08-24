package com.devson.vedtune.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.AudioDiagnostics

@Composable
fun AudioDiagnosticsDialog(
    diagnostics: AudioDiagnostics,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Audio Information & Diagnostics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Source Format Card
                DiagnosticsGroupCard(
                    title = "Source Format (File)",
                    icon = Icons.Default.MusicNote
                ) {
                    DiagnosticItem("Title", diagnostics.source.title)
                    DiagnosticItem("Artist", diagnostics.source.artist)
                    DiagnosticItem("Album", diagnostics.source.album)
                    DiagnosticItem("Codec", diagnostics.source.codec)
                    DiagnosticItem("Container", diagnostics.source.container)
                    DiagnosticItem("Bitrate", diagnostics.source.bitrate)
                    DiagnosticItem("Sample Rate", diagnostics.source.sampleRate)
                    DiagnosticItem("Channels", diagnostics.source.channels)
                    DiagnosticItem("Bit Depth", diagnostics.source.bitDepth)
                    DiagnosticItem("Duration", diagnostics.source.durationFormatted)
                    DiagnosticItem("File Size", diagnostics.source.fileSizeFormatted)
                }

                // 2. Processing (DSP) Card
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

                // 3. Playback / Output Card
                DiagnosticsGroupCard(
                    title = "Playback & Output",
                    icon = Icons.Default.Speaker
                ) {
                    DiagnosticItem("Decoder", diagnostics.output.decoderName)
                    DiagnosticItem("Output Sample Rate", diagnostics.output.outputSampleRate)
                    DiagnosticItem("Output Channels", diagnostics.output.outputChannels)
                    DiagnosticItem("Master Volume", diagnostics.output.masterVolume)
                    DiagnosticItem("Audio Session ID", if (diagnostics.output.audioSessionId > 0) diagnostics.output.audioSessionId.toString() else "System Default")
                    DiagnosticItem(
                        label = "Signal Integrity",
                        value = if (diagnostics.output.isBitPerfectTransparent) "Bit-Perfect Transparent" else "Linear Scaled / DSP Active",
                        isHighlighted = diagnostics.output.isBitPerfectTransparent
                    )
                }

                // Transparency Notice
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Source format shows file tags. Output format reflects active AudioTrack stream. High source sample rates depend on your device's audio HAL.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = {
                    copyDiagnosticsToClipboard(context, diagnostics)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DiagnosticsGroupCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
        modifier = Modifier.fillMaxWidth(),
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
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun copyDiagnosticsToClipboard(context: Context, diagnostics: AudioDiagnostics) {
    val report = buildString {
        appendLine("=== VedTune Audio Diagnostics ===")
        appendLine()
        appendLine("[SOURCE FORMAT]")
        appendLine("Title:         ${diagnostics.source.title}")
        appendLine("Artist:        ${diagnostics.source.artist}")
        appendLine("Album:         ${diagnostics.source.album}")
        appendLine("Codec:         ${diagnostics.source.codec}")
        appendLine("Container:     ${diagnostics.source.container}")
        appendLine("Bitrate:       ${diagnostics.source.bitrate}")
        appendLine("Sample Rate:   ${diagnostics.source.sampleRate}")
        appendLine("Channels:      ${diagnostics.source.channels}")
        appendLine("Bit Depth:     ${diagnostics.source.bitDepth}")
        appendLine("Duration:      ${diagnostics.source.durationFormatted}")
        appendLine("File Size:     ${diagnostics.source.fileSizeFormatted}")
        appendLine()
        appendLine("[PROCESSING / DSP]")
        appendLine("ReplayGain:    ${diagnostics.dsp.replayGain}")
        appendLine("Equalizer:     ${diagnostics.dsp.equalizer}")
        appendLine("Bass Boost:    ${diagnostics.dsp.bassBoost}")
        appendLine("Virtualizer:   ${diagnostics.dsp.virtualizer}")
        appendLine("Normalizer:    ${diagnostics.dsp.loudnessNormalization}")
        appendLine("Limiter:       ${diagnostics.dsp.limiter}")
        appendLine("Active DSPs:   ${diagnostics.dsp.activeDspCount}")
        appendLine()
        appendLine("[OUTPUT / PLAYBACK]")
        appendLine("Decoder:       ${diagnostics.output.decoderName}")
        appendLine("Output Rate:   ${diagnostics.output.outputSampleRate}")
        appendLine("Output Ch:     ${diagnostics.output.outputChannels}")
        appendLine("Master Volume: ${diagnostics.output.masterVolume}")
        appendLine("Session ID:    ${diagnostics.output.audioSessionId}")
        appendLine("Bit-Perfect:   ${if (diagnostics.output.isBitPerfectTransparent) "YES" else "NO"}")
    }

    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText("VedTune Audio Diagnostics", report)
    clipboard?.setPrimaryClip(clip)
    Toast.makeText(context, "Diagnostics report copied to clipboard", Toast.LENGTH_SHORT).show()
}
