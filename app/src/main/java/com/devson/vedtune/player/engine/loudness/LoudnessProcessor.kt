package com.devson.vedtune.player.engine.loudness

import android.content.ContentUris
import android.net.Uri
import android.util.Log
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
 * Loudness normalization processor that adjusts output volume toward configurable LUFS targets.
 *
 * ARCHITECTURAL GUARANTEES:
 * 1. ZERO COMPRESSION: Does NOT apply multi-band compression or artificial dynamic alterations.
 * 2. METADATA-FIRST: Adjusts pure volume scale based on ReplayGain/LUFS tags.
 * 3. TRANSPARENT FALLBACK: When metadata is missing, leaves audio 100% untouched (0.0 dB change).
 * 4. ASYNC EXTRACTION & CACHING: Uses background thread extraction and LRU memory cache.
 */
class LoudnessProcessor(
    private val extractor: ReplayGainExtractor,
    private val cache: ReplayGainCache,
    private val masterVolumeProcessor: MasterVolumeProcessor
) : AudioProcessorModule {

    override val id: String = "loudness_normalization"
    override val name: String = "Loudness Normalization"
    override var isEnabled: Boolean = false
        private set

    companion object {
        private const val TAG = "VedTune-Loudness"
        private const val DSP_KEY = "loudness"
    }

    private var currentSettings: AudioSettings = AudioSettingsFactory.defaults()
    private var lastMetadata: MediaMetadata? = null
    private var lastUri: Uri? = null
    private var lastSongId: Long? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var extractionJob: Job? = null

    override fun onAttach(audioSessionId: Int, player: ExoPlayer) {
        processCurrentTrack(player)
    }

    override fun onApplySettings(settings: AudioSettings, audioSessionId: Int, player: ExoPlayer) {
        val wasEnabled = isEnabled
        val oldSettings = currentSettings
        currentSettings = settings
        isEnabled = settings.loudnessNormalizationEnabled

        if (!isEnabled) {
            if (wasEnabled) {
                masterVolumeProcessor.clearDspMultiplier(DSP_KEY, player)
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Loudness Normalization disabled: Volume reset to Unity")
                }
            }
        } else if (oldSettings != settings || !wasEnabled) {
            processCurrentTrack(player)
        }
    }

    override fun onTrackChanged(mediaMetadata: MediaMetadata, localUri: Uri?, player: ExoPlayer) {
        lastMetadata = mediaMetadata
        lastUri = localUri
        lastSongId = extractSongId(localUri, player)

        if (isEnabled) {
            processCurrentTrack(player)
        } else {
            masterVolumeProcessor.clearDspMultiplier(DSP_KEY, player)
        }
    }

    private fun processCurrentTrack(player: ExoPlayer) {
        if (!isEnabled) {
            masterVolumeProcessor.clearDspMultiplier(DSP_KEY, player)
            return
        }

        val songId = lastSongId ?: extractSongId(lastUri, player)
        val metadata = lastMetadata ?: player.currentMediaItem?.mediaMetadata
        val uri = lastUri ?: player.currentMediaItem?.localConfiguration?.uri

        if (songId != null) {
            val cachedInfo = cache.get(songId)
            if (cachedInfo != null) {
                applyLoudnessGain(cachedInfo, player)
                return
            }
        }

        extractionJob?.cancel()
        extractionJob = scope.launch {
            val extractedInfo = extractor.extract(metadata, uri)
            if (songId != null) {
                cache.put(songId, extractedInfo)
            }
            applyLoudnessGain(extractedInfo, player)
        }
    }

    private fun applyLoudnessGain(info: ReplayGainInfo, player: ExoPlayer) {
        val result = LoudnessCalculator.calculate(
            info = info,
            targetLufs = currentSettings.targetLufs,
            preventClipping = true
        )

        masterVolumeProcessor.setDspMultiplier(DSP_KEY, result.finalLinearGain, player)

        if (BuildConfig.DEBUG) {
            val appliedDb = result.appliedGainDb?.let { String.format(Locale.US, "%.2f dB", it) } ?: "None"
            val targetStr = "${currentSettings.targetLufs} LUFS"
            val multStr = String.format(Locale.US, "%.3fx", result.finalLinearGain)
            Log.d(TAG, "Target: $targetStr | Applied: $appliedDb | LinearMultiplier: $multStr")
        }
    }

    private fun extractSongId(uri: Uri?, player: ExoPlayer): Long? {
        if (uri != null) {
            try {
                if (uri.scheme == "content") {
                    return ContentUris.parseId(uri)
                }
            } catch (e: Exception) {
                // Non-standard content URI
            }
        }
        return player.currentMediaItem?.mediaId?.toLongOrNull()
    }

    override fun onRelease() {
        extractionJob?.cancel()
        scope.cancel()
        isEnabled = false
    }
}
