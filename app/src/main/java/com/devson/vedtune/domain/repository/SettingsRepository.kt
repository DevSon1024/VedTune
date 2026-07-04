package com.devson.vedtune.domain.repository

import com.devson.vedtune.domain.model.AlbumArtClickAction
import com.devson.vedtune.domain.model.FolderFilterMode
import com.devson.vedtune.domain.model.SeekBarStyle
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val showAlbumArt: Flow<Boolean>
    val showRemainingTime: Flow<Boolean>
    val showMiniPlayerProgress: Flow<Boolean>
    val autoplayOnStartup: Flow<Boolean>
    val themeMode: Flow<String>
    val dynamicColorsEnabled: Flow<Boolean>
    val autoSyncOnStartup: Flow<Boolean>
    val audioFadeInEnabled: Flow<Boolean>
    val defaultStartScreen: Flow<String>
    val isGestureMiniPlayerEnabled: Flow<Boolean>
    val seekbarStyle: Flow<SeekBarStyle>
    val enableSwipeToSkip: Flow<Boolean>
    val keepScreenOnWithLyrics: Flow<Boolean>
    val albumArtClickAction: Flow<AlbumArtClickAction>
    val playerBackgroundBlurRadius: Flow<Float>
    val isAmoledDark: Flow<Boolean>
    val albumArtQuality: Flow<com.devson.vedtune.domain.model.AlbumArtQuality>
    val forceSquareArtwork: Flow<Boolean>
    val showLyricsButton: Flow<Boolean>
    val showSleepTimerButton: Flow<Boolean>
    val showShuffleRepeatButtons: Flow<Boolean>

    // Folder filtering
    val folderFilterMode: Flow<FolderFilterMode>
    val blacklistedFolders: Flow<Set<String>>
    val whitelistedFolders: Flow<Set<String>>
    val includeSubfolders: Flow<Boolean>

    suspend fun setShowAlbumArt(show: Boolean)
    suspend fun setShowRemainingTime(show: Boolean)
    suspend fun setShowMiniPlayerProgress(show: Boolean)
    suspend fun setAutoplayOnStartup(show: Boolean)
    suspend fun setThemeMode(mode: String)
    suspend fun setDynamicColorsEnabled(enabled: Boolean)
    suspend fun setAutoSyncOnStartup(enabled: Boolean)
    suspend fun setAudioFadeInEnabled(enabled: Boolean)
    suspend fun setDefaultStartScreen(screen: String)
    suspend fun setGestureMiniPlayerEnabled(enabled: Boolean)
    suspend fun setSeekBarStyle(style: SeekBarStyle)
    suspend fun setEnableSwipeToSkip(enable: Boolean)
    suspend fun setKeepScreenOnWithLyrics(keep: Boolean)
    suspend fun setAlbumArtClickAction(action: AlbumArtClickAction)
    suspend fun setPlayerBackgroundBlurRadius(radius: Float)
    suspend fun setAmoledDark(enabled: Boolean)
    suspend fun setAlbumArtQuality(quality: com.devson.vedtune.domain.model.AlbumArtQuality)
    suspend fun setForceSquareArtwork(enabled: Boolean)
    suspend fun setShowLyricsButton(show: Boolean)
    suspend fun setShowSleepTimerButton(show: Boolean)
    suspend fun setShowShuffleRepeatButtons(show: Boolean)
    suspend fun clearPlaybackQueue()

    // Folder filtering setters
    suspend fun setFolderFilterMode(mode: FolderFilterMode)
    suspend fun setBlacklistedFolders(folders: Set<String>)
    suspend fun setWhitelistedFolders(folders: Set<String>)
    suspend fun setIncludeSubfolders(include: Boolean)
}
