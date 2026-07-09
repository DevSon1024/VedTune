package com.devson.vedtune.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.domain.repository.MediaRepository
import com.devson.vedtune.player.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.devson.vedtune.domain.repository.SettingsRepository

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val playbackConnection: PlaybackConnection,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val currentSongId: StateFlow<Long?> = playbackConnection.currentSongId
    val isPlaying: StateFlow<Boolean> = playbackConnection.isPlaying

    val showArtwork: StateFlow<Boolean> = settingsRepository.showAlbumArt
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val latestSongs: StateFlow<List<Song>> = repository.getAllSongs()
        .map { songs ->
            songs.sortedByDescending { it.dateAdded }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun playSong(song: Song) {
        playbackConnection.playSong(song, latestSongs.value)
    }

    fun playAll() {
        val songs = latestSongs.value
        if (songs.isNotEmpty()) {
            playbackConnection.playSong(songs.first(), songs)
        }
    }

    fun shuffleAll() {
        val songs = latestSongs.value
        if (songs.isNotEmpty()) {
            val shuffled = songs.shuffled()
            playbackConnection.playSong(shuffled.first(), shuffled)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.synchronizeLibrary()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
