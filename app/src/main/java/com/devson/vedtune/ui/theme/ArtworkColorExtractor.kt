package com.devson.vedtune.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/**
 * Reusable utility for deriving subtle, accessible UI colors and gradients from artwork or color tones.
 * Ensures WCAG contrast ratios and avoids aggressive app recoloring.
 */
object ArtworkColorExtractor {

    /**
     * Calculates the relative luminance of a color according to WCAG definitions.
     */
    fun calculateLuminance(color: Color): Float {
        fun channelLuminance(channel: Float): Float {
            return if (channel <= 0.03928f) {
                channel / 12.92f
            } else {
                ((channel + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
            }
        }

        val r = channelLuminance(color.red)
        val g = channelLuminance(color.green)
        val b = channelLuminance(color.blue)

        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    /**
     * Returns an accessible text color (White or Dark Slate) for any background color.
     */
    fun contrastingTextColor(backgroundColor: Color): Color {
        val luminance = calculateLuminance(backgroundColor)
        return if (luminance < 0.45f) Color.White else Color(0xFF0F172A)
    }

    /**
     * Generates a smooth, subtle vertical gradient for player backgrounds that preserves artwork prominence and readability.
     */
    fun playerBackgroundGradient(isDark: Boolean, dominantColor: Color? = null): Brush {
        val baseTint = dominantColor?.copy(alpha = 0.25f) ?: Color.Transparent

        return if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    baseTint.compositeOver(Color.Black.copy(alpha = 0.65f)),
                    Color.Black.copy(alpha = 0.85f),
                    Color.Black.copy(alpha = 0.95f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    baseTint.compositeOver(Color.White.copy(alpha = 0.45f)),
                    Color.White.copy(alpha = 0.75f),
                    Color.White.copy(alpha = 0.90f)
                )
            )
        }
    }

    /**
     * Alpha-composites a foreground color over a background color.
     */
    private fun Color.compositeOver(background: Color): Color {
        val srcAlpha = this.alpha
        val dstAlpha = background.alpha * (1f - srcAlpha)
        val outAlpha = srcAlpha + dstAlpha

        if (outAlpha == 0f) return Color.Transparent

        val outR = (this.red * srcAlpha + background.red * dstAlpha) / outAlpha
        val outG = (this.green * srcAlpha + background.green * dstAlpha) / outAlpha
        val outB = (this.blue * srcAlpha + background.blue * dstAlpha) / outAlpha

        return Color(outR, outG, outB, outAlpha)
    }
}
