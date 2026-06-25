package com.devson.vedtune.data.repository

import com.devson.vedtune.data.local.dao.SongDao
import com.devson.vedtune.data.local.dao.QueueDao
import com.devson.vedtune.data.local.entity.QueueItemEntity
import com.devson.vedtune.data.mapper.toSong
import com.devson.vedtune.data.mapper.toEntity
import com.devson.vedtune.data.sync.MediaSyncEngine
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.domain.model.Album
import com.devson.vedtune.domain.model.Artist
import com.devson.vedtune.data.local.entity.ArtistEntity
import com.devson.vedtune.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.devson.vedtune.data.local.dao.PlaylistDao
import com.devson.vedtune.data.local.entity.PlaylistEntity
import com.devson.vedtune.data.local.entity.PlaylistSongCrossRef
import com.devson.vedtune.domain.model.Playlist
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val queueDao: QueueDao,
    private val playlistDao: PlaylistDao,
    private val syncEngine: MediaSyncEngine
) : MediaRepository {

    override fun getAllSongs(): Flow<List<Song>> {
        return songDao.getAllSongs().map { entities ->
            entities.map { it.toSong() }
        }
    }

    override suspend fun getSongById(id: Long): Song? {
        return songDao.getSongById(id)?.toSong()
    }

    override suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean) {
        songDao.updateFavoriteStatus(id, isFavorite)
    }

    override suspend fun incrementPlayCount(id: Long) {
        songDao.incrementPlayCount(id, System.currentTimeMillis())
    }

    override suspend fun synchronizeLibrary() {
        syncEngine.performSync()
    }

    override suspend fun getQueue(): List<Song> {
        val queueItems = queueDao.getQueueItems()
        if (queueItems.isEmpty()) return emptyList()
        val songIds = queueItems.map { it.songId }
        val songEntities = songDao.getSongsByIds(songIds)
        val songsMap = songEntities.associateBy { it.id }
        return queueItems.mapNotNull { item ->
            songsMap[item.songId]?.toSong()
        }
    }

    override suspend fun saveQueue(songs: List<Song>) {
        val entities = songs.mapIndexed { index, song ->
            QueueItemEntity(
                songId = song.id,
                orderIndex = index
            )
        }
        queueDao.updateQueue(entities)
    }

    override fun getAllAlbums(): Flow<List<Album>> {
        return songDao.getAllAlbums().map { entities ->
            entities.map { entity ->
                Album(
                    id = entity.albumId,
                    title = entity.album,
                    artist = entity.artist,
                    songCount = entity.songCount
                )
            }
        }
    }

    override fun getSongsByAlbumId(albumId: Long): Flow<List<Song>> {
        return songDao.getSongsByAlbumId(albumId).map { entities ->
            entities.map { it.toSong() }
        }
    }

    override fun getAllArtists(): Flow<List<Artist>> {
        return songDao.getAllArtists().map { entities ->
            entities.map { entity ->
                Artist(
                    name = entity.artist,
                    songCount = entity.songCount,
                    albumCount = entity.albumCount
                )
            }
        }
    }

    override fun getSongsByArtist(artist: String): Flow<List<Song>> {
        return songDao.getSongsByArtist(artist).map { entities ->
            entities.map { it.toSong() }
        }
    }

    override fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylistsWithCount().map { entities ->
            entities.map { entity ->
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    songCount = entity.songCount,
                    createdAt = entity.createdAt
                )
            }
        }
    }

    override fun getSongsByPlaylistId(playlistId: Long): Flow<List<Song>> {
        return playlistDao.getPlaylistWithSongs(playlistId).map { relation ->
            relation?.songs?.map { it.toSong() } ?: emptyList()
        }
    }

    override suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylistById(playlistId)
    }

    override suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        playlistDao.insertPlaylistSong(PlaylistSongCrossRef(playlistId, songId))
    }

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.deletePlaylistSong(playlistId, songId)
    }

    override suspend fun deleteSong(songId: Long) {
        songDao.deleteSongs(listOf(songId))
    }

    override suspend fun updateSong(song: Song) {
        songDao.insertSongs(listOf(song.toEntity()))
    }
}
