package com.devson.vedtune.player.engine.replaygain

import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe in-memory LRU cache for parsed ReplayGain metadata.
 *
 * Ensures audio files are parsed once on background threads and subsequent plays, seeks,
 * or queue switches retrieve gain data instantaneously with zero disk/file I/O.
 */
@Singleton
class ReplayGainCache @Inject constructor() {

    private val maxEntries = 1000

    private val cache: MutableMap<Long, ReplayGainInfo> = Collections.synchronizedMap(
        object : LinkedHashMap<Long, ReplayGainInfo>(128, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, ReplayGainInfo>?): Boolean {
                return size > maxEntries
            }
        }
    )

    /**
     * Retrieves cached ReplayGain info for [songId], or null if not yet cached.
     */
    fun get(songId: Long): ReplayGainInfo? = cache[songId]

    /**
     * Stores [info] in cache for [songId].
     */
    fun put(songId: Long, info: ReplayGainInfo) {
        cache[songId] = info
    }

    /**
     * Clears all cached ReplayGain metadata.
     */
    fun clear() {
        cache.clear()
    }
}
