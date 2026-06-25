package com.devson.vedtune.domain.repository

import com.devson.vedtune.domain.model.FolderFilterMode
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

    // Folder filtering
    val folderFilterMode: Flow<FolderFilterMode>
    val blacklistedFolders: Flow<Set<String>>
    val whitelistedFolders: Flow<Set<String>>

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
    suspend fun clearPlaybackQueue()

    // Folder filtering setters
    suspend fun setFolderFilterMode(mode: FolderFilterMode)
    suspend fun setBlacklistedFolders(folders: Set<String>)
    suspend fun setWhitelistedFolders(folders: Set<String>)
}
