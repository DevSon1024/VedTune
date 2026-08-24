package com.devson.vedtune.player.engine

import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import com.devson.vedtune.BuildConfig
import com.devson.vedtune.domain.model.AudioSettings
import kotlin.math.roundToInt

/**
 * Manages standard Android LoudnessEnhancer AudioEffect (API 19+) for target loudness matching.
 */
class LoudnessProcessor : AudioProcessorModule {
    override val id: String = "loudness_enhancer"
    override val name: String = "Loudness Enhancer"
    override var isEnabled: Boolean = false
        private set

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    companion object {
        private const val TAG = "VedTune-Loudness"
    }

    override fun onAttach(audioSessionId: Int, player: ExoPlayer) {
        if (currentSessionId != audioSessionId) {
            releaseLoudnessEnhancer()
            currentSessionId = audioSessionId
        }
    }

    override fun onApplySettings(settings: AudioSettings, audioSessionId: Int, player: ExoPlayer) {
        if (currentSessionId != audioSessionId) {
            releaseLoudnessEnhancer()
            currentSessionId = audioSessionId
        }

        isEnabled = settings.loudnessNormalizationEnabled
        if (!isEnabled) {
            releaseLoudnessEnhancer()
            return
        }

        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == 0) return

        try {
            var le = loudnessEnhancer
            if (le == null) {
                le = LoudnessEnhancer(audioSessionId).also { loudnessEnhancer = it }
            }

            // Map target LUFS to target gain millibels (e.g. -14 LUFS baseline)
            val targetGainMb = (((-14.0f - settings.targetLufs).coerceIn(0.0f, 6.0f)) * 100).roundToInt()
            le.setTargetGain(targetGainMb)
            le.enabled = true
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Failed to apply LoudnessEnhancer: ${e.message}")
            }
            releaseLoudnessEnhancer()
        }
    }

    override fun onTrackChanged(mediaMetadata: MediaMetadata, localUri: Uri?, player: ExoPlayer) {}

    private fun releaseLoudnessEnhancer() {
        try {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            // Ignored
        } finally {
            loudnessEnhancer = null
        }
    }

    override fun onRelease() {
        releaseLoudnessEnhancer()
        currentSessionId = C.AUDIO_SESSION_ID_UNSET
        isEnabled = false
    }
}
