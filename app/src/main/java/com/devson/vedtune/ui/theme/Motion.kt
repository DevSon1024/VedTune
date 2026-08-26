package com.devson.vedtune.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.ContentTransform

/**
 * Standardized motion curves and durations for VedTune.
 * Animations communicate state changes without unnecessary visual noise.
 */
object VedTuneMotion {
    // Durations
    const val DurationShort = 150
    const val DurationMedium = 250
    const val DurationLong = 350
    const val DurationXLong = 500

    // Easings (Material 3 Motion Curves)
    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val DecelerateEasing: Easing = LinearOutSlowInEasing
    val StandardEasing: Easing = FastOutSlowInEasing

    // Standard Tween Specs
    fun <T> standardTween(durationMillis: Int = DurationMedium) = tween<T>(
        durationMillis = durationMillis,
        easing = StandardEasing
    )

    fun <T> emphasizedTween(durationMillis: Int = DurationMedium) = tween<T>(
        durationMillis = durationMillis,
        easing = EmphasizedEasing
    )

    // Spring specs
    fun <T> bouncySpring() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun <T> snappySpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // Common Transitions
    val FadeTransition: ContentTransform = fadeIn(animationSpec = standardTween(DurationShort)) togetherWith
            fadeOut(animationSpec = standardTween(DurationShort))

    val ScaleFadeTransition: ContentTransform =
        (fadeIn(animationSpec = standardTween(DurationShort)) + scaleIn(initialScale = 0.92f, animationSpec = standardTween(DurationShort))) togetherWith
                (fadeOut(animationSpec = standardTween(DurationShort)) + scaleOut(targetScale = 0.92f, animationSpec = standardTween(DurationShort)))

    fun horizontalTrackTransition(isNext: Boolean): ContentTransform {
        return if (isNext) {
            (slideInHorizontally(animationSpec = standardTween(DurationMedium)) { width -> width } + fadeIn(standardTween(DurationMedium))) togetherWith
                    (slideOutHorizontally(animationSpec = standardTween(DurationMedium)) { width -> -width } + fadeOut(standardTween(DurationMedium)))
        } else {
            (slideInHorizontally(animationSpec = standardTween(DurationMedium)) { width -> -width } + fadeIn(standardTween(DurationMedium))) togetherWith
                    (slideOutHorizontally(animationSpec = standardTween(DurationMedium)) { width -> width } + fadeOut(standardTween(DurationMedium)))
        }
    }
}
