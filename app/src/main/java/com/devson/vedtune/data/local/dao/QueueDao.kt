package com.devson.vedtune.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.devson.vedtune.data.local.entity.QueueEntity
import com.devson.vedtune.data.local.entity.QueueItemEntity
import kotlinx.coroutines.flow.Flow

data class QueueWithCount(
    val id: Long,
    val name: String,
    val orderIndex: Int,
    val createdAt: Long,
    val songCount: Int
)

@Dao
interface QueueDao {

    @Query("""
        SELECT q.id, q.name, q.orderIndex, q.createdAt, COUNT(qi.songId) as songCount
        FROM queues q
        LEFT JOIN queue_items qi ON q.id = qi.queueId
        GROUP BY q.id
        ORDER BY q.orderIndex ASC, q.id ASC
    """)
    fun getAllQueuesWithCountFlow(): Flow<List<QueueWithCount>>

    @Query("SELECT * FROM queues WHERE id = :queueId LIMIT 1")
    suspend fun getQueueById(queueId: Long): QueueEntity?

    @Query("SELECT * FROM queues ORDER BY orderIndex ASC, id ASC")
    suspend fun getAllQueuesSync(): List<QueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueue(queue: QueueEntity): Long

    @Update
    suspend fun updateQueue(queue: QueueEntity)

    @Query("UPDATE queues SET name = :newName WHERE id = :queueId")
    suspend fun renameQueue(queueId: Long, newName: String)

    @Query("DELETE FROM queues WHERE id = :queueId")
    suspend fun deleteQueue(queueId: Long)

    @Query("DELETE FROM queues WHERE id != :keepQueueId AND id != 1")
    suspend fun deleteAllQueuesExcept(keepQueueId: Long)

    @Transaction
    suspend fun reorderQueues(orderedQueueIds: List<Long>) {
        orderedQueueIds.forEachIndexed { index, queueId ->
            updateQueueOrderIndex(queueId, index)
        }
    }

    @Query("UPDATE queues SET orderIndex = :newIndex WHERE id = :queueId")
    suspend fun updateQueueOrderIndex(queueId: Long, newIndex: Int)

    // Items
    @Query("SELECT * FROM queue_items WHERE queueId = :queueId ORDER BY orderIndex ASC")
    suspend fun getQueueItems(queueId: Long = 1L): List<QueueItemEntity>

    @Query("SELECT songId FROM queue_items WHERE queueId = :queueId ORDER BY orderIndex ASC")
    suspend fun getQueueSongIds(queueId: Long): List<Long>

    @Query("SELECT * FROM queue_items WHERE queueId = :queueId ORDER BY orderIndex ASC")
    fun getQueueItemsFlow(queueId: Long = 1L): Flow<List<QueueItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItems(items: List<QueueItemEntity>)

    @Query("DELETE FROM queue_items WHERE queueId = :queueId")
    suspend fun clearQueue(queueId: Long = 1L)

    @Query("DELETE FROM queue_items WHERE queueId = :queueId AND songId = :songId")
    suspend fun deleteSongFromQueue(queueId: Long, songId: Long)

    @Transaction
    suspend fun updateQueue(items: List<QueueItemEntity>) {
        val queueId = items.firstOrNull()?.queueId ?: 1L
        clearQueue(queueId)
        insertQueueItems(items)
    }

    @Transaction
    suspend fun updateQueueItemsForQueue(queueId: Long, items: List<QueueItemEntity>) {
        clearQueue(queueId)
        insertQueueItems(items)
    }

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM queue_items WHERE queueId = :queueId")
    suspend fun getMaxOrderIndex(queueId: Long): Int

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM queues")
    suspend fun getMaxQueueOrderIndex(): Int
}
