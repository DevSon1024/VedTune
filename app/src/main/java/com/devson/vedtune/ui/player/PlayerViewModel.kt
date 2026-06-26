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

    val queue: StateFlow<List<Song>> = playbackConnection.playlistQueue

    private val _currentLyrics = MutableStateFlow<String?>(null)
    val currentLyrics: StateFlow<String?> = _currentLyrics.asStateFlow()

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

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        playbackConnection.moveQueueItem(fromIndex, toIndex)
    }

    fun skipToQueueItem(index: Int) {
        playbackConnection.skipToQueueItem(index)
    }

    private fun loadLyrics(songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
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
