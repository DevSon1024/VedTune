package com.devson.vedtune.ui.player.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

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
