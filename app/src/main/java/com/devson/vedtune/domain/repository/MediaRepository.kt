package com.devson.vedtune.domain.repository

import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.domain.model.Album
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getAllSongs(): Flow<List<Song>>
    suspend fun getSongById(id: Long): Song?
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)
    suspend fun incrementPlayCount(id: Long)
    suspend fun synchronizeLibrary()
    suspend fun getQueue(): List<Song>
    suspend fun saveQueue(songs: List<Song>)
    fun getAllAlbums(): Flow<List<Album>>
    fun getSongsByAlbumId(albumId: Long): Flow<List<Song>>
}
