package com.devson.vedtune.ui.lyrics

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.domain.model.SeekBarStyle
import com.devson.vedtune.domain.repository.MediaRepository
import com.devson.vedtune.domain.repository.SettingsRepository
import com.devson.vedtune.player.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Locale
import javax.inject.Inject

data class LrcLine(val timestamp: Long, val text: String)

sealed interface LyricsEditorUiState {
    object Idle : LyricsEditorUiState
    object Loading : LyricsEditorUiState
    object Success : LyricsEditorUiState
    data class Error(val message: String) : LyricsEditorUiState
}

sealed interface LyricsEditorUiEvent {
    data class LaunchIntentSender(val intentSender: android.content.IntentSender) : LyricsEditorUiEvent
    data class ShowToast(val message: String) : LyricsEditorUiEvent
    object SaveSuccess : LyricsEditorUiEvent
}

@HiltViewModel
class LyricsEditorViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val playbackConnection: PlaybackConnection,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val seekbarStyle: StateFlow<SeekBarStyle> = settingsRepository.seekbarStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SeekBarStyle.DEFAULT)

    private val songId: Long = checkNotNull(savedStateHandle["songId"])

    private val _uiState = MutableStateFlow<LyricsEditorUiState>(LyricsEditorUiState.Loading)
    val uiState: StateFlow<LyricsEditorUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<LyricsEditorUiEvent>()
    val uiEvent: SharedFlow<LyricsEditorUiEvent> = _uiEvent.asSharedFlow()

    private val _song = MutableStateFlow<Song?>(null)
    val song: StateFlow<Song?> = _song.asStateFlow()

    private val _rawLyrics = MutableStateFlow("")
    val rawLyrics: StateFlow<String> = _rawLyrics.asStateFlow()

    private val _parsedLines = MutableStateFlow<List<LrcLine>>(emptyList())
    val parsedLines: StateFlow<List<LrcLine>> = _parsedLines.asStateFlow()

    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    // Player state observed from PlaybackConnection
    val currentPosition = playbackConnection.playbackPosition
    val isPlaying = playbackConnection.isPlaying
    val duration = playbackConnection.playbackDuration

    private var seekJob: kotlinx.coroutines.Job? = null

    init {
        loadSongAndLyrics()
    }

    private fun loadSongAndLyrics() {
        viewModelScope.launch {
            _uiState.value = LyricsEditorUiState.Loading
            try {
                val songData = repository.getSongById(songId)
                if (songData == null) {
                    _uiState.value = LyricsEditorUiState.Error("Song not found in library.")
                    return@launch
                }
                _song.value = songData

                val lrcText = withContext(Dispatchers.IO) {
                    // 1. Check internal lrc file
                    val internalFile = File(context.filesDir, "custom_lyrics/$songId.lrc")
                    if (internalFile.exists()) {
                        val text = internalFile.readText(Charsets.UTF_8)
                        if (text.isNotBlank()) return@withContext text
                    }

                    // 2. Check external lrc file in song directory
                    val path = getFilePathFromUri(songId)
                    if (path != null) {
                        val audioFile = File(path)
                        val parentDir = audioFile.parentFile
                        val baseName = audioFile.nameWithoutExtension
                        val lrcFile = File(parentDir, "$baseName.lrc")
                        if (lrcFile.exists()) {
                            val text = lrcFile.readText(Charsets.UTF_8)
                            if (text.isNotBlank()) return@withContext text
                        }

                        // 3. Check embedded lyrics
                        try {
                            val jAudioFile = org.jaudiotagger.audio.AudioFileIO.read(audioFile)
                            val tag = jAudioFile.tag
                            if (tag != null) {
                                val embedded = tag.getFirst(org.jaudiotagger.tag.FieldKey.LYRICS)
                                if (!embedded.isNullOrBlank()) return@withContext embedded
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    ""
                }

                _rawLyrics.value = lrcText
                _parsedLines.value = parseRawLyricsToLines(lrcText)
                _uiState.value = LyricsEditorUiState.Idle
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = LyricsEditorUiState.Error(e.message ?: "Failed to load lyrics.")
            }
        }
    }

    fun setRawLyrics(text: String) {
        _rawLyrics.value = text
    }

    fun importLyricsFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = LyricsEditorUiState.Loading
            try {
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().readText()
                    } ?: throw IOException("Could not open file input stream")
                }
                _rawLyrics.value = content
                _parsedLines.value = parseRawLyricsToLines(content)
                _uiState.value = LyricsEditorUiState.Idle
                _uiEvent.emit(LyricsEditorUiEvent.ShowToast("Lyrics imported successfully"))
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = LyricsEditorUiState.Idle
                _uiEvent.emit(LyricsEditorUiEvent.ShowToast("Failed to read lyrics file: ${e.message}"))
            }
        }
    }

    fun setActiveTab(tab: Int) {
        if (_activeTab.value == tab) return
        if (tab == 1) {
            // Switching from Simple to Synced: parse rawLyrics into parsedLines
            _parsedLines.value = parseRawLyricsToLines(_rawLyrics.value)
        } else {
            // Switching from Synced to Simple: serialize parsedLines into rawLyrics
            _rawLyrics.value = serializeLinesToRawLyrics(_parsedLines.value)
        }
        _activeTab.value = tab
    }

    fun updateLineTimestamp(index: Int, timestamp: Long) {
        val list = _parsedLines.value.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(timestamp = timestamp)
            _parsedLines.value = list
        }
    }

    fun updateLineTextAndTimestamp(index: Int, text: String, timestamp: Long) {
        val list = _parsedLines.value.toMutableList()
        if (index in list.indices) {
            list[index] = LrcLine(timestamp, text)
            _parsedLines.value = list
        }
    }

    fun removeLine(index: Int) {
        val list = _parsedLines.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _parsedLines.value = list
        }
    }

    fun togglePlayPause() {
        if (isPlaying.value) {
            playbackConnection.pause()
        } else {
            playbackConnection.play()
        }
    }

    fun skipBackward() {
        val currentPos = currentPosition.value
        val newPos = (currentPos - 10000L).coerceAtLeast(0L)
        seekTo(newPos)
    }

    fun skipForward() {
        val currentPos = currentPosition.value
        val totalDur = duration.value
        val newPos = (currentPos + 10000L).coerceAtMost(totalDur)
        seekTo(newPos)
    }

    fun seekTo(positionMs: Long) {
        playbackConnection.seekTo(positionMs)
    }

    fun seekToDebounced(positionMs: Long) {
        seekJob?.cancel()
        seekJob = viewModelScope.launch {
            kotlinx.coroutines.delay(100)
            playbackConnection.seekTo(positionMs)
        }
    }

    fun seekAndPlay(timestamp: Long) {
        playbackConnection.seekTo(timestamp)
        playbackConnection.play()
    }

    fun saveLyrics() {
        val songVal = _song.value ?: return
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songVal.id)

        // Make sure we have the latest serialized content if we are in Synced mode
        val lrcText = if (activeTab.value == 0) {
            _rawLyrics.value
        } else {
            serializeLinesToRawLyrics(_parsedLines.value)
        }

        viewModelScope.launch {
            _uiState.value = LyricsEditorUiState.Loading
            try {
                // Save internally first so we don't lose changes
                withContext(Dispatchers.IO) {
                    saveInternalLrcOnly(lrcText)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val pi = MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
                    _uiEvent.emit(LyricsEditorUiEvent.LaunchIntentSender(pi.intentSender))
                } else {
                    executeLyricsSave(lrcText)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // If it fails (e.g. MediaStore.createWriteRequest), we still saved it internally, so we can finish
                _uiState.value = LyricsEditorUiState.Success
                _uiEvent.emit(LyricsEditorUiEvent.SaveSuccess)
            }
        }
    }

    fun onWritePermissionGranted() {
        val lrcText = if (activeTab.value == 0) {
            _rawLyrics.value
        } else {
            serializeLinesToRawLyrics(_parsedLines.value)
        }
        viewModelScope.launch {
            executeLyricsSave(lrcText)
        }
    }

    private suspend fun executeLyricsSave(lrcText: String) = withContext(Dispatchers.IO) {
        val songVal = _song.value ?: return@withContext
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songVal.id)

        // 1. Save internally (done in saveLyrics, but ensure here too)
        saveInternalLrcOnly(lrcText)

        // 2. Save external lrc file next to song if possible
        try {
            val path = getFilePathFromUri(songVal.id)
            if (path != null) {
                val audioFile = File(path)
                val parentDir = audioFile.parentFile
                if (parentDir != null && parentDir.exists()) {
                    val baseName = audioFile.nameWithoutExtension
                    val externalLrcFile = File(parentDir, "$baseName.lrc")
                    externalLrcFile.writeText(lrcText, Charsets.UTF_8)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Embed tags using jaudiotagger
        val path = getFilePathFromUri(songVal.id)
        val extension = if (path != null) {
            File(path).extension.lowercase(Locale.US)
        } else {
            "mp3"
        }
        val tempFile = File(context.cacheDir, "temp_lyrics_edit_${songVal.id}.$extension")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IOException("Failed to open input stream for original audio file.")

            val audioFile = org.jaudiotagger.audio.AudioFileIO.read(tempFile)
            val tag = audioFile.tag ?: audioFile.createDefaultTag().also { audioFile.tag = it }
            tag.setField(org.jaudiotagger.tag.FieldKey.LYRICS, lrcText)
            audioFile.commit()

            context.contentResolver.openOutputStream(uri, "rwt")?.use { output ->
                tempFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: throw IOException("Failed to open output stream to write back modified audio file.")

            val path = getFilePathFromUri(songVal.id)
            if (path != null) {
                android.media.MediaScannerConnection.scanFile(context, arrayOf(path), null) { _, _ -> }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }

        _uiState.value = LyricsEditorUiState.Success
        _uiEvent.emit(LyricsEditorUiEvent.SaveSuccess)
    }

    private fun saveInternalLrcOnly(lrcText: String): Boolean {
        return try {
            val songVal = _song.value ?: return false
            val internalDir = File(context.filesDir, "custom_lyrics")
            if (!internalDir.exists()) internalDir.mkdirs()
            val internalFile = File(internalDir, "${songVal.id}.lrc")
            internalFile.writeText(lrcText, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getFilePathFromUri(songId: Long): String? {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val colIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                if (colIdx != -1) {
                    return cursor.getString(colIdx)
                }
            }
        }
        return null
    }

    fun parseRawLyricsToLines(raw: String): List<LrcLine> {
        val lines = mutableListOf<LrcLine>()
        val timeRegex = Regex("^\\[(\\d{2}):(\\d{2})\\.(\\d{2})](.*)$")
        val timeRegexWithHours = Regex("^\\[(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{2})](.*)$")

        for (lineText in raw.lines()) {
            if (lineText.isBlank()) continue

            val matchHours = timeRegexWithHours.matchEntire(lineText)
            if (matchHours != null) {
                val hh = matchHours.groupValues[1].toLong()
                val mm = matchHours.groupValues[2].toLong()
                val ss = matchHours.groupValues[3].toLong()
                val xx = matchHours.groupValues[4].toLong()
                val text = matchHours.groupValues[5].trim()
                val ms = (hh * 3600000L) + (mm * 60000L) + (ss * 1000L) + (xx * 10L)
                lines.add(LrcLine(ms, text))
            } else {
                val match = timeRegex.matchEntire(lineText)
                if (match != null) {
                    val mm = match.groupValues[1].toLong()
                    val ss = match.groupValues[2].toLong()
                    val xx = match.groupValues[3].toLong()
                    val text = match.groupValues[4].trim()
                    val ms = (mm * 60000L) + (ss * 1000L) + (xx * 10L)
                    lines.add(LrcLine(ms, text))
                } else {
                    lines.add(LrcLine(-1L, lineText.trim()))
                }
            }
        }
        return lines
    }

    fun serializeLinesToRawLyrics(lines: List<LrcLine>): String {
        return lines.joinToString("\n") { line ->
            if (line.timestamp >= 0) {
                "${formatLrcTime(line.timestamp, includeBrackets = true)}${line.text}"
            } else {
                line.text
            }
        }
    }

    fun formatLrcTime(ms: Long, includeBrackets: Boolean = true): String {
        if (ms < 0) return if (includeBrackets) "[00:00.00]" else "00:00.00"
        val hours = ms / 3600000L
        val minutes = (ms % 3600000L) / 60000L
        val seconds = (ms % 60000L) / 1000L
        val hundredths = (ms % 1000L) / 10L
        val formatted = if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d.%02d", hours, minutes, seconds, hundredths)
        } else {
            String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, hundredths)
        }
        return if (includeBrackets) "[$formatted]" else formatted
    }
}
