package com.devson.vedtune.ui.settings

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

sealed interface LyricsConverterUiState {
    data object Idle : LyricsConverterUiState
    data object Processing : LyricsConverterUiState
    data class Success(val fileName: String, val savedPath: String) : LyricsConverterUiState
    data class Error(val message: String) : LyricsConverterUiState
}

class LyricsConverterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<LyricsConverterUiState>(LyricsConverterUiState.Idle)
    val uiState: StateFlow<LyricsConverterUiState> = _uiState.asStateFlow()

    private val _selectedFileName = MutableStateFlow<String?>(null)
    val selectedFileName: StateFlow<String?> = _selectedFileName.asStateFlow()

    private val _history = MutableStateFlow<List<File>>(emptyList())
    val history: StateFlow<List<File>> = _history.asStateFlow()

    private val _previewFileContent = MutableStateFlow<String?>(null)
    val previewFileContent: StateFlow<String?> = _previewFileContent.asStateFlow()

    private val _previewFileName = MutableStateFlow<String?>(null)
    val previewFileName: StateFlow<String?> = _previewFileName.asStateFlow()

    private var selectedUri: Uri? = null

    fun refreshHistory(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "VedTune/Lyrics"
            )
            val filesList = mutableListOf<File>()

            // 1. Try Scoped Storage / MediaStore query for API 29+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
                val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf("Documents/VedTune/Lyrics%")
                try {
                    context.contentResolver.query(
                        MediaStore.Files.getContentUri("external"),
                        projection,
                        selection,
                        selectionArgs,
                        null
                    )?.use { cursor ->
                        val dataIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                        if (dataIndex != -1) {
                            while (cursor.moveToNext()) {
                                val path = cursor.getString(dataIndex)
                                if (path != null && path.endsWith(".lrc", ignoreCase = true)) {
                                    val file = File(path)
                                    if (file.exists()) {
                                        filesList.add(file)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2. Fallback to scanning directory directly
            if (filesList.isEmpty()) {
                try {
                    if (dir.exists() && dir.isDirectory) {
                        val files = dir.listFiles { file -> file.isFile && file.name.endsWith(".lrc", ignoreCase = true) }
                        if (files != null) {
                            filesList.addAll(files)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Sort by last modified date descending
            filesList.sortByDescending { it.lastModified() }
            _history.value = filesList
        }
    }

    fun showFilePreview(file: File) {
        viewModelScope.launch {
            val content = withContext(Dispatchers.IO) {
                runCatching { file.readText() }.getOrNull()
            }
            if (content != null) {
                _previewFileName.value = file.name
                _previewFileContent.value = content
            } else {
                _uiState.value = LyricsConverterUiState.Error("Could not read file content")
            }
        }
    }

    fun dismissPreview() {
        _previewFileContent.value = null
        _previewFileName.value = null
    }

    fun onFileSelected(context: Context, uri: Uri) {
        selectedUri = uri
        _selectedFileName.value = queryFileName(context, uri)
        _uiState.value = LyricsConverterUiState.Idle
    }

    fun convert(context: Context) {
        val uri = selectedUri ?: run {
            _uiState.value = LyricsConverterUiState.Error("No file selected")
            return
        }

        viewModelScope.launch {
            _uiState.value = LyricsConverterUiState.Processing

            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val content = readTextFromUri(context, uri)
                        ?: throw IOException("Unable to read file content")
                    if (content.isBlank()) throw IOException("Selected file is empty")

                    val entries = if (content.contains("-->")) {
                        parseSrt(content)
                    } else {
                        parseGenericTxt(content)
                    }

                    if (entries.isEmpty()) {
                        throw IllegalArgumentException("No valid timestamped lines found in file")
                    }

                    val lrcContent = buildLrc(entries)
                    val originalName = queryFileName(context, uri)
                    val baseName = originalName.substringBeforeLast(".", originalName)
                    val lrcFileName = "$baseName.lrc"

                    val savedPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        saveToMediaStoreQ(context, lrcFileName, lrcContent)
                    } else {
                        saveToLegacyStorage(lrcFileName, lrcContent)
                    }

                    lrcFileName to savedPath
                }
            }

            result.fold(
                onSuccess = { (fileName, path) ->
                    _uiState.value = LyricsConverterUiState.Success(fileName, path)
                },
                onFailure = { e ->
                    _uiState.value = LyricsConverterUiState.Error(e.message ?: "Conversion failed")
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = LyricsConverterUiState.Idle
    }

    // ---------- File I/O ----------

    private fun readTextFromUri(context: Context, uri: Uri): String? {
        return context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader().readText()
        }
    }

    private fun queryFileName(context: Context, uri: Uri): String {
        var name = "lyrics_${System.currentTimeMillis()}"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)?.let { name = it }
            }
        }
        return name
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToMediaStoreQ(context: Context, fileName: String, content: String): String {
        val resolver = context.contentResolver
        val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/VedTune/Lyrics/"
        val collection = MediaStore.Files.getContentUri("external")

        val values = ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
            put(MediaStore.Files.FileColumns.MIME_TYPE, "application/octet-stream")
            put(MediaStore.Files.FileColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.Files.FileColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(collection, values)
            ?: throw IOException("MediaStore insert failed")

        try {
            resolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                ?: throw IOException("Unable to open output stream")
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }

        values.clear()
        values.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)

        return "$relativePath$fileName"
    }

    @Suppress("DEPRECATION")
    private fun saveToLegacyStorage(fileName: String, content: String): String {
        // Requires WRITE_EXTERNAL_STORAGE permission on API < 29
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "VedTune/Lyrics"
        )
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException("Unable to create directory ${dir.absolutePath}")
        }
        val file = File(dir, fileName)
        file.writeText(content)
        return file.absolutePath
    }

    // ---------- Parsing ----------

    private val srtTimeRegex = Regex(
        """(\d{2}):(\d{2}):(\d{2})(?:[,.](\d{1,3}))?\s*-->\s*(\d{2}):(\d{2}):(\d{2})(?:[,.](\d{1,3}))?"""
    )

    private val genericTimeRegex = Regex(
        """^\[?(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]?\s*[-:]?\s*(.+)$"""
    )

    private val htmlTagRegex = Regex("""<[^>]*>""")

    /** Splits on blank lines, extracts the -->  timestamp line per block, joins remaining lines as text. */
    private fun parseSrt(content: String): List<Pair<Long, String>> {
        val normalized = content.replace("\r\n", "\n").replace("\r", "\n")
        val blocks = normalized.split(Regex("\n\\s*\n"))
        val result = mutableListOf<Pair<Long, String>>()

        for (block in blocks) {
            val lines = block.trim().lines()
            if (lines.isEmpty()) continue

            val timeLineIndex = lines.indexOfFirst { srtTimeRegex.containsMatchIn(it) }
            if (timeLineIndex == -1) continue

            val match = srtTimeRegex.find(lines[timeLineIndex]) ?: continue
            val startMs = toMillis(
                h = match.groupValues[1],
                m = match.groupValues[2],
                s = match.groupValues[3],
                ms = match.groupValues[4]
            )

            val text = lines.drop(timeLineIndex + 1)
                .filter { it.isNotBlank() }
                .joinToString(" ") { it.replace(htmlTagRegex, "").trim() }
                .trim()

            if (text.isNotBlank()) result.add(startMs to text)
        }
        return result
    }

    /** Generic line-based parser: `[mm:ss] text`, `mm:ss.xx text`, `mm:ss - text`, etc. */
    private fun parseGenericTxt(content: String): List<Pair<Long, String>> {
        val result = mutableListOf<Pair<Long, String>>()
        content.replace("\r\n", "\n").lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach

            val match = genericTimeRegex.find(line) ?: return@forEach
            val minutes = match.groupValues[1].toLongOrNull() ?: return@forEach
            val seconds = match.groupValues[2].toLongOrNull() ?: return@forEach
            val text = match.groupValues[4].trim()
            if (text.isEmpty()) return@forEach

            val fracMs = fracToMillis(match.groupValues[3])
            val totalMs = minutes * 60_000 + seconds * 1_000 + fracMs
            result.add(totalMs to text)
        }
        return result
    }

    private fun toMillis(h: String, m: String, s: String, ms: String): Long {
        val hh = h.toLongOrNull() ?: 0L
        val mm = m.toLongOrNull() ?: 0L
        val ss = s.toLongOrNull() ?: 0L
        return hh * 3_600_000 + mm * 60_000 + ss * 1_000 + fracToMillis(ms)
    }

    private fun fracToMillis(frac: String): Long {
        if (frac.isEmpty()) return 0L
        return when (frac.length) {
            1 -> frac.toLong() * 100
            2 -> frac.toLong() * 10
            else -> frac.take(3).toLong()
        }
    }

    private fun buildLrc(entries: List<Pair<Long, String>>): String {
        return entries.sortedBy { it.first }.joinToString("\n") { (ms, text) ->
            val totalCenti = ms / 10
            val minutes = totalCenti / 6000
            val seconds = (totalCenti % 6000) / 100
            val centis = totalCenti % 100
            "[%02d:%02d.%02d] %s".format(minutes, seconds, centis, text)
        }
    }
}
