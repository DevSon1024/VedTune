package com.devson.vedtune.player.engine

import android.media.audiofx.Virtualizer
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import com.devson.vedtune.BuildConfig
import com.devson.vedtune.domain.model.AudioSettings

/**
 * Manages spatial surround sound Virtualizer AudioEffect on supported devices.
 *
 * ARCHITECTURAL GUARANTEES:
 * 1. Default OFF: 100% transparent zero-DSP audio path when disabled.
 * 2. Independent: Does not couple or auto-enable alongside EQ or Bass Boost.
 * 3. Graceful Failure: Safely handles unsupported hardware without throwing or interrupting playback.
 * 4. Clean lifecycle: Effect is released immediately upon disabling or session change.
 */
@Suppress("DEPRECATION")
class VirtualizerProcessor : AudioProcessorModule {
    override val id: String = "virtualizer"
    override val name: String = "Virtualizer"
    override var isEnabled: Boolean = false
        private set

    private var virtualizer: Virtualizer? = null
    private var currentSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    companion object {
        private const val TAG = "VedTune-Virtualizer"
    }

    override fun onAttach(audioSessionId: Int, player: ExoPlayer) {
        if (currentSessionId != audioSessionId) {
            releaseVirtualizer()
            currentSessionId = audioSessionId
        }
    }

    override fun onApplySettings(settings: AudioSettings, audioSessionId: Int, player: ExoPlayer) {
        if (currentSessionId != audioSessionId) {
            releaseVirtualizer()
            currentSessionId = audioSessionId
        }

        isEnabled = settings.virtualizerEnabled && settings.virtualizerStrength > 0
        if (!isEnabled) {
            releaseVirtualizer()
            return
        }

        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == 0) return

        try {
            var virt = virtualizer
            if (virt == null) {
                virt = Virtualizer(0, audioSessionId).also { virtualizer = it }
            }

            if (virt.strengthSupported) {
                virt.setStrength(settings.virtualizerStrength.coerceIn(0, 1000).toShort())
            }
            virt.enabled = true
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Failed to apply Virtualizer: ${e.message}")
            }
            releaseVirtualizer()
        }
    }

    override fun onTrackChanged(mediaMetadata: MediaMetadata, localUri: Uri?, player: ExoPlayer) {}

    private fun releaseVirtualizer() {
        try {
            virtualizer?.enabled = false
            virtualizer?.release()
        } catch (e: Exception) {
            // Ignored
        } finally {
            virtualizer = null
        }
    }

    override fun onRelease() {
        releaseVirtualizer()
        currentSessionId = C.AUDIO_SESSION_ID_UNSET
        isEnabled = false
    }
}
