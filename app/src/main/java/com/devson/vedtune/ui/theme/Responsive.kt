package com.devson.vedtune.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Responsive window size classification for mobile, foldable, and tablet form factors.
 */
enum class VedTuneWindowWidthSizeClass {
    COMPACT,    // Typical phones in portrait (< 600dp)
    MEDIUM,     // Small tablets, foldables, phones in landscape (600dp - 839dp)
    EXPANDED    // Tablets, desktop-like displays (>= 840dp)
}

data class VedTuneAdaptiveInfo(
    val widthSizeClass: VedTuneWindowWidthSizeClass,
    val screenWidthDp: Dp,
    val screenHeightDp: Dp,
    val isLandscape: Boolean,
    val isTablet: Boolean
)

@Composable
fun rememberVedTuneAdaptiveInfo(): VedTuneAdaptiveInfo {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    return remember(screenWidthDp, screenHeightDp, isLandscape) {
        val widthSizeClass = when {
            screenWidthDp < 600.dp -> VedTuneWindowWidthSizeClass.COMPACT
            screenWidthDp < 840.dp -> VedTuneWindowWidthSizeClass.MEDIUM
            else -> VedTuneWindowWidthSizeClass.EXPANDED
        }
        val isTablet = widthSizeClass != VedTuneWindowWidthSizeClass.COMPACT

        VedTuneAdaptiveInfo(
            widthSizeClass = widthSizeClass,
            screenWidthDp = screenWidthDp,
            screenHeightDp = screenHeightDp,
            isLandscape = isLandscape,
            isTablet = isTablet
        )
    }
}

/**
 * Calculates adaptive grid columns respecting user preferences while ensuring cards don't look stretched or crushed on larger screens.
 */
fun calculateAdaptiveGridColumns(
    widthSizeClass: VedTuneWindowWidthSizeClass,
    userSelectedSpan: Int
): Int {
    return when (widthSizeClass) {
        VedTuneWindowWidthSizeClass.COMPACT -> userSelectedSpan.coerceIn(1, 6)
        VedTuneWindowWidthSizeClass.MEDIUM -> (userSelectedSpan * 1.5f).toInt().coerceIn(2, 8)
        VedTuneWindowWidthSizeClass.EXPANDED -> (userSelectedSpan * 2).coerceIn(3, 10)
    }
}
