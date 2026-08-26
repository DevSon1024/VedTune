package com.devson.vedtune.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.devson.vedtune.domain.model.AlbumArtClickAction
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.components.ArtworkThumbnailSize
import com.devson.vedtune.ui.components.SongArtwork
import kotlin.math.absoluteValue

/**
 * A high-performance, native Jetpack Compose Overlapping HorizontalPager for PlayerScreen.
 * Displays the previous, current, and next track artwork with physical overlap, scaling, and alpha fading.
 * Automatically synchronizes with Media3 playback state and user physical swipes.
 */
@Composable
fun PlayerArtworkPager(
    queue: List<Song>,
    currentQueueIndex: Int,
    isPlaying: Boolean,
    artworkScale: Float,
    showArtwork: Boolean,
    albumArtClickAction: AlbumArtClickAction,
    playbackProgress: () -> Float,
    onSkipToQueueItem: (Int) -> Unit,
    onToggleLyrics: () -> Unit,
    onPlayPause: () -> Unit,
    onViewAlbumArt: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 28.dp),
    overlapOffset: Dp = 20.dp
) {
    if (queue.isEmpty()) return

    val initialIndex = currentQueueIndex.coerceIn(0, queue.size - 1)
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { queue.size }
    )

    // Sync with external / automated track changes (e.g. song finishes, next/prev button)
    LaunchedEffect(currentQueueIndex) {
        if (currentQueueIndex in 0 until pagerState.pageCount && pagerState.currentPage != currentQueueIndex) {
            pagerState.animateScrollToPage(currentQueueIndex)
        }
    }

    // Sync with user physical swipes: when the user settles on a new page, skip to that queue item
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settledPage ->
            if (settledPage != currentQueueIndex && settledPage in queue.indices) {
                onSkipToQueueItem(settledPage)
            }
        }
    }

    val overlapPx = with(LocalDensity.current) { overlapOffset.toPx() }

    HorizontalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentPadding = contentPadding,
        pageSize = PageSize.Fill,
        beyondViewportPageCount = 2,
        flingBehavior = PagerDefaults.flingBehavior(state = pagerState),
        key = { index -> queue.getOrNull(index)?.id ?: index }
    ) { page ->
        val song = queue.getOrNull(page) ?: return@HorizontalPager
        val rawOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
        val normalizedOffset = rawOffset.absoluteValue.coerceIn(0f, 1f)

        // Interpolate scale and alpha based on MULTI_PAGE_OVERLAP physics
        val minScale = 0.85f * artworkScale
        val scale = lerp(start = minScale, stop = artworkScale, fraction = 1f - normalizedOffset)
        val alpha = lerp(start = 0.6f, stop = 1f, fraction = 1f - normalizedOffset)
        val translationX = rawOffset * overlapPx
        val zIndex = 1f - normalizedOffset

        val isCenter = page == pagerState.currentPage

        val clickModifier = if (isCenter) {
            when (albumArtClickAction) {
                AlbumArtClickAction.DO_NOTHING -> Modifier
                AlbumArtClickAction.SHOW_LYRICS -> Modifier.clickable { onToggleLyrics() }
                AlbumArtClickAction.PLAY_PAUSE -> Modifier.clickable { onPlayPause() }
                AlbumArtClickAction.VIEW_ALBUM_ART -> Modifier.clickable { onViewAlbumArt() }
            }
        } else {
            Modifier.clickable { onSkipToQueueItem(page) }
        }

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
                }
                .clip(MaterialTheme.shapes.extraLarge)
                .then(clickModifier),
            contentAlignment = Alignment.Center
        ) {
            SongArtwork(
                albumId = song.albumId,
                lastModified = song.dateModified,
                modifier = Modifier.fillMaxSize(),
                showArtwork = showArtwork,
                thumbnailSize = ArtworkThumbnailSize.LARGE,
                isPlaying = isPlaying && isCenter,
                playbackProgress = if (isCenter) playbackProgress else { { 0f } },
                showFallbackAnimation = true
            )
        }
    }
}
