package com.devson.vedtune.player.engine.replaygain

import com.devson.vedtune.domain.model.ReplayGainMode
import kotlin.math.min
import kotlin.math.pow

/**
 * Result structure representing the exact stages of ReplayGain gain calculation.
 *
 * @param gainDbUsed The raw ReplayGain dB value extracted from metadata (or null if unavailable).
 * @param peakUsed The peak linear amplitude value used for safety checks (or null if unavailable).
 * @param isAlbumGainApplied True if album gain was selected and available; false if track gain was used.
 * @param desiredLinearGain The ideal linear volume multiplier: 10^((gainDb + preampDb) / 20).
 * @param safetyLinearGain The maximum non-clipping linear gain allowed: 1.0 / peak (or null if peak unknown).
 * @param finalLinearGain The final computed linear volume multiplier applied to the playback pipeline.
 */
data class ReplayGainCalculationResult(
    val gainDbUsed: Float?,
    val peakUsed: Float?,
    val isAlbumGainApplied: Boolean,
    val desiredLinearGain: Float,
    val safetyLinearGain: Float?,
    val finalLinearGain: Float
)

/**
 * Pure mathematical calculation engine for ReplayGain.
 *
 * REPLAYGAIN MATH OVERVIEW:
 * 1. Desired Gain (Linear):
 *    ReplayGain specifies adjustments in decibels relative to an 89 dB SPL standard target.
 *    Total dB Adjustment = Gain(dB) + User Preamp(dB)
 *    Desired Linear Gain = 10^(Total dB / 20)
 *
 * 2. Peak Amplitude & Safety Gain (Anti-Clipping):
 *    The peak value represents the maximum sample amplitude in the audio stream (1.0 = 0 dBFS full scale).
 *    Projected Peak = Peak * Desired Linear Gain
 *    If Projected Peak > 1.0, digital clipping will occur during DAC conversion or integer PCM output.
 *    To prevent clipping:
 *      Safety Linear Gain = 1.0 / Peak (if Peak > 0.0)
 *      Safety Linear Gain = 1.0 (if Peak is unavailable/unknown)
 *
 * 3. Final Gain Determination:
 *    - If ReplayGain is OFF or no gain metadata exists:
 *        Final Gain = 1.0 (100% transparent bit-perfect unity gain)
 *    - If preventClipping is TRUE and Peak is known:
 *        Final Gain = min(Desired Linear Gain, Safety Linear Gain)
 *    - If preventClipping is TRUE and Peak is unknown:
 *        Final Gain = min(Desired Linear Gain, 1.0)
 *    - If preventClipping is FALSE:
 *        Final Gain = Desired Linear Gain
 *
 * 4. Safety Guardrails:
 *    Final Gain is always clamped within [0.0, 4.0] (max +12 dB digital boost) and checked for NaN/Infinity
 *    to prevent hardware damage or audio distortion.
 */
object ReplayGainCalculator {

    /**
     * Calculates the ReplayGain volume multiplier according to user settings and track metadata.
     *
     * @param info Extracted ReplayGain metadata for the current track.
     * @param mode Selected ReplayGain mode (OFF, TRACK, ALBUM).
     * @param preampDb User-specified pre-amplification boost/cut in dB.
     * @param preventClipping Whether anti-clipping protection is enabled.
     * @return [ReplayGainCalculationResult] with transparent breakdown of all intermediate gain values.
     */
    fun calculate(
        info: ReplayGainInfo,
        mode: ReplayGainMode,
        preampDb: Float,
        preventClipping: Boolean
    ): ReplayGainCalculationResult {
        // Mode OFF: Bit-perfect unity gain
        if (mode == ReplayGainMode.OFF || !info.hasReplayGain) {
            return ReplayGainCalculationResult(
                gainDbUsed = null,
                peakUsed = null,
                isAlbumGainApplied = false,
                desiredLinearGain = 1.0f,
                safetyLinearGain = null,
                finalLinearGain = 1.0f
            )
        }

        // Determine target gain and peak based on mode priority
        val (gainDb, peak, isAlbum) = when (mode) {
            ReplayGainMode.ALBUM -> {
                if (info.albumGainDb != null) {
                    Triple(info.albumGainDb, info.albumPeak ?: info.trackPeak, true)
                } else {
                    // Fallback to track gain if album gain is absent
                    Triple(info.trackGainDb, info.trackPeak, false)
                }
            }
            ReplayGainMode.TRACK -> {
                if (info.trackGainDb != null) {
                    Triple(info.trackGainDb, info.trackPeak ?: info.albumPeak, false)
                } else {
                    // Fallback to album gain if track gain is absent
                    Triple(info.albumGainDb, info.albumPeak, true)
                }
            }
            ReplayGainMode.OFF -> Triple(null, null, false)
        }

        if (gainDb == null || !gainDb.isFinite()) {
            return ReplayGainCalculationResult(
                gainDbUsed = null,
                peakUsed = null,
                isAlbumGainApplied = false,
                desiredLinearGain = 1.0f,
                safetyLinearGain = null,
                finalLinearGain = 1.0f
            )
        }

        // 1. Calculate Desired Linear Gain
        val safePreampDb = if (preampDb.isFinite()) preampDb else 0.0f
        val totalGainDb = gainDb + safePreampDb
        val desiredLinearGain = 10.0.pow(totalGainDb.toDouble() / 20.0).toFloat()
            .takeIf { it.isFinite() && it >= 0f } ?: 1.0f

        // 2. Calculate Safety Linear Gain (if peak is available)
        val validPeak = peak?.takeIf { it.isFinite() && it > 0.0f }
        val safetyLinearGain = if (validPeak != null) {
            (1.0f / validPeak).takeIf { it.isFinite() && it >= 0f }
        } else {
            null
        }

        // 3. Determine Final Linear Gain
        val finalGain = if (preventClipping) {
            if (safetyLinearGain != null) {
                // If peak is known, allow positive gain as long as projected peak <= 1.0
                min(desiredLinearGain, safetyLinearGain)
            } else {
                // If peak is unknown, safely clamp positive gain at unity (1.0) to prevent clipping
                min(desiredLinearGain, 1.0f)
            }
        } else {
            // Anti-clipping disabled by user: allow raw desired gain
            desiredLinearGain
        }

        // 4. Final safety guardrail clamp (0.0 to 4.0 = max +12 dB digital gain)
        val clampedFinalGain = finalGain.coerceIn(0.0f, 4.0f)

        return ReplayGainCalculationResult(
            gainDbUsed = gainDb,
            peakUsed = validPeak,
            isAlbumGainApplied = isAlbum,
            desiredLinearGain = desiredLinearGain,
            safetyLinearGain = safetyLinearGain,
            finalLinearGain = clampedFinalGain
        )
    }
}
