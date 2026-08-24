package com.devson.vedtune.player.engine.limiter

import android.content.ContentUris
import android.media.audiofx.DynamicsProcessing
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import com.devson.vedtune.BuildConfig
import com.devson.vedtune.domain.model.AudioSettings
import com.devson.vedtune.domain.model.AudioSettingsFactory
import com.devson.vedtune.player.engine.AudioProcessorModule
import com.devson.vedtune.player.engine.MasterVolumeProcessor
import com.devson.vedtune.player.engine.replaygain.ReplayGainCache
import com.devson.vedtune.player.engine.replaygain.ReplayGainExtractor
import com.devson.vedtune.player.engine.replaygain.ReplayGainInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Safety Limiter and Headroom Management Processor.
 *
 * ARCHITECTURAL GUARANTEES:
 * 1. TWO-STAGE SAFETY PIPELINE:
 *    Stage A: Automatic Technical Clipping Prevention via clean pre-attenuation (no dynamic pumping).
 *    Stage B: User-Selected True Peak Limiter via DynamicsProcessing (API 28+).
 * 2. 100% TRANSPARENT: If both stages are disabled or signal has sufficient headroom, audio remains untouched (0 dB change).
 * 3. NO MBC OR ARTIFICIAL MAKEUP GAIN: Does not alter audio dynamics merely because metadata is absent.
 */
class LimiterProcessor(
    private val extractor: ReplayGainExtractor,
    private val cache: ReplayGainCache,
    private val masterVolumeProcessor: MasterVolumeProcessor
) : AudioProcessorModule {

    override val id: String = "limiter"
    override val name: String = "Peak Limiter"
    override var isEnabled: Boolean = false
        private set

    companion object {
        private const val TAG = "VedTune-Limiter"
        private const val DSP_KEY = "safety_headroom"
    }

    private var dynamicsProcessing: DynamicsProcessing? = null
    private var currentSessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private var currentSettings: AudioSettings = AudioSettingsFactory.defaults()
    private var currentReplayGainInfo: ReplayGainInfo? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var metadataJob: Job? = null

    override fun onAttach(audioSessionId: Int, player: ExoPlayer) {
        if (currentSessionId != audioSessionId) {
            releaseDynamicLimiter()
            currentSessionId = audioSessionId
        }
        applyLimiter(player)
    }

    override fun onApplySettings(settings: AudioSettings, audioSessionId: Int, player: ExoPlayer) {
        if (currentSessionId != audioSessionId) {
            releaseDynamicLimiter()
            currentSessionId = audioSessionId
        }

        currentSettings = settings
        isEnabled = settings.limiterEnabled || settings.preventClipping
        applyLimiter(player)
    }

    override fun onTrackChanged(mediaMetadata: MediaMetadata, localUri: Uri?, player: ExoPlayer) {
        val songId = extractSongId(localUri, player)
        if (songId != null) {
            val cached = cache.get(songId)
            if (cached != null) {
                currentReplayGainInfo = cached
                applyLimiter(player)
                return
            }
        }

        metadataJob?.cancel()
        metadataJob = scope.launch {
            val info = extractor.extract(mediaMetadata, localUri)
            if (songId != null) {
                cache.put(songId, info)
            }
            currentReplayGainInfo = info
            applyLimiter(player)
        }
    }

    private fun applyLimiter(player: ExoPlayer) {
        // Stage 1: Technical Headroom & Clipping Prevention (Pre-attenuation)
        if (currentSettings.preventClipping) {
            val result = SafetyHeadroomCalculator.calculate(
                settings = currentSettings,
                replayGainInfo = currentReplayGainInfo
            )
            masterVolumeProcessor.setDspMultiplier(DSP_KEY, result.safetyLinearMultiplier, player)

            if (BuildConfig.DEBUG && result.clippingPrevented) {
                Log.d(
                    TAG,
                    "Clipping Prevention Active: PotentialPeak: ${String.format(Locale.US, "%.2f dB", result.potentialPeakDb)}, Attenuation: ${String.format(Locale.US, "%.2f dB", result.headroomReductionDb)}"
                )
            }
        } else {
            masterVolumeProcessor.clearDspMultiplier(DSP_KEY, player)
        }

        // Stage 2: User Peak Limiter via DynamicsProcessing
        if (currentSettings.limiterEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (currentSessionId != C.AUDIO_SESSION_ID_UNSET && currentSessionId != 0) {
                try {
                    releaseDynamicLimiter()
                    val config = createLimiterConfig(currentSettings.limiterThresholdDb)
                    dynamicsProcessing = DynamicsProcessing(0, currentSessionId, config).apply {
                        enabled = true
                    }
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Applied Dynamic Limiter: Threshold: ${currentSettings.limiterThresholdDb} dB")
                    }
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "Failed to apply Limiter DynamicsProcessing: ${e.message}")
                    }
                    releaseDynamicLimiter()
                }
            }
        } else {
            releaseDynamicLimiter()
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
            true // Limiter only, NO multiband compression, NO post-gain
        )

        for (channel in 0 until channelCount) {
            val limiter = DynamicsProcessing.Limiter(
                true,
                true,
                0,
                1.0f, // 1 ms attack
                60.0f, // 60 ms release
                10.0f, // 10:1 ratio
                thresholdDb.coerceIn(-12.0f, 0.0f),
                0.0f // 0 dB makeup/post-gain
            )
            builder.setLimiterByChannelIndex(channel, limiter)
        }

        return builder.build()
    }

    private fun extractSongId(uri: Uri?, player: ExoPlayer): Long? {
        if (uri != null && uri.scheme == "content") {
            try {
                return ContentUris.parseId(uri)
            } catch (e: Exception) {
                // Ignore
            }
        }
        return player.currentMediaItem?.mediaId?.toLongOrNull()
    }

    private fun releaseDynamicLimiter() {
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
        metadataJob?.cancel()
        scope.cancel()
        releaseDynamicLimiter()
        currentSessionId = C.AUDIO_SESSION_ID_UNSET
        isEnabled = false
    }
}
