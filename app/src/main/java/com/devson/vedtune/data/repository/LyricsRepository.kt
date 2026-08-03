package com.devson.vedtune.data.repository

import com.devson.vedtune.data.remote.api.LrcLibApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LyricsRepository(
    private val api: LrcLibApi
) {

    suspend fun fetchLyricsForSong(
        trackName: String,
        artistName: String,
        albumName: String? = null,
        durationMillis: Long? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val durationSeconds = durationMillis?.div(1000L)

            val response = api.getLyrics(
                trackName = trackName,
                artistName = artistName,
                albumName = albumName,
                duration = durationSeconds
            )

            if (response.code() == 429) {
                return@withContext Result.failure(Exception("Rate limit exceeded. Try again later."))
            }

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch lyrics. HTTP status: ${response.code()}"))
            }

            val body = response.body()
                ?: return@withContext Result.failure(Exception("Empty response body received from server."))

            val syncedLyrics = body.syncedLyrics
            val plainLyrics = body.plainLyrics

            when {
                !syncedLyrics.isNullOrBlank() -> {
                    Result.success(syncedLyrics)
                }
                !plainLyrics.isNullOrBlank() -> {
                    Result.success(plainLyrics)
                }
                body.instrumental -> {
                    Result.failure(Exception("This is an instrumental track."))
                }
                else -> {
                    Result.failure(Exception("No lyrics available for this track."))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
