package com.devson.vedtune.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.devson.vedtune.data.local.dao.QueueDao
import com.devson.vedtune.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val queueDao: QueueDao
) : SettingsRepository {

    companion object {
        private val KEY_SHOW_ALBUM_ART = booleanPreferencesKey("show_album_art")
        private val KEY_SHOW_REMAINING_TIME = booleanPreferencesKey("show_remaining_time")
        private val KEY_SHOW_MINIPLAYER_PROGRESS = booleanPreferencesKey("show_miniplayer_progress")
        private val KEY_AUTOPLAY_ON_STARTUP = booleanPreferencesKey("autoplay_on_startup")
    }

    override val showAlbumArt: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_SHOW_ALBUM_ART] ?: true
    }

    override val showRemainingTime: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_SHOW_REMAINING_TIME] ?: false
    }

    override val showMiniPlayerProgress: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_SHOW_MINIPLAYER_PROGRESS] ?: true
    }

    override val autoplayOnStartup: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_AUTOPLAY_ON_STARTUP] ?: false
    }

    override suspend fun setShowAlbumArt(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_ALBUM_ART] = show
        }
    }

    override suspend fun setShowRemainingTime(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_REMAINING_TIME] = show
        }
    }

    override suspend fun setShowMiniPlayerProgress(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_MINIPLAYER_PROGRESS] = show
        }
    }

    override suspend fun setAutoplayOnStartup(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTOPLAY_ON_STARTUP] = show
        }
    }

    override suspend fun clearPlaybackQueue() {
        queueDao.clearQueue()
    }
}
