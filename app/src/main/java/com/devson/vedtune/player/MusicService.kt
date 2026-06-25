package com.devson.vedtune.player

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val ACTION_CLOSE = "com.devson.vedtune.ACTION_CLOSE"

@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject
    lateinit var exoPlayer: ExoPlayer

    private var mediaSession: MediaSession? = null
    private var isPlayerReleased = false

    override fun onCreate() {
        super.onCreate()

        // Setup PendingIntent to launch MainActivity when clicking the playback notification
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val callback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val connectionResult = super.onConnect(session, controller)
                val sessionCommands = connectionResult.availableSessionCommands.buildUpon()
                    .add(SessionCommand(ACTION_CLOSE, Bundle.EMPTY))
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
                if (customCommand.customAction == ACTION_CLOSE) {
                    performCleanup(killProcess = true)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                return super.onCustomCommand(session, controller, customCommand, args)
            }
        }

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(pendingIntent)
            .setCallback(callback)
            .build()

        val closeButton = CommandButton.Builder()
            .setDisplayName("Close")
            .setSessionCommand(SessionCommand(ACTION_CLOSE, Bundle.EMPTY))
            .setIconResId(android.R.drawable.ic_menu_close_clear_cancel)
            .build()

        mediaSession?.setCustomLayout(listOf(closeButton))
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        performCleanup(killProcess = false)
    }

    override fun onDestroy() {
        performCleanup(killProcess = false)
        super.onDestroy()
    }

    private fun performCleanup(killProcess: Boolean) {
        // Stop playback and clear media items
        try {
            if (!isPlayerReleased) {
                if (exoPlayer.playbackState != Player.STATE_IDLE) {
                    exoPlayer.stop()
                    exoPlayer.clearMediaItems()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Explicitly release MediaSession and set to null to notify the OS to remove controls
        mediaSession?.let { session ->
            try {
                session.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaSession = null
        }

        // Remove foreground notification forcefully
        try {
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Release player resource safely
        releasePlayerSafely()

        // Terminate the service
        stopSelf()

        if (killProcess) {
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    private fun releasePlayerSafely() {
        if (!isPlayerReleased) {
            try {
                exoPlayer.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isPlayerReleased = true
        }
    }
}
