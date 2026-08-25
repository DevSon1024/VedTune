package com.devson.vedtune.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Standardized shapes and corner radii for VedTune.
 */
val VedTuneShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

object VedTuneShapeTokens {
    val ExtraSmall = RoundedCornerShape(4.dp)
    val Small = RoundedCornerShape(8.dp)
    val Medium = RoundedCornerShape(12.dp)
    val Large = RoundedCornerShape(16.dp)
    val ExtraLarge = RoundedCornerShape(24.dp)
    val Pill = RoundedCornerShape(32.dp)
    val Full = CircleShape

    // Semantic component shapes
    val ArtworkThumbnail = RoundedCornerShape(8.dp)
    val ArtworkCard = RoundedCornerShape(12.dp)
    val Card = RoundedCornerShape(16.dp)
    val Search = RoundedCornerShape(16.dp)
    val Dialog = RoundedCornerShape(24.dp)
    val BottomSheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val MiniPlayer = RoundedCornerShape(32.dp)
}
