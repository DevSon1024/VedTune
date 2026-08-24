package com.devson.vedtune.player.engine.diagnostics

import android.content.Context
import com.devson.vedtune.data.metadata.AudioMetadataExtractor
import com.devson.vedtune.data.metadata.ExtractedMetadata
import com.devson.vedtune.di.IoDispatcher
import com.devson.vedtune.domain.model.AudioDiagnostics
import com.devson.vedtune.domain.model.AudioDspStatus
import com.devson.vedtune.domain.model.AudioPlaybackOutput
import com.devson.vedtune.domain.model.AudioSettings
import com.devson.vedtune.domain.model.AudioSourceFormat
import com.devson.vedtune.domain.model.ReplayGainMode
import com.devson.vedtune.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class AudioDiagnosticsProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val metadataExtractor: AudioMetadataExtractor,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    // In-memory cache for extracted source metadata by song ID
    private val metadataCache = ConcurrentHashMap<Long, ExtractedMetadata>()

    /**
     * Builds comprehensive audio diagnostics. Guaranteed to execute metadata parsing on IO Dispatcher.
     */
    suspend fun getDiagnostics(
        song: Song?,
        audioSettings: AudioSettings,
        audioSessionId: Int = 0
    ): AudioDiagnostics = withContext(ioDispatcher) {
        val source = if (song != null) {
            extractSourceFormat(song)
        } else {
            AudioSourceFormat(
                title = "No Track Playing",
                artist = "Unknown Artist",
                album = "Unknown Album"
            )
        }

        val dsp = computeDspStatus(audioSettings)
        val output = computePlaybackOutput(audioSettings, audioSessionId, dsp.activeDspCount)

        AudioDiagnostics(
            source = source,
            output = output,
            dsp = dsp
        )
    }

    private suspend fun extractSourceFormat(song: Song): AudioSourceFormat {
        val meta = metadataCache.getOrPut(song.id) {
            try {
                metadataExtractor.extractMetadata(song.id)
            } catch (e: Exception) {
                ExtractedMetadata(
                    composer = "", genre = "", lyricist = "", trackNumber = "", discNumber = "",
                    comment = "", year = "", bitrate = "", sampleRate = "", bitsPerSample = "",
                    format = "", encodingType = "", channels = "", fileSizeMb = 0.0, filePath = "",
                    recordLabel = "", copyright = "", language = "", mood = "", vbrIndicator = "",
                    replayGain = "", audioHash = ""
                )
            }
        }

        val formatLower = meta.format.lowercase()
        val pathLower = meta.filePath.lowercase()

        val container = when {
            formatLower.contains("flac") || pathLower.endsWith(".flac") -> "FLAC Container"
            formatLower.contains("mp3") || pathLower.endsWith(".mp3") -> "MPEG Audio"
            formatLower.contains("aac") || formatLower.contains("m4a") || pathLower.endsWith(".m4a") || pathLower.endsWith(".aac") -> "MPEG-4 (.m4a)"
            formatLower.contains("ogg") || formatLower.contains("vorbis") || pathLower.endsWith(".ogg") -> "Ogg Container"
            formatLower.contains("opus") || pathLower.endsWith(".opus") -> "Ogg Opus"
            formatLower.contains("wav") || pathLower.endsWith(".wav") -> "RIFF WAVE"
            meta.format.isNotBlank() -> meta.format
            else -> "Audio File"
        }

        val codec = when {
            formatLower.contains("flac") || pathLower.endsWith(".flac") -> "FLAC (Free Lossless Audio Codec)"
            formatLower.contains("mp3") || pathLower.endsWith(".mp3") -> "MP3 (MPEG-1 Audio Layer III)"
            formatLower.contains("alac") -> "ALAC (Apple Lossless)"
            formatLower.contains("aac") || pathLower.endsWith(".m4a") || pathLower.endsWith(".aac") -> "AAC (Advanced Audio Coding)"
            formatLower.contains("opus") || pathLower.endsWith(".opus") -> "Opus"
            formatLower.contains("vorbis") || pathLower.endsWith(".ogg") -> "Ogg Vorbis"
            formatLower.contains("pcm") || formatLower.contains("wav") || pathLower.endsWith(".wav") -> "PCM (Linear Pulse-Code Modulation)"
            meta.encodingType.isNotBlank() -> meta.encodingType
            else -> "Native Platform Codec"
        }

        val bitDepth = when {
            meta.bitsPerSample.isNotBlank() && meta.bitsPerSample != "0" -> "${meta.bitsPerSample}-bit"
            formatLower.contains("flac") || formatLower.contains("wav") -> "16 / 24-bit PCM"
            formatLower.contains("mp3") || formatLower.contains("aac") || formatLower.contains("ogg") || formatLower.contains("opus") -> "N/A (Lossy Transform)"
            else -> "N/A"
        }

        val sampleRate = if (meta.sampleRate.isNotBlank()) {
            val rateInt = meta.sampleRate.toIntOrNull()
            if (rateInt != null && rateInt > 0) {
                String.format(Locale.US, "%.1f kHz", rateInt / 1000.0)
            } else {
                "${meta.sampleRate} Hz"
            }
        } else {
            "44.1 kHz"
        }

        val bitrate = if (meta.bitrate.isNotBlank()) {
            val bitInt = meta.bitrate.toIntOrNull()
            if (bitInt != null && bitInt > 0) {
                String.format(Locale.US, "%,d kbps", bitInt)
            } else {
                "${meta.bitrate} kbps"
            }
        } else {
            "VBR / N/A"
        }

        val channels = when {
            meta.channels.contains("2") || meta.channels.lowercase().contains("stereo") -> "Stereo (2 ch)"
            meta.channels.contains("1") || meta.channels.lowercase().contains("mono") -> "Mono (1 ch)"
            meta.channels.contains("6") -> "5.1 Surround (6 ch)"
            meta.channels.isNotBlank() -> meta.channels
            else -> "Stereo (2 ch)"
        }

        val durationFormatted = formatDurationMs(song.duration)
        val fileSizeFormatted = if (meta.fileSizeMb > 0.0) {
            String.format(Locale.US, "%.2f MB", meta.fileSizeMb)
        } else {
            "N/A"
        }

        return AudioSourceFormat(
            title = song.title,
            artist = song.artist,
            album = song.album,
            container = container,
            codec = codec,
            bitrate = bitrate,
            sampleRate = sampleRate,
            channels = channels,
            bitDepth = bitDepth,
            durationFormatted = durationFormatted,
            fileSizeFormatted = fileSizeFormatted,
            filePath = meta.filePath
        )
    }

    private fun computeDspStatus(audioSettings: AudioSettings): AudioDspStatus {
        var count = 0

        val replayGain = if (audioSettings.replayGainEnabled && audioSettings.replayGainMode != ReplayGainMode.OFF) {
            count++
            "${audioSettings.replayGainMode.name} Mode (${String.format(Locale.US, "%+.1f dB", audioSettings.replayGainPreampDb)})"
        } else {
            "OFF"
        }

        val equalizer = if (audioSettings.equalizerEnabled) {
            count++
            "ON (${audioSettings.equalizerPreset ?: "Custom"}, Preamp: ${String.format(Locale.US, "%+.1f dB", audioSettings.equalizerPreampDb)})"
        } else {
            "OFF"
        }

        val bassBoost = if (audioSettings.bassBoostEnabled && audioSettings.bassBoostStrength > 0) {
            count++
            "ON (${audioSettings.bassBoostStrength / 10}%)"
        } else {
            "OFF"
        }

        val virtualizer = if (audioSettings.virtualizerEnabled && audioSettings.virtualizerStrength > 0) {
            count++
            "ON (${audioSettings.virtualizerStrength / 10}%)"
        } else {
            "OFF"
        }

        val loudness = if (audioSettings.loudnessNormalizationEnabled) {
            count++
            "ON (Target: ${String.format(Locale.US, "%.1f LUFS", audioSettings.targetLufs)})"
        } else {
            "OFF"
        }

        val limiter = when {
            audioSettings.limiterEnabled -> {
                count++
                "ON (Ceiling: ${String.format(Locale.US, "%.1f dB", audioSettings.limiterThresholdDb)}, Anti-Clip: ${if (audioSettings.preventClipping) "Active" else "OFF"})"
            }
            audioSettings.preventClipping -> {
                "Anti-Clipping Protection Active"
            }
            else -> "OFF"
        }

        return AudioDspStatus(
            replayGain = replayGain,
            equalizer = equalizer,
            bassBoost = bassBoost,
            virtualizer = virtualizer,
            loudnessNormalization = loudness,
            limiter = limiter,
            activeDspCount = count
        )
    }

    private fun computePlaybackOutput(
        audioSettings: AudioSettings,
        audioSessionId: Int,
        activeDspCount: Int
    ): AudioPlaybackOutput {
        val masterVolume = "${(audioSettings.masterVolume * 100).roundToInt()}%"
        val isBitPerfect = activeDspCount == 0 && audioSettings.masterVolume == 1.0f

        return AudioPlaybackOutput(
            decoderName = "MediaCodec Audio Decoder (Platform / ExoPlayer)",
            outputSampleRate = "48.0 kHz (Native AudioTrack Engine)",
            outputChannels = "Stereo (2 ch)",
            audioSessionId = audioSessionId,
            masterVolume = masterVolume,
            isBitPerfectTransparent = isBitPerfect
        )
    }

    private fun formatDurationMs(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
