package com.devson.vedtune.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.devson.vedtune.data.local.AppDatabase
import com.devson.vedtune.data.local.dao.SongDao
import com.devson.vedtune.data.local.dao.QueueDao
import com.devson.vedtune.data.local.dao.PlaylistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.devson.vedtune.domain.model.Playlist

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        val migration3to4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("PRAGMA table_info(songs)")
                var hasPlayCount = false
                var hasLastPlayed = false
                while (cursor.moveToNext()) {
                    val nameIndex = cursor.getColumnIndex("name")
                    if (nameIndex != -1) {
                        val name = cursor.getString(nameIndex)
                        if (name == "playCount") hasPlayCount = true
                        if (name == "lastPlayed") hasLastPlayed = true
                    }
                }
                cursor.close()
                
                if (!hasPlayCount) {
                    db.execSQL("ALTER TABLE songs ADD COLUMN playCount INTEGER NOT NULL DEFAULT 0")
                }
                if (!hasLastPlayed) {
                    db.execSQL("ALTER TABLE songs ADD COLUMN lastPlayed INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        val migration4to5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_artist ON songs(artist)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_album ON songs(album)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_albumId ON songs(albumId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_isFavorite ON songs(isFavorite)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_title ON songs(title)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_dateAdded ON songs(dateAdded)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_playCount ON songs(playCount)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_queue_items_orderIndex ON queue_items(orderIndex)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_queue_items_songId ON queue_items(songId)")
            }
        }

        val migration5to6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS queues (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        orderIndex INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                db.execSQL("INSERT OR IGNORE INTO queues (id, name, orderIndex, createdAt) VALUES (1, 'Default Queue', 0, ${System.currentTimeMillis()})")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS queue_items_new (
                        queueItemId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        queueId INTEGER NOT NULL,
                        songId INTEGER NOT NULL,
                        orderIndex INTEGER NOT NULL,
                        FOREIGN KEY (queueId) REFERENCES queues(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                try {
                    db.execSQL("""
                        INSERT INTO queue_items_new (queueItemId, queueId, songId, orderIndex)
                        SELECT queueItemId, 1, songId, orderIndex FROM queue_items
                    """.trimIndent())
                    db.execSQL("DROP TABLE queue_items")
                } catch (e: Exception) {
                    // In case queue_items table didn't exist or had schema mismatch
                }

                db.execSQL("ALTER TABLE queue_items_new RENAME TO queue_items")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_queue_items_queueId_orderIndex ON queue_items(queueId, orderIndex)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_queue_items_queueId ON queue_items(queueId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_queue_items_songId ON queue_items(songId)")
            }
        }

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "vedtune_database"
        )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("INSERT OR IGNORE INTO playlists (id, name, createdAt) VALUES (${Playlist.FAVORITES_PLAYLIST_ID}, '${Playlist.FAVORITES_PLAYLIST_NAME}', ${System.currentTimeMillis()})")
                db.execSQL("INSERT OR IGNORE INTO queues (id, name, orderIndex, createdAt) VALUES (1, 'All Songs', 0, ${System.currentTimeMillis()})")
            }
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL("INSERT OR IGNORE INTO playlists (id, name, createdAt) VALUES (${Playlist.FAVORITES_PLAYLIST_ID}, '${Playlist.FAVORITES_PLAYLIST_NAME}', ${System.currentTimeMillis()})")
                db.execSQL("INSERT OR IGNORE INTO queues (id, name, orderIndex, createdAt) VALUES (1, 'All Songs', 0, ${System.currentTimeMillis()})")
                db.execSQL("UPDATE queues SET name = 'All Songs' WHERE id = 1 AND name = 'Default Queue'")
            }
        })
        .addMigrations(migration3to4, migration4to5, migration5to6)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideSongDao(database: AppDatabase): SongDao {
        return database.songDao()
    }

    @Provides
    @Singleton
    fun provideQueueDao(database: AppDatabase): QueueDao {
        return database.queueDao()
    }

    @Provides
    @Singleton
    fun providePlaylistDao(database: AppDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("vedtune_settings") }
        )
    }
}
