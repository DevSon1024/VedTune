package com.devson.vedtune.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val showAlbumArt: Flow<Boolean>
    val showRemainingTime: Flow<Boolean>
    val showMiniPlayerProgress: Flow<Boolean>
    val autoplayOnStartup: Flow<Boolean>
    
    suspend fun setShowAlbumArt(show: Boolean)
    suspend fun setShowRemainingTime(show: Boolean)
    suspend fun setShowMiniPlayerProgress(show: Boolean)
    suspend fun setAutoplayOnStartup(show: Boolean)
    suspend fun clearPlaybackQueue()
}
