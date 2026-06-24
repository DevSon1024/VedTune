package com.devson.vedtune.player

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.domain.repository.MediaRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackConnection @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MediaRepository,
    private val dataStore: DataStore<Preferences>
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSongId = MutableStateFlow<Long?>(null)
    val currentSongId: StateFlow<Long?> = _currentSongId.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _playbackDuration = MutableStateFlow(0L)
    val playbackDuration: StateFlow<Long> = _playbackDuration.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _sleepTimerRemaining = MutableStateFlow(0L)
    val sleepTimerRemaining: StateFlow<Long> = _sleepTimerRemaining.asStateFlow()

    private var sleepTimerJob: kotlinx.coroutines.Job? = null

    companion object {
        private val KEY_CURRENT_SON_ID = longPreferencesKey("current_song_id")
        private val KEY_PLAYBACK_POSITION = longPreferencesKey("playback_position")
        private val KEY_REPEAT_MODE = intPreferencesKey("repeat_mode")
        private val KEY_SHUFFLE_MODE = booleanPreferencesKey("shuffle_mode")
    }

    init {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                mediaController?.addListener(playerListener)
                updateState()
                startPositionTracker()
                restorePlaybackState()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (!isPlaying) {
                mediaController?.let { 
                    val pos = it.currentPosition
                    _playbackPosition.value = pos
                    savePlaybackPosition(pos)
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val songId = mediaItem?.mediaId?.toLongOrNull()
            _currentSongId.value = songId
            mediaController?.let {
                _playbackPosition.value = 0L
                _playbackDuration.value = it.duration.coerceAtLeast(0L)
            }
            scope.launch {
                dataStore.edit { preferences ->
                    if (songId != null) {
                        preferences[KEY_CURRENT_SON_ID] = songId
                    } else {
                        preferences.remove(KEY_CURRENT_SON_ID)
                    }
                    preferences[KEY_PLAYBACK_POSITION] = 0L
                }
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
            scope.launch {
                dataStore.edit { preferences ->
                    preferences[KEY_REPEAT_MODE] = repeatMode
                }
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleModeEnabled.value = shuffleModeEnabled
            scope.launch {
                dataStore.edit { preferences ->
                    preferences[KEY_SHUFFLE_MODE] = shuffleModeEnabled
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
        }
    }

    private fun updateState() {
        mediaController?.let { controller ->
            _isPlaying.value = controller.isPlaying
            _currentSongId.value = controller.currentMediaItem?.mediaId?.toLongOrNull()
            _playbackPosition.value = controller.currentPosition
            _playbackDuration.value = controller.duration.coerceAtLeast(0L)
            _repeatMode.value = controller.repeatMode
            _shuffleModeEnabled.value = controller.shuffleModeEnabled
        }
    }

    private fun startPositionTracker() {
        scope.launch {
            var lastSaveTime = 0L
            while (true) {
                if (isPlaying.value) {
                    mediaController?.let { controller ->
                        val currentPos = controller.currentPosition
                        _playbackPosition.value = currentPos
                        _playbackDuration.value = controller.duration.coerceAtLeast(0L)
                        
                        val now = System.currentTimeMillis()
                        if (now - lastSaveTime > 5000) {
                            savePlaybackPosition(currentPos)
                            lastSaveTime = now
                        }
                    }
                }
                delay(500)
            }
        }
    }

    private fun savePlaybackPosition(position: Long) {
        scope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_PLAYBACK_POSITION] = position
            }
        }
    }

    private fun restorePlaybackState() {
        scope.launch {
            mediaController?.let { controller ->
                if (controller.mediaItemCount == 0) {
                    val savedQueue = repository.getQueue()
                    if (savedQueue.isNotEmpty()) {
                        val preferences = dataStore.data.first()
                        val savedSongId = preferences[KEY_CURRENT_SON_ID]
                        val savedPosition = preferences[KEY_PLAYBACK_POSITION] ?: 0L
                        val savedRepeatMode = preferences[KEY_REPEAT_MODE] ?: Player.REPEAT_MODE_OFF
                        val savedShuffleMode = preferences[KEY_SHUFFLE_MODE] ?: false

                        val mediaItems = savedQueue.map { s ->
                            MediaItem.Builder()
                                .setMediaId(s.id.toString())
                                .setUri(ContentUris.withAppendedId(
                                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                    s.id
                                ))
                                .build()
                        }
                        controller.setMediaItems(mediaItems)
                        
                        val index = savedQueue.indexOfFirst { it.id == savedSongId }
                        if (index != -1) {
                            controller.seekTo(index, savedPosition)
                        } else {
                            controller.seekTo(0, savedPosition)
                        }
                        
                        controller.repeatMode = savedRepeatMode
                        controller.shuffleModeEnabled = savedShuffleMode
                        controller.prepare()
                        
                        _repeatMode.value = savedRepeatMode
                        _shuffleModeEnabled.value = savedShuffleMode
                        updateState()
                    }
                } else {
                    _repeatMode.value = controller.repeatMode
                    _shuffleModeEnabled.value = controller.shuffleModeEnabled
                }
            }
        }
    }

    fun playSong(song: Song, playlist: List<Song>) {
        mediaController?.let { controller ->
            val mediaItems = playlist.map { s ->
                MediaItem.Builder()
                    .setMediaId(s.id.toString())
                    .setUri(ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        s.id
                    ))
                    .build()
            }
            controller.setMediaItems(mediaItems)
            val index = playlist.indexOfFirst { it.id == song.id }
            if (index != -1) {
                controller.seekTo(index, 0L)
            }
            controller.prepare()
            controller.play()

            scope.launch {
                repository.saveQueue(playlist)
                dataStore.edit { preferences ->
                    preferences[KEY_CURRENT_SON_ID] = song.id
                    preferences[KEY_PLAYBACK_POSITION] = 0L
                }
            }
        }
    }

    fun play() {
        mediaController?.play()
    }

    fun pause() {
        mediaController?.let { controller ->
            controller.pause()
            savePlaybackPosition(controller.currentPosition)
        }
    }

    fun skipToNext() {
        mediaController?.seekToNext()
    }

    fun skipToPrevious() {
        mediaController?.seekToPrevious()
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _playbackPosition.value = positionMs
        savePlaybackPosition(positionMs)
    }

    fun setRepeatMode(repeatMode: Int) {
        mediaController?.let { controller ->
            controller.repeatMode = repeatMode
            _repeatMode.value = repeatMode
            scope.launch {
                dataStore.edit { preferences ->
                    preferences[KEY_REPEAT_MODE] = repeatMode
                }
            }
        }
    }

    fun setShuffleModeEnabled(enabled: Boolean) {
        mediaController?.let { controller ->
            controller.shuffleModeEnabled = enabled
            _shuffleModeEnabled.value = enabled
            scope.launch {
                dataStore.edit { preferences ->
                    preferences[KEY_SHUFFLE_MODE] = enabled
                }
            }
        }
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemaining.value = 0L
            return
        }
        val durationMs = minutes * 60 * 1000L
        _sleepTimerRemaining.value = durationMs
        
        sleepTimerJob = scope.launch {
            var remaining = durationMs
            while (remaining > 0) {
                delay(1000)
                remaining -= 1000
                _sleepTimerRemaining.value = remaining
            }
            pause()
            _sleepTimerRemaining.value = 0L
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemaining.value = 0L
    }
}
