package com.devson.vedtune.ui.queue

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtune.core.formatDuration
import com.devson.vedtune.domain.model.Playlist
import com.devson.vedtune.domain.model.QueueInfo
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.domain.repository.MediaRepository
import com.devson.vedtune.domain.repository.SettingsRepository
import com.devson.vedtune.player.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

enum class QueueSortMode {
    MANUAL,
    TITLE_ASC,
    TITLE_DESC,
    ARTIST_ASC,
    ALBUM_ASC,
    DURATION_ASC,
    DURATION_DESC
}

data class SuperQueueUiState(
    val queues: List<QueueInfo> = emptyList(),
    val selectedQueueId: Long = 1L,
    val selectedQueueName: String = "All Songs",
    val activeQueueId: Long = 1L,
    val songs: List<Song> = emptyList(),
    val filteredSongs: List<Song> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val sortMode: QueueSortMode = QueueSortMode.MANUAL,
    val isShuffleEnabled: Boolean = false,
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedSongIds: Set<Long> = emptySet(),
    val currentSongTrackNumber: Int = -1,
    val totalTracksCount: Int = 0,
    val totalDurationFormatted: String = "00:00",
    val playlists: List<Playlist> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SuperQueueViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val playbackConnection: PlaybackConnection,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedQueueId = MutableStateFlow(1L)
    val selectedQueueId: StateFlow<Long> = _selectedQueueId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _sortMode = MutableStateFlow(QueueSortMode.MANUAL)
    val sortMode: StateFlow<QueueSortMode> = _sortMode.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedSongIds: StateFlow<Set<Long>> = _selectedSongIds.asStateFlow()

    val queues: StateFlow<List<QueueInfo>> = repository.getAllQueues()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeQueueId: StateFlow<Long> = playbackConnection.activeQueueId

    val currentSong: StateFlow<Song?> = playbackConnection.currentSong
    val isPlaying: StateFlow<Boolean> = playbackConnection.isPlaying
    val isShuffleEnabled: StateFlow<Boolean> = playbackConnection.shuffleModeEnabled

    val playlists: StateFlow<List<Playlist>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tracks in currently selected queue
    private val queueSongsFlow = _selectedQueueId.flatMapLatest { qId ->
        repository.getQueueSongs(qId)
    }

    val uiState: StateFlow<SuperQueueUiState> = combine(
        queues,
        _selectedQueueId,
        activeQueueId,
        queueSongsFlow,
        _searchQuery,
        _isSearchActive,
        _sortMode,
        currentSong,
        isPlaying,
        _isSelectionMode,
        _selectedSongIds,
        playlists,
        playbackConnection.shuffleModeEnabled
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val queueList = args[0] as List<QueueInfo>
        val selId = args[1] as Long
        val actId = args[2] as Long
        @Suppress("UNCHECKED_CAST")
        val rawSongs = args[3] as List<Song>
        val query = args[4] as String
        val searchActive = args[5] as Boolean
        val sMode = args[6] as QueueSortMode
        val curSong = args[7] as? Song
        val playing = args[8] as Boolean
        val selMode = args[9] as Boolean
        @Suppress("UNCHECKED_CAST")
        val selIds = args[10] as Set<Long>
        @Suppress("UNCHECKED_CAST")
        val plist = args[11] as List<Playlist>
        val shuffleOn = args[12] as Boolean

        val selectedQueue = queueList.firstOrNull { it.id == selId }
        val qName = selectedQueue?.name ?: if (selId == 1L) QueueInfo.DEFAULT_QUEUE_NAME else "Queue $selId"

        // Sorted songs - when shuffle is enabled, sorting cannot be applied; original queue/shuffle order is preserved
        val sortedSongs = if (shuffleOn) {
            rawSongs
        } else {
            when (sMode) {
                QueueSortMode.MANUAL -> rawSongs
                QueueSortMode.TITLE_ASC -> rawSongs.sortedBy { it.title.lowercase() }
                QueueSortMode.TITLE_DESC -> rawSongs.sortedByDescending { it.title.lowercase() }
                QueueSortMode.ARTIST_ASC -> rawSongs.sortedBy { it.artist.lowercase() }
                QueueSortMode.ALBUM_ASC -> rawSongs.sortedBy { it.album.lowercase() }
                QueueSortMode.DURATION_ASC -> rawSongs.sortedBy { it.duration }
                QueueSortMode.DURATION_DESC -> rawSongs.sortedByDescending { it.duration }
            }
        }

        // Filtered songs
        val filtered = if (query.isNotBlank() && searchActive) {
            sortedSongs.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
            }
        } else {
            sortedSongs
        }

        val totalDurationSeconds = rawSongs.sumOf { it.duration }
        val formattedDuration = formatDuration(totalDurationSeconds)

        val trackNumber = if (curSong != null && selId == actId) {
            val idx = rawSongs.indexOfFirst { it.id == curSong.id }
            if (idx != -1) idx + 1 else -1
        } else {
            -1
        }

        SuperQueueUiState(
            queues = queueList,
            selectedQueueId = selId,
            selectedQueueName = qName,
            activeQueueId = actId,
            songs = sortedSongs,
            filteredSongs = filtered,
            searchQuery = query,
            isSearchActive = searchActive,
            sortMode = sMode,
            isShuffleEnabled = shuffleOn,
            currentSong = curSong,
            isPlaying = playing,
            isSelectionMode = selMode,
            selectedSongIds = selIds,
            currentSongTrackNumber = trackNumber,
            totalTracksCount = rawSongs.size,
            totalDurationFormatted = formattedDuration,
            playlists = plist
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SuperQueueUiState()
    )

    init {
        // Sync selected queue with active queue on launch
        viewModelScope.launch {
            val currentActive = playbackConnection.activeQueueId.first()
            _selectedQueueId.value = if (currentActive != QueueInfo.LOCAL_QUEUE_ID) currentActive else QueueInfo.DEFAULT_QUEUE_ID
        }
    }

    fun selectQueue(queueId: Long) {
        _selectedQueueId.value = queueId
        clearSelection()
        _searchQuery.value = ""
        _isSearchActive.value = false
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    fun setSortMode(mode: QueueSortMode) {
        if (playbackConnection.shuffleModeEnabled.value) return
        _sortMode.value = mode
    }

    fun playAllInQueue() {
        val currentQueueId = _selectedQueueId.value
        val songs = uiState.value.songs
        if (songs.isNotEmpty()) {
            playbackConnection.playSong(songs.first(), songs, targetQueueId = currentQueueId)
        }
    }

    fun playSong(song: Song) {
        val currentQueueId = _selectedQueueId.value
        val songs = uiState.value.songs
        playbackConnection.playSong(song, songs, targetQueueId = currentQueueId)
    }

    fun playSongAt(index: Int) {
        val currentQueueId = _selectedQueueId.value
        val songs = uiState.value.filteredSongs
        if (index in songs.indices) {
            val targetSong = songs[index]
            val activeId = playbackConnection.activeQueueId.value
            if (currentQueueId == activeId && !playbackConnection.shuffleModeEnabled.value) {
                playbackConnection.playQueueItemById(targetSong.id)
            } else {
                playbackConnection.playSong(targetSong, songs, targetQueueId = currentQueueId)
            }
        }
    }

    fun playNext(song: Song) {
        playbackConnection.playNext(song)
    }

    fun reorderSongs(from: Int, to: Int) {
        viewModelScope.launch {
            val currentList = uiState.value.songs.toMutableList()
            if (from !in currentList.indices || to !in currentList.indices || from == to) return@launch
            val item = currentList.removeAt(from)
            currentList.add(to, item)

            val currentQueueId = _selectedQueueId.value
            val activeId = playbackConnection.activeQueueId.value

            withContext(Dispatchers.IO) {
                repository.saveQueueSongs(currentQueueId, currentList)
            }

            if (currentQueueId == activeId) {
                playbackConnection.moveQueueItem(from, to)
            }
        }
    }

    fun removeSongFromQueue(song: Song, index: Int) {
        viewModelScope.launch {
            val currentQueueId = _selectedQueueId.value
            val activeId = playbackConnection.activeQueueId.value

            withContext(Dispatchers.IO) {
                repository.removeSongFromQueue(currentQueueId, song.id)
            }

            if (currentQueueId == activeId) {
                playbackConnection.removeQueueItem(index)
            }
        }
    }

    fun createNewQueue(name: String, songsToAdd: List<Song> = emptyList()) {
        viewModelScope.launch {
            val newId = withContext(Dispatchers.IO) {
                val qId = repository.createQueue(name)
                if (songsToAdd.isNotEmpty()) {
                    repository.addSongsToQueue(qId, songsToAdd.map { it.id })
                }
                qId
            }
            selectQueue(newId)
        }
    }

    fun renameQueue(queueId: Long, newName: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.renameQueue(queueId, newName)
            }
        }
    }

    fun deleteQueue(queueId: Long) {
        viewModelScope.launch {
            val all = queues.value
            withContext(Dispatchers.IO) {
                repository.deleteQueue(queueId)
            }
            if (_selectedQueueId.value == queueId) {
                val remaining = all.filter { it.id != queueId }
                val nextTarget = remaining.firstOrNull()?.id ?: 1L
                selectQueue(nextTarget)
            }
            if (playbackConnection.activeQueueId.value == queueId) {
                playbackConnection.setActiveQueueId(1L)
                playbackConnection.clearQueue()
            }
        }
    }

    fun removeAllOtherQueues(keepQueueId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.removeAllOtherQueues(keepQueueId)
            }
            selectQueue(keepQueueId)
            playbackConnection.setActiveQueueId(keepQueueId)
        }
    }

    fun reorderQueues(orderedQueueIds: List<Long>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.reorderQueues(orderedQueueIds)
            }
        }
    }

    fun switchActiveQueue(queueId: Long, autoPlay: Boolean = false) {
        playbackConnection.switchActiveQueue(queueId, autoPlay = autoPlay)
        selectQueue(queueId)
    }

    fun addSongsToQueue(queueId: Long, songs: List<Song>, playNext: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.addSongsToQueue(queueId, songs.map { it.id }, atBeginning = playNext)
            }
            val activeId = playbackConnection.activeQueueId.value
            if (queueId == activeId && playNext && songs.size == 1) {
                playbackConnection.playNext(songs.first())
            }
        }
    }

    suspend fun getQueueSongIds(queueId: Long): List<Long> {
        return repository.getQueueSongIds(queueId)
    }

    fun saveQueueAsPlaylist(playlistName: String) {
        viewModelScope.launch {
            val currentSongs = uiState.value.songs
            if (currentSongs.isEmpty()) return@launch
            withContext(Dispatchers.IO) {
                val playlistId = repository.createPlaylist(playlistName.ifBlank { "Saved Queue" })
                for (song in currentSongs) {
                    repository.addSongToPlaylist(playlistId, song.id)
                }
            }
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun createPlaylistAndAddSong(playlistName: String, songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.createPlaylist(playlistName)
            repository.addSongToPlaylist(id, songId)
        }
    }

    // Selection mode
    fun toggleSelectionMode() {
        val next = !_isSelectionMode.value
        _isSelectionMode.value = next
        if (!next) {
            _selectedSongIds.value = emptySet()
        }
    }

    fun toggleSongSelection(songId: Long) {
        val current = _selectedSongIds.value.toMutableSet()
        if (current.contains(songId)) {
            current.remove(songId)
        } else {
            current.add(songId)
        }
        _selectedSongIds.value = current
        if (current.isEmpty()) {
            _isSelectionMode.value = false
        } else {
            _isSelectionMode.value = true
        }
    }

    fun selectAll() {
        _selectedSongIds.value = uiState.value.songs.map { it.id }.toSet()
        _isSelectionMode.value = true
    }

    fun clearSelection() {
        _selectedSongIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun removeSelectedSongs() {
        viewModelScope.launch {
            val toRemove = _selectedSongIds.value
            val currentQueueId = _selectedQueueId.value
            val remainingSongs = uiState.value.songs.filter { it.id !in toRemove }
            withContext(Dispatchers.IO) {
                repository.saveQueueSongs(currentQueueId, remainingSongs)
            }
            if (currentQueueId == playbackConnection.activeQueueId.value) {
                // Refresh playback queue
                playbackConnection.playQueue(currentQueueId, startIndex = 0)
            }
            clearSelection()
        }
    }

    // Export .M3U
    fun exportQueueAsM3u(context: Context) {
        val songs = uiState.value.songs
        if (songs.isEmpty()) {
            Toast.makeText(context, "Queue is empty", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val queueName = uiState.value.selectedQueueName.replace("[^a-zA-Z0-9_.-]".toRegex(), "_")
                val fileName = "$queueName.m3u"
                val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val m3uFile = File(exportDir, fileName)

                val content = buildString {
                    append("#EXTM3U\n")
                    for (s in songs) {
                        append("#EXTINF:${s.duration / 1000},${s.artist} - ${s.title}\n")
                        append("content://media/external/audio/media/${s.id}\n")
                    }
                }
                m3uFile.writeText(content)

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    m3uFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "audio/x-mpegurl"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Queue Export: ${uiState.value.selectedQueueName}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                withContext(Dispatchers.Main) {
                    context.startActivity(Intent.createChooser(shareIntent, "Export Queue as .M3U"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Share songs
    fun shareSongs(context: Context, songsToShare: List<Song>) {
        if (songsToShare.isEmpty()) return
        val uris = ArrayList<Uri>()
        for (song in songsToShare) {
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
            uris.add(uri)
        }
        val intent = Intent().apply {
            action = if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE
            type = "audio/*"
            if (uris.size == 1) {
                putExtra(Intent.EXTRA_STREAM, uris.first())
            } else {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share audio"))
    }
}
