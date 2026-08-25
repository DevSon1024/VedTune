package com.devson.vedtune.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ==========================================
// DARK THEME PALETTE (Slate Midnight & Violet)
// ==========================================
val PrimaryDark = Color(0xFF818CF8)           // Vibrant Indigo/Violet
val OnPrimaryDark = Color(0xFF0F172A)
val PrimaryContainerDark = Color(0xFF312E81)  // Deep Indigo Container
val OnPrimaryContainerDark = Color(0xFFE0E7FF)

val SecondaryDark = Color(0xFFC084FC)         // Soft Electric Purple
val OnSecondaryDark = Color(0xFF3B0764)
val SecondaryContainerDark = Color(0xFF581C87)
val OnSecondaryContainerDark = Color(0xFFF3E8FF)

val TertiaryDark = Color(0xFFF472B6)          // Rose Pink
val OnTertiaryDark = Color(0xFF500724)
val TertiaryContainerDark = Color(0xFF831843)
val OnTertiaryContainerDark = Color(0xFFFCE7F3)

val ErrorDark = Color(0xFFF87171)             // Warm Coral Red
val OnErrorDark = Color(0xFF450A0A)
val ErrorContainerDark = Color(0xFF7F1D1D)
val OnErrorContainerDark = Color(0xFFFEE2E2)

val BackgroundDark = Color(0xFF0B0F19)        // Deep Midnight
val OnBackgroundDark = Color(0xFFF1F5F9)

val SurfaceDark = Color(0xFF111827)           // Elevated Midnight Surface
val OnSurfaceDark = Color(0xFFF1F5F9)
val SurfaceVariantDark = Color(0xFF1E293B)    // Slate Container
val OnSurfaceVariantDark = Color(0xFF94A3B8)

// Tonal Surface Containers (Dark)
val SurfaceContainerLowestDark = Color(0xFF070A10)
val SurfaceContainerLowDark = Color(0xFF0E1422)
val SurfaceContainerDark = Color(0xFF131C2E)
val SurfaceContainerHighDark = Color(0xFF1E293B)
val SurfaceContainerHighestDark = Color(0xFF334155)

val OutlineDark = Color(0xFF475569)
val OutlineVariantDark = Color(0xFF334155)

// ==========================================
// LIGHT THEME PALETTE (Clean Slate & Royal Indigo)
// ==========================================
val PrimaryLight = Color(0xFF4338CA)          // Royal Indigo
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFEEF2FF)
val OnPrimaryContainerLight = Color(0xFF312E81)

val SecondaryLight = Color(0xFF7E22CE)        // Vibrant Purple
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFFAF5FF)
val OnSecondaryContainerLight = Color(0xFF581C87)

val TertiaryLight = Color(0xFFBE185D)         // Deep Rose
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFDF2F8)
val OnTertiaryContainerLight = Color(0xFF831843)

val ErrorLight = Color(0xFFDC2626)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFEF2F2)
val OnErrorContainerLight = Color(0xFF991B1B)

val BackgroundLight = Color(0xFFF8FAFC)       // Crisp Off-White
val OnBackgroundLight = Color(0xFF0F172A)

val SurfaceLight = Color(0xFFFFFFFF)          // Pure Card White
val OnSurfaceLight = Color(0xFF0F172A)
val SurfaceVariantLight = Color(0xFFF1F5F9)
val OnSurfaceVariantLight = Color(0xFF64748B)

// Tonal Surface Containers (Light)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF8FAFC)
val SurfaceContainerLight = Color(0xFFF1F5F9)
val SurfaceContainerHighLight = Color(0xFFE2E8F0)
val SurfaceContainerHighestLight = Color(0xFFCBD5E1)

val OutlineLight = Color(0xFF94A3B8)
val OutlineVariantLight = Color(0xFFE2E8F0)

// ==========================================
// AMOLED THEME OVERRIDES
// ==========================================
val BackgroundAmoled = Color(0xFF000000)
val SurfaceAmoled = Color(0xFF000000)
val SurfaceContainerLowestAmoled = Color(0xFF000000)
val SurfaceContainerLowAmoled = Color(0xFF080808)
val SurfaceContainerAmoled = Color(0xFF101010)
val SurfaceContainerHighAmoled = Color(0xFF181818)
val SurfaceContainerHighestAmoled = Color(0xFF222222)
val SurfaceVariantAmoled = Color(0xFF161616)
val OutlineVariantAmoled = Color(0xFF262626)

// ==========================================
// EXTENDED SEMANTIC TOKENS
// ==========================================
data class VedTuneExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val playerSurface: Color,
    val onPlayerSurface: Color,
    val playingIndicatorBar: Color
)

val ExtendedColorsDark = VedTuneExtendedColors(
    success = Color(0xFF34D399),
    onSuccess = Color(0xFF064E3B),
    successContainer = Color(0xFF065F46),
    onSuccessContainer = Color(0xFFD1FAE5),
    playerSurface = Color(0xFF0F172A),
    onPlayerSurface = Color(0xFFF8FAFC),
    playingIndicatorBar = Color(0xFF818CF8)
)

val ExtendedColorsLight = VedTuneExtendedColors(
    success = Color(0xFF059669),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFECFDF5),
    onSuccessContainer = Color(0xFF065F46),
    playerSurface = Color(0xFFFFFFFF),
    onPlayerSurface = Color(0xFF0F172A),
    playingIndicatorBar = Color(0xFF4338CA)
)

val LocalExtendedColors = staticCompositionLocalOf { ExtendedColorsDark }

val MaterialTheme.extendedColors: VedTuneExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current