package com.devson.vedtune.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC_COLORS_ENABLED = booleanPreferencesKey("dynamic_colors_enabled")
        private val KEY_AUTO_SYNC_ON_STARTUP = booleanPreferencesKey("auto_sync_on_startup")
        private val KEY_AUDIO_FADE_IN_ENABLED = booleanPreferencesKey("audio_fade_in_enabled")
        private val KEY_DEFAULT_START_SCREEN = stringPreferencesKey("default_start_screen")
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

    override val themeMode: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: "SYSTEM"
    }

    override val dynamicColorsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_DYNAMIC_COLORS_ENABLED] ?: true
    }

    override val autoSyncOnStartup: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_AUTO_SYNC_ON_STARTUP] ?: true
    }

    override val audioFadeInEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_AUDIO_FADE_IN_ENABLED] ?: true
    }

    override val defaultStartScreen: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_DEFAULT_START_SCREEN] ?: "songs"
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

    override suspend fun setThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode
        }
    }

    override suspend fun setDynamicColorsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DYNAMIC_COLORS_ENABLED] = enabled
        }
    }

    override suspend fun setAutoSyncOnStartup(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTO_SYNC_ON_STARTUP] = enabled
        }
    }

    override suspend fun setAudioFadeInEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_AUDIO_FADE_IN_ENABLED] = enabled
        }
    }

    override suspend fun setDefaultStartScreen(screen: String) {
        dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_START_SCREEN] = screen
        }
    }

    override suspend fun clearPlaybackQueue() {
        queueDao.clearQueue()
    }
}
