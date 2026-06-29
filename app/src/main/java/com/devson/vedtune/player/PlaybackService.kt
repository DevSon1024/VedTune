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
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.devson.vedtune.MainActivity
import com.devson.vedtune.domain.repository.MediaRepository
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
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
                Player.STATE_IDLE, Player.STATE_ENDED -> {
                    // Let Media3 handle demotion, just request service stop
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
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

        val closeButton = CommandButton.Builder()
            .setDisplayName("Close")
            .setIconResId(com.devson.vedtune.R.drawable.ic_close)
            .setSessionCommand(SessionCommand("ACTION_CLOSE", Bundle.EMPTY))
            .build()

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(pendingIntent)
            .setCustomLayout(listOf(stopButton, closeButton))
            .setCallback(mediaSessionCallback)
            .build()

        // Restore playback state
        serviceScope.launch {
            restorePlaybackState()
        }
    }

    private val mediaSessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val sessionCommands = connectionResult.availableSessionCommands
                .buildUpon()
                .add(SessionCommand("ACTION_CLOSE", Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.accept(
                sessionCommands,
                connectionResult.availablePlayerCommands
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == "ACTION_CLOSE") {
                closeAppAndRelease()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_UNKNOWN))
        }
    }

    private fun closeAppAndRelease() {
        exoPlayer.pause()
        exoPlayer.clearMediaItems()
        mediaSession?.run {
            release()
        }
        mediaSession = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_CLOSE_APP", true)
        }
        startActivity(intent)
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
        super.onTaskRemoved(rootIntent)
        
        // 1. Pause playback so audio doesn't continue in the background
        exoPlayer.pause()
        
        // 2. Explicitly release the MediaSession to instantly kill the System UI ghost notification
        mediaSession?.run {
            release()
        }
        mediaSession = null
        
        // 3. Remove foreground status and stop the service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        exoPlayer.removeListener(playerListener)
        mediaSession?.run {
            release()
        }
        mediaSession = null
        // Note: Do NOT call exoPlayer.release() here. Since ExoPlayer is provided 
        // via Hilt injection, its lifecycle outlives this service. Releasing it 
        // will cause a crash if the user re-opens the app while the process is alive.
        super.onDestroy()
    }
}
