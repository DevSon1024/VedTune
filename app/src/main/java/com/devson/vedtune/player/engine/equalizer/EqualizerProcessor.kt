package com.devson.vedtune.player.engine.equalizer

import android.media.audiofx.Equalizer
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import com.devson.vedtune.BuildConfig
import com.devson.vedtune.domain.model.AudioSettings
import com.devson.vedtune.player.engine.AudioProcessorModule
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.roundToInt

/**
 * Manages the Android system Equalizer AudioEffect, applying 10-band user gains and preamp.
 *
 * ARCHITECTURAL GUARANTEES:
 * 1. Mode OFF: Effect is released/disabled; 100% transparent zero-DSP audio path.
 * 2. Real-time updates: Band and preamp adjustments take effect immediately without restarting songs.
 * 3. OEM / Hardware tolerance: Safely maps 10 virtual user bands to the device's physical hardware bands
 *    via logarithmic center frequency matching.
 * 4. Zero Crash Tolerance: Catches all hardware/driver exceptions gracefully.
 */
class EqualizerProcessor : AudioProcessorModule {

    override val id: String = "equalizer"
    override val name: String = "Equalizer"
    override var isEnabled: Boolean = false
        private set

    companion object {
        private const val TAG = "VedTune-Equalizer"
    }

    private var equalizer: Equalizer? = null
    private var currentSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    override fun onAttach(audioSessionId: Int, player: ExoPlayer) {
        if (currentSessionId != audioSessionId) {
            releaseEqualizer()
            currentSessionId = audioSessionId
        }
    }

    override fun onApplySettings(settings: AudioSettings, audioSessionId: Int, player: ExoPlayer) {
        if (currentSessionId != audioSessionId) {
            releaseEqualizer()
            currentSessionId = audioSessionId
        }

        isEnabled = settings.equalizerEnabled
        if (!isEnabled) {
            releaseEqualizer()
            return
        }

        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == 0) return

        try {
            var eq = equalizer
            if (eq == null) {
                eq = Equalizer(0, audioSessionId).also { equalizer = it }
            }

            val numPhysicalBands = eq.numberOfBands.toInt()
            val bandLevelRange = eq.bandLevelRange
            val minLevel = bandLevelRange.getOrNull(0) ?: -1500
            val maxLevel = bandLevelRange.getOrNull(1) ?: 1500

            val userBands = settings.equalizerBandGains.ifEmpty { EqualizerPresets.defaultBandGains() }
            val preampMb = (settings.equalizerPreampDb.coerceIn(-12.0f, 12.0f) * 100).roundToInt()

            // Map each physical hardware band to the user's 10-band gains
            for (physicalBand in 0 until numPhysicalBands) {
                val physicalCenterFreqHz = (eq.getCenterFreq(physicalBand.toShort()) / 1000).coerceAtLeast(1)
                val closestVirtualIndex = findClosestBandIndex(physicalCenterFreqHz)
                val userGainDb = userBands.getOrNull(closestVirtualIndex) ?: 0.0f
                val totalGainMb = ((userGainDb.coerceIn(-12.0f, 12.0f) * 100).roundToInt() + preampMb)
                    .coerceIn(minLevel.toInt(), maxLevel.toInt())

                eq.setBandLevel(physicalBand.toShort(), totalGainMb.toShort())
            }

            eq.enabled = true

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Applied Equalizer: $numPhysicalBands physical bands, Preamp: ${settings.equalizerPreampDb} dB")
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Failed to apply Equalizer settings: ${e.message}")
            }
            releaseEqualizer()
        }
    }

    override fun onTrackChanged(mediaMetadata: MediaMetadata, localUri: Uri?, player: ExoPlayer) {}

    /**
     * Finds the closest 10-band virtual frequency index using logarithmic distance.
     */
    private fun findClosestBandIndex(physicalFreqHz: Int): Int {
        val physicalLog = log10(physicalFreqHz.toDouble())
        var bestIdx = 0
        var minDistance = Double.MAX_VALUE

        for ((idx, virtualFreq) in EqualizerPresets.FREQUENCIES_HZ.withIndex()) {
            val virtualLog = log10(virtualFreq.toDouble())
            val distance = abs(physicalLog - virtualLog)
            if (distance < minDistance) {
                minDistance = distance
                bestIdx = idx
            }
        }
        return bestIdx
    }

    private fun releaseEqualizer() {
        try {
            equalizer?.enabled = false
            equalizer?.release()
        } catch (e: Exception) {
            // Ignored
        } finally {
            equalizer = null
        }
    }

    override fun onRelease() {
        releaseEqualizer()
        currentSessionId = C.AUDIO_SESSION_ID_UNSET
        isEnabled = false
    }
}
