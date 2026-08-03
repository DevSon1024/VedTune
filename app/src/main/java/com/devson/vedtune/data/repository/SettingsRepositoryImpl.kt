package com.devson.vedtune.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.devson.vedtune.data.local.dao.QueueDao
import com.devson.vedtune.domain.model.AlbumArtClickAction
import com.devson.vedtune.domain.model.FolderFilterMode
import com.devson.vedtune.domain.model.SeekBarStyle
import com.devson.vedtune.domain.model.ViewPreferences
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
        private val KEY_IS_GRID_VIEW = booleanPreferencesKey("is_grid_view")
        private val KEY_GRID_SPAN_COUNT = intPreferencesKey("grid_span_count")
        private val KEY_SHOW_ARTIST = booleanPreferencesKey("show_artist")
        private val KEY_SHOW_ALBUM = booleanPreferencesKey("show_album")
        private val KEY_SHOW_DURATION = booleanPreferencesKey("show_duration")

        private val KEY_SHOW_ALBUM_ART = booleanPreferencesKey("show_album_art")
        private val KEY_SHOW_REMAINING_TIME = booleanPreferencesKey("show_remaining_time")
        private val KEY_SHOW_MINIPLAYER_PROGRESS = booleanPreferencesKey("show_miniplayer_progress")
        private val KEY_AUTOPLAY_ON_STARTUP = booleanPreferencesKey("autoplay_on_startup")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC_COLORS_ENABLED = booleanPreferencesKey("dynamic_colors_enabled")
        private val KEY_AUTO_SYNC_ON_STARTUP = booleanPreferencesKey("auto_sync_on_startup")
        private val KEY_AUDIO_FADE_IN_ENABLED = booleanPreferencesKey("audio_fade_in_enabled")
        private val KEY_DEFAULT_START_SCREEN = stringPreferencesKey("default_start_screen")
        private val KEY_IS_GESTURE_MINIPLAYER_ENABLED = booleanPreferencesKey("is_gesture_miniplayer_enabled")
        private val KEY_SEEKBAR_STYLE = stringPreferencesKey("seekbar_style")
        private val KEY_ENABLE_SWIPE_TO_SKIP = booleanPreferencesKey("enable_swipe_to_skip")
        private val KEY_KEEP_SCREEN_ON_WITH_LYRICS = booleanPreferencesKey("keep_screen_on_with_lyrics")
        private val KEY_ALBUM_ART_CLICK_ACTION = stringPreferencesKey("album_art_click_action")
        private val KEY_PLAYER_BACKGROUND_BLUR_RADIUS = floatPreferencesKey("player_background_blur_radius")
        private val KEY_IS_AMOLED_DARK = booleanPreferencesKey("amoled_dark_mode")
        private val KEY_ALBUM_ART_QUALITY = stringPreferencesKey("album_art_quality")
        private val KEY_FORCE_SQUARE_ARTWORK = booleanPreferencesKey("force_square_artwork")
        private val KEY_SHOW_LYRICS_BUTTON = booleanPreferencesKey("show_lyrics_button")
        private val KEY_SHOW_SLEEP_TIMER_BUTTON = booleanPreferencesKey("show_sleep_timer_button")
        private val KEY_SHOW_SHUFFLE_REPEAT_BUTTONS = booleanPreferencesKey("show_shuffle_repeat_buttons")
        private val KEY_LRC_SEARCH_FIELD = stringPreferencesKey("lrc_search_field")


        // Folder filter
        private val KEY_FOLDER_FILTER_MODE = stringPreferencesKey("folder_filter_mode")
        private val KEY_BLACKLISTED_FOLDERS = stringPreferencesKey("blacklisted_folders")
        private val KEY_WHITELISTED_FOLDERS = stringPreferencesKey("whitelisted_folders")
        private val KEY_INCLUDE_SUBFOLDERS = booleanPreferencesKey("include_subfolders")

        /** Delimiter used to serialise/deserialise folder sets as a single DataStore string. */
        private const val FOLDER_DELIMITER = "|||"
    }

    //  Existing settings flows 

    override val viewPreferences: Flow<ViewPreferences> = dataStore.data.map { preferences ->
        ViewPreferences(
            isGridView = preferences[KEY_IS_GRID_VIEW] ?: false,
            gridSpanCount = preferences[KEY_GRID_SPAN_COUNT] ?: 2,
            showArtist = preferences[KEY_SHOW_ARTIST] ?: true,
            showAlbum = preferences[KEY_SHOW_ALBUM] ?: true,
            showDuration = preferences[KEY_SHOW_DURATION] ?: true,
            showAlbumArt = preferences[KEY_SHOW_ALBUM_ART] ?: true
        )
    }

    override suspend fun setViewPreferences(preferences: ViewPreferences) {
        dataStore.edit { prefs ->
            prefs[KEY_IS_GRID_VIEW] = preferences.isGridView
            prefs[KEY_GRID_SPAN_COUNT] = preferences.gridSpanCount
            prefs[KEY_SHOW_ARTIST] = preferences.showArtist
            prefs[KEY_SHOW_ALBUM] = preferences.showAlbum
            prefs[KEY_SHOW_DURATION] = preferences.showDuration
            prefs[KEY_SHOW_ALBUM_ART] = preferences.showAlbumArt
        }
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

    override val isGestureMiniPlayerEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_IS_GESTURE_MINIPLAYER_ENABLED] ?: false
    }

    override val seekbarStyle: Flow<SeekBarStyle> = dataStore.data.map { preferences ->
        runCatching {
            SeekBarStyle.valueOf(preferences[KEY_SEEKBAR_STYLE] ?: SeekBarStyle.DEFAULT.name)
        }.getOrDefault(SeekBarStyle.DEFAULT)
    }

    override val enableSwipeToSkip: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_ENABLE_SWIPE_TO_SKIP] ?: true
    }

    override val keepScreenOnWithLyrics: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_KEEP_SCREEN_ON_WITH_LYRICS] ?: false
    }

    override val albumArtClickAction: Flow<AlbumArtClickAction> = dataStore.data.map { preferences ->
        runCatching {
            AlbumArtClickAction.valueOf(preferences[KEY_ALBUM_ART_CLICK_ACTION] ?: AlbumArtClickAction.SHOW_LYRICS.name)
        }.getOrDefault(AlbumArtClickAction.SHOW_LYRICS)
    }

    override val playerBackgroundBlurRadius: Flow<Float> = dataStore.data.map { preferences ->
        preferences[KEY_PLAYER_BACKGROUND_BLUR_RADIUS] ?: 40f
    }

    override val isAmoledDark: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_IS_AMOLED_DARK] ?: false
    }

    override val albumArtQuality: Flow<com.devson.vedtune.domain.model.AlbumArtQuality> = dataStore.data.map { preferences ->
        runCatching {
            com.devson.vedtune.domain.model.AlbumArtQuality.valueOf(
                preferences[KEY_ALBUM_ART_QUALITY] ?: com.devson.vedtune.domain.model.AlbumArtQuality.BALANCED.name
            )
        }.getOrDefault(com.devson.vedtune.domain.model.AlbumArtQuality.BALANCED)
    }

    override val forceSquareArtwork: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_FORCE_SQUARE_ARTWORK] ?: true
    }

    override val showLyricsButton: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_SHOW_LYRICS_BUTTON] ?: false
    }

    override val showSleepTimerButton: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_SHOW_SLEEP_TIMER_BUTTON] ?: true
    }

    override val showShuffleRepeatButtons: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_SHOW_SHUFFLE_REPEAT_BUTTONS] ?: true
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

    override val includeSubfolders: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_INCLUDE_SUBFOLDERS] ?: true
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

    override suspend fun setGestureMiniPlayerEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_IS_GESTURE_MINIPLAYER_ENABLED] = enabled }
    }

    override suspend fun setSeekBarStyle(style: SeekBarStyle) {
        dataStore.edit { it[KEY_SEEKBAR_STYLE] = style.name }
    }

    override suspend fun setEnableSwipeToSkip(enable: Boolean) {
        dataStore.edit { it[KEY_ENABLE_SWIPE_TO_SKIP] = enable }
    }

    override suspend fun setKeepScreenOnWithLyrics(keep: Boolean) {
        dataStore.edit { it[KEY_KEEP_SCREEN_ON_WITH_LYRICS] = keep }
    }

    override suspend fun setAlbumArtClickAction(action: AlbumArtClickAction) {
        dataStore.edit { it[KEY_ALBUM_ART_CLICK_ACTION] = action.name }
    }

    override suspend fun setPlayerBackgroundBlurRadius(radius: Float) {
        dataStore.edit { it[KEY_PLAYER_BACKGROUND_BLUR_RADIUS] = radius.coerceIn(10f, 100f) }
    }

    override suspend fun setAmoledDark(enabled: Boolean) {
        dataStore.edit { it[KEY_IS_AMOLED_DARK] = enabled }
    }

    override suspend fun setAlbumArtQuality(quality: com.devson.vedtune.domain.model.AlbumArtQuality) {
        dataStore.edit { it[KEY_ALBUM_ART_QUALITY] = quality.name }
    }

    override suspend fun setForceSquareArtwork(enabled: Boolean) {
        dataStore.edit { it[KEY_FORCE_SQUARE_ARTWORK] = enabled }
    }

    override suspend fun setShowLyricsButton(show: Boolean) {
        dataStore.edit { it[KEY_SHOW_LYRICS_BUTTON] = show }
    }

    override suspend fun setShowSleepTimerButton(show: Boolean) {
        dataStore.edit { it[KEY_SHOW_SLEEP_TIMER_BUTTON] = show }
    }

    override suspend fun setShowShuffleRepeatButtons(show: Boolean) {
        dataStore.edit { it[KEY_SHOW_SHUFFLE_REPEAT_BUTTONS] = show }
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

    override suspend fun setIncludeSubfolders(include: Boolean) {
        dataStore.edit { it[KEY_INCLUDE_SUBFOLDERS] = include }
    }

    override val lrcSearchField: Flow<LrcSearchField> = dataStore.data.map { preferences ->
        val name = preferences[KEY_LRC_SEARCH_FIELD]
        try {
            if (name != null) LrcSearchField.valueOf(name) else LrcSearchField.TRACK_NAME
        } catch (e: Exception) {
            LrcSearchField.TRACK_NAME
        }
    }

    override suspend fun setLrcSearchField(field: LrcSearchField) {
        dataStore.edit { it[KEY_LRC_SEARCH_FIELD] = field.name }
    }


    //  Serialisation helpers 

    private fun encodeFolderSet(folders: Set<String>): String =
        folders.filter { it.isNotBlank() }.joinToString(FOLDER_DELIMITER)

    private fun decodeFolderSet(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(FOLDER_DELIMITER).filter { it.isNotBlank() }.toSet()
    }
}
