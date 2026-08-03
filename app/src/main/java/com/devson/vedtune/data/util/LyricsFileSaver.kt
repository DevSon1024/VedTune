package com.devson.vedtune.data.util

import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object LyricsFileSaver {

    suspend fun saveLyricsToFile(
        songName: String,
        lyricsResult: Result<String>
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val lyricsContent = lyricsResult.getOrElse { throwable ->
                return@withContext Result.failure(throwable)
            }

            val sanitizedSongName = songName.replace(Regex("[\\\\/:*?\"<>|]"), "_")

            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val lyricsFolder = File(documentsDir, "VedTune/Lyrics")

            if (!lyricsFolder.exists()) {
                val created = lyricsFolder.mkdirs()
                if (!created && !lyricsFolder.exists()) {
                    return@withContext Result.failure(Exception("Failed to create lyrics directory at ${lyricsFolder.absolutePath}"))
                }
            }

            val lrcFile = File(lyricsFolder, "$sanitizedSongName.lrc")
            lrcFile.writeText(lyricsContent, Charsets.UTF_8)

            Result.success(lrcFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
