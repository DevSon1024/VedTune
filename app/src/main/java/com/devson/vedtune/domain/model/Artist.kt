package com.devson.vedtune.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Artist(
    val name: String,
    val songCount: Int,
    val albumCount: Int
)
