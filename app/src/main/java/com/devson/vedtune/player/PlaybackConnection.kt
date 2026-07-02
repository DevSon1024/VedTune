package com.devson.vedtune.player

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.domain.repository.MediaRepository
import com.google.common.util.concurrent.ListenableFuture
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackConnection @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MediaRepository,
    private val dataStore: DataStore<Preferences>
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var fadeJob: kotlinx.coroutines.Job? = null
    private var sleepTimerJob: kotlinx.coroutines.Job? = null
    private var consecutiveErrors = 0
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

    private val _playlistQueue = MutableStateFlow<List<Song>>(emptyList())
    val playlistQueue: StateFlow<List<Song>> = _playlistQueue.asStateFlow()

    companion object {
        private val KEY_CURRENT_SON_ID = longPreferencesKey("current_song_id")
        private val KEY_PLAYBACK_POSITION = longPreferencesKey("playback_position")
        private val KEY_REPEAT_MODE = intPreferencesKey("repeat_mode")
        private val KEY_SHUFFLE_MODE = booleanPreferencesKey("shuffle_mode")
        private val KEY_AUDIO_FADE_IN_ENABLED = booleanPreferencesKey("audio_fade_in_enabled")
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (!isPlaying) {
                val pos = mediaController?.currentPosition ?: 0L
                _playbackPosition.value = pos
                savePlaybackPosition(pos)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            consecutiveErrors = 0
            val songId = mediaItem?.mediaId?.toLongOrNull()
            _currentSongId.value = songId
            updateQueue()
            _playbackPosition.value = 0L
            _playbackDuration.value = mediaController?.duration?.coerceAtLeast(0L) ?: 0L

            // Trigger auto transition fade-in
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                scope.launch {
                    val preferences = dataStore.data.first()
                    val fadeIn = preferences[KEY_AUDIO_FADE_IN_ENABLED] ?: true
                    if (fadeIn) {
                        fadeJob?.cancel()
                        fadeJob = launch {
                            val controller = mediaController ?: return@launch
                            var waitCount = 0
                            while (!controller.isPlaying && controller.playWhenReady && waitCount < 20) {
                                delay(50)
                                waitCount++
                            }
                            if (controller.isPlaying) {
                                controller.volume = 0f
                                var vol = 0f
                                while (vol < 1.0f && controller.isPlaying) {
                                    delay(25)
                                    vol += 0.05f
                                    controller.volume = vol.coerceAtMost(1f)
                                }
                                if (controller.isPlaying) {
                                    controller.volume = 1f
                                }
                            }
                        }
                    }
                }
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
            consecutiveErrors++
            scope.launch {
                android.widget.Toast.makeText(
                    context,
                    "Playback error: ${error.localizedMessage ?: "Unknown error"}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()

                try {
                    val controller = getController()
                    if (consecutiveErrors < 3 && controller.nextMediaItemIndex != androidx.media3.common.C.INDEX_UNSET) {
                        controller.seekToNext()
                        controller.prepare()
                        controller.play()
                    } else {
                        controller.stop()
                        consecutiveErrors = 0
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val controller = mediaController ?: return
            _playbackDuration.value = controller.duration.coerceAtLeast(0L)
            _playbackPosition.value = controller.currentPosition
            updateQueue()
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            val controller = mediaController ?: return
            _playbackDuration.value = controller.duration.coerceAtLeast(0L)
            _playbackPosition.value = controller.currentPosition
            updateQueue()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            _playbackPosition.value = newPosition.positionMs
        }
    }

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val controller = future.get()
                mediaController = controller
                controller.addListener(playerListener)
                // Force a manual refresh of playback state immediately on connection
                updateState()
                startPositionTracker()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private suspend fun getController(): MediaController {
        val controller = mediaController
        if (controller != null && controller.isConnected) {
            return controller
        }
        return suspendCancellableCoroutine { continuation ->
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val future = MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture = future
            future.addListener({
                try {
                    val newController = future.get()
                    mediaController = newController
                    newController.addListener(playerListener)
                    // Force a manual refresh of playback state immediately on connection
                    updateState()
                    if (continuation.isActive) {
                        continuation.resume(newController)
                    }
                } catch (e: Exception) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    private fun updateState() {
        val controller = mediaController ?: return
        if (!controller.isConnected) return
        _isPlaying.value = controller.isPlaying
        _currentSongId.value = controller.currentMediaItem?.mediaId?.toLongOrNull()
        _playbackPosition.value = controller.currentPosition
        _playbackDuration.value = controller.duration.coerceAtLeast(0L)
        _repeatMode.value = controller.repeatMode
        _shuffleModeEnabled.value = controller.shuffleModeEnabled
        updateQueue()
    }

    private fun startPositionTracker() {
        scope.launch {
            var lastSaveTime = 0L
            while (true) {
                val controller = mediaController
                if (controller != null && controller.isConnected && controller.isPlaying) {
                    val currentPos = controller.currentPosition
                    _playbackPosition.value = currentPos
                    _playbackDuration.value = controller.duration.coerceAtLeast(0L)
                    
                    val now = System.currentTimeMillis()
                    if (now - lastSaveTime > 5000) {
                        savePlaybackPosition(currentPos)
                        lastSaveTime = now
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

    fun playSong(song: Song, playlist: List<Song>) {
        scope.launch {
            try {
                val controller = getController()
                val mediaItems = playlist.map { s ->
                    MediaItem.Builder()
                        .setMediaId(s.id.toString())
                        .setUri(ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            s.id
                        ))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(s.title)
                                .setArtist(s.artist)
                                .setAlbumTitle(s.album)
                                .setArtworkUri(Uri.parse("content://media/external/audio/albumart/${s.albumId}"))
                                .build()
                        )
                        .build()
                }
                controller.setMediaItems(mediaItems)
                val index = playlist.indexOfFirst { it.id == song.id }
                if (index != -1) {
                    controller.seekTo(index, 0L)
                }
                controller.prepare()
                playWithFadeIn()

                repository.saveQueue(playlist)
                dataStore.edit { preferences ->
                    preferences[KEY_CURRENT_SON_ID] = song.id
                    preferences[KEY_PLAYBACK_POSITION] = 0L
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playNext(song: Song) {
        scope.launch {
            try {
                val controller = getController()
                val currentItemIndex = if (controller.mediaItemCount > 0) controller.currentMediaItemIndex else -1
                val mediaItem = MediaItem.Builder()
                    .setMediaId(song.id.toString())
                    .setUri(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id))
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artist)
                            .setAlbumTitle(song.album)
                            .setArtworkUri(Uri.parse("content://media/external/audio/albumart/${song.albumId}"))
                            .build()
                    )
                    .build()
                if (currentItemIndex != -1) {
                    var existingIndex = -1
                    for (i in 0 until controller.mediaItemCount) {
                        if (controller.getMediaItemAt(i).mediaId == song.id.toString()) {
                            existingIndex = i
                            break
                        }
                    }
                    if (existingIndex != -1) {
                        controller.removeMediaItem(existingIndex)
                    }
                    val insertIndex = if (existingIndex != -1 && existingIndex <= currentItemIndex) {
                        currentItemIndex
                    } else {
                        currentItemIndex + 1
                    }
                    controller.addMediaItem(insertIndex, mediaItem)
                } else {
                    controller.addMediaItem(mediaItem)
                    controller.prepare()
                    playWithFadeIn()
                }
                val currentQueue = repository.getQueue().toMutableList()
                val indexInQueue = currentQueue.indexOfFirst { it.id == song.id }
                if (indexInQueue != -1) {
                    currentQueue.removeAt(indexInQueue)
                }
                val insertIndex = if (currentItemIndex != -1) {
                    val index = currentQueue.indexOfFirst { it.id == currentSongId.value }
                    if (index != -1) index + 1 else 0
                } else {
                    0
                }
                currentQueue.add(insertIndex.coerceIn(0, currentQueue.size), song)
                repository.saveQueue(currentQueue)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playShuffle(song: Song, playlist: List<Song>) {
        scope.launch {
            try {
                val controller = getController()
                val remaining = playlist.filter { it.id != song.id }.shuffled()
                val fullList = listOf(song) + remaining
                
                val mediaItems = fullList.map { s ->
                    MediaItem.Builder()
                        .setMediaId(s.id.toString())
                        .setUri(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, s.id))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(s.title)
                                .setArtist(s.artist)
                                .setAlbumTitle(s.album)
                                .setArtworkUri(Uri.parse("content://media/external/audio/albumart/${s.albumId}"))
                                .build()
                        )
                        .build()
                }
                controller.setMediaItems(mediaItems)
                controller.seekTo(0, 0L)
                controller.prepare()
                playWithFadeIn()
                
                repository.saveQueue(fullList)
                dataStore.edit { preferences ->
                    preferences[KEY_CURRENT_SON_ID] = song.id
                    preferences[KEY_PLAYBACK_POSITION] = 0L
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearQueue() {
        scope.launch {
            try {
                val controller = getController()
                controller.stop()
                controller.clearMediaItems()
                _currentSongId.value = null
                _playbackPosition.value = 0L
                _playbackDuration.value = 0L
                repository.saveQueue(emptyList())
                dataStore.edit { preferences ->
                    preferences.remove(KEY_CURRENT_SON_ID)
                    preferences[KEY_PLAYBACK_POSITION] = 0L
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playWithFadeIn() {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            try {
                val controller = getController()
                val preferences = dataStore.data.first()
                val fadeIn = preferences[KEY_AUDIO_FADE_IN_ENABLED] ?: true
                if (fadeIn) {
                    controller.volume = 0f
                    controller.play()
                    var waitCount = 0
                    while (!controller.isPlaying && controller.playWhenReady && waitCount < 20) {
                        delay(50)
                        waitCount++
                    }
                    var vol = 0f
                    while (vol < 1.0f && controller.isPlaying) {
                        delay(25)
                        vol += 0.05f
                        controller.volume = vol.coerceAtMost(1f)
                    }
                    if (controller.playWhenReady) {
                        controller.volume = 1f
                    }
                } else {
                    controller.volume = 1f
                    controller.play()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun play() {
        playWithFadeIn()
    }

    fun pause() {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            try {
                val controller = getController()
                val preferences = dataStore.data.first()
                val fadeIn = preferences[KEY_AUDIO_FADE_IN_ENABLED] ?: true
                if (fadeIn && controller.isPlaying) {
                    var vol = controller.volume
                    while (vol > 0.0f && controller.isPlaying) {
                        delay(20)
                        vol -= 0.05f
                        controller.volume = vol.coerceAtLeast(0f)
                    }
                    if (controller.isPlaying) {
                        controller.pause()
                    }
                    controller.volume = 1f
                } else {
                    controller.pause()
                }
                savePlaybackPosition(controller.currentPosition)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun skipToNext() {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            try {
                val controller = getController()
                val preferences = dataStore.data.first()
                val fadeIn = preferences[KEY_AUDIO_FADE_IN_ENABLED] ?: true
                if (fadeIn && controller.isPlaying && controller.nextMediaItemIndex != androidx.media3.common.C.INDEX_UNSET) {
                    var vol = controller.volume
                    while (vol > 0.0f && controller.isPlaying) {
                        delay(15)
                        vol -= 0.1f
                        controller.volume = vol.coerceAtLeast(0f)
                    }
                    controller.seekToNext()
                    var waitCount = 0
                    while (!controller.isPlaying && controller.playWhenReady && waitCount < 20) {
                        delay(50)
                        waitCount++
                    }
                    controller.volume = 0f
                    var newVol = 0f
                    while (newVol < 1.0f && controller.isPlaying) {
                        delay(20)
                        newVol += 0.08f
                        controller.volume = newVol.coerceAtMost(1f)
                    }
                    if (controller.playWhenReady) {
                        controller.volume = 1f
                    }
                } else {
                    controller.seekToNext()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun skipToPrevious() {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            try {
                val controller = getController()
                val preferences = dataStore.data.first()
                val fadeIn = preferences[KEY_AUDIO_FADE_IN_ENABLED] ?: true
                if (fadeIn && controller.isPlaying && controller.previousMediaItemIndex != androidx.media3.common.C.INDEX_UNSET) {
                    var vol = controller.volume
                    while (vol > 0.0f && controller.isPlaying) {
                        delay(15)
                        vol -= 0.1f
                        controller.volume = vol.coerceAtLeast(0f)
                    }
                    controller.seekToPrevious()
                    var waitCount = 0
                    while (!controller.isPlaying && controller.playWhenReady && waitCount < 20) {
                        delay(50)
                        waitCount++
                    }
                    controller.volume = 0f
                    var newVol = 0f
                    while (newVol < 1.0f && controller.isPlaying) {
                        delay(20)
                        newVol += 0.08f
                        controller.volume = newVol.coerceAtMost(1f)
                    }
                    if (controller.playWhenReady) {
                        controller.volume = 1f
                    }
                } else {
                    controller.seekToPrevious()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        scope.launch {
            try {
                val controller = getController()
                controller.seekTo(positionMs)
                _playbackPosition.value = positionMs
                savePlaybackPosition(positionMs)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setRepeatMode(repeatMode: Int) {
        scope.launch {
            try {
                val controller = getController()
                controller.repeatMode = repeatMode
                _repeatMode.value = repeatMode
                dataStore.edit { preferences ->
                    preferences[KEY_REPEAT_MODE] = repeatMode
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setShuffleModeEnabled(enabled: Boolean) {
        scope.launch {
            try {
                val controller = getController()
                controller.shuffleModeEnabled = enabled
                _shuffleModeEnabled.value = enabled
                dataStore.edit { preferences ->
                    preferences[KEY_SHUFFLE_MODE] = enabled
                }
            } catch (e: Exception) {
                e.printStackTrace()
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

    private fun updateQueue() {
        val controller = mediaController ?: return
        if (!controller.isConnected) return
        val count = controller.mediaItemCount
        val songIds = (0 until count).mapNotNull { index ->
            controller.getMediaItemAt(index).mediaId?.toLongOrNull()
        }
        scope.launch(Dispatchers.IO) {
            try {
                val songs = repository.getSongsByIds(songIds)
                val songsMap = songs.associateBy { it.id }
                val orderedSongs = songIds.mapNotNull { id -> songsMap[id] }
                _playlistQueue.value = orderedSongs
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        scope.launch {
            try {
                val controller = getController()
                if (fromIndex in 0 until controller.mediaItemCount && toIndex in 0 until controller.mediaItemCount) {
                    controller.moveMediaItem(fromIndex, toIndex)
                    val updatedQueue = _playlistQueue.value.toMutableList()
                    if (fromIndex < updatedQueue.size && toIndex < updatedQueue.size) {
                        val item = updatedQueue.removeAt(fromIndex)
                        updatedQueue.add(toIndex, item)
                        _playlistQueue.value = updatedQueue
                        repository.saveQueue(updatedQueue)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun skipToQueueItem(index: Int) {
        scope.launch {
            try {
                val controller = getController()
                if (index in 0 until controller.mediaItemCount) {
                    controller.seekTo(index, 0L)
                    controller.play()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
