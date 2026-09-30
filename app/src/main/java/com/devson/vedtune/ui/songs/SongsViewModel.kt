package com.devson.vedtune.ui.songs

import androidx.lifecycle.viewModelScope
import com.devson.vedtune.core.BaseViewModel
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.domain.model.ViewPreferences
import com.devson.vedtune.domain.repository.MediaRepository
import com.devson.vedtune.player.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.devson.vedtune.domain.model.Playlist
import com.devson.vedtune.domain.model.QueueInfo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.devson.vedtune.domain.repository.SettingsRepository
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@HiltViewModel
class SongsViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val playbackConnection: PlaybackConnection,
    private val settingsRepository: SettingsRepository
) : BaseViewModel<SongsUiState, SongsUiEvent>(SongsUiState(isLoading = true)) {

    val currentSongId: StateFlow<Long?> = playbackConnection.currentSongId
    val isPlaying: StateFlow<Boolean> = playbackConnection.isPlaying
    val playbackPosition: StateFlow<Long> = playbackConnection.playbackPosition
    val playbackDuration: StateFlow<Long> = playbackConnection.playbackDuration

    fun play() {
        playbackConnection.play()
    }

    fun pause() {
        playbackConnection.pause()
    }

    fun seekTo(positionMs: Long) {
        playbackConnection.seekTo(positionMs)
    }

    val playlists: StateFlow<List<Playlist>> = repository.getAllPlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val queues: StateFlow<List<QueueInfo>> = repository.getAllQueues()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addSongsToQueue(queueId: Long, songIds: List<Long>, playNext: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addSongsToQueue(queueId, songIds, atBeginning = playNext)
        }
    }

    fun createQueueAndAddSongs(name: String, songIds: List<Long>, playNext: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val qId = repository.createQueue(name)
            repository.addSongsToQueue(qId, songIds, atBeginning = playNext)
        }
    }

    suspend fun getQueueSongIds(queueId: Long): List<Long> {
        return repository.getQueueSongIds(queueId)
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun createPlaylistAndAddSong(playlistName: String, songId: Long) {
        viewModelScope.launch {
            val playlistId = repository.createPlaylist(playlistName)
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    private val _searchQuery = MutableStateFlow("")
    private val _sortBy = MutableStateFlow(SortBy.TITLE)
    private val _sortOrder = MutableStateFlow(SortOrder.ASCENDING)

    private val songsFlow = combine(
        repository.getAllSongs(),
        _searchQuery,
        _sortBy,
        _sortOrder
    ) { songs, query, sortBy, sortOrder ->
        var filtered = songs
        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
            }
        }

        val sorted = when (sortBy) {
            SortBy.TITLE -> if (sortOrder == SortOrder.ASCENDING) filtered.sortedBy { it.title.lowercase() } else filtered.sortedByDescending { it.title.lowercase() }
            SortBy.ARTIST -> if (sortOrder == SortOrder.ASCENDING) filtered.sortedBy { it.artist.lowercase() } else filtered.sortedByDescending { it.artist.lowercase() }
            SortBy.ALBUM -> if (sortOrder == SortOrder.ASCENDING) filtered.sortedBy { it.album.lowercase() } else filtered.sortedByDescending { it.album.lowercase() }
            SortBy.DATE_ADDED -> if (sortOrder == SortOrder.ASCENDING) filtered.sortedBy { it.dateAdded } else filtered.sortedByDescending { it.dateAdded }
            SortBy.DURATION -> if (sortOrder == SortOrder.ASCENDING) filtered.sortedBy { it.duration } else filtered.sortedByDescending { it.duration }
        }

        sorted
    }.flowOn(Dispatchers.Default)

    init {
        viewModelScope.launch {
            songsFlow.collect { sortedSongs ->
                val totalCount = sortedSongs.size
                val totalDuration = sortedSongs.sumOf { it.duration }
                updateState {
                    it.copy(
                        songs = sortedSongs,
                        isLoading = false,
                        totalItemCount = totalCount,
                        totalDurationMs = totalDuration
                    )
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.viewPreferences.collect { prefs ->
                updateState {
                    it.copy(
                        viewPreferences = prefs,
                        isGridView = prefs.isGridView,
                        showArtwork = prefs.showAlbumArt
                    )
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        updateState { it.copy(searchQuery = query) }
    }

    fun setSortBy(sortBy: SortBy) {
        _sortBy.value = sortBy
        updateState { it.copy(sortBy = sortBy) }
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _sortOrder.value = sortOrder
        updateState { it.copy(sortOrder = sortOrder) }
    }

    fun toggleLayoutView() {
        viewModelScope.launch {
            val currentPrefs = currentState.viewPreferences
            settingsRepository.setViewPreferences(currentPrefs.copy(isGridView = !currentPrefs.isGridView))
        }
    }

    fun updateViewPreferences(preferences: ViewPreferences) {
        viewModelScope.launch {
            settingsRepository.setViewPreferences(preferences)
        }
    }

    fun playSong(song: Song) {
        playbackConnection.playSong(song, currentState.songs, targetQueueId = QueueInfo.DEFAULT_QUEUE_ID)
    }

    fun refresh() {
        viewModelScope.launch {
            updateState { it.copy(isRefreshing = true) }
            try {
                repository.synchronizeLibrary()
            } catch (e: Exception) {
                sendEvent(SongsUiEvent.ShowError(e.message ?: "Failed to sync library"))
            } finally {
                updateState { it.copy(isRefreshing = false) }
            }
        }
    }

    data class PendingTagUpdate(
        val song: Song,
        val title: String,
        val artist: String,
        val album: String,
        val track: Int,
        val year: Int,
        val customArtworkUri: Uri?
    )

    private var pendingTagUpdate: PendingTagUpdate? = null
    private var pendingDeleteSongId: Long? = null
    private val pendingDeleteSongIds = mutableSetOf<Long>()

    fun enterSelectionMode(initialSongId: Long) {
        updateState {
            it.copy(
                isSelectionMode = true,
                selectedSongIds = setOf(initialSongId)
            )
        }
    }

    fun toggleSongSelection(songId: Long) {
        updateState { state ->
            val updated = state.selectedSongIds.toMutableSet()
            if (updated.contains(songId)) {
                updated.remove(songId)
            } else {
                updated.add(songId)
            }
            state.copy(
                isSelectionMode = updated.isNotEmpty(),
                selectedSongIds = updated
            )
        }
    }

    fun selectAll() {
        updateState { state ->
            state.copy(
                isSelectionMode = true,
                selectedSongIds = state.songs.map { it.id }.toSet()
            )
        }
    }

    fun clearSelection() {
        updateState {
            it.copy(
                selectedSongIds = emptySet()
            )
        }
    }

    fun exitSelectionMode() {
        updateState {
            it.copy(
                isSelectionMode = false,
                selectedSongIds = emptySet()
            )
        }
    }

    fun playSelectedSongs() {
        val selectedSongs = currentState.songs.filter { it.id in currentState.selectedSongIds }
        if (selectedSongs.isNotEmpty()) {
            playbackConnection.playSong(selectedSongs.first(), selectedSongs, targetQueueId = QueueInfo.DEFAULT_QUEUE_ID)
            exitSelectionMode()
        }
    }

    fun playNextSelectedSongs() {
        val selectedSongs = currentState.songs.filter { it.id in currentState.selectedSongIds }
        selectedSongs.reversed().forEach { song ->
            playbackConnection.playNext(song)
        }
        exitSelectionMode()
    }

    fun addSelectedToPlaylist(playlistId: Long) {
        val selectedIds = currentState.selectedSongIds.toList()
        viewModelScope.launch {
            selectedIds.forEach { songId ->
                repository.addSongToPlaylist(playlistId, songId)
            }
            exitSelectionMode()
        }
    }

    fun createPlaylistAndAddSelected(playlistName: String) {
        val selectedIds = currentState.selectedSongIds.toList()
        viewModelScope.launch {
            val playlistId = repository.createPlaylist(playlistName)
            selectedIds.forEach { songId ->
                repository.addSongToPlaylist(playlistId, songId)
            }
            exitSelectionMode()
        }
    }

    fun deleteSelectedPermanently(context: Context) {
        val selectedIds = currentState.selectedSongIds.toList()
        if (selectedIds.isEmpty()) return

        val uris = selectedIds.map { songId ->
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
        }

        viewModelScope.launch {
            pendingDeleteSongIds.clear()
            pendingDeleteSongIds.addAll(selectedIds)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val pi = MediaStore.createDeleteRequest(context.contentResolver, uris)
                    sendEvent(SongsUiEvent.LaunchIntentSender(pi.intentSender))
                } else {
                    val deletedAny = withContext(Dispatchers.IO) {
                        var success = false
                        for (uri in uris) {
                            try {
                                if (context.contentResolver.delete(uri, null, null) > 0) {
                                    success = true
                                }
                            } catch (e: RecoverableSecurityException) {
                                sendEvent(SongsUiEvent.LaunchIntentSender(e.userAction.actionIntent.intentSender))
                            }
                        }
                        success
                    }
                    if (deletedAny) {
                        selectedIds.forEach { repository.deleteSong(it) }
                        exitSelectionMode()
                    }
                }
            } catch (e: Exception) {
                sendEvent(SongsUiEvent.ShowError(e.message ?: "Failed to delete selected songs"))
            }
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song.id)
        }
    }

    fun playNext(song: Song) {
        playbackConnection.playNext(song)
    }

    fun playShuffle(song: Song) {
        playbackConnection.playShuffle(song, currentState.songs)
    }

    fun playShuffleAll() {
        val songs = currentState.songs
        if (songs.isNotEmpty()) {
            val randomSong = songs.random()
            playShuffle(randomSong)
        }
    }

    fun deleteSongPermanently(context: Context, song: Song) {
        viewModelScope.launch {
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
            pendingDeleteSongId = song.id
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val pi = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                    sendEvent(SongsUiEvent.LaunchIntentSender(pi.intentSender))
                } else {
                    val deleted = withContext(Dispatchers.IO) {
                        try {
                            context.contentResolver.delete(uri, null, null) > 0
                        } catch (e: RecoverableSecurityException) {
                            sendEvent(SongsUiEvent.LaunchIntentSender(e.userAction.actionIntent.intentSender))
                            false
                        }
                    }
                    if (deleted) {
                        repository.deleteSong(song.id)
                    }
                }
            } catch (e: Exception) {
                sendEvent(SongsUiEvent.ShowError(e.message ?: "Failed to delete song"))
            }
        }
    }

    fun onDeletePermissionGranted() {
        pendingDeleteSongId?.let { songId ->
            viewModelScope.launch {
                repository.deleteSong(songId)
                pendingDeleteSongId = null
            }
        }
        if (pendingDeleteSongIds.isNotEmpty()) {
            val idsToDelete = pendingDeleteSongIds.toList()
            viewModelScope.launch {
                idsToDelete.forEach { repository.deleteSong(it) }
                pendingDeleteSongIds.clear()
                exitSelectionMode()
            }
        }
    }

    fun updateSongTags(
        context: Context,
        song: Song,
        title: String,
        artist: String,
        album: String,
        track: Int,
        year: Int,
        customArtworkUri: Uri?
    ) {
        val update = PendingTagUpdate(song, title, artist, album, track, year, customArtworkUri)
        pendingTagUpdate = update
        viewModelScope.launch {
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val pi = MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
                    sendEvent(SongsUiEvent.LaunchIntentSender(pi.intentSender))
                } else {
                    val success = executeTagUpdate(context, update)
                    if (success) {
                        pendingTagUpdate = null
                    }
                }
            } catch (e: Exception) {
                sendEvent(SongsUiEvent.ShowError(e.message ?: "Failed to update metadata"))
            }
        }
    }

    fun onWritePermissionGranted(context: Context) {
        pendingTagUpdate?.let { update ->
            viewModelScope.launch {
                executeTagUpdate(context, update)
                pendingTagUpdate = null
            }
        }
    }

    private suspend fun executeTagUpdate(context: Context, update: PendingTagUpdate): Boolean = withContext(Dispatchers.IO) {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, update.song.id)
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.TITLE, update.title)
            put(MediaStore.Audio.Media.ARTIST, update.artist)
            put(MediaStore.Audio.Media.ALBUM, update.album)
            put(MediaStore.Audio.Media.TRACK, update.track)
            put(MediaStore.Audio.Media.YEAR, update.year)
        }
        try {
            context.contentResolver.update(uri, values, null, null)
            
            // Save custom artwork if picked
            update.customArtworkUri?.let { artUri ->
                val dir = File(context.filesDir, "custom_artwork")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "${update.song.albumId}.jpg")
                context.contentResolver.openInputStream(artUri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                com.devson.vedtune.ui.components.ArtworkCache.addCustomArtwork(update.song.albumId)
            }
            
            repository.synchronizeLibrary()
            true
        } catch (e: RecoverableSecurityException) {
            sendEvent(SongsUiEvent.LaunchIntentSender(e.userAction.actionIntent.intentSender))
            false
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                sendEvent(SongsUiEvent.ShowError(e.message ?: "Failed to write metadata"))
            }
            false
        }
    }

    fun clearPlaybackHistory(songId: Long) {
        viewModelScope.launch {
            repository.clearPlaybackHistory(songId)
        }
    }
}

