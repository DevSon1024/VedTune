package com.devson.vedtune.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.devson.vedtune.data.local.dao.SongDao
import com.devson.vedtune.data.local.dao.QueueDao
import com.devson.vedtune.data.local.dao.PlaylistDao
import com.devson.vedtune.data.local.entity.SongEntity
import com.devson.vedtune.data.local.entity.QueueItemEntity
import com.devson.vedtune.data.local.entity.PlaylistEntity
import com.devson.vedtune.data.local.entity.PlaylistSongCrossRef

@Database(
    entities = [
        SongEntity::class,
        QueueItemEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun queueDao(): QueueDao
    abstract fun playlistDao(): PlaylistDao
}
