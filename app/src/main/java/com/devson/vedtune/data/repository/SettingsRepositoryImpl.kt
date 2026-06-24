package com.devson.vedtune.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.devson.vedtune.data.local.dao.QueueDao
import com.devson.vedtune.domain.model.FolderFilterMode
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

        // Folder filter
        private val KEY_FOLDER_FILTER_MODE = stringPreferencesKey("folder_filter_mode")
        private val KEY_BLACKLISTED_FOLDERS = stringPreferencesKey("blacklisted_folders")
        private val KEY_WHITELISTED_FOLDERS = stringPreferencesKey("whitelisted_folders")

        /** Delimiter used to serialise/deserialise folder sets as a single DataStore string. */
        private const val FOLDER_DELIMITER = "|||"
    }

    //  Existing settings flows 

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

    //  Folder filtering flows 

    override val folderFilterMode: Flow<FolderFilterMode> = dataStore.data.map { preferences ->
        runCatching {
            FolderFilterMode.valueOf(
                preferences[KEY_FOLDER_FILTER_MODE] ?: FolderFilterMode.NONE.name
            )
        }.getOrDefault(FolderFilterMode.NONE)
    }

    override val blacklistedFolders: Flow<Set<String>> = dataStore.data.map { preferences ->
        decodeFolderSet(preferences[KEY_BLACKLISTED_FOLDERS])
    }

    override val whitelistedFolders: Flow<Set<String>> = dataStore.data.map { preferences ->
        decodeFolderSet(preferences[KEY_WHITELISTED_FOLDERS])
    }

    //  Existing setters 

    override suspend fun setShowAlbumArt(show: Boolean) {
        dataStore.edit { it[KEY_SHOW_ALBUM_ART] = show }
    }

    override suspend fun setShowRemainingTime(show: Boolean) {
        dataStore.edit { it[KEY_SHOW_REMAINING_TIME] = show }
    }

    override suspend fun setShowMiniPlayerProgress(show: Boolean) {
        dataStore.edit { it[KEY_SHOW_MINIPLAYER_PROGRESS] = show }
    }

    override suspend fun setAutoplayOnStartup(show: Boolean) {
        dataStore.edit { it[KEY_AUTOPLAY_ON_STARTUP] = show }
    }

    override suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    override suspend fun setDynamicColorsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_DYNAMIC_COLORS_ENABLED] = enabled }
    }

    override suspend fun setAutoSyncOnStartup(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_SYNC_ON_STARTUP] = enabled }
    }

    override suspend fun setAudioFadeInEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_AUDIO_FADE_IN_ENABLED] = enabled }
    }

    override suspend fun setDefaultStartScreen(screen: String) {
        dataStore.edit { it[KEY_DEFAULT_START_SCREEN] = screen }
    }

    override suspend fun clearPlaybackQueue() {
        queueDao.clearQueue()
    }

    //  Folder filtering setters 

    override suspend fun setFolderFilterMode(mode: FolderFilterMode) {
        dataStore.edit { it[KEY_FOLDER_FILTER_MODE] = mode.name }
    }

    override suspend fun setBlacklistedFolders(folders: Set<String>) {
        dataStore.edit { it[KEY_BLACKLISTED_FOLDERS] = encodeFolderSet(folders) }
    }

    override suspend fun setWhitelistedFolders(folders: Set<String>) {
        dataStore.edit { it[KEY_WHITELISTED_FOLDERS] = encodeFolderSet(folders) }
    }

    //  Serialisation helpers 

    private fun encodeFolderSet(folders: Set<String>): String =
        folders.filter { it.isNotBlank() }.joinToString(FOLDER_DELIMITER)

    private fun decodeFolderSet(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(FOLDER_DELIMITER).filter { it.isNotBlank() }.toSet()
    }
}
