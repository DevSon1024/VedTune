package com.devson.vedtune.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val createdAt: Long
) {
    companion object {
        const val FAVORITES_PLAYLIST_ID = 1L
        const val FAVORITES_PLAYLIST_NAME = "Favorites"
    }
}
