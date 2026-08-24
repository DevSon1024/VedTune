package com.devson.vedtune.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import kotlin.math.absoluteValue

/**
 * A high-performance, native Jetpack Compose Overlap Carousel.
 * Replicates the "MULTI_PAGE_OVERLAP" effect with scale, alpha fading, and physical translationX overlap.
 *
 * @param items Generic list of items to display.
 * @param modifier Modifier applied to the outer HorizontalPager.
 * @param pagerState Optional hoisted PagerState.
 * @param contentPadding Padding applied to reveal partial previous and next pages.
 * @param minScale Scale factor for off-center items (e.g. 0.85f).
 * @param minAlpha Alpha transparency for off-center items (e.g. 0.6f).
 * @param overlapOffset Distance in Dp to pull adjacent cards towards center for physical overlap.
 * @param key Key selector for pager items.
 * @param onPageSelected Callback when a page settles in the center position.
 * @param itemContent Composable slot rendering each item with item, page index, and normalized offset.
 */
@Composable
fun <T> VedTuneOverlapCarousel(
    items: List<T>,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState(pageCount = { items.size }),
    contentPadding: PaddingValues = PaddingValues(horizontal = 48.dp),
    minScale: Float = 0.85f,
    minAlpha: Float = 0.6f,
    overlapOffset: Dp = 32.dp,
    key: ((item: T) -> Any)? = null,
    onPageSelected: ((item: T, page: Int) -> Unit)? = null,
    itemContent: @Composable (item: T, page: Int, pageOffset: Float) -> Unit
) {
    if (items.isEmpty()) return

    if (onPageSelected != null) {
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }.collect { page ->
                if (page in items.indices) {
                    onPageSelected(items[page], page)
                }
            }
        }
    }

    val overlapPx = with(LocalDensity.current) { overlapOffset.toPx() }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        pageSize = PageSize.Fill,
        beyondViewportPageCount = 2,
        flingBehavior = PagerDefaults.flingBehavior(state = pagerState),
        key = key?.let { selector -> { index -> selector(items[index]) } }
    ) { page ->
        val rawOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
        val normalizedOffset = rawOffset.absoluteValue.coerceIn(0f, 1f)

        // Interpolate scale and alpha so center card is 1f and adjacent cards are scaled & faded
        val scale = lerp(start = minScale, stop = 1f, fraction = 1f - normalizedOffset)
        val alpha = lerp(start = minAlpha, stop = 1f, fraction = 1f - normalizedOffset)

        // Pull adjacent items closer to the center to generate the physical overlap effect
        val translationX = rawOffset * overlapPx
        val zIndex = 1f - normalizedOffset

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(zIndex)
                .graphicsLayer {
                    this.scaleX = scale
                    this.scaleY = scale
                    this.alpha = alpha
                    this.translationX = translationX
                    this.shadowElevation = if (normalizedOffset < 0.1f) 12.dp.toPx() else 4.dp.toPx()
                },
            contentAlignment = Alignment.Center
        ) {
            if (page in items.indices) {
                itemContent(items[page], page, normalizedOffset)
            }
        }
    }
}

/**
 * Convenience overload of [VedTuneOverlapCarousel] with simplified item content lambda.
 */
@Composable
fun <T> VedTuneOverlapCarousel(
    items: List<T>,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState(pageCount = { items.size }),
    contentPadding: PaddingValues = PaddingValues(horizontal = 48.dp),
    minScale: Float = 0.85f,
    minAlpha: Float = 0.6f,
    overlapOffset: Dp = 32.dp,
    key: ((item: T) -> Any)? = null,
    onPageSelected: ((item: T, page: Int) -> Unit)? = null,
    content: @Composable (item: T) -> Unit
) {
    VedTuneOverlapCarousel(
        items = items,
        modifier = modifier,
        pagerState = pagerState,
        contentPadding = contentPadding,
        minScale = minScale,
        minAlpha = minAlpha,
        overlapOffset = overlapOffset,
        key = key,
        onPageSelected = onPageSelected
    ) { item, _, _ ->
        content(item)
    }
}
