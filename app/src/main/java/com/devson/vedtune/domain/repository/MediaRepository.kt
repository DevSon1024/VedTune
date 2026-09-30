package com.devson.vedtune.domain.repository

import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.domain.model.Album
import com.devson.vedtune.domain.model.Artist
import com.devson.vedtune.domain.model.Playlist
import com.devson.vedtune.domain.model.QueueInfo
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getAllSongs(): Flow<List<Song>>
    suspend fun getSongById(id: Long): Song?
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)
    suspend fun incrementPlayCount(id: Long)
    suspend fun clearPlaybackHistory(songId: Long)
    suspend fun removeSongFromHistory(songId: Long)
    suspend fun clearAllPlaybackHistory()
    fun getRecentlyPlayedSongs(): Flow<List<Song>>
    suspend fun synchronizeLibrary()

    // Multi-Queue operations
    fun getAllQueues(): Flow<List<QueueInfo>>
    fun getQueueSongs(queueId: Long): Flow<List<Song>>
    suspend fun getQueueSongsSync(queueId: Long): List<Song>
    suspend fun createQueue(name: String): Long
    suspend fun renameQueue(queueId: Long, newName: String)
    suspend fun deleteQueue(queueId: Long)
    suspend fun removeAllOtherQueues(keepQueueId: Long)
    suspend fun reorderQueues(orderedQueueIds: List<Long>)
    suspend fun saveQueueSongs(queueId: Long, songs: List<Song>)
    suspend fun addSongsToQueue(queueId: Long, songIds: List<Long>, atBeginning: Boolean = false)
    suspend fun removeSongFromQueue(queueId: Long, songId: Long)
    suspend fun clearQueueById(queueId: Long)

    // Legacy Queue operations (defaults to active/default queue)
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
    suspend fun updateSong(song: Song)
    fun isSongInPlaylist(playlistId: Long, songId: Long): Flow<Boolean>
    suspend fun getSongsByIds(ids: List<Long>): List<Song>
    suspend fun getUniqueArtists(): List<String>
    suspend fun getUniqueAlbums(): List<String>
    suspend fun getUniqueComposers(): List<String>
    suspend fun getUniqueGenres(): List<String>
    suspend fun toggleFavorite(songId: Long): Boolean
    fun isFavoriteFlow(songId: Long): Flow<Boolean>
    fun getFavoriteSongIdsFlow(): Flow<Set<Long>>
    fun getSongsByGenre(genre: String): Flow<List<Song>>
}
