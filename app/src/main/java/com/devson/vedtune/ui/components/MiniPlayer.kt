package com.devson.vedtune.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneMotion
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.spacing
import kotlinx.coroutines.launch

/**
 * Standardized, reusable Mini Player for VedTune.
 * Provides accessible controls, smooth track change transitions, swipe gesture support, and battery-efficient progress updates.
 */
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
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var isNext by remember { mutableStateOf(true) }

    LaunchedEffect(song?.id) {
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
            val accessibilityLabel = "Now playing: ${song.title} by ${song.artist}"

            Card(
                modifier = Modifier
                    .padding(
                        horizontal = MaterialTheme.spacing.m,
                        vertical = MaterialTheme.spacing.xs
                    )
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(VedTuneShapeTokens.MiniPlayer)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .semantics {
                        contentDescription = accessibilityLabel
                    }
                    .then(
                        if (isGestureEnabled) {
                            Modifier
                                .pointerInput(song.id) {
                                    detectTapGestures(
                                        onTap = { onClick() },
                                        onDoubleTap = {
                                            onPlayPauseClick()
                                            scope.launch {
                                                scale.animateTo(0.92f, animationSpec = VedTuneMotion.standardTween(80))
                                                scale.animateTo(1.04f, animationSpec = VedTuneMotion.standardTween(100))
                                                scale.animateTo(1f, animationSpec = VedTuneMotion.standardTween(80))
                                            }
                                        }
                                    )
                                }
                                .pointerInput(song.id) {
                                    var dragAccumulator = 0f
                                    detectHorizontalDragGestures(
                                        onDragStart = { dragAccumulator = 0f },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            dragAccumulator += dragAmount
                                        },
                                        onDragEnd = {
                                            val threshold = 100f
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
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = MaterialTheme.spacing.s,
                                end = MaterialTheme.spacing.xs,
                                top = MaterialTheme.spacing.xs,
                                bottom = MaterialTheme.spacing.xs
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Artwork & Track Details with smooth horizontal transition
                        AnimatedContent(
                            targetState = song,
                            transitionSpec = {
                                VedTuneMotion.horizontalTrackTransition(isNext)
                            },
                            label = "mini_player_track_content",
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = isGestureEnabled, onClick = onClick)
                        ) { targetSong ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (showArtwork) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(VedTuneShapeTokens.Small)
                                    ) {
                                        SongArtwork(
                                            albumId = targetSong.albumId,
                                            lastModified = targetSong.dateModified,
                                            modifier = Modifier.fillMaxSize(),
                                            showArtwork = true
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.m))
                                }

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = targetSong.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xxs))
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

                        // Playback Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isGestureEnabled) {
                                VedTuneIconButton(
                                    icon = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous track",
                                    onClick = {
                                        isNext = false
                                        onSkipPreviousClick()
                                    },
                                    iconSize = VedTuneIconSizes.Medium,
                                    touchTargetSize = 48.dp,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            VedTuneIconButton(
                                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                onClick = onPlayPauseClick,
                                iconSize = VedTuneIconSizes.Large,
                                touchTargetSize = 48.dp,
                                tint = MaterialTheme.colorScheme.primary
                            )

                            VedTuneIconButton(
                                icon = Icons.Default.SkipNext,
                                contentDescription = "Next track",
                                onClick = {
                                    isNext = true
                                    onSkipNextClick()
                                },
                                iconSize = VedTuneIconSizes.Medium,
                                touchTargetSize = 48.dp,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Progress indicator aligned at the bottom (lambda-based for zero recomposition overhead)
                    if (showProgress) {
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .align(Alignment.BottomCenter),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }
    }
}
