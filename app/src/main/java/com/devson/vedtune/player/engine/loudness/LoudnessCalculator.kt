package com.devson.vedtune.player.engine.loudness

import com.devson.vedtune.player.engine.replaygain.ReplayGainInfo
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow

/**
 * Result data holder for loudness normalization gain calculations.
 */
data class LoudnessCalculationResult(
    val finalLinearGain: Float,
    val rawGainDb: Float?,
    val peakHeadroomGainDb: Float?,
    val appliedGainDb: Float?,
    val targetLufs: Float
)

/**
 * Pure mathematical calculation engine for LUFS loudness normalization.
 *
 * ARCHITECTURAL PRINCIPLES:
 * 1. ZERO MULTI-BAND COMPRESSION: Modulates purely clean linear track volume.
 * 2. METADATA INDEPENDENCE: If metadata is absent, returns unity gain 1.0f (0.0 dB change).
 * 3. HEADROOM PROTECTION: Ensures the boosted audio does not exceed 0 dBFS based on peak metadata.
 */
object LoudnessCalculator {

    /**
     * Standard baseline reference for ReplayGain (calibrated to 89 dB SPL / -14 LUFS digital equivalence).
     */
    const val REPLAYGAIN_REFERENCE_LUFS = -14.0f

    /**
     * Calculates the transparent loudness normalization gain based on metadata.
     *
     * Pipeline:
     *   Source -> Loudness Gain (LUFS target alignment) -> Optional Peak Protection Clamping -> Output
     */
    fun calculate(
        info: ReplayGainInfo?,
        targetLufs: Float,
        preventClipping: Boolean = true
    ): LoudnessCalculationResult {
        if (info == null) {
            return LoudnessCalculationResult(
                finalLinearGain = 1.0f,
                rawGainDb = null,
                peakHeadroomGainDb = null,
                appliedGainDb = 0.0f,
                targetLufs = targetLufs
            )
        }

        // Use track gain if present, else album gain
        val baseGainDb = info.trackGainDb ?: info.albumGainDb
        if (baseGainDb == null) {
            return LoudnessCalculationResult(
                finalLinearGain = 1.0f,
                rawGainDb = null,
                peakHeadroomGainDb = null,
                appliedGainDb = 0.0f,
                targetLufs = targetLufs
            )
        }

        // Adjust for user's desired target LUFS relative to reference
        val lufsOffsetDb = targetLufs - REPLAYGAIN_REFERENCE_LUFS
        val targetGainDb = baseGainDb + lufsOffsetDb

        val peak = info.trackPeak ?: info.albumPeak
        var peakLimitGainDb: Float? = null

        val finalGainDb = if (preventClipping && peak != null && peak > 0.0f) {
            // max gain before clipping: peak * 10^(gain/20) <= 1.0 => gain <= -20*log10(peak)
            val maxSafeGainDb = -20.0f * log10(peak)
            peakLimitGainDb = maxSafeGainDb
            min(targetGainDb, maxSafeGainDb)
        } else {
            targetGainDb
        }

        val linearGain = 10.0.pow(finalGainDb / 20.0).toFloat().coerceIn(0.0f, 4.0f)

        return LoudnessCalculationResult(
            finalLinearGain = linearGain,
            rawGainDb = targetGainDb,
            peakHeadroomGainDb = peakLimitGainDb,
            appliedGainDb = finalGainDb,
            targetLufs = targetLufs
        )
    }
}
