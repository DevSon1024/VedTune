package com.devson.vedtune.core

import android.app.Application
import com.devson.vedtune.data.sync.MediaStoreObserver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VedTuneApp : Application() {

    @Inject
    lateinit var mediaStoreObserver: MediaStoreObserver

    override fun onCreate() {
        super.onCreate()
        mediaStoreObserver.register()
    }
}
