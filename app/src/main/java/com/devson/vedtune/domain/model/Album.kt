package com.devson.vedtune.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val songCount: Int
)
