package com.devson.vedtune.ui.history

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
class HistoryViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val playbackConnection: PlaybackConnection,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val historySongs: StateFlow<List<Song>> = mediaRepository.getRecentlyPlayedSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<Playlist>> = mediaRepository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentSongId: StateFlow<Long?> = playbackConnection.currentSong
        .map { it?.id }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isPlaying: StateFlow<Boolean> = playbackConnection.isPlaying

    val showAlbumArt: StateFlow<Boolean> = settingsRepository.showAlbumArt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun playSong(song: Song) {
        val songs = historySongs.value
        playbackConnection.playSong(song, songs)
    }

    fun playAll() {
        val songs = historySongs.value
        if (songs.isNotEmpty()) {
            playbackConnection.playSong(songs.first(), songs)
        }
    }

    fun shuffleAll() {
        val songs = historySongs.value
        if (songs.isNotEmpty()) {
            playbackConnection.playShuffle(songs.random(), songs)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            mediaRepository.clearAllPlaybackHistory()
        }
    }

    fun removeSongFromHistory(songId: Long) {
        viewModelScope.launch {
            mediaRepository.removeSongFromHistory(songId)
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            mediaRepository.toggleFavorite(song.id)
        }
    }

    fun playNext(song: Song) {
        playbackConnection.playNext(song)
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            mediaRepository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun createPlaylistAndAddSong(name: String, songId: Long) {
        viewModelScope.launch {
            val newPlaylistId = mediaRepository.createPlaylist(name)
            mediaRepository.addSongToPlaylist(newPlaylistId, songId)
        }
    }
}
