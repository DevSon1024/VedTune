package com.devson.vedtune.player

import android.content.Context
import android.media.audiofx.DynamicsProcessing
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * VolumeNormalizationManager handles two-tier audio loudness normalization for VedTune:
 *
 * Tier 1: ReplayGain Metadata Extraction.
 * Checks audio file metadata for standard REPLAYGAIN_TRACK_GAIN or REPLAYGAIN_ALBUM_GAIN tags.
 * Converts decibel value (e.g. -6.5 dB) into linear volume multiplier:
 *     linearGain = 10^(dB / 20)
 * Applies this directly to ExoPlayer via player.volume = linearGain.
 *
 * Tier 2: Real-Time Dynamics Processing (Fallback).
 * If no ReplayGain tag is found, fallback to Android DynamicsProcessing API (Android 9.0+ / API 28+).
 * Employs a Multi-Band Compressor (MBC) configured as an Auto Gain Control (AGC) & Limiter
 * to achieve consistent ~-14 LUFS playback without distortion or clipping.
 */
@Singleton
class VolumeNormalizationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var player: ExoPlayer? = null
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var isNormalizationEnabled: Boolean = true
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            mediaItem?.let { processMediaItem(it) }
        }
    }

    /**
     * Attaches VolumeNormalizationManager to the given ExoPlayer instance.
     */
    fun attachPlayer(exoPlayer: ExoPlayer) {
        detachPlayer()
        this.player = exoPlayer
        exoPlayer.addListener(playerListener)

        exoPlayer.currentMediaItem?.let { processMediaItem(it) }
        setupDynamicsProcessing()
    }

    /**
     * Enables or disables loudness normalization.
     */
    fun setEnabled(enabled: Boolean) {
        this.isNormalizationEnabled = enabled
        if (!enabled) {
            resetPlayerVolume()
            setDynamicsProcessingEnabled(false)
        } else {
            player?.currentMediaItem?.let { processMediaItem(it) }
        }
    }

    /**
     * Processes track metadata for ReplayGain tags.
     */
    private fun processMediaItem(mediaItem: MediaItem) {
        val targetPlayer = player ?: return
        if (!isNormalizationEnabled) {
            resetPlayerVolume()
            setDynamicsProcessingEnabled(false)
            return
        }

        scope.launch {
            val replayGainDb = withContext(Dispatchers.IO) {
                extractReplayGainDb(mediaItem)
            }

            if (replayGainDb != null) {
                // Tier 1: ReplayGain Tag Found
                // Math: linearGain = 10^(dB / 20)
                // For example:
                //   -6.0 dB -> 10^(-0.3) ≈ 0.501 (50% volume)
                //   -3.0 dB -> 10^(-0.15) ≈ 0.708 (70.8% volume)
                //   +2.0 dB -> clamped to 1.0f to prevent digital clipping / distortion
                val linearGain = 10.0.pow(replayGainDb / 20.0).toFloat().coerceIn(0.0f, 1.0f)
                withContext(Dispatchers.Main) {
                    targetPlayer.volume = linearGain
                    setDynamicsProcessingEnabled(false) // Disable DSP when ReplayGain tag is present
                }
            } else {
                // Tier 2 Fallback: ReplayGain tag missing
                withContext(Dispatchers.Main) {
                    resetPlayerVolume()
                    setDynamicsProcessingEnabled(true) // Enable Real-time Dynamics Processing AGC
                }
            }
        }
    }

    /**
     * Extracts ReplayGain dB value from MediaItem or file ID3/Vorbis tags.
     */
    private fun extractReplayGainDb(mediaItem: MediaItem): Double? {
        // 1. Check ExoPlayer MediaMetadata extras & entries
        val metadataGain = parseReplayGainFromMediaMetadata(mediaItem.mediaMetadata)
        if (metadataGain != null) return metadataGain

        // 2. Read file tags via jAudioTagger on Dispatchers.IO
        val localPath = mediaItem.localConfiguration?.uri?.path ?: return null
        val audioFile = File(localPath)
        if (!audioFile.exists()) return null

        try {
            val jFile = AudioFileIO.read(audioFile)
            val tag = jFile.tag ?: return null

            // Check standard REPLAYGAIN_TRACK_GAIN or REPLAYGAIN_ALBUM_GAIN
            val trackGainStr = tag.getFirst("REPLAYGAIN_TRACK_GAIN")
                .ifBlank { tag.getFirst("REPLAYGAIN_ALBUM_GAIN") }

            if (trackGainStr.isNotBlank()) {
                return parseDbString(trackGainStr)
            }

            // Check ID3v2 TXXX frames explicitly
            if (tag is ID3v24Tag) {
                val fields = tag.getFields("TXXX")
                for (field in fields) {
                    if (field is AbstractID3v2Frame) {
                        val body = field.body
                        if (body is FrameBodyTXXX) {
                            val description = body.description
                            if (description.equals("REPLAYGAIN_TRACK_GAIN", ignoreCase = true) ||
                                description.equals("REPLAYGAIN_ALBUM_GAIN", ignoreCase = true)
                            ) {
                                val text = body.text
                                return parseDbString(text)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    private fun parseReplayGainFromMediaMetadata(mediaMetadata: MediaMetadata): Double? {
        val extras = mediaMetadata.extras ?: return null
        val trackGain = extras.getString("REPLAYGAIN_TRACK_GAIN")
            ?: extras.getString("REPLAYGAIN_ALBUM_GAIN")
        return trackGain?.let { parseDbString(it) }
    }

    /**
     * Converts a ReplayGain decibel string (e.g. "-6.5 dB", "+1.2 dB", "-6.50") into Double.
     */
    private fun parseDbString(raw: String): Double? {
        val cleanStr = raw.lowercase(Locale.US)
            .replace("db", "")
            .trim()
        return cleanStr.toDoubleOrNull()
    }

    private fun resetPlayerVolume() {
        player?.volume = 1.0f
    }

    /**
     * Initializes Android DynamicsProcessing AudioEffect attached to ExoPlayer's audioSessionId.
     */
    private fun setupDynamicsProcessing() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return

        val targetPlayer = player ?: return
        val audioSessionId = targetPlayer.audioSessionId
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == 0) return

        try {
            releaseDynamicsProcessing()
            val config = createDynamicsProcessingConfig()
            dynamicsProcessing = DynamicsProcessing(0, audioSessionId, config).apply {
                enabled = isNormalizationEnabled
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Creates a Multi-Band Compressor (MBC) & Limiter DynamicsProcessing.Config
     * to perform real-time loudness normalization to ~-14 LUFS.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private fun createDynamicsProcessingConfig(): DynamicsProcessing.Config {
        val channelCount = 2 // Stereo channels
        val bandCount = 4    // 4 Frequency bands

        // Enable Multi-Band Compressor (MBC) and Limiter
        val builder = DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
            channelCount,
            false, // PreEQ disabled
            0,
            true,  // Multi-Band Compressor (MBC) enabled
            bandCount,
            false, // PostEQ disabled
            0,
            true   // Limiter enabled
        )

        // Define 4-band MBC cutoff frequencies (Hz): Sub-Bass, Bass/Mid, Mid/Treble, Highs
        val cutoffFrequencies = floatArrayOf(200f, 1000f, 5000f)

        // Multi-Band Compressor Parameters for AGC/Normalization:
        // - Quiet signals (<-24 dB) receive gentle boost (Ratio 2.5:1, PostGain +3.0 dB)
        // - Medium signals (-18 dB to -6 dB) compressed softly (Ratio 2.0:1)
        // - Loud signals (> -3 dB) limited (Ratio 10:1) to prevent clipping
        for (channel in 0 until channelCount) {
            val mbcStage = DynamicsProcessing.Mbc(true, true, bandCount)

            for (bandIndex in 0 until bandCount) {
                val cutoff = if (bandIndex < 3) cutoffFrequencies[bandIndex] else 20000f
                val mbcBand = DynamicsProcessing.MbcBand(
                    true,                // inUse
                    cutoff,              // cutoffFrequency (Hz)
                    10f,                 // attackTime (ms)
                    100f,                // releaseTime (ms)
                    2.5f,                // ratio
                    -24f,                // threshold (dB)
                    6f,                  // kneeWidth (dB)
                    -60f,                // noiseGateThreshold (dB)
                    1.0f,                // expanderRatio
                    0f,                  // preGain (dB)
                    3.0f                 // postGain (dB)
                )
                mbcStage.setBand(bandIndex, mbcBand)
            }

            builder.setMbcByChannelIndex(channel, mbcStage)

            // Configure Limiter band on output to prevent peak digital clipping
            val limiter = DynamicsProcessing.Limiter(
                true,      // inUse
                true,      // enabled
                0,         // linkGroup
                1f,        // attackTime (ms)
                50f,       // releaseTime (ms)
                10f,       // ratio
                -2.0f,     // threshold (dB)
                0f         // postGain (dB)
            )
            builder.setLimiterByChannelIndex(channel, limiter)
        }

        return builder.build()
    }

    private fun setDynamicsProcessingEnabled(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                dynamicsProcessing?.enabled = enabled
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun releaseDynamicsProcessing() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                dynamicsProcessing?.enabled = false
                dynamicsProcessing?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                dynamicsProcessing = null
            }
        }
    }

    /**
     * Releases player listeners, audio effects, and coroutine scope to prevent memory leaks.
     */
    fun release() {
        detachPlayer()
        scope.cancel()
    }

    private fun detachPlayer() {
        player?.removeListener(playerListener)
        releaseDynamicsProcessing()
        resetPlayerVolume()
        player = null
    }
}
