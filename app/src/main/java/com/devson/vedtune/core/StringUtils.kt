package com.devson.vedtune.core

import java.util.Locale

/**
 * Truncates a string in the middle with an ellipsis if it exceeds maxLength.
 */
fun truncateMiddle(name: String, maxLength: Int = 36): String {
    if (name.length <= maxLength) return name
    val half = (maxLength - 3) / 2
    return name.take(half) + "..." + name.takeLast(half)
}

/**
 * Formats an integer to a 2-digit padded string (e.g. 1 -> "01").
 */
fun Int.toTwoDigitString(): String = String.format(Locale.getDefault(), "%02d", this)
