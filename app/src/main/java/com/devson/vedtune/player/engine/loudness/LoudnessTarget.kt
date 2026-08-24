package com.devson.vedtune.player.engine.loudness

/**
 * Standard industry targets for LUFS (Loudness Units relative to Full Scale) loudness normalization.
 */
enum class LoudnessTarget(
    val lufs: Float,
    val title: String,
    val description: String
) {
    EBU_R128(
        -23.0f,
        "-23 LUFS",
        "EBU R128 Broadcast standard - Maximum dynamic range retention"
    ),
    AES_RECOMMENDED(
        -18.0f,
        "-18 LUFS",
        "AES recommendation - Audiophile reference"
    ),
    APPLE_YOUTUBE(
        -16.0f,
        "-16 LUFS",
        "Apple Music & YouTube standard - Balanced dynamics"
    ),
    STREAMING_STANDARD(
        -14.0f,
        "-14 LUFS",
        "Spotify & Tidal standard (Default)"
    ),
    HIGH_ENERGY(
        -12.0f,
        "-12 LUFS",
        "Club / High energy mastering"
    );

    companion object {
        val ALL_TARGETS: List<LoudnessTarget> = values().toList()

        fun fromLufs(lufs: Float): LoudnessTarget? {
            return values().firstOrNull { it.lufs == lufs }
        }
    }
}
