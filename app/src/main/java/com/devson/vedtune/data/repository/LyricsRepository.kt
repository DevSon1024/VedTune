package com.devson.vedtune.data.repository

import com.devson.vedtune.data.remote.api.LrcLibApi
import com.devson.vedtune.data.remote.model.LrcLibResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class LrcSearchField(val label: String) {
    TRACK_NAME("Track Name"),
    ARTIST_NAME("Artist Name"),
    ALBUM_NAME("Album Name")
}

@Singleton
class LyricsRepository @Inject constructor(
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

    suspend fun searchLyrics(
        field: LrcSearchField,
        query: String
    ): Result<List<LrcLibResponse>> = withContext(Dispatchers.IO) {
        try {
            val trimmedQuery = query.trim()
            if (trimmedQuery.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            val trackParam = if (field == LrcSearchField.TRACK_NAME) trimmedQuery else null
            val artistParam = if (field == LrcSearchField.ARTIST_NAME) trimmedQuery else null
            val albumParam = if (field == LrcSearchField.ALBUM_NAME) trimmedQuery else null

            val response = api.searchLyrics(
                query = null,
                trackName = trackParam,
                artistName = artistParam,
                albumName = albumParam
            )

            if (response.code() == 429) {
                return@withContext Result.failure(Exception("Rate limit exceeded. Try again later."))
            }

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Search failed with status code ${response.code()}."))
            }

            val body = response.body() ?: emptyList()
            Result.success(body)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
