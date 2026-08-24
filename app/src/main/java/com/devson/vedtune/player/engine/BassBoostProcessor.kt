package com.devson.vedtune.player.engine

import android.media.audiofx.BassBoost
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import com.devson.vedtune.BuildConfig
import com.devson.vedtune.domain.model.AudioSettings

/**
 * Manages low-frequency BassBoost AudioEffect on supported devices.
 */
class BassBoostProcessor : AudioProcessorModule {
    override val id: String = "bass_boost"
    override val name: String = "Bass Boost"
    override var isEnabled: Boolean = false
        private set

    private var bassBoost: BassBoost? = null
    private var currentSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    companion object {
        private const val TAG = "VedTune-BassBoost"
    }

    override fun onAttach(audioSessionId: Int, player: ExoPlayer) {
        if (currentSessionId != audioSessionId) {
            releaseBassBoost()
            currentSessionId = audioSessionId
        }
    }

    override fun onApplySettings(settings: AudioSettings, audioSessionId: Int, player: ExoPlayer) {
        if (currentSessionId != audioSessionId) {
            releaseBassBoost()
            currentSessionId = audioSessionId
        }

        isEnabled = settings.bassBoostEnabled && settings.bassBoostStrength > 0
        if (!isEnabled) {
            releaseBassBoost()
            return
        }

        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == 0) return

        try {
            var bb = bassBoost
            if (bb == null) {
                bb = BassBoost(0, audioSessionId).also { bassBoost = it }
            }

            if (bb.strengthSupported) {
                bb.setStrength(settings.bassBoostStrength.coerceIn(0, 1000).toShort())
            }
            bb.enabled = true
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Failed to apply BassBoost: ${e.message}")
            }
            releaseBassBoost()
        }
    }

    override fun onTrackChanged(mediaMetadata: MediaMetadata, localUri: Uri?, player: ExoPlayer) {}

    private fun releaseBassBoost() {
        try {
            bassBoost?.enabled = false
            bassBoost?.release()
        } catch (e: Exception) {
            // Ignored
        } finally {
            bassBoost = null
        }
    }

    override fun onRelease() {
        releaseBassBoost()
        currentSessionId = C.AUDIO_SESSION_ID_UNSET
        isEnabled = false
    }
}
