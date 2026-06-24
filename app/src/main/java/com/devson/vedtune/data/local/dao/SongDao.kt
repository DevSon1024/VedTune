package com.devson.vedtune.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.devson.vedtune.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: Long): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteSongs(ids: List<Long>)

    @Query("SELECT id, dateModified FROM songs")
    suspend fun getSongIdAndModifiedMap(): List<SongIdAndModified>

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayed = :timestamp WHERE id = :id")
    suspend fun incrementPlayCount(id: Long, timestamp: Long)

    @Transaction
    suspend fun syncMediaStore(
        toInsert: List<SongEntity>,
        toDeleteIds: List<Long>
    ) {
        if (toDeleteIds.isNotEmpty()) {
            deleteSongs(toDeleteIds)
        }
        if (toInsert.isNotEmpty()) {
            insertSongs(toInsert)
        }
    }
}

data class SongIdAndModified(
    val id: Long,
    val dateModified: Long
)
