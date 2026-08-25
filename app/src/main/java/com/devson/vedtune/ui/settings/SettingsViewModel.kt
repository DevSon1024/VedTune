package com.devson.vedtune.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtune.domain.model.AlbumArtClickAction
import com.devson.vedtune.domain.model.AudioSettings
import com.devson.vedtune.domain.model.AudioSettingsFactory
import com.devson.vedtune.domain.model.FolderFilterMode
import com.devson.vedtune.domain.model.ReplayGainMode
import com.devson.vedtune.domain.model.SeekBarStyle
import com.devson.vedtune.domain.repository.SettingsRepository
import com.devson.vedtune.player.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.devson.vedtune.domain.model.AudioDiagnostics
import com.devson.vedtune.player.engine.diagnostics.AudioDiagnosticsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val playbackConnection: PlaybackConnection,
    private val audioDiagnosticsProvider: AudioDiagnosticsProvider
) : ViewModel() {

    private val _audioDiagnostics = MutableStateFlow<AudioDiagnostics?>(null)
    val audioDiagnostics: StateFlow<AudioDiagnostics?> = _audioDiagnostics.asStateFlow()

    fun loadAudioDiagnostics() {
        viewModelScope.launch {
            val songId = playbackConnection.currentSongId.value
            val currentSong = songId?.let { playbackConnection.queueSongMap.value[it] }
            val currentSettings = audioSettings.value
            _audioDiagnostics.value = audioDiagnosticsProvider.getDiagnostics(
                song = currentSong,
                audioSettings = currentSettings
            )
        }
    }

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

    val showLyricsButton: StateFlow<Boolean> = settingsRepository.showLyricsButton
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showSleepTimerButton: StateFlow<Boolean> = settingsRepository.showSleepTimerButton
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showShuffleRepeatButtons: StateFlow<Boolean> = settingsRepository.showShuffleRepeatButtons
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

    // AudioSettings
    val audioSettings: StateFlow<AudioSettings> = settingsRepository.audioSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioSettingsFactory.defaults())

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

    fun setShowLyricsButton(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowLyricsButton(show) }
    }

    fun setShowSleepTimerButton(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowSleepTimerButton(show) }
    }

    fun setShowShuffleRepeatButtons(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowShuffleRepeatButtons(show) }
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

    // AudioSettings setters
    fun setMasterVolume(volume: Float) {
        viewModelScope.launch { settingsRepository.setMasterVolume(volume) }
    }

    fun setGaplessPlaybackEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setGaplessPlaybackEnabled(enabled) }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setCrossfadeEnabled(enabled) }
    }

    fun setCrossfadeDurationMs(durationMs: Int) {
        viewModelScope.launch { settingsRepository.setCrossfadeDurationMs(durationMs) }
    }

    fun setReplayGainEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReplayGainEnabled(enabled) }
    }

    fun setReplayGainMode(mode: ReplayGainMode) {
        viewModelScope.launch { settingsRepository.setReplayGainMode(mode) }
    }

    fun setReplayGainPreampDb(preampDb: Float) {
        viewModelScope.launch { settingsRepository.setReplayGainPreampDb(preampDb) }
    }

    fun setReplayGainPreventClipping(prevent: Boolean) {
        viewModelScope.launch { settingsRepository.setReplayGainPreventClipping(prevent) }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setEqualizerEnabled(enabled) }
    }

    fun setEqualizerPreampDb(preampDb: Float) {
        viewModelScope.launch { settingsRepository.setEqualizerPreampDb(preampDb) }
    }

    fun setEqualizerBandGains(bandGains: List<Float>) {
        viewModelScope.launch { settingsRepository.setEqualizerBandGains(bandGains) }
    }

    fun setEqualizerPreset(preset: String?) {
        viewModelScope.launch { settingsRepository.setEqualizerPreset(preset) }
    }

    fun selectEqualizerPreset(presetName: String) {
        val preset = com.devson.vedtune.player.engine.equalizer.EqualizerPresets.getPresetByName(presetName)
        if (preset != null) {
            viewModelScope.launch {
                settingsRepository.updateAudioSettings { current ->
                    current.copy(
                        equalizerPreset = preset.name,
                        equalizerBandGains = preset.bandGains,
                        equalizerPreampDb = preset.preampDb
                    )
                }
            }
        }
    }

    fun updateEqualizerBand(bandIndex: Int, gainDb: Float) {
        viewModelScope.launch {
            settingsRepository.updateAudioSettings { current ->
                val currentBands = current.equalizerBandGains.ifEmpty {
                    com.devson.vedtune.player.engine.equalizer.EqualizerPresets.defaultBandGains()
                }.toMutableList()

                while (currentBands.size <= bandIndex) {
                    currentBands.add(0.0f)
                }
                currentBands[bandIndex] = gainDb.coerceIn(-12.0f, 12.0f)

                current.copy(
                    equalizerBandGains = currentBands,
                    equalizerPreset = com.devson.vedtune.player.engine.equalizer.EqualizerPresets.PRESET_CUSTOM
                )
            }
        }
    }

    fun resetEqualizer() {
        viewModelScope.launch {
            settingsRepository.updateAudioSettings { current ->
                current.copy(
                    equalizerEnabled = false,
                    equalizerPreampDb = 0.0f,
                    equalizerBandGains = com.devson.vedtune.player.engine.equalizer.EqualizerPresets.defaultBandGains(),
                    equalizerPreset = null
                )
            }
        }
    }

    fun setBassBoostEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBassBoostEnabled(enabled) }
    }

    fun setBassBoostStrength(strength: Int) {
        viewModelScope.launch { settingsRepository.setBassBoostStrength(strength) }
    }

    fun resetBassBoost() {
        viewModelScope.launch { settingsRepository.resetBassBoost() }
    }

    fun setVirtualizerEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVirtualizerEnabled(enabled) }
    }

    fun setVirtualizerStrength(strength: Int) {
        viewModelScope.launch { settingsRepository.setVirtualizerStrength(strength) }
    }

    fun resetVirtualizer() {
        viewModelScope.launch { settingsRepository.resetVirtualizer() }
    }

    fun resetPlaybackSettings() {
        viewModelScope.launch { settingsRepository.resetPlaybackSettings() }
    }

    fun resetReplayGain() {
        viewModelScope.launch { settingsRepository.resetReplayGain() }
    }

    fun resetBassAndEffects() {
        viewModelScope.launch { settingsRepository.resetBassAndEffects() }
    }

    fun setLoudnessNormalizationEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setLoudnessNormalizationEnabled(enabled) }
    }

    fun setTargetLufs(targetLufs: Float) {
        viewModelScope.launch { settingsRepository.setTargetLufs(targetLufs) }
    }

    fun resetLoudnessNormalization() {
        viewModelScope.launch { settingsRepository.resetLoudnessNormalization() }
    }

    fun setLimiterEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setLimiterEnabled(enabled) }
    }

    fun setLimiterThresholdDb(thresholdDb: Float) {
        viewModelScope.launch { settingsRepository.setLimiterThresholdDb(thresholdDb) }
    }

    fun setPreventClipping(prevent: Boolean) {
        viewModelScope.launch { settingsRepository.setPreventClipping(prevent) }
    }

    fun resetLimiter() {
        viewModelScope.launch { settingsRepository.resetLimiter() }
    }

    fun setAudioProcessingEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAudioProcessingEnabled(enabled) }
    }

    fun updateAudioSettings(transform: (AudioSettings) -> AudioSettings) {
        viewModelScope.launch { settingsRepository.updateAudioSettings(transform) }
    }

    fun resetAudioSettings() {
        viewModelScope.launch { settingsRepository.resetAudioSettings() }
    }

    fun resetAppearanceSettings() {
        viewModelScope.launch {
            settingsRepository.setThemeMode("SYSTEM")
            settingsRepository.setAmoledDark(false)
            settingsRepository.setDynamicColorsEnabled(true)
            settingsRepository.setShowAlbumArt(true)
            settingsRepository.setForceSquareArtwork(true)
            settingsRepository.setAlbumArtQuality(com.devson.vedtune.domain.model.AlbumArtQuality.BALANCED)
            settingsRepository.setDefaultStartScreen("songs")
        }
    }

    fun resetLibrarySettings() {
        viewModelScope.launch {
            settingsRepository.setAutoSyncOnStartup(true)
            settingsRepository.setFolderFilterMode(FolderFilterMode.NONE)
            settingsRepository.setBlacklistedFolders(emptySet())
            settingsRepository.setWhitelistedFolders(emptySet())
            settingsRepository.setIncludeSubfolders(true)
        }
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            resetAppearanceSettings()
            settingsRepository.resetPlaybackSettings()
            settingsRepository.resetAudioSettings()
            resetLibrarySettings()
        }
    }
}
