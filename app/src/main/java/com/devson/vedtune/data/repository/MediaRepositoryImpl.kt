package com.devson.vedtune.data.repository

import com.devson.vedtune.data.local.dao.SongDao
import com.devson.vedtune.data.local.dao.QueueDao
import com.devson.vedtune.data.local.dao.QueueWithCount
import com.devson.vedtune.data.local.entity.QueueEntity
import com.devson.vedtune.data.local.entity.QueueItemEntity
import com.devson.vedtune.domain.model.QueueInfo
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import com.devson.vedtune.data.local.dao.PlaylistDao
import com.devson.vedtune.data.local.entity.PlaylistEntity
import com.devson.vedtune.data.local.entity.PlaylistSongCrossRef
import com.devson.vedtune.domain.model.Playlist
import android.content.Context
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val queueDao: QueueDao,
    private val playlistDao: PlaylistDao,
    private val syncEngine: MediaSyncEngine,
    @ApplicationContext private val context: Context
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
        songDao.incrementPlayCount(songId = id, timestamp = System.currentTimeMillis())
    }

    override suspend fun clearPlaybackHistory(songId: Long) {
        songDao.clearPlaybackHistory(songId)
    }

    override suspend fun removeSongFromHistory(songId: Long) {
        songDao.removeSongFromHistory(songId)
    }

    override suspend fun clearAllPlaybackHistory() {
        songDao.clearAllPlaybackHistory()
    }

    override fun getRecentlyPlayedSongs(): Flow<List<Song>> {
        return songDao.getRecentlyPlayedSongs().map { entities ->
            entities.map { it.toSong() }
        }
    }

    override suspend fun synchronizeLibrary() {
        syncEngine.performSync()
    }

    override fun getAllQueues(): Flow<List<QueueInfo>> {
        return kotlinx.coroutines.flow.combine(
            queueDao.getAllQueuesWithCountFlow(),
            songDao.getAllSongs()
        ) { list, allSongs ->
            list.map { entity ->
                val count = if (entity.id == QueueInfo.DEFAULT_QUEUE_ID && entity.songCount == 0) {
                    allSongs.size
                } else {
                    entity.songCount
                }
                val displayName = if (entity.id == QueueInfo.DEFAULT_QUEUE_ID && (entity.name == "Default Queue" || entity.name.isBlank())) {
                    QueueInfo.DEFAULT_QUEUE_NAME
                } else {
                    entity.name
                }
                QueueInfo(
                    id = entity.id,
                    name = displayName,
                    orderIndex = entity.orderIndex,
                    songCount = count,
                    createdAt = entity.createdAt
                )
            }
        }
    }

    override fun getQueueSongs(queueId: Long): Flow<List<Song>> {
        return combine(
            queueDao.getQueueItemsFlow(queueId),
            songDao.getAllSongs()
        ) { items, allSongs ->
            if (items.isEmpty()) {
                if (queueId == QueueInfo.DEFAULT_QUEUE_ID) {
                    allSongs.map { it.toSong() }
                } else {
                    emptyList()
                }
            } else {
                val songsMap = allSongs.associateBy { it.id }
                items.mapNotNull { item -> songsMap[item.songId]?.toSong() }
            }
        }
    }

    override suspend fun getQueueSongsSync(queueId: Long): List<Song> {
        val items = queueDao.getQueueItems(queueId)
        if (items.isEmpty()) {
            if (queueId == QueueInfo.DEFAULT_QUEUE_ID) {
                return songDao.getAllSongsList().map { it.toSong() }
            }
            return emptyList()
        }
        val songIds = items.map { it.songId }
        val songEntities = songDao.getSongsByIds(songIds)
        val songsMap = songEntities.associateBy { it.id }
        return items.mapNotNull { item -> songsMap[item.songId]?.toSong() }
    }

    override suspend fun createQueue(name: String): Long {
        val maxOrder = queueDao.getMaxQueueOrderIndex()
        val entity = QueueEntity(
            name = name.ifBlank { "New Queue" },
            orderIndex = maxOrder + 1,
            createdAt = System.currentTimeMillis()
        )
        return queueDao.insertQueue(entity)
    }

    override suspend fun renameQueue(queueId: Long, newName: String) {
        queueDao.renameQueue(queueId, newName.trim().ifBlank { "Queue $queueId" })
    }

    override suspend fun deleteQueue(queueId: Long) {
        queueDao.deleteQueue(queueId)
    }

    override suspend fun removeAllOtherQueues(keepQueueId: Long) {
        queueDao.deleteAllQueuesExcept(keepQueueId)
    }

    override suspend fun reorderQueues(orderedQueueIds: List<Long>) {
        queueDao.reorderQueues(orderedQueueIds)
    }

    override suspend fun saveQueueSongs(queueId: Long, songs: List<Song>) {
        val entities = songs.mapIndexed { index, song ->
            QueueItemEntity(
                queueId = queueId,
                songId = song.id,
                orderIndex = index
            )
        }
        queueDao.updateQueueItemsForQueue(queueId, entities)
    }

    override suspend fun addSongsToQueue(queueId: Long, songIds: List<Long>, atBeginning: Boolean) {
        if (songIds.isEmpty()) return
        val currentItems = queueDao.getQueueItems(queueId)
        val currentSongIds = currentItems.map { it.songId }.toMutableList()
        if (atBeginning) {
            currentSongIds.addAll(0, songIds)
        } else {
            currentSongIds.addAll(songIds)
        }
        val newEntities = currentSongIds.mapIndexed { index, id ->
            QueueItemEntity(
                queueId = queueId,
                songId = id,
                orderIndex = index
            )
        }
        queueDao.updateQueueItemsForQueue(queueId, newEntities)
    }

    override suspend fun removeSongFromQueue(queueId: Long, songId: Long) {
        val currentItems = queueDao.getQueueItems(queueId)
        val updatedSongIds = currentItems.map { it.songId }.toMutableList()
        val removeIndex = updatedSongIds.indexOfFirst { it == songId }
        if (removeIndex != -1) {
            updatedSongIds.removeAt(removeIndex)
            val newEntities = updatedSongIds.mapIndexed { index, id ->
                QueueItemEntity(
                    queueId = queueId,
                    songId = id,
                    orderIndex = index
                )
            }
            queueDao.updateQueueItemsForQueue(queueId, newEntities)
        }
    }

    override suspend fun clearQueueById(queueId: Long) {
        queueDao.clearQueue(queueId)
    }

    override suspend fun getQueue(): List<Song> {
        return getQueueSongsSync(1L)
    }

    override suspend fun saveQueue(songs: List<Song>) {
        saveQueueSongs(1L, songs)
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

    override fun isSongInPlaylist(playlistId: Long, songId: Long): Flow<Boolean> {
        return playlistDao.isSongInPlaylist(playlistId, songId)
    }

    override suspend fun toggleFavorite(songId: Long): Boolean {
        val isFav = playlistDao.isSongInPlaylist(Playlist.FAVORITES_PLAYLIST_ID, songId).first()
        val nextFavState = !isFav
        if (nextFavState) {
            playlistDao.insertPlaylistSong(PlaylistSongCrossRef(Playlist.FAVORITES_PLAYLIST_ID, songId))
            songDao.updateFavoriteStatus(songId, true)
        } else {
            playlistDao.deletePlaylistSong(Playlist.FAVORITES_PLAYLIST_ID, songId)
            songDao.updateFavoriteStatus(songId, false)
        }
        return nextFavState
    }

    override fun isFavoriteFlow(songId: Long): Flow<Boolean> {
        return playlistDao.isSongInPlaylist(Playlist.FAVORITES_PLAYLIST_ID, songId)
    }

    override fun getFavoriteSongIdsFlow(): Flow<Set<Long>> {
        return playlistDao.getPlaylistWithSongs(Playlist.FAVORITES_PLAYLIST_ID).map { playlistWithSongs ->
            playlistWithSongs?.songs?.map { it.id }?.toSet() ?: emptySet()
        }
    }

    override suspend fun getSongsByIds(ids: List<Long>): List<Song> {
        return songDao.getSongsByIds(ids).map { it.toSong() }
    }

    override suspend fun getUniqueArtists(): List<String> {
        return songDao.getUniqueArtists()
    }

    override suspend fun getUniqueAlbums(): List<String> {
        return songDao.getUniqueAlbums()
    }

    override suspend fun getUniqueComposers(): List<String> {
        val composers = mutableSetOf<String>()
        val projection = arrayOf(MediaStore.Audio.Media.COMPOSER)
        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Audio.Media.COMPOSER} IS NOT NULL AND ${MediaStore.Audio.Media.COMPOSER} != ''",
                null,
                null
            )?.use { cursor ->
                val col = cursor.getColumnIndex(MediaStore.Audio.Media.COMPOSER)
                if (col != -1) {
                    while (cursor.moveToNext()) {
                        cursor.getString(col)?.let { composers.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return composers.toList().sorted()
    }

    override suspend fun getUniqueGenres(): List<String> {
        val genres = mutableSetOf<String>()
        val projection = arrayOf(MediaStore.Audio.Genres.NAME)
        try {
            context.contentResolver.query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val col = cursor.getColumnIndex(MediaStore.Audio.Genres.NAME)
                if (col != -1) {
                    while (cursor.moveToNext()) {
                        cursor.getString(col)?.let { genres.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return genres.toList().sorted()
    }

    override fun getSongsByGenre(genre: String): Flow<List<Song>> = flow {
        val songIds = mutableListOf<Long>()
        try {
            var genreId: Long? = null
            context.contentResolver.query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Genres._ID),
                "${MediaStore.Audio.Genres.NAME} = ?",
                arrayOf(genre),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    genreId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID))
                }
            }

            genreId?.let { id ->
                val uri = MediaStore.Audio.Genres.Members.getContentUri("external", id)
                val projection = arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID)
                context.contentResolver.query(
                    uri,
                    projection,
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val colIdx = cursor.getColumnIndex(MediaStore.Audio.Genres.Members.AUDIO_ID)
                    if (colIdx != -1) {
                        while (cursor.moveToNext()) {
                            songIds.add(cursor.getLong(colIdx))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (songIds.isNotEmpty()) {
            val songEntities = songDao.getSongsByIds(songIds)
            emit(songEntities.map { it.toSong() })
        } else {
            emit(emptyList())
        }
    }
}
