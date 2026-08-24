package com.devson.vedtune.player.engine

import android.net.Uri
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import com.devson.vedtune.domain.model.AudioSettings

/**
 * Interface representing an independent, pluggable audio processor in VedTune's AudioEngine.
 *
 * Every processor is independently enabled/disabled and must handle audio effect lifecycle,
 * API level variances, and hardware limitations gracefully with zero crash risk.
 */
interface AudioProcessorModule {
    val id: String
    val name: String
    val isEnabled: Boolean

    /**
     * Called when the ExoPlayer audio session is initialized or changed.
     */
    fun onAttach(audioSessionId: Int, player: ExoPlayer)

    /**
     * Called when AudioSettings change. Applies updates in real-time without restarting playback.
     */
    fun onApplySettings(settings: AudioSettings, audioSessionId: Int, player: ExoPlayer)

    /**
     * Called when the active track changes.
     */
    fun onTrackChanged(mediaMetadata: MediaMetadata, localUri: Uri?, player: ExoPlayer)

    /**
     * Releases any attached AudioEffect or native handles to prevent memory leaks.
     */
    fun onRelease()
}
