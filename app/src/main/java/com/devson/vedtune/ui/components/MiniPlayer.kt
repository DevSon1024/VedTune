package com.devson.vedtune.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.Song
import kotlinx.coroutines.launch

@Composable
fun MiniPlayer(
    song: Song?,
    isPlaying: Boolean,
    progress: Float,
    onPlayPauseClick: () -> Unit,
    onSkipNextClick: () -> Unit,
    onSkipPreviousClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showArtwork: Boolean = true,
    showProgress: Boolean = true,
    isGestureEnabled: Boolean = false
) {
    val rotationAngle = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                val startValue = rotationAngle.value % 360f
                rotationAngle.snapTo(startValue)
                rotationAngle.animateTo(
                    targetValue = startValue + 360f,
                    animationSpec = tween(durationMillis = 15000, easing = LinearEasing)
                )
            }
        }
    }

    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var isNext by remember { mutableStateOf(true) }

    LaunchedEffect(song) {
        // Reset to true so natural progression transitions forward
        isNext = true
    }

    AnimatedVisibility(
        visible = song != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        if (song != null) {
            Card(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 8.dp)
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .then(
                        if (isGestureEnabled) {
                            Modifier
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { onClick() },
                                        onDoubleTap = {
                                            onPlayPauseClick()
                                            scope.launch {
                                                scale.animateTo(0.90f, animationSpec = tween(100))
                                                scale.animateTo(1.05f, animationSpec = tween(100))
                                                scale.animateTo(1f, animationSpec = tween(80))
                                            }
                                        }
                                    )
                                }
                                .pointerInput(Unit) {
                                    var dragAccumulator = 0f
                                    detectHorizontalDragGestures(
                                        onDragStart = { dragAccumulator = 0f },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            dragAccumulator += dragAmount
                                        },
                                        onDragEnd = {
                                            val threshold = 120f // pixels
                                            if (dragAccumulator < -threshold) {
                                                isNext = true
                                                onSkipNextClick()
                                            } else if (dragAccumulator > threshold) {
                                                isNext = false
                                                onSkipPreviousClick()
                                            }
                                        }
                                    )
                                }
                        } else {
                            Modifier.clickable(onClick = onClick)
                        }
                    ),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Animated track information section (Artwork, Title, Artist)
                        AnimatedContent(
                            targetState = song,
                            transitionSpec = {
                                if (isNext) {
                                    (slideInHorizontally { width -> width } + fadeIn()) togetherWith 
                                    (slideOutHorizontally { width -> -width } + fadeOut())
                                } else {
                                    (slideInHorizontally { width -> -width } + fadeIn()) togetherWith 
                                    (slideOutHorizontally { width -> width } + fadeOut())
                                }
                            },
                            label = "track_transition",
                            modifier = Modifier.weight(1f)
                        ) { targetSong ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SongArtwork(
                                    albumId = targetSong.albumId,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .rotate(rotationAngle.value)
                                        .clip(CircleShape),
                                    showArtwork = showArtwork
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = targetSong.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = targetSong.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Statically rendered controls only if gesture mode is disabled
                        if (!isGestureEnabled) {
                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(onClick = {
                                isNext = false
                                onSkipPreviousClick()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Skip Previous",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(onClick = onPlayPauseClick) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(onClick = {
                                isNext = true
                                onSkipNextClick()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Skip Next",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Progress indicator aligned at the bottom
                    if (showProgress) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .align(Alignment.BottomCenter),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}
