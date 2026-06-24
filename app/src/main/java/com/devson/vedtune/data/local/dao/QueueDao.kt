package com.devson.vedtune.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.devson.vedtune.data.local.entity.QueueItemEntity

@Dao
interface QueueDao {

    @Query("SELECT * FROM queue_items ORDER BY orderIndex ASC")
    suspend fun getQueueItems(): List<QueueItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItems(items: List<QueueItemEntity>)

    @Query("DELETE FROM queue_items")
    suspend fun clearQueue()

    @Transaction
    suspend fun updateQueue(items: List<QueueItemEntity>) {
        clearQueue()
        insertQueueItems(items)
    }
}
