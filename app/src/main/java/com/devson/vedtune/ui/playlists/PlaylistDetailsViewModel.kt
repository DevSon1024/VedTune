package com.devson.vedtune.ui.playlists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtune.domain.model.Playlist
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.domain.repository.MediaRepository
import com.devson.vedtune.domain.repository.SettingsRepository
import com.devson.vedtune.player.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailsViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val playbackConnection: PlaybackConnection,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val currentSongId: StateFlow<Long?> = playbackConnection.currentSongId
    val isPlaying: StateFlow<Boolean> = playbackConnection.isPlaying

    val playlistId: Long = checkNotNull(savedStateHandle["playlistId"])
    val isFavoritePlaylist: Boolean = (playlistId == Playlist.FAVORITES_PLAYLIST_ID)

    val showAlbumArt: StateFlow<Boolean> = settingsRepository.showAlbumArt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val songs: StateFlow<List<Song>> = repository.getSongsByPlaylistId(playlistId)
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val previewAlbumIds: StateFlow<List<Long>> = songs
        .map { songList ->
            songList.map { it.albumId }.filter { it > 0 }.distinct().take(4)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalDurationMs: StateFlow<Long> = songs
        .map { songList ->
            songList.sumOf { it.duration }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val playlistDetails: StateFlow<Playlist?> = repository.getAllPlaylists()
        .map { playlists ->
            playlists.firstOrNull { it.id == playlistId }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun playSong(song: Song) {
        val list = songs.value
        if (list.isNotEmpty()) {
            playbackConnection.playSong(song, list)
        }
    }

    fun playNext(song: Song) {
        playbackConnection.playNext(song)
    }

    fun playPlaylist() {
        val songList = songs.value
        if (songList.isNotEmpty()) {
            playbackConnection.playSong(songList.first(), songList)
        }
    }

    fun shufflePlaylist() {
        val songList = songs.value
        if (songList.isNotEmpty()) {
            val randomSong = songList.random()
            playbackConnection.playShuffle(randomSong, songList)
        }
    }

    fun removeSongFromPlaylist(songId: Long) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }
}
