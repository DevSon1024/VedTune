package com.devson.vedtune.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "queue_items")
data class QueueItemEntity(
    @PrimaryKey(autoGenerate = true) val queueItemId: Long = 0L,
    val songId: Long,
    val orderIndex: Int
)
