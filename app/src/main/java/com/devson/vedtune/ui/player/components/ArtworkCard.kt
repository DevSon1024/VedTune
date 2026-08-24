package com.devson.vedtune.ui.player.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.devson.vedtune.domain.model.AlbumArtClickAction
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.components.SongArtwork
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun ArtworkCard(
    song: Song,
    showArtwork: Boolean,
    isPlaying: Boolean,
    artworkScale: Float,
    enableSwipeToSkip: Boolean,
    albumArtClickAction: AlbumArtClickAction,
    onToggleLyrics: () -> Unit,
    onPlayPause: () -> Unit,
    onViewAlbumArt: () -> Unit,
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dragOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Reset graphicsLayer states when the new track loads
    LaunchedEffect(song.id) {
        dragOffset.snapTo(0f)
    }

    val clickModifier = when (albumArtClickAction) {
        AlbumArtClickAction.DO_NOTHING -> Modifier
        AlbumArtClickAction.SHOW_LYRICS -> Modifier.clickable { onToggleLyrics() }
        AlbumArtClickAction.PLAY_PAUSE -> Modifier.clickable { onPlayPause() }
        AlbumArtClickAction.VIEW_ALBUM_ART -> Modifier.clickable { onViewAlbumArt() }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .aspectRatio(1f)
            .scale(artworkScale),
        contentAlignment = Alignment.Center
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val swipeThreshold = widthPx * 0.40f

        val swipeModifier = if (enableSwipeToSkip) {
            Modifier.pointerInput(song.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val current = dragOffset.value
                        scope.launch {
                            if (current > swipeThreshold) {
                                dragOffset.animateTo(
                                    targetValue = widthPx * 1.2f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                                onSwipePrevious()
                                dragOffset.snapTo(0f)
                            } else if (current < -swipeThreshold) {
                                dragOffset.animateTo(
                                    targetValue = -widthPx * 1.2f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                                onSwipeNext()
                                dragOffset.snapTo(0f)
                            } else {
                                dragOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            dragOffset.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            dragOffset.snapTo(dragOffset.value + dragAmount)
                        }
                    }
                )
            }
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val progress = if (widthPx > 0f) {
                        (dragOffset.value / widthPx).coerceIn(-1f, 1f)
                    } else 0f

                    translationX = dragOffset.value
                    rotationY = progress * 24f
                    val scaleFactor = (1f - abs(progress) * 0.12f).coerceIn(0.85f, 1f)
                    scaleX = scaleFactor
                    scaleY = scaleFactor
                    cameraDistance = 16f * density.density
                }
                .clip(MaterialTheme.shapes.extraLarge)
                .then(swipeModifier)
                .then(clickModifier)
        ) {
            SongArtwork(
                albumId = song.albumId,
                modifier = Modifier.fillMaxSize(),
                showArtwork = showArtwork,
                isPlaying = isPlaying
            )
        }
    }
}
