package com.devson.vedtune.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standardized spacing tokens for VedTune.
 * Eliminates arbitrary hardcoded padding and margin values throughout the app.
 */
data class VedTuneSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 12.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
    val huge: Dp = 40.dp,
    val massive: Dp = 48.dp
)

val LocalSpacing = staticCompositionLocalOf { VedTuneSpacing() }

val MaterialTheme.spacing: VedTuneSpacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current
