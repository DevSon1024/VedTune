package com.devson.vedtune.core

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.devson.vedtune.data.sync.MediaStoreObserver
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class VedTuneApp : Application(), ImageLoaderFactory {

    @Inject
    lateinit var mediaStoreObserver: MediaStoreObserver

    override fun onCreate() {
        super.onCreate()
        mediaStoreObserver.register()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .allowRgb565(true)
            .build()
    }
}
