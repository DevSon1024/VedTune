package com.devson.vedtune.core

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.devson.vedtune.data.sync.MediaStoreObserver
import com.devson.vedtune.ui.components.ArtworkCache
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class VedTuneApp : Application(), ImageLoaderFactory {

    @Inject
    lateinit var mediaStoreObserver: MediaStoreObserver

    override fun onCreate() {
        super.onCreate()
        mediaStoreObserver.register()
        CoroutineScope(Dispatchers.IO).launch {
            ArtworkCache.init(applicationContext)
        }
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
            .crossfade(false)
            .allowHardware(true)
            .allowRgb565(true)
            .build()
    }
}
