package com.devson.vedtune.domain.model

/**
 * Operating mode for ReplayGain loudness adjustment.
 */
enum class ReplayGainMode {
    OFF,
    TRACK,
    ALBUM
}

/**
 * Immutable data model representing all user-configurable audio behavior in VedTune.
 *
 * FACTORY DEFAULT PHILOSOPHY:
 * All defaults prioritize 100% bit-perfect, transparent offline playback.
 * - Master Volume: 1.0 (Unity Gain / 0.0 dB)
 * - Gapless Playback: ON
 * - Crossfade: OFF
 * - ReplayGain: OFF (0.0 dB Preamp, Peak Clipping Prevention: ON)
 * - Equalizer: OFF (0.0 dB Preamp, Empty/Flat bands)
 * - Bass Boost: OFF (0 strength)
 * - Loudness Normalization: OFF (-14.0 LUFS target)
 * - Limiter: OFF (-0.5 dB threshold)
 * - Audio Processing Engine: OFF
 */
data class AudioSettings(
    // Playback
    val masterVolume: Float = 1.0f,
    val gaplessPlaybackEnabled: Boolean = true,
    val crossfadeEnabled: Boolean = false,
    val crossfadeDurationMs: Int = 2000,

    // ReplayGain
    val replayGainEnabled: Boolean = false,
    val replayGainMode: ReplayGainMode = ReplayGainMode.OFF,
    val replayGainPreampDb: Float = 0.0f,
    val replayGainPreventClipping: Boolean = true,

    // Equalizer
    val equalizerEnabled: Boolean = false,
    val equalizerPreampDb: Float = 0.0f,
    val equalizerBandGains: List<Float> = emptyList(),
    val equalizerPreset: String? = null,

    // Bass
    val bassBoostEnabled: Boolean = false,
    val bassBoostStrength: Int = 0,

    // Virtualizer
    val virtualizerEnabled: Boolean = false,
    val virtualizerStrength: Int = 0,

    // Loudness
    val loudnessNormalizationEnabled: Boolean = false,
    val targetLufs: Float = -14.0f,

    // Limiter
    val limiterEnabled: Boolean = false,
    val limiterThresholdDb: Float = -0.5f,

    // Advanced
    val audioProcessingEnabled: Boolean = false
) {
    companion object {
        val FactoryDefaults = AudioSettings()
        fun defaults(): AudioSettings = FactoryDefaults
    }
}

/**
 * Factory object providing the immutable default AudioSettings.
 */
object AudioSettingsFactory {
    fun defaults(): AudioSettings = AudioSettings.FactoryDefaults
}
