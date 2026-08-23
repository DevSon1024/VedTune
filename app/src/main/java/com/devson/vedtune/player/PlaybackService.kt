package com.devson.vedtune.player

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var exoPlayer: ExoPlayer

    @Inject
    lateinit var repository: MediaRepository

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Inject
    lateinit var volumeNormalizationManager: VolumeNormalizationManager

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var favoriteSongIds: Set<Long> = emptySet()
    private var mediaSessionButtonRefreshJob: Job? = null
    private var lastAppliedMediaButtonSignature: String? = null

    companion object {
        const val CUSTOM_COMMAND_CLOSE_PLAYER = "com.devson.vedtune.CLOSE_PLAYER"
        const val CUSTOM_COMMAND_LIKE = "com.devson.vedtune.LIKE"

        private val KEY_CURRENT_SONG_ID = longPreferencesKey("current_song_id")
        private val KEY_PLAYBACK_POSITION = longPreferencesKey("playback_position")
        private val KEY_REPEAT_MODE = intPreferencesKey("repeat_mode")
        private val KEY_SHUFFLE_MODE = booleanPreferencesKey("shuffle_mode")
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaSession?.let { refreshMediaSessionUi(it) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE, Player.STATE_ENDED -> {
                    if (!exoPlayer.playWhenReady) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    }
                }
                Player.STATE_READY, Player.STATE_BUFFERING -> {
                    mediaSession?.let { refreshMediaSessionUi(it) }
                }
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady && exoPlayer.playbackState == Player.STATE_ENDED) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        exoPlayer.addListener(playerListener)
        volumeNormalizationManager.attachPlayer(exoPlayer)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val initialLayout = buildInitialCustomLayout()

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(pendingIntent)
            .setCustomLayout(initialLayout)
            .setBitmapLoader(CoilBitmapLoader(this, serviceScope))
            .setCallback(mediaSessionCallback)
            .build()

        val notificationProvider = LocalOnlyMediaNotificationProvider(this).apply {
            setSmallIcon(com.devson.vedtune.R.drawable.ic_notification)
        }
        setMediaNotificationProvider(notificationProvider)

        // Restore playback state if needed
        serviceScope.launch {
            restorePlaybackState()
            mediaSession?.let { refreshMediaSessionUi(it, force = true) }
        }

        // Observe favorite changes to dynamically update notification heart button in real-time
        serviceScope.launch {
            repository.getFavoriteSongIdsFlow().collect { ids ->
                val oldIds = favoriteSongIds
                favoriteSongIds = ids
                val currentSongId = mediaSession?.player?.currentMediaItem?.mediaId?.toLongOrNull()
                if (currentSongId != null) {
                    val wasFav = oldIds.contains(currentSongId)
                    val isFav = ids.contains(currentSongId)
                    if (wasFav != isFav) {
                        mediaSession?.let { refreshMediaSessionUi(it) }
                    }
                }
            }
        }
    }

    private fun buildInitialCustomLayout(): List<CommandButton> {
        val likeButton = CommandButton.Builder()
            .setDisplayName("Favorite")
            .setIconResId(com.devson.vedtune.R.drawable.ic_favorite_border)
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_LIKE, Bundle.EMPTY))
            .build()

        val closeButton = CommandButton.Builder()
            .setDisplayName("Close")
            .setIconResId(com.devson.vedtune.R.drawable.ic_close)
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_CLOSE_PLAYER, Bundle.EMPTY))
            .build()

        return listOf(likeButton, closeButton)
    }

    private val mediaSessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val defaultResult = super.onConnect(session, controller)
            val customCommands = listOf(
                CUSTOM_COMMAND_CLOSE_PLAYER,
                CUSTOM_COMMAND_LIKE,
                "ACTION_CLOSE" // Retain compatibility for legacy callers
            ).map { SessionCommand(it, Bundle.EMPTY) }

            val sessionCommandsBuilder = defaultResult.availableSessionCommands.buildUpon()
            customCommands.forEach { sessionCommandsBuilder.add(it) }

            return MediaSession.ConnectionResult.accept(
                sessionCommandsBuilder.build(),
                defaultResult.availablePlayerCommands
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CUSTOM_COMMAND_LIKE -> {
                    val currentSongId = session.player.currentMediaItem?.mediaId?.toLongOrNull()
                    if (currentSongId != null) {
                        serviceScope.launch {
                            repository.toggleFavorite(currentSongId)
                            mediaSession?.let { refreshMediaSessionUi(it, force = true) }
                        }
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                CUSTOM_COMMAND_CLOSE_PLAYER, "ACTION_CLOSE" -> {
                    stopPlaybackAndUnload(killProcess = true)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_UNKNOWN))
        }
    }

    private fun buildMediaButtonPreferencesSignature(session: MediaSession): String {
        val player = session.player
        val songId = player.currentMediaItem?.mediaId?.toLongOrNull()
        val isFav = songId != null && favoriteSongIds.contains(songId)
        return "$songId|$isFav|${player.repeatMode}|${player.shuffleModeEnabled}"
    }

    private fun buildCustomLayout(session: MediaSession): List<CommandButton> {
        val player = session.player
        val songId = player.currentMediaItem?.mediaId?.toLongOrNull()
        val isFavorite = songId != null && favoriteSongIds.contains(songId)

        val likeButton = CommandButton.Builder()
            .setDisplayName(if (isFavorite) "Remove from Favorites" else "Add to Favorites")
            .setIconResId(
                if (isFavorite) com.devson.vedtune.R.drawable.ic_favorite_filled
                else com.devson.vedtune.R.drawable.ic_favorite_border
            )
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_LIKE, Bundle.EMPTY))
            .build()

        val closeButton = CommandButton.Builder()
            .setDisplayName("Close")
            .setIconResId(com.devson.vedtune.R.drawable.ic_close)
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_CLOSE_PLAYER, Bundle.EMPTY))
            .build()

        return listOf(likeButton, closeButton)
    }

    private fun refreshMediaSessionUi(session: MediaSession, force: Boolean = false) {
        val pendingSignature = buildMediaButtonPreferencesSignature(session)
        if (!force && pendingSignature == lastAppliedMediaButtonSignature) {
            return
        }

        mediaSessionButtonRefreshJob?.cancel()
        mediaSessionButtonRefreshJob = serviceScope.launch {
            if (mediaSession !== session) return@launch
            val buttons = buildCustomLayout(session)
            session.setCustomLayout(buttons)
            lastAppliedMediaButtonSignature = pendingSignature
        }
    }

    private fun stopPlaybackAndUnload(killProcess: Boolean = false) {
        mediaSessionButtonRefreshJob?.cancel()
        serviceScope.cancel()
        runCatching {
            exoPlayer.pause()
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.cancelAll()
        stopSelf()
        if (killProcess) {
            Process.killProcess(Process.myPid())
        }
    }

    private suspend fun restorePlaybackState() {
        if (exoPlayer.mediaItemCount == 0) {
            val savedQueue = repository.getQueue()
            if (savedQueue.isNotEmpty()) {
                val preferences = dataStore.data.first()
                val savedSongId = preferences[KEY_CURRENT_SONG_ID]
                val savedPosition = preferences[KEY_PLAYBACK_POSITION] ?: 0L
                val savedRepeatMode = preferences[KEY_REPEAT_MODE] ?: Player.REPEAT_MODE_OFF

                val mediaItems = savedQueue.map { s ->
                    MediaItem.Builder()
                        .setMediaId(s.id.toString())
                        .setUri(
                            ContentUris.withAppendedId(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                s.id
                            )
                        )
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(s.title)
                                .setArtist(s.artist)
                                .setAlbumTitle(s.album)
                                .setArtworkUri(
                                    ContentUris.withAppendedId(
                                        Uri.parse("content://media/external/audio/albumart"),
                                        s.albumId
                                    )
                                )
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
                exoPlayer.shuffleModeEnabled = false
                exoPlayer.prepare()
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val isActivelyPlaying = exoPlayer.playWhenReady &&
            exoPlayer.playbackState != Player.STATE_IDLE &&
            exoPlayer.playbackState != Player.STATE_ENDED

        // If not actively playing when removed from Recents, dismiss notification, clean up, and kill process
        if (!isActivelyPlaying) {
            stopPlaybackAndUnload(killProcess = true)
        }
        // If actively playing, playback continues smoothly in background
    }

    override fun onDestroy() {
        serviceScope.cancel()
        exoPlayer.removeListener(playerListener)
        volumeNormalizationManager.release()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
