package com.devson.vedtune.player.engine

import android.media.audiofx.DynamicsProcessing
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import com.devson.vedtune.BuildConfig
import com.devson.vedtune.domain.model.AudioSettings

/**
 * Manages peak output Limiter via DynamicsProcessing on Android 9.0+ (API 28+) devices.
 * Inactive and null by default to maintain bit-perfect transparency.
 */
class LimiterProcessor : AudioProcessorModule {
    override val id: String = "limiter"
    override val name: String = "Peak Limiter"
    override var isEnabled: Boolean = false
        private set

    private var dynamicsProcessing: DynamicsProcessing? = null
    private var currentSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    companion object {
        private const val TAG = "VedTune-Limiter"
    }

    override fun onAttach(audioSessionId: Int, player: ExoPlayer) {
        if (currentSessionId != audioSessionId) {
            releaseLimiter()
            currentSessionId = audioSessionId
        }
    }

    override fun onApplySettings(settings: AudioSettings, audioSessionId: Int, player: ExoPlayer) {
        if (currentSessionId != audioSessionId) {
            releaseLimiter()
            currentSessionId = audioSessionId
        }

        isEnabled = settings.limiterEnabled
        if (!isEnabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            releaseLimiter()
            return
        }

        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == 0) return

        try {
            releaseLimiter()
            val config = createLimiterConfig(settings.limiterThresholdDb)
            dynamicsProcessing = DynamicsProcessing(0, audioSessionId, config).apply {
                enabled = true
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Failed to apply Limiter DynamicsProcessing: ${e.message}")
            }
            releaseLimiter()
        }
    }

    private fun createLimiterConfig(thresholdDb: Float): DynamicsProcessing.Config {
        val channelCount = 2
        val builder = DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
            channelCount,
            false, 0,
            false, 0,
            false, 0,
            true // Limiter only, NO multi-band compression, NO post-gain
        )

        for (channel in 0 until channelCount) {
            val limiter = DynamicsProcessing.Limiter(
                true,
                true,
                0,
                1.0f,
                50.0f,
                10.0f,
                thresholdDb.coerceIn(-12.0f, 0.0f),
                0.0f // Unity post gain
            )
            builder.setLimiterByChannelIndex(channel, limiter)
        }

        return builder.build()
    }

    override fun onTrackChanged(mediaMetadata: MediaMetadata, localUri: Uri?, player: ExoPlayer) {}

    private fun releaseLimiter() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                dynamicsProcessing?.enabled = false
                dynamicsProcessing?.release()
            } catch (e: Exception) {
                // Ignored
            } finally {
                dynamicsProcessing = null
            }
        }
    }

    override fun onRelease() {
        releaseLimiter()
        currentSessionId = C.AUDIO_SESSION_ID_UNSET
        isEnabled = false
    }
}
