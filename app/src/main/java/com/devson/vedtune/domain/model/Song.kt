package com.devson.vedtune.domain.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val track: Int,
    val year: Int,
    val dateAdded: Long,
    val dateModified: Long,
    val isFavorite: Boolean,
    val playCount: Int,
    val lastPlayed: Long
)
