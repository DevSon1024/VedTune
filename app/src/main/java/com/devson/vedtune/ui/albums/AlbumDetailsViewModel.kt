package com.devson.vedtune.ui.albums

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtune.domain.model.Album
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.domain.repository.MediaRepository
import com.devson.vedtune.player.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

import com.devson.vedtune.domain.model.Playlist
import com.devson.vedtune.domain.model.QueueInfo
import com.devson.vedtune.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class AlbumDetailsViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val playbackConnection: PlaybackConnection,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
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

    suspend fun getQueueSongIds(queueId: Long): List<Long> {
        return repository.getQueueSongIds(queueId)
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

    val currentSongId: StateFlow<Long?> = playbackConnection.currentSongId
    val isPlaying: StateFlow<Boolean> = playbackConnection.isPlaying

    val showAlbumArt: StateFlow<Boolean> = settingsRepository.showAlbumArt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val albumId: Long = checkNotNull(savedStateHandle["albumId"])

    val songs: StateFlow<List<Song>> = repository.getSongsByAlbumId(albumId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albumDetails: StateFlow<Album?> = songs.map { songList ->
        if (songList.isNotEmpty()) {
            val firstSong = songList.first()
            Album(
                id = albumId,
                title = firstSong.album,
                artist = firstSong.artist,
                songCount = songList.size
            )
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun playSong(song: Song) {
        playbackConnection.playSong(song, songs.value)
    }

    fun playAlbum() {
        val songList = songs.value
        if (songList.isNotEmpty()) {
            playbackConnection.playSong(songList.first(), songList)
        }
    }

    fun shuffleAlbum() {
        val songList = songs.value
        if (songList.isNotEmpty()) {
            val shuffled = songList.shuffled()
            playbackConnection.playSong(shuffled.first(), shuffled)
        }
    }
}
