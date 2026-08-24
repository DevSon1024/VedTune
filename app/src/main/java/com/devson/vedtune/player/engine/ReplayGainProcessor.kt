package com.devson.vedtune.player.engine

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import com.devson.vedtune.domain.model.AudioSettings
import com.devson.vedtune.domain.model.ReplayGainMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.id3.AbstractID3v2Frame
import org.jaudiotagger.tag.id3.ID3v24Tag
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX
import java.io.File
import java.util.Locale
import kotlin.math.pow

/**
 * Parses ReplayGain track/album gain tags on background thread and applies linear volume scaling.
 */
class ReplayGainProcessor(
    private val context: Context,
    private val masterVolumeProcessor: MasterVolumeProcessor
) : AudioProcessorModule {
    override val id: String = "replay_gain"
    override val name: String = "ReplayGain"
    override var isEnabled: Boolean = false
        private set

    private var currentSettings: AudioSettings = AudioSettings.FactoryDefaults
    private var lastMetadata: MediaMetadata? = null
    private var lastUri: Uri? = null
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
            }
        } else if (oldSettings != settings || !wasEnabled) {
            processCurrentTrack(player)
        }
    }

    override fun onTrackChanged(mediaMetadata: MediaMetadata, localUri: Uri?, player: ExoPlayer) {
        lastMetadata = mediaMetadata
        lastUri = localUri
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

        val metadata = lastMetadata ?: player.currentMediaItem?.mediaMetadata
        val uri = lastUri ?: player.currentMediaItem?.localConfiguration?.uri

        extractionJob?.cancel()
        extractionJob = scope.launch {
            val gainDb = withContext(Dispatchers.IO) {
                extractReplayGainDb(metadata, uri, currentSettings.replayGainMode)
            }

            if (gainDb != null) {
                val totalDb = gainDb + currentSettings.replayGainPreampDb
                var multiplier = 10.0.pow(totalDb / 20.0).toFloat()
                if (currentSettings.replayGainPreventClipping) {
                    multiplier = multiplier.coerceAtMost(1.0f)
                }
                multiplier = multiplier.coerceIn(0.0f, 2.0f)
                masterVolumeProcessor.setDspVolumeMultiplier(multiplier, player)
            } else {
                masterVolumeProcessor.setDspVolumeMultiplier(1.0f, player)
            }
        }
    }

    private fun extractReplayGainDb(metadata: MediaMetadata?, uri: Uri?, mode: ReplayGainMode): Double? {
        // 1. Check MediaMetadata extras
        if (metadata?.extras != null) {
            val trackGain = metadata.extras?.getString("REPLAYGAIN_TRACK_GAIN")
            val albumGain = metadata.extras?.getString("REPLAYGAIN_ALBUM_GAIN")
            val selected = if (mode == ReplayGainMode.ALBUM) albumGain ?: trackGain else trackGain ?: albumGain
            if (selected != null) {
                parseDbString(selected)?.let { return it }
            }
        }

        // 2. Read physical file tags via jaudiotagger on Dispatchers.IO
        val path = uri?.path ?: return null
        val file = File(path)
        if (!file.exists()) return null

        return try {
            val jFile = AudioFileIO.read(file)
            val tag = jFile.tag ?: return null

            val trackGain = tag.getFirst("REPLAYGAIN_TRACK_GAIN").ifBlank { tag.getFirst("replaygain_track_gain") }
            val albumGain = tag.getFirst("REPLAYGAIN_ALBUM_GAIN").ifBlank { tag.getFirst("replaygain_album_gain") }

            val raw = if (mode == ReplayGainMode.ALBUM) albumGain.ifBlank { trackGain } else trackGain.ifBlank { albumGain }
            if (raw.isNotBlank()) {
                parseDbString(raw)
            } else if (tag is ID3v24Tag) {
                var foundDb: Double? = null
                for (field in tag.getFields("TXXX")) {
                    if (field is AbstractID3v2Frame) {
                        val body = field.body
                        if (body is FrameBodyTXXX) {
                            val desc = body.description
                            val match = if (mode == ReplayGainMode.ALBUM) {
                                desc.equals("REPLAYGAIN_ALBUM_GAIN", ignoreCase = true) || desc.equals("REPLAYGAIN_TRACK_GAIN", ignoreCase = true)
                            } else {
                                desc.equals("REPLAYGAIN_TRACK_GAIN", ignoreCase = true) || desc.equals("REPLAYGAIN_ALBUM_GAIN", ignoreCase = true)
                            }
                            if (match) {
                                foundDb = parseDbString(body.text)
                                if (foundDb != null) break
                            }
                        }
                    }
                }
                foundDb
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDbString(raw: String): Double? {
        val cleanStr = raw.lowercase(Locale.US)
            .replace("db", "")
            .trim()
        return cleanStr.toDoubleOrNull()
    }

    override fun onRelease() {
        extractionJob?.cancel()
        scope.cancel()
        isEnabled = false
    }
}
