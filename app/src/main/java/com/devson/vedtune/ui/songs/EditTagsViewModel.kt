package com.devson.vedtune.ui.songs

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.domain.repository.MediaRepository
import com.devson.vedtune.player.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.id3.AbstractID3v2Tag
import org.jaudiotagger.tag.id3.framebody.AbstractFrameBodyTextInfo
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag
import org.jaudiotagger.tag.mp4.Mp4Tag
import org.jaudiotagger.tag.mp4.Mp4FieldKey
import java.io.File
import java.util.Locale
import javax.inject.Inject

sealed interface EditTagsUiState {
    object Loading : EditTagsUiState
    object Success : EditTagsUiState
    data class Error(val message: String) : EditTagsUiState
}

sealed interface EditTagsUiEvent {
    data class LaunchIntentSender(val intentSender: android.content.IntentSender) : EditTagsUiEvent
    data class ShowError(val message: String) : EditTagsUiEvent
    object SaveSuccess : EditTagsUiEvent
}

@HiltViewModel
class EditTagsViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val playbackConnection: PlaybackConnection,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val songId: Long = checkNotNull(savedStateHandle["songId"])

    var uiState by mutableStateOf<EditTagsUiState>(EditTagsUiState.Loading)
        private set

    private val _uiEvent = MutableSharedFlow<EditTagsUiEvent>()
    val uiEvent: SharedFlow<EditTagsUiEvent> = _uiEvent.asSharedFlow()

    private val _artistSuggestions = MutableStateFlow<List<String>>(emptyList())
    val artistSuggestions: StateFlow<List<String>> = _artistSuggestions.asStateFlow()

    private val _albumSuggestions = MutableStateFlow<List<String>>(emptyList())
    val albumSuggestions: StateFlow<List<String>> = _albumSuggestions.asStateFlow()

    private val _composerSuggestions = MutableStateFlow<List<String>>(emptyList())
    val composerSuggestions: StateFlow<List<String>> = _composerSuggestions.asStateFlow()

    private val _genreSuggestions = MutableStateFlow<List<String>>(emptyList())
    val genreSuggestions: StateFlow<List<String>> = _genreSuggestions.asStateFlow()

    // Expose flows from PlaybackConnection for sync tool
    val isPlaying = playbackConnection.isPlaying
    val playbackPosition = playbackConnection.playbackPosition

    // Field states
    var song: Song? by mutableStateOf(null)
        private set
    var filePath: String? by mutableStateOf(null)
        private set

    var customArtworkUri by mutableStateOf<Uri?>(null)

    var hasExistingCustomArtwork by mutableStateOf(false)
        private set
    var shouldRemoveArtwork by mutableStateOf(false)
        private set

    fun removeCustomArtwork() {
        shouldRemoveArtwork = true
        customArtworkUri = null
    }

    fun undoRemoveCustomArtwork() {
        shouldRemoveArtwork = false
    }

    // Initial states for change tracking
    private var initialTitle = ""
    private var initialArtist = ""
    private var initialAlbum = ""
    private var initialAlbumArtist = ""
    private var initialComposer = ""
    private var initialGenre = ""
    private var initialLyricist = ""
    private var initialRecordLabel = ""
    private var initialCopyright = ""
    private var initialLanguage = ""
    private var initialMood = ""
    private var initialYear = ""
    private var initialComment = ""
    private var initialTrack = ""
    private var initialDiscNo = ""
    private var initialLyrics = ""
    private var initialCustomArtworkUri: Uri? = null
    private var initialShouldRemoveArtwork = false

    var title by mutableStateOf("")
    var artist by mutableStateOf("")
    var album by mutableStateOf("")
    var albumArtist by mutableStateOf("")
    var composer by mutableStateOf("")
    var genre by mutableStateOf("")
    var lyricist by mutableStateOf("")
    var recordLabel by mutableStateOf("")
    var copyright by mutableStateOf("")
    var language by mutableStateOf("")
    var mood by mutableStateOf("")
    var year by mutableStateOf("")
    var comment by mutableStateOf("")
    var track by mutableStateOf("")
    var discNo by mutableStateOf("")
    var lyrics by mutableStateOf(TextFieldValue(""))

    init {
        loadSongAndTags()
        loadSuggestions()
    }

    private fun loadSuggestions() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _artistSuggestions.value = repository.getUniqueArtists()
                _albumSuggestions.value = repository.getUniqueAlbums()
                _composerSuggestions.value = repository.getUniqueComposers()
                _genreSuggestions.value = repository.getUniqueGenres()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadSongAndTags() {
        viewModelScope.launch {
            try {
                val songData = repository.getSongById(songId)
                if (songData == null) {
                    uiState = EditTagsUiState.Error("Song not found in library.")
                    return@launch
                }
                song = songData
                
                // Initialize basic fields from Room
                title = songData.title
                artist = songData.artist
                album = songData.album
                track = songData.track.toString()
                year = songData.year.toString()

                // Resolve file path from MediaStore
                val path = getFilePathFromUri(context, songId)
                if (path == null) {
                    uiState = EditTagsUiState.Error("Failed to locate audio file path.")
                    return@launch
                }
                filePath = path

                val customFile = File(context.filesDir, "custom_artwork/${songData.albumId}.jpg")
                hasExistingCustomArtwork = customFile.exists()

                // Read advanced tags via jaudiotagger: try direct read first, fallback to dynamically-named temp file
                val loadSuccess = withContext(Dispatchers.IO) {
                    var tempFile: File? = null
                    var isTempUsed = false
                    var audioFile: org.jaudiotagger.audio.AudioFile? = null

                    if (path.isNotEmpty()) {
                        val actualFile = File(path)
                        try {
                            audioFile = AudioFileIO.read(actualFile)
                        } catch (e: Exception) {
                            // Fallback to temp file
                            try {
                                val ext = actualFile.extension.ifEmpty { "mp3" }
                                tempFile = File(context.cacheDir, "temp_tag_load_${songId}.$ext")
                                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
                                context.contentResolver.openInputStream(uri)?.use { input ->
                                    tempFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                } ?: throw java.io.IOException("Failed to open input stream for original audio file.")

                                audioFile = AudioFileIO.read(tempFile)
                                isTempUsed = true
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                    }

                    if (audioFile != null) {
                        try {
                            val tag = if (audioFile is org.jaudiotagger.audio.mp3.MP3File) {
                                audioFile.iD3v2Tag ?: audioFile.tag
                            } else {
                                audioFile.tag
                            }
                            if (tag != null) {
                                albumArtist = tag.getFirst(FieldKey.ALBUM_ARTIST) ?: ""
                                composer = tag.getFirst(FieldKey.COMPOSER) ?: ""
                                genre = tag.getFirst(FieldKey.GENRE) ?: ""
                                lyricist = tag.getFirst(FieldKey.LYRICIST) ?: ""
                                recordLabel = tag.getFirst(FieldKey.RECORD_LABEL) ?: ""
                                copyright = tag.getFirst("COPYRIGHT").ifEmpty { tag.getFirst("TCOP") }.ifEmpty { tag.getFirst("cprt") }
                                language = tag.getFirst(FieldKey.LANGUAGE) ?: ""
                                mood = tag.getFirst(FieldKey.MOOD) ?: ""
                                comment = tag.getFirst(FieldKey.COMMENT) ?: ""
                                discNo = tag.getFirst(FieldKey.DISC_NO) ?: ""
                                val fileLyrics = tag.getFirst(FieldKey.LYRICS) ?: ""
                                lyrics = TextFieldValue(fileLyrics)
                            }
                            true
                        } catch (e: Exception) {
                            e.printStackTrace()
                            false
                        } finally {
                            if (isTempUsed) {
                                tempFile?.delete()
                            }
                        }
                    } else {
                        false
                    }
                }

                if (loadSuccess) {
                    initialTitle = title
                    initialArtist = artist
                    initialAlbum = album
                    initialAlbumArtist = albumArtist
                    initialComposer = composer
                    initialGenre = genre
                    initialLyricist = lyricist
                    initialRecordLabel = recordLabel
                    initialCopyright = copyright
                    initialLanguage = language
                    initialMood = mood
                    initialYear = year
                    initialComment = comment
                    initialTrack = track
                    initialDiscNo = discNo
                    initialLyrics = lyrics.text
                    initialCustomArtworkUri = customArtworkUri
                    initialShouldRemoveArtwork = shouldRemoveArtwork

                    uiState = EditTagsUiState.Success
                } else {
                    uiState = EditTagsUiState.Error("Failed to read audio tags from file. Make sure the file is not corrupted.")
                }
            } catch (e: Exception) {
                uiState = EditTagsUiState.Error(e.message ?: "Failed to load audio tags.")
            }
        }
    }

    fun hasUnsavedChanges(): Boolean {
        return title != initialTitle ||
               artist != initialArtist ||
               album != initialAlbum ||
               albumArtist != initialAlbumArtist ||
               composer != initialComposer ||
               genre != initialGenre ||
               lyricist != initialLyricist ||
               recordLabel != initialRecordLabel ||
               copyright != initialCopyright ||
               language != initialLanguage ||
               mood != initialMood ||
               year != initialYear ||
               comment != initialComment ||
               track != initialTrack ||
               discNo != initialDiscNo ||
               lyrics.text != initialLyrics ||
               customArtworkUri != initialCustomArtworkUri ||
               shouldRemoveArtwork != initialShouldRemoveArtwork
    }

    private fun getFilePathFromUri(context: Context, songId: Long): String? {
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

    fun captureTimestamp() {
        val pos = playbackPosition.value
        val timestamp = formatLrcTimestamp(pos)

        val text = lyrics.text
        val selection = lyrics.selection
        val cursor = selection.start

        val lastNewline = text.lastIndexOf('\n', cursor - 1)
        val lineStartIndex = if (lastNewline == -1) 0 else lastNewline + 1

        val newText = text.substring(0, lineStartIndex) + timestamp + " " + text.substring(lineStartIndex)
        val newCursor = cursor + timestamp.length + 1

        lyrics = lyrics.copy(
            text = newText,
            selection = androidx.compose.ui.text.TextRange(newCursor)
        )
    }

    private fun formatLrcTimestamp(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val hundredths = (ms % 1000) / 10
        return String.format(Locale.US, "[%02d:%02d.%02d]", minutes, seconds, hundredths)
    }

    fun onSaveClick() {
        val targetSong = song ?: return
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, targetSong.id)
        viewModelScope.launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val pi = MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
                    _uiEvent.emit(EditTagsUiEvent.LaunchIntentSender(pi.intentSender))
                } else {
                    executeTagSave()
                }
            } catch (e: Exception) {
                _uiEvent.emit(EditTagsUiEvent.ShowError(e.message ?: "Failed to save tags."))
            }
        }
    }

    fun onWritePermissionGranted() {
        viewModelScope.launch {
            executeTagSave()
        }
    }

    private suspend fun executeTagSave() = withContext(Dispatchers.IO) {
        val path = filePath ?: return@withContext
        val targetSong = song ?: return@withContext
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, targetSong.id)
        val ext = File(path).extension.ifEmpty { "mp3" }
        val tempFile = File(context.cacheDir, "temp_tag_edit_${targetSong.id}.$ext")

        try {
            if (shouldRemoveArtwork) {
                val dir = File(context.filesDir, "custom_artwork")
                val file = File(dir, "${targetSong.albumId}.jpg")
                if (file.exists()) {
                    file.delete()
                }
                com.devson.vedtune.ui.components.ArtworkCache.removeCustomArtwork(targetSong.albumId)
            } else {
                // Save custom artwork if picked
                customArtworkUri?.let { artUri ->
                    val dir = File(context.filesDir, "custom_artwork")
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, "${targetSong.albumId}.jpg")
                    context.contentResolver.openInputStream(artUri)?.use { input ->
                        java.io.FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    com.devson.vedtune.ui.components.ArtworkCache.addCustomArtwork(targetSong.albumId)
                }
            }

            // 1. Copy original file to temp file in internal cache
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw java.io.IOException("Failed to open input stream for original audio file.")

            // 2. Physically write advanced tags to the temp file using jaudiotagger
            val audioFile = AudioFileIO.read(tempFile)
            val tag = audioFile.tag ?: audioFile.createDefaultTag().also { audioFile.tag = it }

            tag.setField(FieldKey.TITLE, title)
            tag.setField(FieldKey.ARTIST, artist)
            tag.setField(FieldKey.ALBUM, album)
            tag.setField(FieldKey.ALBUM_ARTIST, albumArtist)
            tag.setField(FieldKey.COMPOSER, composer)
            tag.setField(FieldKey.GENRE, genre)
            tag.setField(FieldKey.LYRICIST, lyricist)
            tag.setField(FieldKey.RECORD_LABEL, recordLabel)
            writeCopyrightSafe(tag, copyright)
            tag.setField(FieldKey.LANGUAGE, language)
            tag.setField(FieldKey.MOOD, mood)
            tag.setField(FieldKey.YEAR, year)
            tag.setField(FieldKey.COMMENT, comment)
            tag.setField(FieldKey.TRACK, track)
            tag.setField(FieldKey.DISC_NO, discNo)
            tag.setField(FieldKey.LYRICS, lyrics.text)

            audioFile.commit()

            // 3. Write the modified temp file back to the original file via MediaStore
            context.contentResolver.openOutputStream(uri, "rwt")?.use { output ->
                tempFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: throw java.io.IOException("Failed to open output stream to write back modified audio file.")

            // 4. Update MediaStore database so changes are visible to Android immediately
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.TITLE, title)
                put(MediaStore.Audio.Media.ARTIST, artist)
                put(MediaStore.Audio.Media.ALBUM, album)
                put(MediaStore.Audio.Media.TRACK, track.toIntOrNull() ?: 0)
                put(MediaStore.Audio.Media.YEAR, year.toIntOrNull() ?: 0)
            }
            context.contentResolver.update(uri, values, null, null)

            // 5. Trigger MediaScanner to refresh files
            MediaScannerConnection.scanFile(context, arrayOf(path), null) { _, _ -> }

            // 6. Update the local Room Database entity
            val updatedSong = targetSong.copy(
                title = title,
                artist = artist,
                album = album,
                track = track.toIntOrNull() ?: 0,
                year = year.toIntOrNull() ?: 0,
                dateModified = System.currentTimeMillis() / 1000
            )
            repository.updateSong(updatedSong)

            _uiEvent.emit(EditTagsUiEvent.SaveSuccess)
        } catch (e: Exception) {
            e.printStackTrace()
            _uiEvent.emit(EditTagsUiEvent.ShowError(e.message ?: "Failed to save tags physically to file."))
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    private fun writeCopyrightSafe(tag: org.jaudiotagger.tag.Tag, value: String) {
        try {
            if (value.isBlank()) {
                tag.deleteField("COPYRIGHT")
                tag.deleteField("TCOP")
                tag.deleteField("cprt")
                return
            }
            when (tag) {
                is AbstractID3v2Tag -> {
                    val frame = tag.createFrame("TCOP")
                    (frame.body as? AbstractFrameBodyTextInfo)?.setText(value)
                    tag.setField(frame)
                }
                is VorbisCommentTag -> {
                    val field = tag.createField("COPYRIGHT", value)
                    tag.setField(field)
                }
                is Mp4Tag -> {
                    val field = tag.createField(Mp4FieldKey.COPYRIGHT, value)
                    tag.setField(field)
                }
                else -> {
                    try {
                        tag.deleteField("COPYRIGHT")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
