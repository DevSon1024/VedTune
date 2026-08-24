package com.devson.vedtune.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formats a duration in milliseconds into "m:ss" or "h:mm:ss" if duration exceeds 1 hour.
 */
fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "0:00"
    val totalSeconds = durationMs / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}

/**
 * Extension function for [formatDuration].
 */
fun Long.toFormattedSongDuration(): String = formatDuration(this)

/**
 * Formats a total duration in milliseconds into "hh:mm:ss".
 */
fun Long.toFormattedDuration(): String {
    if (this <= 0L) return "00:00:00"
    val totalSeconds = this / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}

/**
 * Formats player/playback position and duration into "mm:ss" or "hh:mm:ss".
 */
fun formatPlayerTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

/**
 * Extension function for [formatPlayerTime].
 */
fun Long.toFormattedPlayerTime(): String = formatPlayerTime(this)

/**
 * Formats millisecond timestamps into LRC timestamp format "[mm:ss.xx]" or "[hh:mm:ss.xx]".
 */
fun formatLrcTime(ms: Long, includeBrackets: Boolean = true): String {
    if (ms < 0L) return if (includeBrackets) "[00:00.00]" else "00:00.00"
    val hours = ms / 3600000L
    val minutes = (ms % 3600000L) / 60000L
    val seconds = (ms % 60000L) / 1000L
    val hundredths = (ms % 1000L) / 10L
    val formatted = if (hours > 0L) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d.%02d", hours, minutes, seconds, hundredths)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, hundredths)
    }
    return if (includeBrackets) "[$formatted]" else formatted
}

/**
 * Extension function for [formatLrcTime].
 */
fun Long.toFormattedLrcTime(includeBrackets: Boolean = true): String = formatLrcTime(this, includeBrackets)

/**
 * Formats a timestamp into a readable date format "MMM dd, yyyy hh:mm a".
 */
fun formatTimestamp(timestamp: Long, isMediaStoreTime: Boolean = false): String {
    if (timestamp <= 0L) return "Unknown"
    val millis = if (isMediaStoreTime) timestamp * 1000L else timestamp
    val date = Date(millis)
    val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
    return sdf.format(date)
}

/**
 * Extension function for [formatTimestamp].
 */
fun Long.toFormattedDate(isMediaStoreTime: Boolean = false): String = formatTimestamp(this, isMediaStoreTime)
