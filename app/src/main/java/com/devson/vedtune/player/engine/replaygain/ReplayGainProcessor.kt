package com.devson.vedtune.player.engine.replaygain

import android.content.ContentUris
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import com.devson.vedtune.BuildConfig
import com.devson.vedtune.domain.model.AudioSettings
import com.devson.vedtune.domain.model.AudioSettingsFactory
import com.devson.vedtune.domain.model.ReplayGainMode
import com.devson.vedtune.player.engine.AudioProcessorModule
import com.devson.vedtune.player.engine.MasterVolumeProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ReplayGain processor responsible for calculating and applying ReplayGain volume attenuation.
 *
 * ARCHITECTURAL GUARANTEES:
 * 1. Mode OFF: ReplayGain multiplier is exactly 1.0 (bit-perfect transparent output).
 * 2. Missing metadata: ReplayGain multiplier is exactly 1.0 (no fallback compressor, no artificial gain boost).
 * 3. Asynchronous extraction: Metadata extraction executes exclusively on Dispatchers.IO.
 * 4. In-Memory caching: Parsed tags are stored in [ReplayGainCache] to prevent duplicate disk reads.
 * 5. Anti-Clipping protection: Uses track/album peak data to prevent integer audio clipping.
 */
class ReplayGainProcessor(
    private val extractor: ReplayGainExtractor,
    private val cache: ReplayGainCache,
    private val masterVolumeProcessor: MasterVolumeProcessor
) : AudioProcessorModule {

    override val id: String = "replay_gain"
    override val name: String = "ReplayGain"
    override var isEnabled: Boolean = false
        private set

    companion object {
        private const val TAG = "VedTune-ReplayGain"
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
        isEnabled = settings.replayGainEnabled && settings.replayGainMode != ReplayGainMode.OFF

        if (!isEnabled) {
            if (wasEnabled) {
                masterVolumeProcessor.setDspVolumeMultiplier(1.0f, player)
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "ReplayGain disabled: Gain reset to 1.0 (Unity)")
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
            masterVolumeProcessor.setDspVolumeMultiplier(1.0f, player)
        }
    }

    private fun processCurrentTrack(player: ExoPlayer) {
        if (!isEnabled) {
            masterVolumeProcessor.setDspVolumeMultiplier(1.0f, player)
            return
        }

        val songId = lastSongId ?: extractSongId(lastUri, player)
        val metadata = lastMetadata ?: player.currentMediaItem?.mediaMetadata
        val uri = lastUri ?: player.currentMediaItem?.localConfiguration?.uri

        // Check if metadata is already cached in memory
        if (songId != null) {
            val cachedInfo = cache.get(songId)
            if (cachedInfo != null) {
                applyReplayGain(cachedInfo, player)
                return
            }
        }

        // Asynchronously extract metadata on Dispatchers.IO
        extractionJob?.cancel()
        extractionJob = scope.launch {
            val extractedInfo = extractor.extract(metadata, uri)
            if (songId != null) {
                cache.put(songId, extractedInfo)
            }
            applyReplayGain(extractedInfo, player)
        }
    }

    private fun applyReplayGain(info: ReplayGainInfo, player: ExoPlayer) {
        val result = ReplayGainCalculator.calculate(
            info = info,
            mode = currentSettings.replayGainMode,
            preampDb = currentSettings.replayGainPreampDb,
            preventClipping = currentSettings.replayGainPreventClipping
        )

        masterVolumeProcessor.setDspVolumeMultiplier(result.finalLinearGain, player)

        if (BuildConfig.DEBUG) {
            val modeStr = currentSettings.replayGainMode.name
            val usedGainDb = result.gainDbUsed?.let { String.format(Locale.US, "%.2f dB", it) } ?: "None"
            val peakStr = result.peakUsed?.let { String.format(Locale.US, "%.4f", it) } ?: "N/A"
            val finalGainStr = String.format(Locale.US, "%.3fx", result.finalLinearGain)
            Log.d(
                TAG,
                "[$modeStr] TargetGain: $usedGainDb | Peak: $peakStr | Preamp: ${currentSettings.replayGainPreampDb} dB | FinalMultiplier: $finalGainStr (Safety: ${result.safetyLinearGain})"
            )
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
