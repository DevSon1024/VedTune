package com.devson.vedtune.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.domain.repository.MediaRepository
import com.devson.vedtune.player.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.devson.vedtune.domain.repository.SettingsRepository

import android.content.ContentUris
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.devson.vedtune.domain.model.Playlist
import java.io.File
import kotlinx.coroutines.withContext
import android.app.RecoverableSecurityException
import android.os.Build

sealed interface PlayerUiEvent {
    data class ShowToast(val message: String) : PlayerUiEvent
    data class LaunchIntentSender(val intentSender: android.content.IntentSender) : PlayerUiEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val playbackConnection: PlaybackConnection,
    private val settingsRepository: SettingsRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    val showAlbumArt: StateFlow<Boolean> = settingsRepository.showAlbumArt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showRemainingTime: StateFlow<Boolean> = settingsRepository.showRemainingTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isPlaying: StateFlow<Boolean> = playbackConnection.isPlaying

    val currentSong: StateFlow<Song?> = playbackConnection.currentSongId
        .flatMapLatest { id ->
            if (id != null) {
                kotlinx.coroutines.flow.flow {
                    emit(repository.getSongById(id))
                }
            } else {
                flowOf<Song?>(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val playbackPosition: StateFlow<Long> = playbackConnection.playbackPosition
    val playbackDuration: StateFlow<Long> = playbackConnection.playbackDuration
    val repeatMode: StateFlow<Int> = playbackConnection.repeatMode
    val shuffleModeEnabled: StateFlow<Boolean> = playbackConnection.shuffleModeEnabled
    val sleepTimerRemaining: StateFlow<Long> = playbackConnection.sleepTimerRemaining

    val isFavorite: StateFlow<Boolean> = currentSong
        .flatMapLatest { song ->
            if (song != null) {
                repository.isSongInPlaylist(Playlist.FAVORITES_PLAYLIST_ID, song.id)
            } else {
                flowOf(false)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val playlists: StateFlow<List<Playlist>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiEvent = MutableSharedFlow<PlayerUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _currentLyrics = MutableStateFlow<String?>(null)
    val currentLyrics: StateFlow<String?> = _currentLyrics.asStateFlow()

    private val _lyricsFontSize = MutableStateFlow("Medium")
    val lyricsFontSize: StateFlow<String> = _lyricsFontSize.asStateFlow()

    private val _lyricsAlignment = MutableStateFlow("Center")
    val lyricsAlignment: StateFlow<String> = _lyricsAlignment.asStateFlow()

    private val sharedPrefs = context.getSharedPreferences("player_settings", android.content.Context.MODE_PRIVATE)

    private val _showForwardBackward = MutableStateFlow(sharedPrefs.getBoolean("show_forward_backward", true))
    val showForwardBackward: StateFlow<Boolean> = _showForwardBackward.asStateFlow()

    private val _seekInterval = MutableStateFlow(sharedPrefs.getInt("seek_interval", 10))
    val seekInterval: StateFlow<Int> = _seekInterval.asStateFlow()

    private var pendingDeleteSongId: Long? = null

    init {
        viewModelScope.launch {
            currentSong.collect { song ->
                if (song != null) {
                    _currentLyrics.value = null
                    loadLyrics(song.id)
                } else {
                    _currentLyrics.value = null
                }
            }
        }
    }

    fun setLyricsFontSize(size: String) {
        _lyricsFontSize.value = size
    }

    fun setLyricsAlignment(alignment: String) {
        _lyricsAlignment.value = alignment
    }

    fun setShowForwardBackward(show: Boolean) {
        _showForwardBackward.value = show
        sharedPrefs.edit().putBoolean("show_forward_backward", show).apply()
    }

    fun setSeekInterval(seconds: Int) {
        _seekInterval.value = seconds.coerceIn(5, 60)
        sharedPrefs.edit().putInt("seek_interval", seconds.coerceIn(5, 60)).apply()
    }

    fun skipForward() {
        val currentPos = playbackPosition.value
        val totalDur = playbackDuration.value
        val intervalMs = _seekInterval.value * 1000L
        val newPos = (currentPos + intervalMs).coerceAtMost(totalDur)
        seekTo(newPos)
    }

    fun skipBackward() {
        val currentPos = playbackPosition.value
        val intervalMs = _seekInterval.value * 1000L
        val newPos = (currentPos - intervalMs).coerceAtLeast(0L)
        seekTo(newPos)
    }

    fun togglePlayPause() {
        if (isPlaying.value) {
            playbackConnection.pause()
        } else {
            playbackConnection.play()
        }
    }

    fun skipToNext() {
        playbackConnection.skipToNext()
    }

    fun skipToPrevious() {
        playbackConnection.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        playbackConnection.seekTo(positionMs)
    }

    fun setRepeatMode(repeatMode: Int) {
        playbackConnection.setRepeatMode(repeatMode)
    }

    fun setShuffleModeEnabled(enabled: Boolean) {
        playbackConnection.setShuffleModeEnabled(enabled)
    }

    fun startSleepTimer(minutes: Int) {
        playbackConnection.startSleepTimer(minutes)
    }

    fun cancelSleepTimer() {
        playbackConnection.cancelSleepTimer()
    }

    fun toggleRemainingTime() {
        viewModelScope.launch {
            settingsRepository.setShowRemainingTime(!showRemainingTime.value)
        }
    }

    fun toggleFavorite() {
        val song = currentSong.value ?: return
        viewModelScope.launch {
            val isFav = isFavorite.value
            if (isFav) {
                repository.removeSongFromPlaylist(Playlist.FAVORITES_PLAYLIST_ID, song.id)
            } else {
                repository.addSongToPlaylist(Playlist.FAVORITES_PLAYLIST_ID, song.id)
            }
            repository.updateFavoriteStatus(song.id, !isFav)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun createPlaylistAndAddSong(playlistName: String, songId: Long) {
        viewModelScope.launch {
            val playlistId = repository.createPlaylist(playlistName)
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun deleteSongPermanently(context: android.content.Context, song: Song) {
        viewModelScope.launch {
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
            pendingDeleteSongId = song.id
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val pi = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                    _uiEvent.emit(PlayerUiEvent.LaunchIntentSender(pi.intentSender))
                } else {
                    val deleted = withContext(Dispatchers.IO) {
                        try {
                            context.contentResolver.delete(uri, null, null) > 0
                        } catch (e: RecoverableSecurityException) {
                            _uiEvent.emit(PlayerUiEvent.LaunchIntentSender(e.userAction.actionIntent.intentSender))
                            false
                        }
                    }
                    if (deleted) {
                        repository.deleteSong(song.id)
                        pendingDeleteSongId = null
                        playbackConnection.skipToNext()
                    }
                }
            } catch (e: Exception) {
                _uiEvent.emit(PlayerUiEvent.ShowToast(e.message ?: "Failed to delete song"))
            }
        }
    }

    fun onDeletePermissionGranted() {
        pendingDeleteSongId?.let { songId ->
            viewModelScope.launch {
                repository.deleteSong(songId)
                pendingDeleteSongId = null
                playbackConnection.skipToNext()
            }
        }
    }

    fun importLrcFile(context: android.content.Context, uri: android.net.Uri) {
        val song = currentSong.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dir = File(context.filesDir, "custom_lyrics")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val file = File(dir, "${song.id}.lrc")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                _uiEvent.emit(PlayerUiEvent.ShowToast("Lyrics imported successfully"))
                loadLyrics(song.id)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiEvent.emit(PlayerUiEvent.ShowToast(e.message ?: "Failed to import lyrics"))
            }
        }
    }

    private fun loadLyrics(songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Check app's internal custom lyrics directory
                val internalLrcFile = File(context.filesDir, "custom_lyrics/$songId.lrc")
                if (internalLrcFile.exists()) {
                    val lrcText = internalLrcFile.readText(Charsets.UTF_8)
                    if (lrcText.isNotBlank()) {
                        _currentLyrics.value = lrcText
                        return@launch
                    }
                }

                // 2. Check the media folder where the song is located
                val path = getFilePathFromUri(songId)
                if (path != null) {
                    val audioFile = File(path)
                    val parentDir = audioFile.parentFile
                    val baseName = audioFile.nameWithoutExtension
                    val lrcFile = File(parentDir, "$baseName.lrc")
                    if (lrcFile.exists()) {
                        val lrcText = lrcFile.readText(Charsets.UTF_8)
                        if (lrcText.isNotBlank()) {
                            _currentLyrics.value = lrcText
                            return@launch
                        }
                    }

                    // 3. Fallback to reading embedded lyrics tags via jaudiotagger
                    try {
                        val jAudioFile = org.jaudiotagger.audio.AudioFileIO.read(audioFile)
                        val tag = jAudioFile.tag
                        if (tag != null) {
                            val embeddedLyrics = tag.getFirst(org.jaudiotagger.tag.FieldKey.LYRICS)
                            if (!embeddedLyrics.isNullOrBlank()) {
                                _currentLyrics.value = embeddedLyrics
                                return@launch
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                _currentLyrics.value = ""
            } catch (e: Exception) {
                e.printStackTrace()
                _currentLyrics.value = ""
            }
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
}
