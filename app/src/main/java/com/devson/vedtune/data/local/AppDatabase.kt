package com.devson.vedtune.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.devson.vedtune.data.local.dao.SongDao
import com.devson.vedtune.data.local.entity.SongEntity

@Database(
    entities = [SongEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
}
