package com.devson.vedtune.ui.genres

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.domain.repository.MediaRepository
import com.devson.vedtune.player.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import com.devson.vedtune.domain.model.Playlist
import com.devson.vedtune.domain.model.QueueInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

@HiltViewModel
class GenreDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MediaRepository,
    private val playbackConnection: PlaybackConnection
) : ViewModel() {

    val queues: StateFlow<List<QueueInfo>> = repository.getAllQueues()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSongsToQueue(queueId: Long, songIds: List<Long>, playNext: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addSongsToQueue(queueId, songIds, atBeginning = playNext)
        }
    }

    fun createQueueAndAddSongs(name: String, songIds: List<Long>, playNext: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val qId = repository.createQueue(name)
            repository.addSongsToQueue(qId, songIds, atBeginning = playNext)
        }
    }

    fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            songIds.forEach { songId ->
                repository.addSongToPlaylist(playlistId, songId)
            }
        }
    }

    fun createPlaylistAndAddSongs(name: String, songIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            val playlistId = repository.createPlaylist(name)
            songIds.forEach { songId ->
                repository.addSongToPlaylist(playlistId, songId)
            }
        }
    }

    fun playNext(song: Song) {
        playbackConnection.playNext(song)
    }

    val genreName: String = URLDecoder.decode(
        savedStateHandle.get<String>("genreName") ?: "",
        "UTF-8"
    )

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val currentSongId: StateFlow<Long?> = playbackConnection.currentSongId
    val isPlaying: StateFlow<Boolean> = playbackConnection.isPlaying

    init {
        loadSongs()
    }

    private fun loadSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getSongsByGenre(genreName)
                .catch { e ->
                    e.printStackTrace()
                    _songs.value = emptyList()
                }
                .collect { songList ->
                    _songs.value = songList
                    _isLoading.value = false
                }
        }
    }

    fun playSong(song: Song) {
        playbackConnection.playSong(song, _songs.value)
    }

    fun playAll() {
        val songList = _songs.value
        if (songList.isNotEmpty()) {
            playbackConnection.playSong(songList.first(), songList)
        }
    }

    fun shuffleAll() {
        val songList = _songs.value
        if (songList.isNotEmpty()) {
            val shuffled = songList.shuffled()
            playbackConnection.playSong(shuffled.first(), shuffled)
        }
    }
}
