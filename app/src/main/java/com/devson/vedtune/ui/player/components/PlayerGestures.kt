package com.devson.vedtune.ui.player.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Detects single-pointer and multi-pointer (two-finger) taps without interfering with drag/scroll gestures.
 */
fun Modifier.detectPlayerArtworkGestures(
    onSingleTap: () -> Unit,
    onDoublePointerTap: () -> Unit,
    touchSlop: Float? = null
): Modifier = pointerInput(Unit) {
    val actualTouchSlop = touchSlop ?: viewConfiguration.touchSlop
    awaitEachGesture {
        val firstDown = awaitFirstDown(requireUnconsumed = false)
        val startTime = System.currentTimeMillis()
        var maxPointers = 1
        var hasMovedBeyondSlop = false
        val startPositions = mutableMapOf<PointerId, Offset>()
        startPositions[firstDown.id] = firstDown.position

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val pressedPointers = event.changes.filter { it.pressed }
            if (pressedPointers.size > maxPointers) {
                maxPointers = pressedPointers.size
            }

            for (change in event.changes) {
                if (change.pressed && !startPositions.containsKey(change.id)) {
                    startPositions[change.id] = change.position
                }
                val startPos = startPositions[change.id]
                if (startPos != null && (change.position - startPos).getDistance() > actualTouchSlop) {
                    hasMovedBeyondSlop = true
                }
            }

            if (pressedPointers.isEmpty()) {
                val duration = System.currentTimeMillis() - startTime
                if (!hasMovedBeyondSlop && duration < 500) {
                    if (maxPointers >= 2) {
                        event.changes.forEach { it.consume() }
                        onDoublePointerTap()
                    } else if (maxPointers == 1) {
                        val anyConsumed = event.changes.any { it.isConsumed }
                        if (!anyConsumed) {
                            event.changes.forEach { it.consume() }
                            onSingleTap()
                        }
                    }
                }
                break
            }
        }
    }
}

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
