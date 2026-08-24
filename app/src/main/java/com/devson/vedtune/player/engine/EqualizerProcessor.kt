package com.devson.vedtune.player.engine

import android.media.audiofx.Equalizer
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import com.devson.vedtune.BuildConfig
import com.devson.vedtune.domain.model.AudioSettings
import kotlin.math.roundToInt

/**
 * Manages Android system Equalizer AudioEffect, applying custom band gains and preamp in real time.
 */
class EqualizerProcessor : AudioProcessorModule {
    override val id: String = "equalizer"
    override val name: String = "Equalizer"
    override var isEnabled: Boolean = false
        private set

    private var equalizer: Equalizer? = null
    private var currentSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    companion object {
        private const val TAG = "VedTune-Equalizer"
    }

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

            val numBands = eq.numberOfBands.toInt()
            val bandLevelRange = eq.bandLevelRange
            val minLevel = bandLevelRange.getOrNull(0) ?: -1500
            val maxLevel = bandLevelRange.getOrNull(1) ?: 1500

            val bandGains = settings.equalizerBandGains
            val preampMb = (settings.equalizerPreampDb * 100).roundToInt().toShort()

            for (i in 0 until numBands) {
                val gainDb = bandGains.getOrNull(i) ?: 0.0f
                val targetMb = ((gainDb * 100).roundToInt() + preampMb).coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                eq.setBandLevel(i.toShort(), targetMb)
            }

            eq.enabled = true
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Failed to apply Equalizer settings: ${e.message}")
            }
            releaseEqualizer()
        }
    }

    override fun onTrackChanged(mediaMetadata: MediaMetadata, localUri: Uri?, player: ExoPlayer) {}

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
