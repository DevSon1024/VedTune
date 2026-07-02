package com.devson.vedtune.ui.components

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.Transformation
import java.io.File

object ArtworkCache {
    private val lock = Any()
    private var isInitialized = false
    private val customAlbums = mutableSetOf<Long>()

    private fun initIfNeeded(context: Context) {
        synchronized(lock) {
            if (!isInitialized) {
                val dir = File(context.filesDir, "custom_artwork")
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.forEach { file ->
                        if (file.isFile && file.name.endsWith(".jpg")) {
                            file.name.removeSuffix(".jpg").toLongOrNull()?.let { albumId ->
                                customAlbums.add(albumId)
                            }
                        }
                    }
                }
                isInitialized = true
            }
        }
    }

    fun hasCustomArtwork(context: Context, albumId: Long): Boolean {
        initIfNeeded(context)
        synchronized(lock) {
            return customAlbums.contains(albumId)
        }
    }

    fun addCustomArtwork(albumId: Long) {
        synchronized(lock) {
            customAlbums.add(albumId)
        }
    }

    fun removeCustomArtwork(albumId: Long) {
        synchronized(lock) {
            customAlbums.remove(albumId)
        }
    }
}

class BlurTransformation(
    private val context: Context,
    private val radius: Float = 25f
) : Transformation {

    override val cacheKey: String = "blur_$radius"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        if (radius <= 0f) return input
        val safeRadius = radius.coerceIn(0.5f, 25f)
        val width = input.width
        val height = input.height
        val output = Bitmap.createBitmap(width, height, input.config ?: Bitmap.Config.ARGB_8888)

        var rs: android.renderscript.RenderScript? = null
        var inputAlloc: android.renderscript.Allocation? = null
        var outputAlloc: android.renderscript.Allocation? = null
        var blurScript: android.renderscript.ScriptIntrinsicBlur? = null

        try {
            rs = android.renderscript.RenderScript.create(context)
            inputAlloc = android.renderscript.Allocation.createFromBitmap(rs, input)
            outputAlloc = android.renderscript.Allocation.createFromBitmap(rs, output)
            blurScript = android.renderscript.ScriptIntrinsicBlur.create(rs, android.renderscript.Element.U8_4(rs))

            blurScript.setRadius(safeRadius)
            blurScript.setInput(inputAlloc)
            blurScript.forEach(outputAlloc)

            outputAlloc.copyTo(output)
        } catch (e: Exception) {
            e.printStackTrace()
            return input
        } finally {
            inputAlloc?.destroy()
            outputAlloc?.destroy()
            blurScript?.destroy()
            rs?.destroy()
        }

        return output
    }
}

@Composable
fun SongArtwork(
    albumId: Long,
    modifier: Modifier = Modifier,
    showArtwork: Boolean = true,
    lastModified: Long = 0L,
    ignoreCustomArtwork: Boolean = false,
    blurRadius: Int = 0,
    isPlaying: Boolean = false
) {
    val context = LocalContext.current
    var isError by remember(albumId, lastModified, ignoreCustomArtwork) { mutableStateOf(false) }

    val artworkData = remember(albumId, lastModified, ignoreCustomArtwork) {
        if (!ignoreCustomArtwork && ArtworkCache.hasCustomArtwork(context, albumId)) {
            File(context.filesDir, "custom_artwork/$albumId.jpg")
        } else {
            ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                albumId
            )
        }
    }

    if (!showArtwork || isError) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            PlayingIndicator(
                isPlaying = isPlaying,
                modifier = Modifier.size(80.dp)
            )
        }
    } else {
        val model = remember(artworkData, blurRadius, lastModified) {
            val builder = ImageRequest.Builder(context)
                .data(artworkData)
                .memoryCacheKey("artwork_${albumId}_${lastModified}_blur_${blurRadius}")
                .crossfade(true)
            if (blurRadius > 0) {
                builder.transformations(BlurTransformation(context, blurRadius.toFloat()))
            }
            builder.build()
        }

        AsyncImage(
            model = model,
            contentDescription = "Album Artwork",
            modifier = modifier,
            contentScale = ContentScale.Crop,
            onError = { isError = true },
            onSuccess = { isError = false }
        )
    }
}
