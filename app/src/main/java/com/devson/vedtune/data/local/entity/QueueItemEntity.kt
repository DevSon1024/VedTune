package com.devson.vedtune.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "queue_items",
    indices = [
        Index(value = ["orderIndex"]),
        Index(value = ["songId"])
    ]
)
data class QueueItemEntity(
    @PrimaryKey(autoGenerate = true) val queueItemId: Long = 0L,
    val songId: Long,
    val orderIndex: Int
)
