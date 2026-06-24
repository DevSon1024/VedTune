package com.devson.vedtune.player

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.devson.vedtune.domain.model.Song
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
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackConnection @Inject constructor(
    @ApplicationContext private val context: Context
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

    init {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                mediaController?.addListener(playerListener)
                updateState()
                startPositionTracker()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (!isPlaying) {
                mediaController?.let { _playbackPosition.value = it.currentPosition }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentSongId.value = mediaItem?.mediaId?.toLongOrNull()
            mediaController?.let {
                _playbackPosition.value = 0L
                _playbackDuration.value = it.duration.coerceAtLeast(0L)
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
        }
    }

    private fun startPositionTracker() {
        scope.launch {
            while (true) {
                if (isPlaying.value) {
                    mediaController?.let { controller ->
                        _playbackPosition.value = controller.currentPosition
                        _playbackDuration.value = controller.duration.coerceAtLeast(0L)
                    }
                }
                delay(500)
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
        }
    }

    fun play() {
        mediaController?.play()
    }

    fun pause() {
        mediaController?.pause()
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
    }
}
