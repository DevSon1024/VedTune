package com.devson.vedtune.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtune.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.devson.vedtune.player.PlaybackConnection

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val playbackConnection: PlaybackConnection
) : ViewModel() {

    val showAlbumArt: StateFlow<Boolean> = settingsRepository.showAlbumArt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showRemainingTime: StateFlow<Boolean> = settingsRepository.showRemainingTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showMiniPlayerProgress: StateFlow<Boolean> = settingsRepository.showMiniPlayerProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoplayOnStartup: StateFlow<Boolean> = settingsRepository.autoplayOnStartup
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val themeMode: StateFlow<String> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val dynamicColorsEnabled: StateFlow<Boolean> = settingsRepository.dynamicColorsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoSyncOnStartup: StateFlow<Boolean> = settingsRepository.autoSyncOnStartup
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val audioFadeInEnabled: StateFlow<Boolean> = settingsRepository.audioFadeInEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val defaultStartScreen: StateFlow<String> = settingsRepository.defaultStartScreen
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "songs")

    fun setShowAlbumArt(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowAlbumArt(show)
        }
    }

    fun setShowRemainingTime(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowRemainingTime(show)
        }
    }

    fun setShowMiniPlayerProgress(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowMiniPlayerProgress(show)
        }
    }

    fun setAutoplayOnStartup(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoplayOnStartup(show)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setDynamicColorsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColorsEnabled(enabled)
        }
    }

    fun setAutoSyncOnStartup(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoSyncOnStartup(enabled)
        }
    }

    fun setAudioFadeInEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAudioFadeInEnabled(enabled)
        }
    }

    fun setDefaultStartScreen(screen: String) {
        viewModelScope.launch {
            settingsRepository.setDefaultStartScreen(screen)
        }
    }

    fun clearPlaybackQueue() {
        viewModelScope.launch {
            settingsRepository.clearPlaybackQueue()
            playbackConnection.clearQueue()
        }
    }
}
