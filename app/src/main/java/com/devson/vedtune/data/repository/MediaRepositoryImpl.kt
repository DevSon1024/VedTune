package com.devson.vedtune.data.repository

import com.devson.vedtune.data.local.dao.SongDao
import com.devson.vedtune.data.mapper.toSong
import com.devson.vedtune.data.sync.MediaSyncEngine
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
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
}
