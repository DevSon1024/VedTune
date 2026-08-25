package com.devson.vedtune.ui.folders

import android.content.Context
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.domain.repository.MediaRepository
import com.devson.vedtune.player.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class FolderSortBy {
    NAME, TRACK_COUNT, PATH
}

enum class FolderSortOrder {
    ASCENDING, DESCENDING
}

data class FolderItem(
    val name: String,
    val path: String,
    val songs: List<Song>
) {
    val songCount: Int get() = songs.size
}

data class FoldersUiState(
    val folders: List<FolderItem> = emptyList(),
    val selectedFolder: FolderItem? = null,
    val sortBy: FolderSortBy = FolderSortBy.NAME,
    val sortOrder: FolderSortOrder = FolderSortOrder.ASCENDING,
    val isLoading: Boolean = true
)

@HiltViewModel
class FoldersViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MediaRepository,
    private val playbackConnection: PlaybackConnection
) : ViewModel() {

    private val _sortBy = MutableStateFlow(FolderSortBy.NAME)
    val sortBy: StateFlow<FolderSortBy> = _sortBy

    private val _sortOrder = MutableStateFlow(FolderSortOrder.ASCENDING)
    val sortOrder: StateFlow<FolderSortOrder> = _sortOrder

    private val _selectedFolderPath = MutableStateFlow<String?>(null)
    val selectedFolderPath: StateFlow<String?> = _selectedFolderPath

    val currentSongId: StateFlow<Long?> = playbackConnection.currentSongId
    val isPlaying: StateFlow<Boolean> = playbackConnection.isPlaying

    val uiState: StateFlow<FoldersUiState> = combine(
        repository.getAllSongs(),
        _sortBy,
        _sortOrder,
        _selectedFolderPath
    ) { allSongs, sortBy, sortOrder, selectedPath ->
        val songMap = allSongs.associateBy { it.id }
        val songFolderMap = getSongFolderMap()

        // Group songs by folder path
        val folderGroups = mutableMapOf<String, MutableList<Song>>()
        for (song in allSongs) {
            val folderPath = songFolderMap[song.id] ?: "/Music"
            folderGroups.getOrPut(folderPath) { mutableListOf() }.add(song)
        }

        val folderItems = folderGroups.map { (path, songs) ->
            val name = path.substringAfterLast('/', path.substringAfterLast('\\', "Music")).ifBlank { "Music" }
            FolderItem(
                name = name,
                path = path,
                songs = songs
            )
        }

        val sortedFolders = when (sortBy) {
            FolderSortBy.NAME -> if (sortOrder == FolderSortOrder.ASCENDING) folderItems.sortedBy { it.name.lowercase() } else folderItems.sortedByDescending { it.name.lowercase() }
            FolderSortBy.TRACK_COUNT -> if (sortOrder == FolderSortOrder.ASCENDING) folderItems.sortedBy { it.songCount } else folderItems.sortedByDescending { it.songCount }
            FolderSortBy.PATH -> if (sortOrder == FolderSortOrder.ASCENDING) folderItems.sortedBy { it.path.lowercase() } else folderItems.sortedByDescending { it.path.lowercase() }
        }

        val selectedFolder = selectedPath?.let { path ->
            sortedFolders.find { it.path == path }
        }

        FoldersUiState(
            folders = sortedFolders,
            selectedFolder = selectedFolder,
            sortBy = sortBy,
            sortOrder = sortOrder,
            isLoading = false
        )
    }.flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FoldersUiState(isLoading = true)
    )

    private suspend fun getSongFolderMap(): Map<Long, String> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<Long, String>()
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA)
        val selection = "${MediaStore.Audio.Media.IS_ALARM} == 0 AND ${MediaStore.Audio.Media.IS_RINGTONE} == 0 AND ${MediaStore.Audio.Media.IS_NOTIFICATION} == 0"
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val data = cursor.getString(dataCol)
                if (data != null) {
                    val folder = data.substringBeforeLast('/')
                    if (folder.isNotBlank()) {
                        map[id] = folder
                    }
                }
            }
        }
        map
    }

    fun selectFolder(folderPath: String?) {
        _selectedFolderPath.value = folderPath
    }

    fun setSortBy(sortBy: FolderSortBy) {
        _sortBy.value = sortBy
    }

    fun toggleSortOrder() {
        _sortOrder.value = if (_sortOrder.value == FolderSortOrder.ASCENDING) {
            FolderSortOrder.DESCENDING
        } else {
            FolderSortOrder.ASCENDING
        }
    }

    fun playSongInFolder(song: Song, folder: FolderItem) {
        playbackConnection.playSong(song, folder.songs)
    }

    fun playAllInFolder(folder: FolderItem) {
        if (folder.songs.isNotEmpty()) {
            playbackConnection.playSong(folder.songs.first(), folder.songs)
        }
    }

    fun shuffleAllInFolder(folder: FolderItem) {
        if (folder.songs.isNotEmpty()) {
            playbackConnection.playShuffle(folder.songs.random(), folder.songs)
        }
    }

    fun playShuffleAll() {
        val allSongs = uiState.value.folders.flatMap { it.songs }
        if (allSongs.isNotEmpty()) {
            playbackConnection.playShuffle(allSongs.random(), allSongs)
        }
    }
}
