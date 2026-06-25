package com.devson.vedtune.ui.songs

import androidx.lifecycle.viewModelScope
import com.devson.vedtune.core.BaseViewModel
import com.devson.vedtune.domain.model.Song
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    val playlists: StateFlow<List<Playlist>> = repository.getAllPlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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

        when (sortBy) {
            SortBy.TITLE -> if (sortOrder == SortOrder.ASCENDING) filtered.sortedBy { it.title.lowercase() } else filtered.sortedByDescending { it.title.lowercase() }
            SortBy.ARTIST -> if (sortOrder == SortOrder.ASCENDING) filtered.sortedBy { it.artist.lowercase() } else filtered.sortedByDescending { it.artist.lowercase() }
            SortBy.ALBUM -> if (sortOrder == SortOrder.ASCENDING) filtered.sortedBy { it.album.lowercase() } else filtered.sortedByDescending { it.album.lowercase() }
            SortBy.DATE_ADDED -> if (sortOrder == SortOrder.ASCENDING) filtered.sortedBy { it.dateAdded } else filtered.sortedByDescending { it.dateAdded }
        }
    }.flowOn(Dispatchers.Default)

    init {
        viewModelScope.launch {
            songsFlow.collect { filteredSongs ->
                val totalCount = filteredSongs.size
                val totalDuration = filteredSongs.sumOf { it.duration }
                updateState {
                    it.copy(
                        songs = filteredSongs,
                        isLoading = false,
                        totalItemCount = totalCount,
                        totalDurationMs = totalDuration
                    )
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.showAlbumArt.collect { show ->
                updateState {
                    it.copy(showArtwork = show)
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
        val newGridView = !currentState.isGridView
        updateState { it.copy(isGridView = newGridView) }
    }

    fun playSong(song: Song) {
        playbackConnection.playSong(song, currentState.songs)
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

    fun playNext(song: Song) {
        playbackConnection.playNext(song)
    }

    fun playShuffle(song: Song) {
        playbackConnection.playShuffle(song, currentState.songs)
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
}

