package com.devson.vedtune.player.engine.replaygain

/**
 * Data model holding parsed ReplayGain metadata for an audio track.
 *
 * @param trackGainDb ReplayGain track gain in decibels (e.g. -6.5 dB)
 * @param trackPeak ReplayGain track peak linear amplitude (e.g. 0.988, where 1.0 = full digital scale)
 * @param albumGainDb ReplayGain album gain in decibels (e.g. -5.2 dB)
 * @param albumPeak ReplayGain album peak linear amplitude (e.g. 1.02)
 */
data class ReplayGainInfo(
    val trackGainDb: Float? = null,
    val trackPeak: Float? = null,
    val albumGainDb: Float? = null,
    val albumPeak: Float? = null
) {
    val hasReplayGain: Boolean
        get() = trackGainDb != null || albumGainDb != null

    companion object {
        val EMPTY = ReplayGainInfo()
    }
}
