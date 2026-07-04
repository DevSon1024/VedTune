package com.devson.vedtune.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtune.domain.model.AlbumArtClickAction
import com.devson.vedtune.domain.model.FolderFilterMode
import com.devson.vedtune.domain.model.SeekBarStyle
import com.devson.vedtune.domain.repository.SettingsRepository
import com.devson.vedtune.player.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val playbackConnection: PlaybackConnection
) : ViewModel() {

    //  Existing settings 

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

    val isGestureMiniPlayerEnabled: StateFlow<Boolean> = settingsRepository.isGestureMiniPlayerEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val seekbarStyle: StateFlow<SeekBarStyle> = settingsRepository.seekbarStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SeekBarStyle.DEFAULT)

    val enableSwipeToSkip: StateFlow<Boolean> = settingsRepository.enableSwipeToSkip
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val keepScreenOnWithLyrics: StateFlow<Boolean> = settingsRepository.keepScreenOnWithLyrics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val albumArtClickAction: StateFlow<AlbumArtClickAction> = settingsRepository.albumArtClickAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlbumArtClickAction.SHOW_LYRICS)

    val playerBackgroundBlurRadius: StateFlow<Float> = settingsRepository.playerBackgroundBlurRadius
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 40f)

    val isAmoledDark: StateFlow<Boolean> = settingsRepository.isAmoledDark
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val albumArtQuality: StateFlow<com.devson.vedtune.domain.model.AlbumArtQuality> = settingsRepository.albumArtQuality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.devson.vedtune.domain.model.AlbumArtQuality.BALANCED)

    val forceSquareArtwork: StateFlow<Boolean> = settingsRepository.forceSquareArtwork
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    //  Folder filtering 

    val folderFilterMode: StateFlow<FolderFilterMode> = settingsRepository.folderFilterMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FolderFilterMode.NONE)

    val blacklistedFolders: StateFlow<Set<String>> = settingsRepository.blacklistedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val whitelistedFolders: StateFlow<Set<String>> = settingsRepository.whitelistedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val includeSubfolders: StateFlow<Boolean> = settingsRepository.includeSubfolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    //  Existing dispatchers 

    fun setShowAlbumArt(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowAlbumArt(show) }
    }

    fun setShowRemainingTime(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowRemainingTime(show) }
    }

    fun setShowMiniPlayerProgress(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowMiniPlayerProgress(show) }
    }

    fun setAutoplayOnStartup(show: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoplayOnStartup(show) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setDynamicColorsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColorsEnabled(enabled) }
    }

    fun setAutoSyncOnStartup(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoSyncOnStartup(enabled) }
    }

    fun setAudioFadeInEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAudioFadeInEnabled(enabled) }
    }

    fun setDefaultStartScreen(screen: String) {
        viewModelScope.launch { settingsRepository.setDefaultStartScreen(screen) }
    }

    fun setGestureMiniPlayerEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setGestureMiniPlayerEnabled(enabled) }
    }

    fun setSeekBarStyle(style: SeekBarStyle) {
        viewModelScope.launch { settingsRepository.setSeekBarStyle(style) }
    }

    fun setEnableSwipeToSkip(enable: Boolean) {
        viewModelScope.launch { settingsRepository.setEnableSwipeToSkip(enable) }
    }

    fun setKeepScreenOnWithLyrics(keep: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepScreenOnWithLyrics(keep) }
    }

    fun setAlbumArtClickAction(action: AlbumArtClickAction) {
        viewModelScope.launch { settingsRepository.setAlbumArtClickAction(action) }
    }

    fun setPlayerBackgroundBlurRadius(radius: Float) {
        viewModelScope.launch { settingsRepository.setPlayerBackgroundBlurRadius(radius) }
    }

    fun setAmoledDark(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAmoledDark(enabled) }
    }

    fun setAlbumArtQuality(quality: com.devson.vedtune.domain.model.AlbumArtQuality) {
        viewModelScope.launch { settingsRepository.setAlbumArtQuality(quality) }
    }

    fun setForceSquareArtwork(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setForceSquareArtwork(enabled) }
    }

    fun clearPlaybackQueue() {
        viewModelScope.launch {
            settingsRepository.clearPlaybackQueue()
            playbackConnection.clearQueue()
        }
    }

    //  Folder filter dispatchers 

    fun setFolderFilterMode(mode: FolderFilterMode) {
        viewModelScope.launch { settingsRepository.setFolderFilterMode(mode) }
    }

    fun addToBlacklist(path: String) {
        viewModelScope.launch {
            val current = blacklistedFolders.value
            settingsRepository.setBlacklistedFolders(current + path)
        }
    }

    fun removeFromBlacklist(path: String) {
        viewModelScope.launch {
            val current = blacklistedFolders.value
            settingsRepository.setBlacklistedFolders(current - path)
        }
    }

    fun clearBlacklist() {
        viewModelScope.launch { settingsRepository.setBlacklistedFolders(emptySet()) }
    }

    fun addToWhitelist(path: String) {
        viewModelScope.launch {
            val current = whitelistedFolders.value
            settingsRepository.setWhitelistedFolders(current + path)
        }
    }

    fun removeFromWhitelist(path: String) {
        viewModelScope.launch {
            val current = whitelistedFolders.value
            settingsRepository.setWhitelistedFolders(current - path)
        }
    }

    fun clearWhitelist() {
        viewModelScope.launch { settingsRepository.setWhitelistedFolders(emptySet()) }
    }

    fun setIncludeSubfolders(include: Boolean) {
        viewModelScope.launch { settingsRepository.setIncludeSubfolders(include) }
    }
}
