package com.devson.vedtune.core

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.io.IOException

object LyricsStorageUtils {

    private const val RELATIVE_LYRICS_DIR = "Documents/VedTune/Lyrics"

    /**
     * Saves lyrics content to Documents/VedTune/Lyrics/ directory.
     * Uses MediaStore Files API on Android 10+ (API 29+) and legacy file I/O on older versions.
     * Returns the absolute or display path where the file was saved.
     */
    suspend fun saveLyricsToDocuments(
        context: Context,
        fileName: String,
        content: String
    ): String = withContext(Dispatchers.IO) {
        val sanitizedName = sanitizeFileName(if (fileName.endsWith(".lrc", ignoreCase = true)) fileName else "$fileName.lrc")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = MediaStore.Files.getContentUri("external")

            // Check if file already exists in MediaStore to overwrite or update
            val projection = arrayOf(MediaStore.Files.FileColumns._ID)
            val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} = ? AND ${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf(sanitizedName, "$RELATIVE_LYRICS_DIR%")

            var existingUri: Uri? = null
            try {
                resolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                        if (idIndex != -1) {
                            val id = cursor.getLong(idIndex)
                            existingUri = ContentUris.withAppendedId(collection, id)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val targetUri = existingUri ?: run {
                val values = ContentValues().apply {
                    put(MediaStore.Files.FileColumns.DISPLAY_NAME, sanitizedName)
                    put(MediaStore.Files.FileColumns.MIME_TYPE, "application/octet-stream")
                    put(MediaStore.Files.FileColumns.RELATIVE_PATH, "$RELATIVE_LYRICS_DIR/")
                    put(MediaStore.Files.FileColumns.IS_PENDING, 1)
                }
                resolver.insert(collection, values)
            } ?: throw IOException("Failed to create MediaStore entry for lyrics")

            try {
                resolver.openOutputStream(targetUri, "rwt")?.use { output ->
                    output.write(content.toByteArray(Charsets.UTF_8))
                } ?: throw IOException("Failed to open output stream for lyrics")

                if (existingUri == null) {
                    val finishValues = ContentValues().apply {
                        put(MediaStore.Files.FileColumns.IS_PENDING, 0)
                    }
                    resolver.update(targetUri, finishValues, null, null)
                }
            } catch (e: Exception) {
                if (existingUri == null) {
                    resolver.delete(targetUri, null, null)
                }
                throw e
            }

            "$RELATIVE_LYRICS_DIR/$sanitizedName"
        } else {
            val docDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val lyricsDir = File(docDir, "VedTune/Lyrics")
            if (!lyricsDir.exists()) {
                lyricsDir.mkdirs()
            }
            val targetFile = File(lyricsDir, sanitizedName)
            targetFile.writeText(content, Charsets.UTF_8)
            targetFile.absolutePath
        }
    }

    /**
     * Looks up matching .lrc files in the Documents/VedTune/Lyrics directory.
     * Searches by:
     * 1. "$songTitle - $songArtist.lrc"
     * 2. "$songTitle.lrc"
     * 3. "$baseFileName.lrc"
     * 4. "$songId.lrc"
     */
    suspend fun findLyricsInDocuments(
        context: Context,
        songTitle: String,
        songArtist: String,
        baseFileName: String?,
        songId: Long
    ): String? = withContext(Dispatchers.IO) {
        val candidateNames = buildList {
            val cleanTitle = songTitle.trim()
            val cleanArtist = songArtist.trim()
            if (cleanTitle.isNotEmpty() && cleanArtist.isNotEmpty()) {
                add(sanitizeFileName("$cleanTitle - $cleanArtist.lrc"))
                add(sanitizeFileName("$cleanArtist - $cleanTitle.lrc"))
            }
            if (cleanTitle.isNotEmpty()) {
                add(sanitizeFileName("$cleanTitle.lrc"))
            }
            if (!baseFileName.isNullOrBlank()) {
                val base = baseFileName.substringBeforeLast(".")
                add(sanitizeFileName("$base.lrc"))
            }
            if (songId > 0L) {
                add("$songId.lrc")
            }
        }.distinct()

        // 1. On Android 10+, query MediaStore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME)
            val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.lrc'"
            val selectionArgs = arrayOf("$RELATIVE_LYRICS_DIR%")

            try {
                resolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                    val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)

                    if (idCol != -1 && nameCol != -1) {
                        val fileMap = mutableMapOf<String, Long>()
                        while (cursor.moveToNext()) {
                            val name = cursor.getString(nameCol) ?: continue
                            val id = cursor.getLong(idCol)
                            fileMap[name.lowercase()] = id
                        }

                        for (candidate in candidateNames) {
                            val foundId = fileMap[candidate.lowercase()]
                            if (foundId != null) {
                                val uri = ContentUris.withAppendedId(collection, foundId)
                                val text = resolver.openInputStream(uri)?.use { stream ->
                                    stream.bufferedReader(Charsets.UTF_8).readText()
                                }
                                if (!text.isNullOrBlank()) {
                                    return@withContext text
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Direct directory access fallback (also primary for API < 29)
        try {
            val docDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val lyricsDir = File(docDir, "VedTune/Lyrics")
            if (lyricsDir.exists() && lyricsDir.isDirectory) {
                for (candidate in candidateNames) {
                    val file = File(lyricsDir, candidate)
                    if (file.exists() && file.length() > 0) {
                        val text = file.readText(Charsets.UTF_8)
                        if (text.isNotBlank()) {
                            return@withContext text
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        null
    }

    /**
     * Resolves lyrics following the prioritized multi-tier fallback:
     * 1. Public Documents/VedTune/Lyrics/ directory
     * 2. Internal app cache (filesDir/custom_lyrics/$songId.lrc)
     * 3. Sidecar file in the media folder next to the song (<songDir>/<baseName>.lrc)
     * 4. Embedded ID3 / Vorbis / MP4 tags via Jaudiotagger (FieldKey.LYRICS)
     */
    suspend fun resolveLyricsForSong(
        context: Context,
        songId: Long,
        songTitle: String,
        songArtist: String,
        filePath: String?
    ): String? = withContext(Dispatchers.IO) {
        val baseFileName = filePath?.let { File(it).nameWithoutExtension }

        // Tier 1: Documents/VedTune/Lyrics
        val publicDocLyrics = findLyricsInDocuments(context, songTitle, songArtist, baseFileName, songId)
        if (!publicDocLyrics.isNullOrBlank()) {
            return@withContext publicDocLyrics
        }

        // Tier 2: Internal app cache
        val internalFile = File(context.filesDir, "custom_lyrics/$songId.lrc")
        if (internalFile.exists() && internalFile.length() > 0) {
            try {
                val text = internalFile.readText(Charsets.UTF_8)
                if (text.isNotBlank()) return@withContext text
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Tier 3: Sidecar .lrc in audio file directory
        if (!filePath.isNullOrBlank()) {
            try {
                val audioFile = File(filePath)
                val parentDir = audioFile.parentFile
                if (parentDir != null && parentDir.exists()) {
                    val sidecarFile = File(parentDir, "${audioFile.nameWithoutExtension}.lrc")
                    if (sidecarFile.exists() && sidecarFile.length() > 0) {
                        val text = sidecarFile.readText(Charsets.UTF_8)
                        if (text.isNotBlank()) return@withContext text
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Tier 4: Embedded tags via Jaudiotagger
            try {
                val audioFile = File(filePath)
                if (audioFile.exists()) {
                    val jAudioFile = AudioFileIO.read(audioFile)
                    val tag = jAudioFile.tag
                    if (tag != null) {
                        val embedded = tag.getFirst(FieldKey.LYRICS)
                        if (!embedded.isNullOrBlank()) return@withContext embedded
                    }
                }
            } catch (e: Exception) {
                // Ignore jaudiotagger read failure
            }
        }

        null
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }
}
