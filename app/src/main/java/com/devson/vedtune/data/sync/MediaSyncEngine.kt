package com.devson.vedtune.data.sync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.devson.vedtune.data.local.dao.SongDao
import com.devson.vedtune.data.local.entity.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Synchronization engine that coordinates incremental syncing between Android MediaStore
 * and Room local cache. Optimized for large libraries (50,000+ songs).
 */
@Singleton
class MediaSyncEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao
) {

    suspend fun performSync() = withContext(Dispatchers.IO) {
        if (!hasStoragePermission()) return@withContext

        // 1. Fetch current songs in Room (ID and dateModified)
        val roomSongs = songDao.getSongIdAndModifiedMap()
        val roomSongsMap = roomSongs.associate { it.id to it.dateModified }

        // 2. Fetch MediaStore songs (ID and dateModified)
        val mediaStoreSongsMap = mutableMapOf<Long, Long>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATE_MODIFIED
        )

        // Query only music files
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val dateModified = cursor.getLong(dateModifiedCol)
                mediaStoreSongsMap[id] = dateModified
            }
        }

        // 3. Determine delta (inserts/updates and deletions)
        val toDeleteIds = roomSongsMap.keys.filter { id -> !mediaStoreSongsMap.containsKey(id) }
        val toFetchIds = mediaStoreSongsMap.filter { (id, dateModified) ->
            val cachedModified = roomSongsMap[id]
            cachedModified == null || dateModified > cachedModified
        }.keys.toList()

        if (toDeleteIds.isEmpty() && toFetchIds.isEmpty()) {
            return@withContext // Up to date!
        }

        // 4. Fetch full metadata of new/modified IDs in chunks of 500 (avoiding SQLite parameter limits)
        val toInsertEntities = mutableListOf<SongEntity>()
        if (toFetchIds.isNotEmpty()) {
            val chunkedIds = toFetchIds.chunked(500)
            for (chunk in chunkedIds) {
                val fullEntities = fetchFullMetadataForIds(chunk)
                toInsertEntities.addAll(fullEntities)
            }
        }

        // 5. Execute database sync in a single transaction
        songDao.syncMediaStore(toInsertEntities, toDeleteIds)
    }

    private fun fetchFullMetadataForIds(ids: List<Long>): List<SongEntity> {
        if (ids.isEmpty()) return emptyList()

        val entities = mutableListOf<SongEntity>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED
        )

        val idsPlaceholder = ids.joinToString(",") { it.toString() }
        val selection = "${MediaStore.Audio.Media._ID} IN ($idsPlaceholder)"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Unknown"
                val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                val album = cursor.getString(albumCol) ?: "Unknown Album"
                val albumId = cursor.getLong(albumIdCol)
                val duration = cursor.getLong(durationCol)
                val track = cursor.getInt(trackCol)
                val year = cursor.getInt(yearCol)
                val dateAdded = cursor.getLong(dateAddedCol)
                val dateModified = cursor.getLong(dateModifiedCol)

                entities.add(
                    SongEntity(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        albumId = albumId,
                        duration = duration,
                        track = track,
                        year = year,
                        dateAdded = dateAdded,
                        dateModified = dateModified
                    )
                )
            }
        }
        return entities
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
