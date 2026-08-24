package com.devson.vedtune.player.engine

import android.net.Uri
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import com.devson.vedtune.domain.model.AudioSettings
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages master output volume and coordinates combined volume scaling with DSP modules (e.g. ReplayGain, Loudness).
 */
class MasterVolumeProcessor : AudioProcessorModule {
    override val id: String = "master_volume"
    override val name: String = "Master Volume"
    override var isEnabled: Boolean = true
        private set

    private var currentMasterVolume: Float = 1.0f
    private val dspMultipliers = ConcurrentHashMap<String, Float>()

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
     * Updates a named DSP volume multiplier (e.g. "replay_gain", "loudness") in real time.
     */
    fun setDspMultiplier(source: String, multiplier: Float, player: ExoPlayer? = null) {
        dspMultipliers[source] = multiplier.coerceIn(0.0f, 4.0f)
        applyCombinedVolume(player)
    }

    /**
     * Clears a named DSP volume multiplier.
     */
    fun clearDspMultiplier(source: String, player: ExoPlayer? = null) {
        dspMultipliers.remove(source)
        applyCombinedVolume(player)
    }

    /**
     * Legacy helper for single DSP multiplier.
     */
    fun setDspVolumeMultiplier(multiplier: Float, player: ExoPlayer? = null) {
        setDspMultiplier("replay_gain", multiplier, player)
    }

    fun getEffectiveVolume(): Float {
        val totalDspMultiplier = if (dspMultipliers.isEmpty()) {
            1.0f
        } else {
            dspMultipliers.values.fold(1.0f) { acc, m -> (acc * m).coerceIn(0.0f, 4.0f) }
        }
        return (currentMasterVolume * totalDspMultiplier).coerceIn(0.0f, 1.0f)
    }

    private fun applyCombinedVolume(player: ExoPlayer?) {
        val targetVolume = getEffectiveVolume()
        player?.let {
            if (it.volume != targetVolume) {
                it.volume = targetVolume
            }
        }
    }

    override fun onRelease() {
        dspMultipliers.clear()
    }
}
