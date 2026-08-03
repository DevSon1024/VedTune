package com.devson.vedtune.data.remote.api

import com.devson.vedtune.data.remote.model.LrcLibResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface LrcLibApi {
    @GET("api/get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("album_name") albumName: String? = null,
        @Query("duration") duration: Long? = null
    ): Response<LrcLibResponse>
}
