package com.devson.vedtune.ui.songs

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mediaarea.mediainfo.lib.MediaInfo

data class AudioMetadata(
    val format: String = "",
    val fileSize: String = "",
    val duration: String = "",
    val bitRate: String = "",
    val samplingRate: String = "",
    val channels: String = "",
    val codec: String = "",
    val location: String = ""
)

object MediaInfoHelper {
    suspend fun getAudioMetadata(context: Context, uri: Uri): AudioMetadata = withContext(Dispatchers.IO) {
        var pfd: android.os.ParcelFileDescriptor? = null
        val mi = MediaInfo()
        try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext AudioMetadata()
            val fd = pfd.detachFd()
            mi.Open(fd, "")
            
            val format = mi.Get(MediaInfo.Stream.General, 0, "Format")
            val fileSize = mi.Get(MediaInfo.Stream.General, 0, "FileSize/String")
            val duration = mi.Get(MediaInfo.Stream.General, 0, "Duration/String3")
            val bitRate = mi.Get(MediaInfo.Stream.General, 0, "OverallBitRate/String")
            val samplingRate = mi.Get(MediaInfo.Stream.Audio, 0, "SamplingRate/String")
            val channels = mi.Get(MediaInfo.Stream.Audio, 0, "Channel(s)/String")
            val codec = mi.Get(MediaInfo.Stream.Audio, 0, "CodecID")
            val location = mi.Get(MediaInfo.Stream.General, 0, "CompleteName")

            AudioMetadata(
                format = format,
                fileSize = fileSize,
                duration = duration,
                bitRate = bitRate,
                samplingRate = samplingRate,
                channels = channels,
                codec = codec,
                location = location
            )
        } catch (e: Exception) {
            e.printStackTrace()
            AudioMetadata()
        } finally {
            mi.Close()
            pfd?.close()
        }
    }
}
