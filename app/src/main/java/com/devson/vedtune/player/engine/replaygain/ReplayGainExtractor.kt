package com.devson.vedtune.player.engine.replaygain

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.media3.common.MediaMetadata
import com.devson.vedtune.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.id3.AbstractID3v2Frame
import org.jaudiotagger.tag.id3.ID3v22Tag
import org.jaudiotagger.tag.id3.ID3v23Tag
import org.jaudiotagger.tag.id3.ID3v24Tag
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX
import org.jaudiotagger.tag.mp4.Mp4Tag
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.pow

/**
 * Asynchronously extracts ReplayGain metadata from MediaMetadata, ContentResolver, and audio file tags.
 *
 * Designed to strictly adhere to Android MediaStore guidelines:
 * - Never assumes content:// URIs can be converted via uri.path
 * - Uses ContentResolver queries with scoped try-with-resources
 * - Operates entirely on Dispatchers.IO
 */
@Singleton
class ReplayGainExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "VedTune-RGAExtractor"

        // Common ReplayGain Tag Keys across formats
        private val TRACK_GAIN_KEYS = listOf(
            "REPLAYGAIN_TRACK_GAIN",
            "replaygain_track_gain",
            "replaygain-track-gain",
            "TRACK_GAIN",
            "RG_RADIO_GAIN"
        )
        private val TRACK_PEAK_KEYS = listOf(
            "REPLAYGAIN_TRACK_PEAK",
            "replaygain_track_peak",
            "replaygain-track-peak",
            "TRACK_PEAK",
            "RG_RADIO_PEAK"
        )
        private val ALBUM_GAIN_KEYS = listOf(
            "REPLAYGAIN_ALBUM_GAIN",
            "replaygain_album_gain",
            "replaygain-album-gain",
            "ALBUM_GAIN",
            "RG_AUDIOPHILE_GAIN"
        )
        private val ALBUM_PEAK_KEYS = listOf(
            "REPLAYGAIN_ALBUM_PEAK",
            "replaygain_album_peak",
            "replaygain-album-peak",
            "ALBUM_PEAK",
            "RG_AUDIOPHILE_PEAK"
        )

        /**
         * Parses ReplayGain gain string (e.g. "-6.50 dB", "+2.3 dB", "-4.120000").
         */
        fun parseGainDb(raw: String?): Float? {
            if (raw.isNullOrBlank()) return null
            val cleaned = raw.lowercase(Locale.US)
                .replace("db", "")
                .replace(",", ".")
                .trim()
            val parsed = cleaned.toFloatOrNull()
            return if (parsed != null && parsed.isFinite()) parsed else null
        }

        /**
         * Parses ReplayGain peak string (e.g. "0.988000", "1.054", "-0.1 dB").
         */
        fun parsePeak(raw: String?): Float? {
            if (raw.isNullOrBlank()) return null
            val cleaned = raw.lowercase(Locale.US)
                .replace(",", ".")
                .trim()

            if (cleaned.endsWith("db")) {
                val dbStr = cleaned.replace("db", "").trim()
                val dbVal = dbStr.toFloatOrNull() ?: return null
                if (!dbVal.isFinite()) return null
                // Convert dB peak to linear peak: 10^(dB / 20)
                return 10.0.pow(dbVal.toDouble() / 20.0).toFloat().takeIf { it.isFinite() && it > 0f }
            }

            val parsed = cleaned.toFloatOrNull()
            return if (parsed != null && parsed.isFinite() && parsed > 0.0f) parsed else null
        }

        /**
         * Parses Apple SoundCheck iTunNORM comments into standard ReplayGain values.
         *
         * Format: 8 to 10 hex numbers separated by spaces:
         *   " 0000042A 0000042A 00001B93 00001B93 00025175 00025175 00007FFF 00007FFF 00000000 00000000"
         */
        fun parseSoundCheck(iTunNorm: String?): ReplayGainInfo? {
            if (iTunNorm.isNullOrBlank()) return null
            return try {
                val tokens = iTunNorm.trim().split("\\s+".toRegex())
                if (tokens.size >= 8) {
                    // Tokens 0 & 1: Volume adjustment 1/1000 dB
                    val leftGainInt = tokens[0].toLong(16)
                    val rightGainInt = tokens[1].toLong(16)
                    val gainInt = (leftGainInt + rightGainInt) / 2.0

                    // Formula: SoundCheck volume in dB = -10 * log10(val / 1000.0)
                    val gainDb = if (gainInt > 0) (-10.0 * log10(gainInt / 1000.0)).toFloat() else 0.0f

                    // Tokens 6 & 7: Peak amplitude out of 32768 (15-bit unsigned) or 65536
                    val leftPeakInt = tokens[6].toLong(16)
                    val rightPeakInt = tokens[7].toLong(16)
                    val maxPeakInt = maxOf(leftPeakInt, rightPeakInt)
                    val peakLinear = (maxPeakInt.toFloat() / 32768.0f).coerceAtLeast(0.0f)

                    ReplayGainInfo(
                        trackGainDb = gainDb.takeIf { it.isFinite() },
                        trackPeak = peakLinear.takeIf { it.isFinite() && it > 0f }
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Extracts ReplayGain info from available metadata and content URI on [Dispatchers.IO].
     */
    suspend fun extract(metadata: MediaMetadata?, contentUri: Uri?): ReplayGainInfo = withContext(Dispatchers.IO) {
        // 1. Try extracting from MediaMetadata extras if already parsed
        val fromExtras = extractFromMediaMetadata(metadata)
        if (fromExtras.hasReplayGain) {
            return@withContext fromExtras
        }

        // 2. If contentUri is provided, inspect physical file/stream
        if (contentUri != null) {
            val fromUri = extractFromContentUri(contentUri)
            if (fromUri.hasReplayGain) {
                return@withContext fromUri
            }
        }

        ReplayGainInfo.EMPTY
    }

    private fun extractFromMediaMetadata(metadata: MediaMetadata?): ReplayGainInfo {
        val extras = metadata?.extras ?: return ReplayGainInfo.EMPTY

        val trackGain = extras.getString("REPLAYGAIN_TRACK_GAIN")?.let { parseGainDb(it) }
        val trackPeak = extras.getString("REPLAYGAIN_TRACK_PEAK")?.let { parsePeak(it) }
        val albumGain = extras.getString("REPLAYGAIN_ALBUM_GAIN")?.let { parseGainDb(it) }
        val albumPeak = extras.getString("REPLAYGAIN_ALBUM_PEAK")?.let { parsePeak(it) }

        return ReplayGainInfo(
            trackGainDb = trackGain,
            trackPeak = trackPeak,
            albumGainDb = albumGain,
            albumPeak = albumPeak
        )
    }

    private fun extractFromContentUri(uri: Uri): ReplayGainInfo {
        val contentResolver = context?.contentResolver ?: return ReplayGainInfo.EMPTY

        // Safe resolution of filesystem path via MediaStore ContentResolver query
        var directFilePath: String? = null
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            try {
                contentResolver.query(
                    uri,
                    arrayOf(MediaStore.Audio.Media.DATA),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val colIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                        if (colIdx >= 0) {
                            directFilePath = cursor.getString(colIdx)
                        }
                    }
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "Could not query MediaStore path for $uri: ${e.message}")
                }
            }
        } else if (uri.scheme == "file") {
            directFilePath = uri.path
        }

        // Attempt reading via Jaudiotagger if file is accessible
        if (!directFilePath.isNullOrBlank()) {
            val file = File(directFilePath)
            if (file.exists() && file.canRead()) {
                val fromFile = extractFromAudioFile(file)
                if (fromFile.hasReplayGain) {
                    return fromFile
                }
            }
        }

        // Fallback: Attempt reading using ParcelFileDescriptor if direct file path failed
        try {
            contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                // Check if file descriptor is readable
                if (pfd.fileDescriptor.valid()) {
                    // In future, custom stream parser can be invoked here if needed
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Error opening FileDescriptor for $uri: ${e.message}")
            }
        }

        return ReplayGainInfo.EMPTY
    }

    private fun extractFromAudioFile(file: File): ReplayGainInfo {
        return try {
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tag ?: return ReplayGainInfo.EMPTY

            var trackGain: Float? = null
            var trackPeak: Float? = null
            var albumGain: Float? = null
            var albumPeak: Float? = null

            // 1. Direct Tag Lookups (Vorbis / FLAC / Generic)
            for (key in TRACK_GAIN_KEYS) {
                val value = tag.getFirst(key)
                if (!value.isNullOrBlank()) {
                    trackGain = parseGainDb(value)
                    if (trackGain != null) break
                }
            }
            for (key in TRACK_PEAK_KEYS) {
                val value = tag.getFirst(key)
                if (!value.isNullOrBlank()) {
                    trackPeak = parsePeak(value)
                    if (trackPeak != null) break
                }
            }
            for (key in ALBUM_GAIN_KEYS) {
                val value = tag.getFirst(key)
                if (!value.isNullOrBlank()) {
                    albumGain = parseGainDb(value)
                    if (albumGain != null) break
                }
            }
            for (key in ALBUM_PEAK_KEYS) {
                val value = tag.getFirst(key)
                if (!value.isNullOrBlank()) {
                    albumPeak = parsePeak(value)
                    if (albumPeak != null) break
                }
            }

            // 2. ID3v2 TXXX User-Defined Text Frames
            if (tag is ID3v24Tag || tag is ID3v23Tag || tag is ID3v22Tag) {
                val id3Fields = tag.fields
                while (id3Fields.hasNext()) {
                    val field = id3Fields.next()
                    if (field is AbstractID3v2Frame) {
                        val body = field.body
                        if (body is FrameBodyTXXX) {
                            val desc = body.description.uppercase(Locale.US)
                            val text = body.text
                            when {
                                desc in TRACK_GAIN_KEYS || desc == "REPLAYGAIN_TRACK_GAIN" -> {
                                    if (trackGain == null) trackGain = parseGainDb(text)
                                }
                                desc in TRACK_PEAK_KEYS || desc == "REPLAYGAIN_TRACK_PEAK" -> {
                                    if (trackPeak == null) trackPeak = parsePeak(text)
                                }
                                desc in ALBUM_GAIN_KEYS || desc == "REPLAYGAIN_ALBUM_GAIN" -> {
                                    if (albumGain == null) albumGain = parseGainDb(text)
                                }
                                desc in ALBUM_PEAK_KEYS || desc == "REPLAYGAIN_ALBUM_PEAK" -> {
                                    if (albumPeak == null) albumPeak = parsePeak(text)
                                }
                            }
                        }
                    }
                }
            }

            // 3. MP4 / iTunes Atoms & SoundCheck iTunNORM
            if (tag is Mp4Tag) {
                val iTunNorm = tag.getFirst("----:com.apple.iTunes:iTunNORM").ifBlank {
                    tag.getFirst("iTunNORM")
                }
                if (iTunNorm.isNotBlank() && trackGain == null) {
                    val soundCheckInfo = parseSoundCheck(iTunNorm)
                    if (soundCheckInfo != null) {
                        trackGain = soundCheckInfo.trackGainDb
                        trackPeak = soundCheckInfo.trackPeak
                    }
                }
            }

            ReplayGainInfo(
                trackGainDb = trackGain,
                trackPeak = trackPeak,
                albumGainDb = albumGain,
                albumPeak = albumPeak
            )
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Could not extract tags from file ${file.name}: ${e.message}")
            }
            ReplayGainInfo.EMPTY
        }
    }
}
