package com.devson.vedtune.player.engine

import android.content.Context
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.devson.vedtune.BuildConfig
import com.devson.vedtune.domain.model.AudioSettings
import com.devson.vedtune.domain.model.AudioSettingsFactory
import com.devson.vedtune.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10

/**
 * AudioEngine is the central, modular audio processing orchestrator for VedTune.
 *
 * It is the ONLY component responsible for applying audio processing modules to ExoPlayer.
 *
 * ARCHITECTURE:
 *   PlaybackService
 *       ↓
 *   ExoPlayer (Decoder)
 *       ↓
 *   AudioEngine (Coordinator)
 *       ├── MasterVolumeProcessor
 *       ├── ReplayGainProcessor
 *       ├── EqualizerProcessor
 *       ├── BassBoostProcessor
 *       ├── LoudnessProcessor
 *       └── LimiterProcessor
 *       ↓
 *   Direct PCM Stream / Android AudioTrack Hardware Output
 *
 * GUARANTEES:
 * 1. 100% transparent offline playback by default (zero DSP active).
 * 2. Real-time settings updates without track restarts or ExoPlayer recreation.
 * 3. Independent enabling/disabling and graceful failure handling per processor.
 * 4. Extensible for future native DSP plugins without changing PlaybackService.
 */
@Singleton
class AudioEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    companion object {
        private const val TAG = "VedTune-AudioEngine"
    }

    private var player: ExoPlayer? = null
    private var currentAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private var currentSettings: AudioSettings = AudioSettingsFactory.defaults()
    private var lastLoggedFormatKey: String? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var settingsObservationJob: Job? = null

    // Core Audio Processors
    private val masterVolumeProcessor = MasterVolumeProcessor()
    private val replayGainProcessor = ReplayGainProcessor(context, masterVolumeProcessor)
    private val equalizerProcessor = EqualizerProcessor()
    private val bassBoostProcessor = BassBoostProcessor()
    private val loudnessProcessor = LoudnessProcessor()
    private val limiterProcessor = LimiterProcessor()

    private val customProcessors = CopyOnWriteArrayList<AudioProcessorModule>()

    private val allProcessors: List<AudioProcessorModule>
        get() = listOf(
            masterVolumeProcessor,
            replayGainProcessor,
            equalizerProcessor,
            bassBoostProcessor,
            loudnessProcessor,
            limiterProcessor
        ) + customProcessors

    private val playerListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            currentAudioSessionId = audioSessionId
            player?.let { p ->
                for (processor in allProcessors) {
                    try {
                        processor.onAttach(audioSessionId, p)
                        processor.onApplySettings(currentSettings, audioSessionId, p)
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) {
                            Log.e(TAG, "Error attaching processor ${processor.id} to session $audioSessionId", e)
                        }
                    }
                }
            }
            logDiagnostics("AudioSessionChanged")
        }

        override fun onTracksChanged(tracks: Tracks) {
            logFormatDiagnostics(tracks)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            player?.let { p ->
                val metadata = mediaItem?.mediaMetadata ?: p.mediaMetadata
                val uri = mediaItem?.localConfiguration?.uri
                for (processor in allProcessors) {
                    try {
                        processor.onTrackChanged(metadata, uri, p)
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) {
                            Log.e(TAG, "Error in onTrackChanged for processor ${processor.id}", e)
                        }
                    }
                }
            }
            logDiagnostics("MediaItemTransition")
        }

        override fun onVolumeChanged(volume: Float) {
            if (BuildConfig.DEBUG) {
                val db = if (volume > 0f) 20.0 * log10(volume.toDouble()) else -100.0
                Log.d(TAG, "Player Volume Changed: $volume (${String.format(Locale.US, "%.1f dB", db)})")
            }
        }
    }

    /**
     * Attaches the AudioEngine to an ExoPlayer instance and starts observing AudioSettings.
     */
    fun attach(exoPlayer: ExoPlayer) {
        detach()
        this.player = exoPlayer
        this.currentAudioSessionId = exoPlayer.audioSessionId
        exoPlayer.addListener(playerListener)

        for (processor in allProcessors) {
            try {
                processor.onAttach(currentAudioSessionId, exoPlayer)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Error attaching processor: ${processor.id}", e)
                }
            }
        }

        // Start observing AudioSettings reactively from DataStore repository
        settingsObservationJob?.cancel()
        settingsObservationJob = scope.launch {
            settingsRepository.audioSettings.collect { settings ->
                updateSettings(settings)
            }
        }

        logDiagnostics("AudioEngineAttached")
    }

    /**
     * Applies new AudioSettings across all processors without restarting playback.
     */
    fun updateSettings(settings: AudioSettings) {
        this.currentSettings = settings
        val p = player ?: return

        for (processor in allProcessors) {
            try {
                processor.onApplySettings(settings, currentAudioSessionId, p)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Error applying settings to processor: ${processor.id}", e)
                }
            }
        }

        logDiagnostics("SettingsUpdated")
    }

    /**
     * Registers an external / custom DSP module (e.g. future native DSP plugin).
     */
    fun registerCustomProcessor(processor: AudioProcessorModule) {
        customProcessors.add(processor)
        player?.let { p ->
            try {
                processor.onAttach(currentAudioSessionId, p)
                processor.onApplySettings(currentSettings, currentAudioSessionId, p)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Error registering custom processor: ${processor.id}", e)
                }
            }
        }
        logDiagnostics("CustomProcessorRegistered: ${processor.id}")
    }

    /**
     * Unregisters a custom DSP module.
     */
    fun unregisterCustomProcessor(processorId: String) {
        val iterator = customProcessors.iterator()
        while (iterator.hasNext()) {
            val processor = iterator.next()
            if (processor.id == processorId) {
                try {
                    processor.onRelease()
                } catch (e: Exception) {
                    // Ignored
                }
                customProcessors.remove(processor)
                break
            }
        }
        logDiagnostics("CustomProcessorUnregistered: $processorId")
    }

    /**
     * Returns the active audio session ID.
     */
    fun getAudioSessionId(): Int = currentAudioSessionId

    /**
     * Detaches the AudioEngine from the player and releases all audio effects.
     */
    fun detach() {
        settingsObservationJob?.cancel()
        settingsObservationJob = null

        player?.removeListener(playerListener)
        player = null

        for (processor in allProcessors) {
            try {
                processor.onRelease()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Error releasing processor: ${processor.id}", e)
                }
            }
        }

        currentAudioSessionId = C.AUDIO_SESSION_ID_UNSET
        lastLoggedFormatKey = null
    }

    /**
     * Fully releases all resources when the service is destroyed.
     */
    fun release() {
        detach()
        customProcessors.clear()
        scope.cancel()
    }

    private fun logDiagnostics(event: String) {
        if (!BuildConfig.DEBUG) return
        val p = player ?: return

        val activeDsp = allProcessors.filter { it.isEnabled && it.id != "master_volume" }.map { it.name }
        val dspDescription = if (activeDsp.isEmpty()) "None (100% Transparent Bit-Perfect Mode)" else activeDsp.joinToString()
        val volume = p.volume
        val db = if (volume > 0f) 20.0 * log10(volume.toDouble()) else -100.0

        Log.d(TAG, "[$event] SessionId: $currentAudioSessionId | Active DSP: $dspDescription | Master Gain: $volume (${String.format(Locale.US, "%.1f dB", db)})")
    }

    private fun logFormatDiagnostics(tracks: Tracks) {
        if (!BuildConfig.DEBUG) return

        var selectedAudioFormat: Format? = null
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO && group.isSelected) {
                for (i in 0 until group.length) {
                    if (group.isTrackSelected(i)) {
                        selectedAudioFormat = group.getTrackFormat(i)
                        break
                    }
                }
            }
        }

        if (selectedAudioFormat != null) {
            val mime = selectedAudioFormat.sampleMimeType ?: "audio/unknown"
            val sampleRate = selectedAudioFormat.sampleRate
            val channelCount = selectedAudioFormat.channelCount
            val bitrate = selectedAudioFormat.bitrate
            val pcmEncoding = when (selectedAudioFormat.pcmEncoding) {
                C.ENCODING_PCM_8BIT -> "8-bit PCM"
                C.ENCODING_PCM_16BIT -> "16-bit PCM"
                C.ENCODING_PCM_24BIT -> "24-bit PCM"
                C.ENCODING_PCM_32BIT -> "32-bit PCM"
                C.ENCODING_PCM_FLOAT -> "32-bit Float PCM"
                else -> "Default/Direct"
            }

            val formatKey = "$mime|$sampleRate|$channelCount|$bitrate|$pcmEncoding"
            if (formatKey != lastLoggedFormatKey) {
                lastLoggedFormatKey = formatKey
                val bitrateStr = if (bitrate > 0) "${bitrate / 1000} kbps" else "VBR/N/A"
                val sampleRateStr = if (sampleRate > 0) "$sampleRate Hz" else "N/A"
                val channelsStr = when (channelCount) {
                    1 -> "1 ch (Mono)"
                    2 -> "2 ch (Stereo)"
                    6 -> "6 ch (5.1 Surround)"
                    8 -> "8 ch (7.1 Surround)"
                    else -> "$channelCount ch"
                }

                Log.d(TAG, "Audio Stream Format: $mime | $sampleRateStr | $channelsStr | $pcmEncoding | $bitrateStr")
            }
        }
    }
}
