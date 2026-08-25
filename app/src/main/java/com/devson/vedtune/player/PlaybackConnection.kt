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
    private var originalQueue: List<Song> = emptyList()

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

    private val _queueSongMap = MutableStateFlow<Map<Long, Song>>(emptyMap())
    val queueSongMap: StateFlow<Map<Long, Song>> = _queueSongMap.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

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
            _currentSong.value = songId?.let { _queueSongMap.value[it] }
            updateQueue()
            _playbackPosition.value = 0L
            _playbackDuration.value = mediaController?.duration?.coerceAtLeast(0L) ?: 0L

            scope.launch(Dispatchers.IO) {
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
            updateQueue()
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
        scope.launch(Dispatchers.IO) {
            try {
                originalQueue = repository.getQueue()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
        scope.launch {
            val preferences = dataStore.data.first()
            val savedShuffleMode = preferences[KEY_SHUFFLE_MODE] ?: false
            _shuffleModeEnabled.value = savedShuffleMode
        }
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
                delay(150)
            }
        }
    }

    private fun savePlaybackPosition(position: Long) {
        scope.launch(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[KEY_PLAYBACK_POSITION] = position
            }
        }
    }

    fun playSong(song: Song, playlist: List<Song>) {
        scope.launch {
            try {
                val controller = getController()
                originalQueue = playlist
                
                val finalPlaylist = if (_shuffleModeEnabled.value) {
                    val clicked = song
                    val remaining = playlist.filter { it.id != song.id }.shuffled()
                    listOf(clicked) + remaining
                } else {
                    playlist
                }
                
                val mediaItems = finalPlaylist.map { s ->
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
                
                val index = if (_shuffleModeEnabled.value) 0 else finalPlaylist.indexOfFirst { it.id == song.id }
                if (index != -1) {
                    controller.seekTo(index, 0L)
                }
                controller.prepare()
                controller.volume = 1f
                controller.play()
                
                // Update in-memory state immediately
                val songsMap = HashMap<Long, Song>(finalPlaylist.size).apply {
                    finalPlaylist.forEach { put(it.id, it) }
                }
                _playlistQueue.value = finalPlaylist
                _queueSongMap.value = songsMap
                _currentSongId.value = song.id
                _currentSong.value = song
                
                scope.launch(Dispatchers.IO) {
                    try {
                        repository.saveQueue(finalPlaylist)
                        dataStore.edit { preferences ->
                            preferences[KEY_CURRENT_SON_ID] = song.id
                            preferences[KEY_PLAYBACK_POSITION] = 0L
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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
                    controller.volume = 1f
                    controller.play()
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
                
                // Update originalQueue in parallel
                val updatedOriginal = originalQueue.toMutableList()
                val idxInOrig = updatedOriginal.indexOfFirst { it.id == song.id }
                if (idxInOrig != -1) {
                    updatedOriginal.removeAt(idxInOrig)
                }
                val insertIdxInOrig = if (currentItemIndex != -1) {
                    val index = updatedOriginal.indexOfFirst { it.id == currentSongId.value }
                    if (index != -1) index + 1 else 0
                } else {
                    0
                }
                updatedOriginal.add(insertIdxInOrig.coerceIn(0, updatedOriginal.size), song)
                originalQueue = updatedOriginal

                scope.launch(Dispatchers.IO) {
                    repository.saveQueue(currentQueue)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playShuffle(song: Song, playlist: List<Song>) {
        scope.launch {
            try {
                val controller = getController()
                originalQueue = playlist
                _shuffleModeEnabled.value = true
                scope.launch(Dispatchers.IO) {
                    dataStore.edit { preferences ->
                        preferences[KEY_SHUFFLE_MODE] = true
                    }
                }
                
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
                controller.volume = 1f
                controller.play()
                
                val songsMap = HashMap<Long, Song>(fullList.size).apply {
                    fullList.forEach { put(it.id, it) }
                }
                _playlistQueue.value = fullList
                _queueSongMap.value = songsMap
                _currentSongId.value = song.id
                _currentSong.value = song

                scope.launch(Dispatchers.IO) {
                    try {
                        repository.saveQueue(fullList)
                        dataStore.edit { preferences ->
                            preferences[KEY_CURRENT_SON_ID] = song.id
                            preferences[KEY_PLAYBACK_POSITION] = 0L
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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
                _currentSong.value = null
                _playbackPosition.value = 0L
                _playbackDuration.value = 0L
                _queueSongMap.value = emptyMap()
                originalQueue = emptyList()
                scope.launch(Dispatchers.IO) {
                    repository.saveQueue(emptyList())
                    dataStore.edit { preferences ->
                        preferences.remove(KEY_CURRENT_SON_ID)
                        preferences[KEY_PLAYBACK_POSITION] = 0L
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun play() {
        scope.launch {
            try {
                val controller = getController()
                controller.volume = 1f
                controller.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun pause() {
        scope.launch {
            try {
                val controller = getController()
                controller.pause()
                savePlaybackPosition(controller.currentPosition)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun skipToNext() {
        scope.launch {
            try {
                val controller = getController()
                if (controller.nextMediaItemIndex != androidx.media3.common.C.INDEX_UNSET) {
                    controller.seekToNext()
                    controller.volume = 1f
                    controller.play()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun skipToPrevious() {
        scope.launch {
            try {
                val controller = getController()
                if (controller.previousMediaItemIndex != androidx.media3.common.C.INDEX_UNSET) {
                    controller.seekToPrevious()
                    controller.volume = 1f
                    controller.play()
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
                val currentSongIdVal = _currentSongId.value
                
                if (enabled) {
                    _shuffleModeEnabled.value = true
                    dataStore.edit { preferences ->
                        preferences[KEY_SHUFFLE_MODE] = true
                    }
                    
                    if (originalQueue.isEmpty()) {
                        originalQueue = _playlistQueue.value
                    }
                    val currentSong = originalQueue.firstOrNull { it.id == currentSongIdVal }
                    
                    if (currentSong != null) {
                        val remaining = originalQueue.filter { it.id != currentSong.id }
                        val shuffledRest = remaining.shuffled()
                        val finalPlaylist = listOf(currentSong) + shuffledRest
                        
                        val currentPosition = controller.currentPosition
                        
                        val mediaItems = finalPlaylist.map { s ->
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
                        controller.seekTo(0, currentPosition)
                        
                        _playlistQueue.value = finalPlaylist
                        repository.saveQueue(finalPlaylist)
                    }
                } else {
                    _shuffleModeEnabled.value = false
                    dataStore.edit { preferences ->
                        preferences[KEY_SHUFFLE_MODE] = false
                    }
                    
                    if (originalQueue.isNotEmpty()) {
                        val currentPosition = controller.currentPosition
                        val originalIndex = originalQueue.indexOfFirst { it.id == currentSongIdVal }
                        
                        val mediaItems = originalQueue.map { s ->
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
                        if (originalIndex != -1) {
                            controller.seekTo(originalIndex, currentPosition)
                        }
                        
                        _playlistQueue.value = originalQueue
                        repository.saveQueue(originalQueue)
                    }
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
        val timeline = controller.currentTimeline
        if (timeline.isEmpty) {
            _playlistQueue.value = emptyList()
            _queueSongMap.value = emptyMap()
            return
        }
        val window = androidx.media3.common.Timeline.Window()
        val songIds = mutableListOf<Long>()
        
        var index = timeline.getFirstWindowIndex(false)
        while (index != androidx.media3.common.C.INDEX_UNSET) {
            timeline.getWindow(index, window)
            window.mediaItem.mediaId?.toLongOrNull()?.let { songIds.add(it) }
            index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, false)
        }
        
        scope.launch(Dispatchers.Default) {
            try {
                val songs = kotlinx.coroutines.withContext(Dispatchers.IO) { repository.getSongsByIds(songIds) }
                val songsMap = HashMap<Long, Song>(songs.size).apply {
                    songs.forEach { put(it.id, it) }
                }
                val orderedSongs = ArrayList<Song>(songIds.size)
                for (id in songIds) {
                    songsMap[id]?.let { orderedSongs.add(it) }
                }
                _playlistQueue.value = orderedSongs
                _queueSongMap.value = songsMap
                val curId = _currentSongId.value
                if (curId != null && (_currentSong.value == null || _currentSong.value?.id != curId)) {
                    _currentSong.value = songsMap[curId]
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        scope.launch {
            try {
                val controller = getController()
                val currentQueue = _playlistQueue.value
                if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices) return@launch
                
                val fromSong = currentQueue[fromIndex]
                val toSong = currentQueue[toIndex]
                
                var unshuffledFromIndex = -1
                var unshuffledToIndex = -1
                for (i in 0 until controller.mediaItemCount) {
                    val mediaId = controller.getMediaItemAt(i).mediaId
                    if (mediaId == fromSong.id.toString()) unshuffledFromIndex = i
                    if (mediaId == toSong.id.toString()) unshuffledToIndex = i
                }
                
                if (unshuffledFromIndex != -1 && unshuffledToIndex != -1) {
                    controller.moveMediaItem(unshuffledFromIndex, unshuffledToIndex)
                    val updatedQueue = currentQueue.toMutableList()
                    val item = updatedQueue.removeAt(fromIndex)
                    updatedQueue.add(toIndex, item)
                    _playlistQueue.value = updatedQueue
                    repository.saveQueue(updatedQueue)
                    
                    val updatedOriginal = originalQueue.toMutableList()
                    val fromOriginalIndex = updatedOriginal.indexOfFirst { it.id == fromSong.id }
                    val toOriginalIndex = updatedOriginal.indexOfFirst { it.id == toSong.id }
                    if (fromOriginalIndex != -1 && toOriginalIndex != -1) {
                        val originalItem = updatedOriginal.removeAt(fromOriginalIndex)
                        updatedOriginal.add(toOriginalIndex, originalItem)
                        originalQueue = updatedOriginal
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeQueueItem(index: Int) {
        scope.launch {
            try {
                val controller = getController()
                val currentQueue = _playlistQueue.value
                if (index !in currentQueue.indices) return@launch
                
                val targetSong = currentQueue[index]
                var unshuffledIndex = -1
                for (i in 0 until controller.mediaItemCount) {
                    if (controller.getMediaItemAt(i).mediaId == targetSong.id.toString()) {
                        unshuffledIndex = i
                        break
                    }
                }
                
                if (unshuffledIndex != -1) {
                    controller.removeMediaItem(unshuffledIndex)
                    val updatedQueue = currentQueue.toMutableList()
                    updatedQueue.removeAt(index)
                    _playlistQueue.value = updatedQueue
                    repository.saveQueue(updatedQueue)
                    
                    originalQueue = originalQueue.filter { it.id != targetSong.id }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playQueueItemById(songId: Long) {
        scope.launch {
            try {
                val controller = getController()
                var targetIndex = -1
                for (i in 0 until controller.mediaItemCount) {
                    if (controller.getMediaItemAt(i).mediaId == songId.toString()) {
                        targetIndex = i
                        break
                    }
                }
                if (targetIndex != -1) {
                    controller.seekTo(targetIndex, 0L)
                    controller.play()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

