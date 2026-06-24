package com.devson.vedtune.data.mapper

import com.devson.vedtune.data.local.entity.SongEntity
import com.devson.vedtune.domain.model.Song

fun SongEntity.toSong(): Song {
    return Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        duration = duration,
        track = track,
        year = year,
        dateAdded = dateAdded,
        dateModified = dateModified,
        isFavorite = isFavorite,
        playCount = playCount,
        lastPlayed = lastPlayed
    )
}

fun Song.toEntity(): SongEntity {
    return SongEntity(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        duration = duration,
        track = track,
        year = year,
        dateAdded = dateAdded,
        dateModified = dateModified,
        isFavorite = isFavorite,
        playCount = playCount,
        lastPlayed = lastPlayed
    )
}
