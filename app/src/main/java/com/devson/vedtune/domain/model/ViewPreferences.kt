package com.devson.vedtune.domain.model

data class ViewPreferences(
    val isGridView: Boolean = false,
    val gridSpanCount: Int = 2,
    val showArtist: Boolean = true,
    val showAlbum: Boolean = true,
    val showDuration: Boolean = true,
    val showAlbumArt: Boolean = true
)
