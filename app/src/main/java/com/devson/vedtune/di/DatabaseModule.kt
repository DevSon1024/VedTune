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
