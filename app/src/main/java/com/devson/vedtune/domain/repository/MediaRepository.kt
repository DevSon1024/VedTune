package com.devson.vedtune.domain.repository

import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.domain.model.Album
import com.devson.vedtune.domain.model.Artist
import com.devson.vedtune.domain.model.Playlist
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
    fun getAllArtists(): Flow<List<Artist>>
    fun getSongsByArtist(artist: String): Flow<List<Song>>
    fun getAllPlaylists(): Flow<List<Playlist>>
    fun getSongsByPlaylistId(playlistId: Long): Flow<List<Song>>
    suspend fun createPlaylist(name: String): Long
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long)
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)
    suspend fun deleteSong(songId: Long)
}
