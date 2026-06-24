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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    val playlistId: Long = checkNotNull(savedStateHandle["playlistId"])

    val showAlbumArt: StateFlow<Boolean> = settingsRepository.showAlbumArt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val songs: StateFlow<List<Song>> = repository.getSongsByPlaylistId(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlistDetails: StateFlow<Playlist?> = repository.getAllPlaylists()
        .map { playlists ->
            playlists.firstOrNull { it.id == playlistId }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun playSong(song: Song) {
        playbackConnection.playSong(song, songs.value)
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
            val shuffled = songList.shuffled()
            playbackConnection.playSong(shuffled.first(), shuffled)
        }
    }

    fun removeSongFromPlaylist(songId: Long) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }
}
