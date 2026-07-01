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

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "vedtune_database"
        )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("INSERT OR IGNORE INTO playlists (id, name, createdAt) VALUES (${Playlist.FAVORITES_PLAYLIST_ID}, '${Playlist.FAVORITES_PLAYLIST_NAME}', ${System.currentTimeMillis()})")
            }
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL("INSERT OR IGNORE INTO playlists (id, name, createdAt) VALUES (${Playlist.FAVORITES_PLAYLIST_ID}, '${Playlist.FAVORITES_PLAYLIST_NAME}', ${System.currentTimeMillis()})")
            }
        })
        .addMigrations(migration3to4)
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
