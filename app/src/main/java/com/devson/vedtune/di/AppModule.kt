package com.devson.vedtune.di

import com.devson.vedtune.data.local.dao.SongDao
import com.devson.vedtune.data.local.dao.QueueDao
import com.devson.vedtune.data.local.dao.PlaylistDao
import com.devson.vedtune.data.repository.MediaRepositoryImpl
import com.devson.vedtune.data.sync.MediaSyncEngine
import com.devson.vedtune.domain.repository.MediaRepository
import com.devson.vedtune.domain.repository.SettingsRepository
import com.devson.vedtune.data.repository.SettingsRepositoryImpl
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @Singleton
    fun provideMediaRepository(
        songDao: SongDao,
        queueDao: QueueDao,
        playlistDao: PlaylistDao,
        syncEngine: MediaSyncEngine
    ): MediaRepository {
        return MediaRepositoryImpl(songDao, queueDao, playlistDao, syncEngine)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        dataStore: DataStore<Preferences>,
        queueDao: QueueDao
    ): SettingsRepository {
        return SettingsRepositoryImpl(dataStore, queueDao)
    }
}
