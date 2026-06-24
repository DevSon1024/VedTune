package com.devson.vedtune.domain.model

/**
 * Controls how the folder filter list is applied during library synchronization.
 *
 * NONE      — No filtering; all audio files discovered by MediaStore are included.
 * WHITELIST — Only songs from folders in the whitelist are included.
 *             An empty whitelist hides all songs.
 * BLACKLIST — Songs from folders in the blacklist are excluded.
 *             Other folders remain visible.
 */
enum class FolderFilterMode {
    NONE,
    WHITELIST,
    BLACKLIST
}
