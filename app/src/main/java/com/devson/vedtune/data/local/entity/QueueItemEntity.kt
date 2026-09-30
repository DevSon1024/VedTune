package com.devson.vedtune.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "queue_items",
    indices = [
        Index(value = ["queueId", "orderIndex"]),
        Index(value = ["queueId"]),
        Index(value = ["songId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = QueueEntity::class,
            parentColumns = ["id"],
            childColumns = ["queueId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class QueueItemEntity(
    @PrimaryKey(autoGenerate = true) val queueItemId: Long = 0L,
    val queueId: Long = 1L,
    val songId: Long,
    val orderIndex: Int
)
