package com.devson.vedtune.player

import android.content.Context
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.devson.vedtune.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10

/**
 * Modular interface for future DSP extensions (Equalizer, Spatializer, etc.).
 * All modules are inactive by default to maintain bit-perfect transparent audio reproduction.
 */
interface AudioDspModule {
    val id: String
    val name: String
    val isEnabled: Boolean
    fun onAttach(audioSessionId: Int, player: ExoPlayer)
    fun onRelease()
}

/**
 * AudioPipelineManager is the central audio coordinator for VedTune.
 *
 * Default Audio Signal Path:
 *   MediaStore Audio Source
 *     -> Media3 / ExoPlayer Decoder
 *     -> Direct PCM Stream (Unity Gain: 1.0f, Zero DSP)
 *     -> Android AudioTrack / Hardware Output
 *
 * It provides:
 * 1. Bit-perfect, natural audio playback without artificial compression, limiting, or dynamic EQ.
 * 2. Audio session and stream format tracking.
 * 3. Extensible, inactive-by-default DSP module registry for future enhancements.
 * 4. Event-driven diagnostic logging for DEBUG builds.
 */
@Singleton
class AudioPipelineManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "VedTune-AudioPipeline"
    }

    private var player: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val dspModules = CopyOnWriteArrayList<AudioDspModule>()

    private var currentAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private var lastLoggedFormatKey: String? = null

    private val playerListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            currentAudioSessionId = audioSessionId
            logPipelineDiagnostics("AudioSessionChanged")
            notifyDspModulesSessionChanged(audioSessionId)
        }

        override fun onTracksChanged(tracks: Tracks) {
            logFormatDiagnostics(tracks)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Ensure transparent unity gain on track transitions
            player?.let { p ->
                if (p.volume != 1.0f && dspModules.none { it.isEnabled }) {
                    p.volume = 1.0f
                }
            }
            logPipelineDiagnostics("MediaItemTransition")
        }

        override fun onVolumeChanged(volume: Float) {
            if (BuildConfig.DEBUG) {
                val db = if (volume > 0f) 20.0 * log10(volume.toDouble()) else -100.0
                Log.d(TAG, "Master Volume Changed: $volume (${String.format(Locale.US, "%.1f", db)} dB)")
            }
        }
    }

    /**
     * Attaches the AudioPipelineManager to an ExoPlayer instance.
     */
    fun attachPlayer(exoPlayer: ExoPlayer) {
        detachPlayer()
        this.player = exoPlayer
        this.currentAudioSessionId = exoPlayer.audioSessionId
        exoPlayer.addListener(playerListener)

        // Ensure default transparent master volume
        if (dspModules.none { it.isEnabled }) {
            exoPlayer.volume = 1.0f
        }

        logPipelineDiagnostics("PlayerAttached")
    }

    /**
     * Registers a DSP module for future audio processing capabilities.
     */
    fun registerDspModule(module: AudioDspModule) {
        dspModules.add(module)
        player?.let { p ->
            if (module.isEnabled && currentAudioSessionId != C.AUDIO_SESSION_ID_UNSET && currentAudioSessionId != 0) {
                module.onAttach(currentAudioSessionId, p)
            }
        }
        logPipelineDiagnostics("DspModuleRegistered: ${module.id}")
    }

    /**
     * Unregisters a DSP module.
     */
    fun unregisterDspModule(moduleId: String) {
        val iterator = dspModules.iterator()
        while (iterator.hasNext()) {
            val module = iterator.next()
            if (module.id == moduleId) {
                module.onRelease()
                dspModules.remove(module)
                break
            }
        }
        logPipelineDiagnostics("DspModuleUnregistered: $moduleId")
    }

    /**
     * Returns the current audio session ID.
     */
    fun getAudioSessionId(): Int = currentAudioSessionId

    /**
     * Logs current pipeline diagnostics on state change in DEBUG builds.
     */
    private fun logPipelineDiagnostics(triggerReason: String) {
        if (!BuildConfig.DEBUG) return

        val p = player ?: return
        val activeDsp = dspModules.filter { it.isEnabled }.map { it.name }
        val dspStatus = if (activeDsp.isEmpty()) "None (Transparent Bit-Perfect Output)" else activeDsp.joinToString()
        val volume = p.volume
        val db = if (volume > 0f) 20.0 * log10(volume.toDouble()) else -100.0

        Log.d(TAG, "[$triggerReason] SessionId: $currentAudioSessionId | Active DSP: $dspStatus | Master Gain: $volume (${String.format(Locale.US, "%.1f", db)} dB)")
    }

    /**
     * Logs audio format specifications from the active audio track in DEBUG builds.
     */
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
            val pcmEncoding = formatPcmEncoding(selectedAudioFormat.pcmEncoding)

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

    private fun formatPcmEncoding(encoding: Int): String {
        return when (encoding) {
            C.ENCODING_PCM_8BIT -> "8-bit PCM"
            C.ENCODING_PCM_16BIT -> "16-bit PCM"
            C.ENCODING_PCM_24BIT -> "24-bit PCM"
            C.ENCODING_PCM_32BIT -> "32-bit PCM"
            C.ENCODING_PCM_FLOAT -> "32-bit Float PCM"
            else -> "Default/Direct"
        }
    }

    private fun notifyDspModulesSessionChanged(audioSessionId: Int) {
        val p = player ?: return
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == 0) return

        for (module in dspModules) {
            if (module.isEnabled) {
                try {
                    module.onAttach(audioSessionId, p)
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Error attaching DSP module: ${module.id}", e)
                    }
                }
            }
        }
    }

    /**
     * Releases player listeners, registered DSP modules, and coroutine scope.
     */
    fun release() {
        detachPlayer()
        for (module in dspModules) {
            try {
                module.onRelease()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Error releasing DSP module: ${module.id}", e)
                }
            }
        }
        dspModules.clear()
        scope.cancel()
    }

    private fun detachPlayer() {
        player?.removeListener(playerListener)
        player = null
        lastLoggedFormatKey = null
    }
}
