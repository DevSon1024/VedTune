package com.devson.vedtune.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["artist"]),
        Index(value = ["album"]),
        Index(value = ["albumId"]),
        Index(value = ["isFavorite"]),
        Index(value = ["title"]),
        Index(value = ["dateAdded"]),
        Index(value = ["playCount"])
    ]
)
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val track: Int,
    val year: Int,
    val dateAdded: Long,
    val dateModified: Long,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayed: Long = 0L
)
