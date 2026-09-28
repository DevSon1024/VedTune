package com.devson.vedtune.ui.player.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Detects single tap (e.g. toggle lyrics panel) and double tap (e.g. play/pause).
 */
fun Modifier.detectPlayerArtworkGestures(
    onSingleTap: () -> Unit,
    onDoubleTap: () -> Unit
): Modifier = pointerInput(onSingleTap, onDoubleTap) {
    detectTapGestures(
        onDoubleTap = {
            onDoubleTap()
        },
        onTap = {
            onSingleTap()
        }
    )
}

/**
 * Backwards-compatibility overload for callers passing onDoublePointerTap.
 */
fun Modifier.detectPlayerArtworkGestures(
    onSingleTap: () -> Unit,
    onDoublePointerTap: () -> Unit,
    touchSlop: Float? = null
): Modifier = detectPlayerArtworkGestures(
    onSingleTap = onSingleTap,
    onDoubleTap = onDoublePointerTap
)

/**
 * Interactive drag gesture modifier that tracks vertical drag downwards 1:1 with the finger,
 * smoothly translating the screen and animating down to dismiss or springing back up.
 */
fun Modifier.interactiveSwipeDown(
    offsetY: Animatable<Float, AnimationVector1D>,
    maxOffsetPx: Float,
    dismissThresholdPx: Float,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit
): Modifier = pointerInput(maxOffsetPx, dismissThresholdPx) {
    val velocityTracker = VelocityTracker()
    detectVerticalDragGestures(
        onDragStart = {
            velocityTracker.resetTracking()
        },
        onDragCancel = {
            coroutineScope.launch {
                offsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        },
        onDragEnd = {
            val velocityY = velocityTracker.calculateVelocity().y
            coroutineScope.launch {
                if (offsetY.value > dismissThresholdPx || velocityY > 1000f) {
                    offsetY.animateTo(
                        targetValue = maxOffsetPx,
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                    )
                    onDismiss()
                } else {
                    offsetY.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            }
        },
        onVerticalDrag = { change, dragAmount ->
            velocityTracker.addPosition(change.uptimeMillis, change.position)
            if (dragAmount > 0f || offsetY.value > 0f) {
                change.consume()
                val next = (offsetY.value + dragAmount).coerceAtLeast(0f)
                coroutineScope.launch {
                    offsetY.snapTo(next)
                }
            }
        }
    )
}
