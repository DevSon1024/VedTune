package com.devson.vedtune.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneMotion
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.spacing
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
        enter = slideInVertically(
            animationSpec = VedTuneMotion.standardTween(VedTuneMotion.DurationMedium),
            initialOffsetY = { it }
        ) + fadeIn(VedTuneMotion.standardTween(VedTuneMotion.DurationMedium)),
        exit = slideOutVertically(
            animationSpec = VedTuneMotion.standardTween(VedTuneMotion.DurationShort),
            targetOffsetY = { it }
        ) + fadeOut(VedTuneMotion.standardTween(VedTuneMotion.DurationShort)),
        modifier = modifier
    ) {
        if (song != null) {
            Card(
                modifier = Modifier
                    .padding(
                        start = MaterialTheme.spacing.l,
                        end = MaterialTheme.spacing.l,
                        bottom = MaterialTheme.spacing.m,
                        top = MaterialTheme.spacing.s
                    )
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(VedTuneShapeTokens.MiniPlayer)
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
                                                scale.animateTo(0.90f, animationSpec = VedTuneMotion.standardTween(100))
                                                scale.animateTo(1.05f, animationSpec = VedTuneMotion.standardTween(100))
                                                scale.animateTo(1f, animationSpec = VedTuneMotion.standardTween(80))
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
                shape = VedTuneShapeTokens.MiniPlayer,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = MaterialTheme.spacing.l, vertical = MaterialTheme.spacing.s),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Animated track information section (Artwork, Title, Artist)
                        AnimatedContent(
                            targetState = song,
                            transitionSpec = {
                                VedTuneMotion.horizontalTrackTransition(isNext)
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

                                Spacer(modifier = Modifier.width(MaterialTheme.spacing.m))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = targetSong.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
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
                            Spacer(modifier = Modifier.width(MaterialTheme.spacing.s))

                            VedTuneIconButton(
                                icon = Icons.Default.SkipPrevious,
                                contentDescription = "Skip Previous",
                                onClick = {
                                    isNext = false
                                    onSkipPreviousClick()
                                },
                                iconSize = VedTuneIconSizes.Medium,
                                touchTargetSize = 36.dp,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val playPauseInteractionSource = remember { MutableInteractionSource() }
                            val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()
                            val playPauseScale by animateFloatAsState(
                                targetValue = if (isPlayPausePressed) 0.85f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "MiniPlayerPlayPauseBounce"
                            )

                            androidx.compose.material3.IconButton(
                                onClick = onPlayPauseClick,
                                interactionSource = playPauseInteractionSource,
                                modifier = Modifier
                                    .size(40.dp)
                                    .graphicsLayer {
                                        scaleX = playPauseScale
                                        scaleY = playPauseScale
                                    }
                            ) {
                                AnimatedContent(
                                    targetState = isPlaying,
                                    transitionSpec = {
                                        (scaleIn(initialScale = 0.8f) + fadeIn(VedTuneMotion.standardTween(VedTuneMotion.DurationShort)))
                                            .togetherWith(scaleOut(targetScale = 0.8f) + fadeOut(VedTuneMotion.standardTween(VedTuneMotion.DurationShort)))
                                    },
                                    label = "MiniPlayerPlayPauseIcon"
                                ) { playing ->
                                    androidx.compose.material3.Icon(
                                        imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (playing) "Pause" else "Play",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(VedTuneIconSizes.Large)
                                    )
                                }
                            }

                            VedTuneIconButton(
                                icon = Icons.Default.SkipNext,
                                contentDescription = "Skip Next",
                                onClick = {
                                    isNext = true
                                    onSkipNextClick()
                                },
                                iconSize = VedTuneIconSizes.Medium,
                                touchTargetSize = 36.dp,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
