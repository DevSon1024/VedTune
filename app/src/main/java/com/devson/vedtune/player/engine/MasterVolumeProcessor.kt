package com.devson.vedtune.player.engine

import android.net.Uri
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import com.devson.vedtune.domain.model.AudioSettings

/**
 * Manages master output volume and coordinates combined volume scaling with DSP modules (e.g. ReplayGain).
 */
class MasterVolumeProcessor : AudioProcessorModule {
    override val id: String = "master_volume"
    override val name: String = "Master Volume"
    override var isEnabled: Boolean = true
        private set

    private var currentMasterVolume: Float = 1.0f
    private var dspVolumeMultiplier: Float = 1.0f

    override fun onAttach(audioSessionId: Int, player: ExoPlayer) {
        applyCombinedVolume(player)
    }

    override fun onApplySettings(settings: AudioSettings, audioSessionId: Int, player: ExoPlayer) {
        currentMasterVolume = settings.masterVolume.coerceIn(0.0f, 1.0f)
        applyCombinedVolume(player)
    }

    override fun onTrackChanged(mediaMetadata: MediaMetadata, localUri: Uri?, player: ExoPlayer) {
        applyCombinedVolume(player)
    }

    /**
     * Updates DSP volume multiplier (e.g. ReplayGain linear attenuation) in real time.
     */
    fun setDspVolumeMultiplier(multiplier: Float, player: ExoPlayer? = null) {
        dspVolumeMultiplier = multiplier.coerceIn(0.0f, 2.0f)
        applyCombinedVolume(player)
    }

    fun getEffectiveVolume(): Float = (currentMasterVolume * dspVolumeMultiplier).coerceIn(0.0f, 1.0f)

    private fun applyCombinedVolume(player: ExoPlayer?) {
        val targetVolume = getEffectiveVolume()
        player?.let {
            if (it.volume != targetVolume) {
                it.volume = targetVolume
            }
        }
    }

    override fun onRelease() {
        dspVolumeMultiplier = 1.0f
    }
}
