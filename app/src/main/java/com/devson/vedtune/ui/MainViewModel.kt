package com.devson.vedtune.ui

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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val playbackConnection: PlaybackConnection,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val showAlbumArt: StateFlow<Boolean> = settingsRepository.showAlbumArt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showMiniPlayerProgress: StateFlow<Boolean> = settingsRepository.showMiniPlayerProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val themeMode: StateFlow<String> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val dynamicColorsEnabled: StateFlow<Boolean> = settingsRepository.dynamicColorsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoSyncOnStartup: StateFlow<Boolean> = settingsRepository.autoSyncOnStartup
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val defaultStartScreen: StateFlow<String> = settingsRepository.defaultStartScreen
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "songs")

    val isPlaying: StateFlow<Boolean> = playbackConnection.isPlaying

    val currentSong: StateFlow<Song?> = playbackConnection.currentSongId
        .flatMapLatest { id ->
            if (id != null) {
                // Retrieve the song entity from Room to reflect favorites/details changes
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

    fun play() {
        playbackConnection.play()
    }

    fun pause() {
        playbackConnection.pause()
    }

    fun skipToNext() {
        playbackConnection.skipToNext()
    }

    fun syncLibrary() {
        viewModelScope.launch {
            try {
                repository.synchronizeLibrary()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
