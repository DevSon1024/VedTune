package com.devson.vedtune.player.engine.equalizer

/**
 * 10-Band Standard Equalizer Presets and frequency definitions for VedTune.
 */
object EqualizerPresets {

    /**
     * Standard ISO 10-band center frequencies in Hertz (Hz).
     */
    val FREQUENCIES_HZ = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

    /**
     * Human-readable labels for the 10 graphic equalizer bands.
     */
    val BAND_LABELS = listOf(
        "31Hz", "62Hz", "125Hz", "250Hz", "500Hz",
        "1kHz", "2kHz", "4kHz", "8kHz", "16kHz"
    )

    const val PRESET_FLAT = "Flat"
    const val PRESET_BASS = "Bass"
    const val PRESET_BASS_TREBLE = "Bass & Treble"
    const val PRESET_VOCAL = "Vocal"
    const val PRESET_CLASSICAL = "Classical"
    const val PRESET_ROCK = "Rock"
    const val PRESET_POP = "Pop"
    const val PRESET_CUSTOM = "Custom"

    data class Preset(
        val name: String,
        val bandGains: List<Float>,
        val preampDb: Float = 0.0f
    )

    val ALL_PRESETS = listOf(
        Preset(
            name = PRESET_FLAT,
            bandGains = listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
            preampDb = 0.0f
        ),
        Preset(
            name = PRESET_BASS,
            bandGains = listOf(6.0f, 5.5f, 4.5f, 2.5f, 0.5f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
            preampDb = -3.0f
        ),
        Preset(
            name = PRESET_BASS_TREBLE,
            bandGains = listOf(5.0f, 4.0f, 2.0f, 0.0f, -1.0f, 0.0f, 2.0f, 3.5f, 5.0f, 6.0f),
            preampDb = -3.0f
        ),
        Preset(
            name = PRESET_VOCAL,
            bandGains = listOf(-2.0f, -1.5f, 0.0f, 2.0f, 4.0f, 4.0f, 3.0f, 1.0f, 0.0f, -1.0f),
            preampDb = -1.0f
        ),
        Preset(
            name = PRESET_CLASSICAL,
            bandGains = listOf(4.5f, 3.5f, 2.5f, 2.0f, -1.0f, -1.0f, 0.0f, 2.5f, 3.5f, 4.0f),
            preampDb = -2.0f
        ),
        Preset(
            name = PRESET_ROCK,
            bandGains = listOf(5.0f, 4.0f, 2.5f, -0.5f, -1.5f, 0.5f, 2.5f, 4.0f, 5.0f, 5.5f),
            preampDb = -2.5f
        ),
        Preset(
            name = PRESET_POP,
            bandGains = listOf(-1.0f, 1.0f, 2.5f, 3.5f, 4.0f, 2.5f, 1.0f, 1.5f, 2.5f, 2.0f),
            preampDb = -1.5f
        )
    )

    fun getPresetByName(name: String?): Preset? {
        if (name == null || name == PRESET_CUSTOM) return null
        return ALL_PRESETS.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * Default zero gains list for transparent baseline.
     */
    fun defaultBandGains(): List<Float> = List(FREQUENCIES_HZ.size) { 0.0f }
}
