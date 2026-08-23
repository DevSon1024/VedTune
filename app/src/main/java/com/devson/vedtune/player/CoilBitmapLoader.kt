package com.devson.vedtune.player

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class CoilBitmapLoader(
    private val context: Context,
    private val scope: CoroutineScope
) : BitmapLoader {

    companion object {
        private const val MAX_NOTIFICATION_ARTWORK_SIZE_PX = 1024
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        return loadBitmapInternal(uri)
    }

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        return loadBitmapInternal(data)
    }

    private fun loadBitmapInternal(data: Any): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()

        scope.launch {
            try {
                val request = ImageRequest.Builder(context)
                    .data(data)
                    .size(MAX_NOTIFICATION_ARTWORK_SIZE_PX, MAX_NOTIFICATION_ARTWORK_SIZE_PX)
                    .precision(Precision.INEXACT)
                    .allowHardware(false) // Hardware bitmaps cannot be transferred over IPC / MediaSession
                    .memoryCachePolicy(CachePolicy.DISABLED) // Prevent Coil from recycling bitmap while MediaSession uses it
                    .build()

                val result = context.imageLoader.execute(request)
                val drawable = result.drawable

                if (drawable != null) {
                    val bitmap = drawable.toBitmap()
                    future.set(bitmap)
                } else {
                    future.setException(IllegalStateException("Failed to load drawable for artwork"))
                }
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    override fun supportsMimeType(mimeType: String): Boolean {
        return true
    }
}
