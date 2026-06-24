package com.devson.vedtune.ui.components

import android.content.ContentUris
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
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
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun SongArtwork(
    albumId: Long,
    modifier: Modifier = Modifier,
    showArtwork: Boolean = true,
    lastModified: Long = 0L
) {
    val context = LocalContext.current
    var artworkData by remember(albumId, lastModified) { mutableStateOf<Any?>(null) }
    var isError by remember(albumId, lastModified) { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(albumId, lastModified) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val customFile = java.io.File(context.filesDir, "custom_artwork/$albumId.jpg")
            if (customFile.exists()) {
                artworkData = customFile
            } else {
                artworkData = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )
            }
        }
    }

    if (!showArtwork || isError || artworkData == null) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Music Placeholder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(artworkData)
                .memoryCacheKey("artwork_${albumId}_${lastModified}")
                .crossfade(true)
                .build(),
            contentDescription = "Album Artwork",
            modifier = modifier,
            contentScale = ContentScale.Crop,
            onError = { isError = true },
            onSuccess = { isError = false }
        )
    }
}
