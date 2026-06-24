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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val playbackConnection: PlaybackConnection,
    private val settingsRepository: SettingsRepository
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
            repository.updateFavoriteStatus(song.id, !song.isFavorite)
        }
    }
}
