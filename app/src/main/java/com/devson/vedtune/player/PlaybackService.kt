package com.devson.vedtune.player

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.devson.vedtune.MainActivity
import com.devson.vedtune.domain.repository.MediaRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var exoPlayer: ExoPlayer

    @Inject
    lateinit var repository: MediaRepository

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        private val KEY_CURRENT_SON_ID = longPreferencesKey("current_song_id")
        private val KEY_PLAYBACK_POSITION = longPreferencesKey("playback_position")
        private val KEY_REPEAT_MODE = intPreferencesKey("repeat_mode")
        private val KEY_SHUFFLE_MODE = booleanPreferencesKey("shuffle_mode")
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE -> {
                    // Playback stopped or released
                    stopPlaybackAndRelease()
                }
                Player.STATE_ENDED -> {
                    // End of playlist
                    stopPlaybackAndRelease()
                }
                Player.STATE_READY, Player.STATE_BUFFERING -> {
                    // Handled automatically by MediaSessionService
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        exoPlayer.addListener(playerListener)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopButton = CommandButton.Builder()
            .setDisplayName("Stop")
            .setIconResId(com.devson.vedtune.R.drawable.ic_stop)
            .setPlayerCommand(Player.COMMAND_STOP)
            .build()

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(pendingIntent)
            .setCustomLayout(listOf(stopButton))
            .build()

        // Restore playback state
        serviceScope.launch {
            restorePlaybackState()
        }
    }

    private suspend fun restorePlaybackState() {
        if (exoPlayer.mediaItemCount == 0) {
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
                exoPlayer.setMediaItems(mediaItems)
                
                val index = savedQueue.indexOfFirst { it.id == savedSongId }
                if (index != -1) {
                    exoPlayer.seekTo(index, savedPosition)
                } else {
                    exoPlayer.seekTo(0, savedPosition)
                }
                
                exoPlayer.repeatMode = savedRepeatMode
                exoPlayer.shuffleModeEnabled = savedShuffleMode
                exoPlayer.prepare()
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null) {
            if (player.playWhenReady && player.playbackState == Player.STATE_READY) {
                // Case 1: Music is playing. Keep foreground service and notification active.
            } else {
                // Case 2: Music is paused/stopped. Stop everything cleanly.
                stopPlaybackAndRelease()
            }
        } else {
            stopPlaybackAndRelease()
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun stopPlaybackAndRelease() {
        mediaSession?.run {
            if (player.isPlaying) {
                player.pause()
            }
            player.stop()
            player.release()
            release()
        }
        mediaSession = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        exoPlayer.removeListener(playerListener)
        stopPlaybackAndRelease()
        super.onDestroy()
    }
}
