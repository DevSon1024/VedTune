package com.devson.vedtune.player.engine.limiter

import com.devson.vedtune.domain.model.AudioSettings
import com.devson.vedtune.domain.model.ReplayGainMode
import com.devson.vedtune.player.engine.replaygain.ReplayGainInfo
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

data class SafetyHeadroomResult(
    val safetyLinearMultiplier: Float,
    val potentialPeakLinear: Float,
    val potentialPeakDb: Float,
    val headroomReductionDb: Float,
    val clippingPrevented: Boolean,
    val isLimiterActive: Boolean
)

/**
 * Pure mathematical calculation engine for technical clipping prevention and headroom safety.
 *
 * ARCHITECTURAL PRINCIPLES:
 * 1. Distinction: Separates automatic technical clipping prevention (clean pre-attenuation) from user peak limiting.
 * 2. Dynamics Preservation: When preventClipping is true, calculates exact static gain reduction rather than
 *    applying aggressive dynamic compression or causing audible pumping.
 * 3. Transparent: If no DSP boost is causing peak headroom violation, returns 1.0x (0.0 dB attenuation).
 */
object SafetyHeadroomCalculator {

    /**
     * Calculates the required pre-attenuation multiplier to mathematically guarantee no inter-stage clipping.
     */
    fun calculate(
        settings: AudioSettings,
        replayGainInfo: ReplayGainInfo? = null,
        explicitPeak: Float? = null
    ): SafetyHeadroomResult {
        // 1. Calculate Equalizer potential boost
        val eqBoostDb = if (settings.equalizerEnabled) {
            val maxBand = settings.equalizerBandGains.maxOrNull() ?: 0.0f
            (maxBand + settings.equalizerPreampDb).coerceAtLeast(0.0f)
        } else {
            0.0f
        }

        // 2. Calculate Bass Boost potential boost (approx. +6.0 dB at max strength)
        val bassBoostDb = if (settings.bassBoostEnabled && settings.bassBoostStrength > 0) {
            (settings.bassBoostStrength.toFloat() / 1000.0f) * 6.0f
        } else {
            0.0f
        }

        // 3. Calculate ReplayGain / Loudness potential boost
        val gainBoostDb = when {
            settings.replayGainEnabled && settings.replayGainMode != ReplayGainMode.OFF -> {
                val base = if (settings.replayGainMode == ReplayGainMode.TRACK) {
                    replayGainInfo?.trackGainDb ?: 0.0f
                } else {
                    replayGainInfo?.albumGainDb ?: 0.0f
                }
                (base + settings.replayGainPreampDb).coerceAtLeast(0.0f)
            }
            settings.loudnessNormalizationEnabled -> {
                val base = (replayGainInfo?.trackGainDb ?: replayGainInfo?.albumGainDb) ?: 0.0f
                val offset = settings.targetLufs - (-14.0f)
                (base + offset).coerceAtLeast(0.0f)
            }
            else -> 0.0f
        }

        // Total positive gain boost in decibels across active DSP chain
        val totalBoostDb = eqBoostDb + bassBoostDb + gainBoostDb

        // Base peak linear level (defaults to 1.0f or metadata peak)
        val rawPeak = explicitPeak
            ?: replayGainInfo?.trackPeak
            ?: replayGainInfo?.albumPeak
            ?: 1.0f
        val peakLinear = rawPeak.coerceAtLeast(0.01f)

        // Potential peak after DSP boosts
        val boostLinear = 10.0.pow(totalBoostDb / 20.0).toFloat()
        val potentialPeakLinear = peakLinear * boostLinear
        val potentialPeakDb = 20.0f * log10(potentialPeakLinear.coerceAtLeast(0.0001f))

        val clippingPrevented: Boolean
        val safetyLinearMultiplier: Float
        val headroomReductionDb: Float

        if (settings.preventClipping && potentialPeakLinear > 1.0f) {
            // Apply exact clean pre-attenuation to bring peak to 1.0 (0 dBFS)
            safetyLinearMultiplier = (1.0f / potentialPeakLinear).coerceIn(0.05f, 1.0f)
            headroomReductionDb = 20.0f * log10(safetyLinearMultiplier)
            clippingPrevented = true
        } else {
            safetyLinearMultiplier = 1.0f
            headroomReductionDb = 0.0f
            clippingPrevented = false
        }

        return SafetyHeadroomResult(
            safetyLinearMultiplier = safetyLinearMultiplier,
            potentialPeakLinear = potentialPeakLinear,
            potentialPeakDb = potentialPeakDb,
            headroomReductionDb = headroomReductionDb,
            clippingPrevented = clippingPrevented,
            isLimiterActive = settings.limiterEnabled
        )
    }
}
