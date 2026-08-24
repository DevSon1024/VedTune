package com.devson.vedtune.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.songs.SortBy
import com.devson.vedtune.ui.songs.SortOrder
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * Reusable, high-performance FastScroller component for LazyListState (LazyColumn).
 * Only renders when content is scrollable and exceeds the device viewport.
 */
@Composable
fun FastScroller(
    listState: LazyListState,
    sectionIndices: Map<String, Int>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    popupColor: Color = MaterialTheme.colorScheme.primaryContainer,
    popupContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    thumbHeight: Dp = 48.dp
) {
    val totalItems by remember {
        derivedStateOf { listState.layoutInfo.totalItemsCount }
    }
    val firstVisibleItemIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }
    val isScrollable by remember {
        derivedStateOf {
            listState.canScrollForward || listState.canScrollBackward ||
            listState.layoutInfo.visibleItemsInfo.size < listState.layoutInfo.totalItemsCount
        }
    }

    FastScrollerCore(
        totalItemsCount = totalItems,
        firstVisibleIndex = firstVisibleItemIndex,
        isScrollable = isScrollable,
        onScrollToIndex = { targetIndex ->
            listState.scrollToItem(targetIndex)
        },
        sectionIndices = sectionIndices,
        contentPadding = contentPadding,
        modifier = modifier,
        thumbColor = thumbColor,
        trackColor = trackColor,
        popupColor = popupColor,
        popupContentColor = popupContentColor,
        thumbHeight = thumbHeight
    )
}

/**
 * Reusable, high-performance FastScroller component for LazyGridState (LazyVerticalGrid).
 * Only renders when content is scrollable and exceeds the device viewport.
 */
@Composable
fun FastScroller(
    gridState: LazyGridState,
    sectionIndices: Map<String, Int>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    popupColor: Color = MaterialTheme.colorScheme.primaryContainer,
    popupContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    thumbHeight: Dp = 48.dp
) {
    val totalItems by remember {
        derivedStateOf { gridState.layoutInfo.totalItemsCount }
    }
    val firstVisibleItemIndex by remember {
        derivedStateOf { gridState.firstVisibleItemIndex }
    }
    val isScrollable by remember {
        derivedStateOf {
            gridState.canScrollForward || gridState.canScrollBackward ||
            gridState.layoutInfo.visibleItemsInfo.size < gridState.layoutInfo.totalItemsCount
        }
    }

    FastScrollerCore(
        totalItemsCount = totalItems,
        firstVisibleIndex = firstVisibleItemIndex,
        isScrollable = isScrollable,
        onScrollToIndex = { targetIndex ->
            gridState.scrollToItem(targetIndex)
        },
        sectionIndices = sectionIndices,
        contentPadding = contentPadding,
        modifier = modifier,
        thumbColor = thumbColor,
        trackColor = trackColor,
        popupColor = popupColor,
        popupContentColor = popupContentColor,
        thumbHeight = thumbHeight
    )
}

/**
 * Core stateless FastScroller layout and gesture engine with isolated recomposition scopes.
 */
@Composable
private fun FastScrollerCore(
    totalItemsCount: Int,
    firstVisibleIndex: Int,
    isScrollable: Boolean,
    onScrollToIndex: suspend (Int) -> Unit,
    sectionIndices: Map<String, Int>,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    popupColor: Color = MaterialTheme.colorScheme.primaryContainer,
    popupContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    thumbHeight: Dp = 48.dp
) {
    val sections = remember(sectionIndices) { sectionIndices.keys.toList() }
    
    // Only show if content is scrollable, exceeds viewport, and has sections
    if (!isScrollable || sections.isEmpty() || totalItemsCount <= 1) {
        return
    }

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current

    var isDragging by remember { mutableStateOf(false) }
    var activeLabel by remember { mutableStateOf(sections.firstOrNull() ?: "") }
    var dragProgressFraction by remember { mutableFloatStateOf(0f) }

    // Provide safe access to the latest section map inside gesture handlers
    val currentSections by rememberUpdatedState(sections)
    val currentSectionIndices by rememberUpdatedState(sectionIndices)
    val currentScrollHandler by rememberUpdatedState(onScrollToIndex)

    // Compute idle thumb position without forcing layout passes
    val idleProgressFraction by remember {
        derivedStateOf {
            if (totalItemsCount > 1) {
                (firstVisibleIndex.toFloat() / (totalItemsCount - 1).toFloat()).coerceIn(0f, 1f)
            } else 0f
        }
    }

    val effectiveProgressFraction = if (isDragging) dragProgressFraction else idleProgressFraction

    // Trigger subtle tactile haptic feedback on section changes during drag
    LaunchedEffect(activeLabel) {
        if (isDragging && activeLabel.isNotEmpty()) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        val topPaddingPx = with(density) { contentPadding.calculateTopPadding().toPx() }
        val bottomPaddingPx = with(density) { contentPadding.calculateBottomPadding().toPx() }
        val thumbHeightPx = with(density) { thumbHeight.toPx() }
        val availableHeightPx = constraints.maxHeight.toFloat()
        val trackHeightPx = (availableHeightPx - topPaddingPx - bottomPaddingPx - thumbHeightPx).coerceAtLeast(1f)

        // Touch & Drag tracking modifier
        val gestureModifier = Modifier.pointerInput(topPaddingPx, trackHeightPx, thumbHeightPx) {
            fun processDrag(y: Float) {
                val clampedY = (y - topPaddingPx - (thumbHeightPx / 2f)).coerceIn(0f, trackHeightPx)
                val fraction = (clampedY / trackHeightPx).coerceIn(0f, 1f)
                dragProgressFraction = fraction

                if (currentSections.isNotEmpty()) {
                    val sectionIndex = (fraction * (currentSections.size - 1)).roundToInt().coerceIn(0, currentSections.lastIndex)
                    val sectionKey = currentSections[sectionIndex]
                    activeLabel = sectionKey

                    val targetItemIndex = currentSectionIndices[sectionKey] ?: 0
                    coroutineScope.launch {
                        currentScrollHandler(targetItemIndex)
                    }
                }
            }

            detectVerticalDragGestures(
                onDragStart = { offset ->
                    isDragging = true
                    processDrag(offset.y)
                },
                onDragEnd = {
                    isDragging = false
                },
                onDragCancel = {
                    isDragging = false
                },
                onVerticalDrag = { change, _ ->
                    change.consume()
                    processDrag(change.position.y)
                }
            )
        }

        // 1. Interactive Fast Scroller Track & Thumb
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(36.dp)
                .then(gestureModifier),
            contentAlignment = Alignment.TopCenter
        ) {
            // Background track line
            Box(
                modifier = Modifier
                    .padding(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding())
                    .fillMaxHeight()
                    .width(4.dp)
                    .clip(CircleShape)
                    .background(trackColor)
            )

            // Drag Thumb
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = topPaddingPx + (effectiveProgressFraction * trackHeightPx)
                    }
                    .padding(horizontal = 14.dp)
                    .width(8.dp)
                    .height(thumbHeight)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isDragging) thumbColor else thumbColor.copy(alpha = 0.85f))
            )
        }

        // 2. Pop-up Label Bubble (Isolated recomposition scope, rendered only while dragging)
        AnimatedVisibility(
            visible = isDragging,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier
                .padding(end = 48.dp)
                .graphicsLayer {
                    val targetY = topPaddingPx + (effectiveProgressFraction * trackHeightPx) - with(density) { 8.dp.toPx() }
                    translationY = targetY.coerceIn(topPaddingPx, availableHeightPx - bottomPaddingPx - with(density) { 48.dp.toPx() })
                }
        ) {
            FastScrollerPopupBubble(
                label = activeLabel,
                containerColor = popupColor,
                contentColor = popupContentColor
            )
        }
    }
}

/**
 * Material 3 styled popup bubble chip displaying current sort header.
 */
@Composable
private fun FastScrollerPopupBubble(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 6.dp,
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .sizeIn(minWidth = 48.dp, minHeight = 44.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (label.length > 3) 14.sp else 18.sp
                ),
                textAlign = TextAlign.Center,
                color = contentColor
            )
        }
    }
}

/**
 * Precomputes section indices with O(1) mapping for songs according to active sort order.
 */
fun computeSongSectionIndices(
    songs: List<Song>,
    sortBy: SortBy = SortBy.TITLE,
    sortOrder: SortOrder = SortOrder.ASCENDING
): Map<String, Int> {
    if (songs.isEmpty()) return emptyMap()

    val result = linkedMapOf<String, Int>()

    when (sortBy) {
        SortBy.DATE_ADDED -> {
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

            songs.forEachIndexed { index, song ->
                calendar.timeInMillis = if (song.dateAdded > 1000000000000L) song.dateAdded else song.dateAdded * 1000L
                val songYear = calendar.get(Calendar.YEAR)
                val label = if (songYear == currentYear) {
                    monthNames.getOrElse(calendar.get(Calendar.MONTH)) { songYear.toString() }
                } else {
                    songYear.toString()
                }
                if (!result.containsKey(label)) {
                    result[label] = index
                }
            }
        }
        SortBy.ARTIST -> {
            songs.forEachIndexed { index, song ->
                val firstChar = song.artist.trim().firstOrNull()?.uppercaseChar()
                val label = when {
                    firstChar == null -> "#"
                    firstChar in 'A'..'Z' -> firstChar.toString()
                    else -> "#"
                }
                if (!result.containsKey(label)) {
                    result[label] = index
                }
            }
        }
        SortBy.ALBUM -> {
            songs.forEachIndexed { index, song ->
                val firstChar = song.album.trim().firstOrNull()?.uppercaseChar()
                val label = when {
                    firstChar == null -> "#"
                    firstChar in 'A'..'Z' -> firstChar.toString()
                    else -> "#"
                }
                if (!result.containsKey(label)) {
                    result[label] = index
                }
            }
        }
        SortBy.TITLE -> {
            songs.forEachIndexed { index, song ->
                val firstChar = song.title.trim().firstOrNull()?.uppercaseChar()
                val label = when {
                    firstChar == null -> "#"
                    firstChar in 'A'..'Z' -> firstChar.toString()
                    else -> "#"
                }
                if (!result.containsKey(label)) {
                    result[label] = index
                }
            }
        }
    }

    return result
}

/**
 * Generic alphabetical section precomputations for titles/names (e.g. Albums, Artists, Genres, Playlists).
 */
fun <T> List<T>.buildAlphabeticalSectionIndices(titleSelector: (T) -> String): Map<String, Int> {
    if (isEmpty()) return emptyMap()
    val result = linkedMapOf<String, Int>()
    forEachIndexed { index, item ->
        val title = titleSelector(item).trim()
        val firstChar = title.firstOrNull()?.uppercaseChar()
        val header = when {
            firstChar == null -> "#"
            firstChar in 'A'..'Z' -> firstChar.toString()
            else -> "#"
        }
        if (!result.containsKey(header)) {
            result[header] = index
        }
    }
    return result
}
