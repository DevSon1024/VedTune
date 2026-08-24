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
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.pill
import androidx.graphics.shapes.star
import androidx.compose.runtime.collectAsState

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
        val safeRadius = radius.coerceIn(1f, 25f).toInt()
        return try {
            stackBlur(input, safeRadius)
        } catch (e: Exception) {
            e.printStackTrace()
            input
        }
    }

    private fun stackBlur(sentBitmap: Bitmap, radius: Int): Bitmap {
        val bitmap = sentBitmap.copy(Bitmap.Config.ARGB_8888, true)
        if (radius < 1) return bitmap

        val w = bitmap.width
        val h = bitmap.height

        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(maxOf(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (idx in 0 until 256 * divsum) {
            dv[idx] = idx / divsum
        }

        yw = 0
        yi = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        for (curY in 0 until h) {
            rinsum = 0
            ginsum = 0
            binsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            for (curI in -radius..radius) {
                p = pix[yi + minOf(wm, maxOf(curI, 0))]
                sir = stack[curI + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)
                rbs = r1 - kotlin.math.abs(curI)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (curI > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
            }
            stackpointer = radius

            for (curX in 0 until w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (curY == 0) {
                    vmin[curX] = minOf(curX + radius + 1, wm)
                }
                p = pix[yw + vmin[curX]]

                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi++
            }
            yw += w
        }

        for (curX in 0 until w) {
            rinsum = 0
            ginsum = 0
            binsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            yp = -radius * w
            for (curI in -radius..radius) {
                yi = maxOf(0, yp) + curX
                sir = stack[curI + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - kotlin.math.abs(curI)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (curI > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (curI < hm) {
                    yp += w
                }
            }
            yi = curX
            stackpointer = radius
            for (curY in 0 until h) {
                pix[yi] = (0xff000000.toInt() and pix[yi]) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (curX == 0) {
                    vmin[curY] = minOf(curY + r1, hm) * w
                }
                p = curX + vmin[curY]

                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi += w
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SongArtworkEntryPoint {
    fun settingsRepository(): com.devson.vedtune.domain.repository.SettingsRepository
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveArtworkFallback(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    progress: (() -> Float)? = null,
    showAnimation: Boolean = true
) {
    val polygons = remember {
        listOf(
            RoundedPolygon.star(numVerticesPerRadius = 10, innerRadius = 0.65f, rounding = CornerRounding(0.2f)),
            RoundedPolygon.circle(numVertices = 12),
            RoundedPolygon.pill(width = 1f, height = 0.8f, smoothing = 0.3f)
        )
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        if (showAnimation) {
            if (progress != null) {
                LoadingIndicator(
                    progress = progress,
                    modifier = Modifier.size(56.dp),
                    color = MaterialTheme.colorScheme.primary,
                    polygons = polygons
                )
            } else {
                LoadingIndicator(
                    modifier = Modifier.size(56.dp),
                    color = MaterialTheme.colorScheme.primary,
                    polygons = polygons
                )
            }
        }
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
    isPlaying: Boolean = false,
    playbackProgress: (() -> Float)? = null,
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector = androidx.compose.material.icons.Icons.Default.MusicNote,
    showFallbackAnimation: Boolean = false
) {
    val context = LocalContext.current
    var isError by remember(albumId, lastModified, ignoreCustomArtwork) { mutableStateOf(false) }

    val settingsRepository = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            SongArtworkEntryPoint::class.java
        ).settingsRepository()
    }

    val forceSquare by settingsRepository.forceSquareArtwork.collectAsState(initial = true)
    val quality by settingsRepository.albumArtQuality.collectAsState(initial = com.devson.vedtune.domain.model.AlbumArtQuality.BALANCED)

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

    val model = remember(artworkData, blurRadius, lastModified, quality) {
        val builder = ImageRequest.Builder(context)
            .data(artworkData)
            .memoryCacheKey("artwork_${albumId}_${lastModified}_blur_${blurRadius}_quality_${quality.name}")
            .crossfade(true)
        
        when (quality) {
            com.devson.vedtune.domain.model.AlbumArtQuality.SAVE_SPACE -> {
                builder.size(150, 150)
            }
            com.devson.vedtune.domain.model.AlbumArtQuality.BALANCED -> {
                builder.size(400, 400)
            }
            com.devson.vedtune.domain.model.AlbumArtQuality.HIGH_QUALITY -> {
                // Full resolution / no restriction
            }
        }

        if (blurRadius > 0) {
            builder.transformations(BlurTransformation(context, blurRadius.toFloat()))
        }
        builder.build()
    }

    Crossfade(
        targetState = (!showArtwork || isError),
        label = "ArtworkCrossfade",
        modifier = modifier
    ) { fallbackActive ->
        if (fallbackActive) {
            if (showFallbackAnimation) {
                ExpressiveArtworkFallback(
                    isPlaying = isPlaying,
                    progress = playbackProgress,
                    showAnimation = true,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = fallbackIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        } else {
            AsyncImage(
                model = model,
                contentDescription = "Album Artwork",
                modifier = Modifier.fillMaxSize(),
                contentScale = if (forceSquare) ContentScale.Crop else ContentScale.Fit,
                onError = { isError = true },
                onSuccess = { isError = false }
            )
        }
    }
}
