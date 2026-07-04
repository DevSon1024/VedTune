package com.devson.vedtune.data.metadata

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.devson.vedtune.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.mp3.MP3File
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioMetadataExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun extractMetadata(songId: Long): ExtractedMetadata = withContext(ioDispatcher) {
        var tempFile: File? = null
        var fileSizeMb = 0.0
        var filePath = ""
        
        // 1. Query path and size from MediaStore
        try {
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
            val projection = arrayOf(MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.SIZE)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val dataIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                    val sizeIndex = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                    if (dataIndex != -1) filePath = cursor.getString(dataIndex) ?: ""
                    if (sizeIndex != -1) {
                        val sizeBytes = cursor.getLong(sizeIndex)
                        fileSizeMb = sizeBytes / (1024.0 * 1024.0)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Read tags using jaudiotagger from a cached temp file
        var composer = ""
        var genre = ""
        var lyricist = ""
        var trackNumber = ""
        var discNumber = ""
        var comment = ""
        var year = ""
        var bitrate = ""
        var sampleRate = ""
        var bitsPerSample = ""
        var format = ""
        var encodingType = ""
        var channels = ""
        var recordLabel = ""
        var copyright = ""
        var language = ""
        var mood = ""
        var vbrIndicator = ""
        var replayGain = ""
        var audioHash = ""

        var audioFile: org.jaudiotagger.audio.AudioFile? = null
        var isTempUsed = false

        if (filePath.isNotEmpty()) {
            val actualFile = File(filePath)
            try {
                audioFile = AudioFileIO.read(actualFile)
            } catch (e: Exception) {
                // Fallback to temp file with dynamic extension
                try {
                    val ext = actualFile.extension.ifEmpty { "mp3" }
                    tempFile = File(context.cacheDir, "temp_metadata_extract_${songId}.$ext")
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw IOException("Could not open input stream")

                    audioFile = AudioFileIO.read(tempFile)
                    isTempUsed = true
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }

        if (audioFile != null) {
            try {
                val tag = if (audioFile is MP3File) {
                    audioFile.iD3v2Tag ?: audioFile.tag
                } else {
                    audioFile.tag
                }
                if (tag != null) {
                    composer = tag.getFirst(FieldKey.COMPOSER).orEmpty()
                    genre = tag.getFirst(FieldKey.GENRE).orEmpty()
                    lyricist = tag.getFirst(FieldKey.LYRICIST).orEmpty()
                    trackNumber = tag.getFirst(FieldKey.TRACK).orEmpty()
                    discNumber = tag.getFirst(FieldKey.DISC_NO).orEmpty()
                    comment = tag.getFirst(FieldKey.COMMENT).orEmpty()
                    year = tag.getFirst(FieldKey.YEAR).orEmpty()
                    
                    // New tags
                    recordLabel = tag.getFirst(FieldKey.RECORD_LABEL).orEmpty()
                    copyright = tag.getFirst("COPYRIGHT").ifEmpty { tag.getFirst("TCOP") }.ifEmpty { tag.getFirst("cprt") }.orEmpty()
                    language = tag.getFirst(FieldKey.LANGUAGE).orEmpty()
                    mood = tag.getFirst(FieldKey.MOOD).orEmpty()

                    // ReplayGain extraction
                    val trackGain = tag.getFirst("REPLAYGAIN_TRACK_GAIN").ifEmpty { tag.getFirst("replaygain_track_gain") }.orEmpty()
                    val albumGain = tag.getFirst("REPLAYGAIN_ALBUM_GAIN").ifEmpty { tag.getFirst("replaygain_album_gain") }.orEmpty()
                    replayGain = when {
                        trackGain.isNotEmpty() && albumGain.isNotEmpty() -> "Track: $trackGain, Album: $albumGain"
                        trackGain.isNotEmpty() -> "Track: $trackGain"
                        albumGain.isNotEmpty() -> "Album: $albumGain"
                        else -> ""
                    }
                }
                
                val audioHeader = audioFile.audioHeader
                if (audioHeader != null) {
                    bitrate = runCatching { audioHeader.bitRate }.getOrDefault("")
                    sampleRate = runCatching { audioHeader.sampleRate }.getOrDefault("")
                    bitsPerSample = runCatching { audioHeader.bitsPerSample.toString() }.getOrDefault("")
                    format = runCatching { audioHeader.format }.getOrDefault("")
                    encodingType = runCatching { audioHeader.encodingType }.getOrDefault("")
                    channels = runCatching { audioHeader.channels }.getOrDefault("")
                    
                    // VBR/CBR Indicator
                    vbrIndicator = if (audioHeader.isVariableBitRate) "(VBR)" else "(CBR)"
                }

                // Safe & efficient MD5 generation using a buffer
                try {
                    val fileToHash = if (isTempUsed && tempFile != null) tempFile else File(filePath)
                    if (fileToHash.exists()) {
                        audioHash = calculateMd5(fileToHash)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (isTempUsed) {
                    tempFile?.delete()
                }
            }
        }

        ExtractedMetadata(
            composer = composer,
            genre = genre,
            lyricist = lyricist,
            trackNumber = trackNumber,
            discNumber = discNumber,
            comment = comment,
            year = year,
            bitrate = bitrate,
            sampleRate = sampleRate,
            bitsPerSample = bitsPerSample,
            format = format,
            encodingType = encodingType,
            channels = channels,
            fileSizeMb = fileSizeMb,
            filePath = filePath,
            recordLabel = recordLabel,
            copyright = copyright,
            language = language,
            mood = mood,
            vbrIndicator = vbrIndicator,
            replayGain = replayGain,
            audioHash = audioHash
        )
    }

    private fun calculateMd5(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead = input.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = input.read(buffer)
            }
        }
        val md5Bytes = digest.digest()
        return md5Bytes.joinToString("") { String.format("%02x", it) }
    }
}

data class ExtractedMetadata(
    val composer: String,
    val genre: String,
    val lyricist: String,
    val trackNumber: String,
    val discNumber: String,
    val comment: String,
    val year: String,
    val bitrate: String,
    val sampleRate: String,
    val bitsPerSample: String,
    val format: String,
    val encodingType: String,
    val channels: String,
    val fileSizeMb: Double,
    val filePath: String,
    val recordLabel: String,
    val copyright: String,
    val language: String,
    val mood: String,
    val vbrIndicator: String,
    val replayGain: String,
    val audioHash: String
)
