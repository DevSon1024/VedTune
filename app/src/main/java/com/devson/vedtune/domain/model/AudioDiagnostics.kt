package com.devson.vedtune.domain.model

data class AudioSourceFormat(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val container: String = "Unknown",
    val codec: String = "Unknown",
    val bitrate: String = "N/A",
    val sampleRate: String = "N/A",
    val channels: String = "Stereo",
    val bitDepth: String = "N/A",
    val durationFormatted: String = "0:00",
    val fileSizeFormatted: String = "N/A",
    val filePath: String = ""
)

data class AudioPlaybackOutput(
    val decoderName: String = "Platform MediaCodec / Direct",
    val outputSampleRate: String = "48.0 kHz",
    val outputChannels: String = "Stereo (2 ch)",
    val audioSessionId: Int = 0,
    val masterVolume: String = "100%",
    val isBitPerfectTransparent: Boolean = true
)

data class AudioDspStatus(
    val replayGain: String = "OFF",
    val equalizer: String = "OFF",
    val bassBoost: String = "OFF",
    val virtualizer: String = "OFF",
    val loudnessNormalization: String = "OFF",
    val limiter: String = "OFF",
    val activeDspCount: Int = 0
)

/**
 * Diagnostic model providing real-time audio information for playback and DSP stages.
 *
 * ARCHITECTURAL RULE:
 * Strictly separates SOURCE FORMAT from OUTPUT FORMAT and never claims bit-perfect
 * output unless verified (zero active DSP and 1.0x volume multiplier).
 */
data class AudioDiagnostics(
    val source: AudioSourceFormat,
    val output: AudioPlaybackOutput,
    val dsp: AudioDspStatus
)
