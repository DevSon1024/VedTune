package com.devson.vedtune.ui.player.components

/**
 * Formats milliseconds to player time (delegates to core [com.devson.vedtune.core.formatPlayerTime]).
 */
fun formatPlayerTime(ms: Long): String = com.devson.vedtune.core.formatPlayerTime(ms)
