package com.devson.vedtune.data.sync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.devson.vedtune.data.local.dao.SongDao
import com.devson.vedtune.data.local.entity.SongEntity
import com.devson.vedtune.domain.model.FolderFilterMode
import com.devson.vedtune.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Synchronization engine that coordinates incremental syncing between Android MediaStore
 * and the Room local cache. Optimized for large libraries (50,000+ songs).
 *
 * Folder filtering:
 *   - NONE      → all audio files are included (default behaviour).
 *   - WHITELIST → only songs whose folder path is in the whitelist are kept.
 *                 An empty whitelist produces an empty library.
 *   - BLACKLIST → songs whose folder path is in the blacklist are excluded.
 *                 All other songs remain visible.
 */
@Singleton
class MediaSyncEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val settingsRepository: SettingsRepository
) {

    private val syncMutex = kotlinx.coroutines.sync.Mutex()

    suspend fun performSync() = withContext(Dispatchers.IO) {
        if (!hasStoragePermission()) return@withContext
        if (!syncMutex.tryLock()) return@withContext // Skip concurrent sync runs

        try {
            //  Read active folder filter settings 
            val filterMode = settingsRepository.folderFilterMode.first()
            val blacklist = settingsRepository.blacklistedFolders.first()
            val whitelist = settingsRepository.whitelistedFolders.first()

        //  1. Fetch current songs in Room (ID and dateModified) 
        val roomSongs = songDao.getSongIdAndModifiedMap()
        val roomSongsMap = roomSongs.associate { it.id to it.dateModified }

        //  2. Fetch MediaStore songs (ID, dateModified, DATA path) 
        //       We need DATA to apply the folder filter efficiently.
        val mediaStoreSongsMap = mutableMapOf<Long, Long>()   // id → dateModified
        val mediaStoreDataMap = mutableMapOf<Long, String>()  // id → file path

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DATA
        )
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
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val dateModified = cursor.getLong(dateModifiedCol)
                val filePath = cursor.getString(dataCol) ?: continue
                mediaStoreSongsMap[id] = dateModified
                mediaStoreDataMap[id] = filePath
            }
        }

        //  3. Apply folder filter 
        val filteredMediaStoreMap: Map<Long, Long> = when (filterMode) {
            FolderFilterMode.NONE -> mediaStoreSongsMap

            FolderFilterMode.WHITELIST -> {
                if (whitelist.isEmpty()) {
                    // Empty whitelist → show nothing
                    emptyMap()
                } else {
                    mediaStoreSongsMap.filter { (id, _) ->
                        val folder = mediaStoreDataMap[id]?.substringBeforeLast('/') ?: return@filter false
                        whitelist.any { whitelisted -> folder == whitelisted || folder.startsWith("$whitelisted/") }
                    }
                }
            }

            FolderFilterMode.BLACKLIST -> {
                if (blacklist.isEmpty()) {
                    mediaStoreSongsMap
                } else {
                    mediaStoreSongsMap.filter { (id, _) ->
                        val folder = mediaStoreDataMap[id]?.substringBeforeLast('/') ?: return@filter true
                        blacklist.none { blacklisted -> folder == blacklisted || folder.startsWith("$blacklisted/") }
                    }
                }
            }
        }

        //  4. Determine delta (inserts/updates and deletions) 
        val toDeleteIds = roomSongsMap.keys.filter { id -> !filteredMediaStoreMap.containsKey(id) }
        val toFetchIds = filteredMediaStoreMap.filter { (id, dateModified) ->
            val cachedModified = roomSongsMap[id]
            cachedModified == null || dateModified > cachedModified
        }.keys.toList()

        if (toDeleteIds.isEmpty() && toFetchIds.isEmpty()) {
            return@withContext // Up to date!
        }

        //  5. Fetch full metadata of new/modified IDs in chunks of 500 
        val toInsertEntities = mutableListOf<SongEntity>()
        if (toFetchIds.isNotEmpty()) {
            val chunkedIds = toFetchIds.chunked(500)
            for (chunk in chunkedIds) {
                val fullEntities = fetchFullMetadataForIds(chunk)
                toInsertEntities.addAll(fullEntities)
            }
        }

        //  6. Execute database sync in a single transaction 
        songDao.syncMediaStore(toInsertEntities, toDeleteIds)
        } finally {
            syncMutex.unlock()
        }
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
