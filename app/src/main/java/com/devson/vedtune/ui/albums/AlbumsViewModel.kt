package com.devson.vedtune.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtune.domain.model.Album
import com.devson.vedtune.domain.repository.MediaRepository
import com.devson.vedtune.player.PlaybackConnection
import com.devson.vedtune.ui.songs.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.devson.vedtune.domain.repository.SettingsRepository

enum class AlbumSortBy {
    TITLE, ARTIST, SONG_COUNT
}

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val playbackConnection: PlaybackConnection,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val showAlbumArt: StateFlow<Boolean> = settingsRepository.showAlbumArt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortBy = MutableStateFlow(AlbumSortBy.TITLE)
    val sortBy: StateFlow<AlbumSortBy> = _sortBy

    private val _sortOrder = MutableStateFlow(SortOrder.ASCENDING)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView

    val albums: StateFlow<List<Album>> = combine(
        repository.getAllAlbums(),
        _searchQuery,
        _sortBy,
        _sortOrder
    ) { albums, query, sortBy, sortOrder ->
        var filtered = albums
        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true)
            }
        }
        val sorted = when (sortBy) {
            AlbumSortBy.TITLE -> {
                if (sortOrder == SortOrder.ASCENDING) filtered.sortedBy { it.title.lowercase() }
                else filtered.sortedByDescending { it.title.lowercase() }
            }
            AlbumSortBy.ARTIST -> {
                if (sortOrder == SortOrder.ASCENDING) filtered.sortedBy { it.artist.lowercase() }
                else filtered.sortedByDescending { it.artist.lowercase() }
            }
            AlbumSortBy.SONG_COUNT -> {
                if (sortOrder == SortOrder.ASCENDING) filtered.sortedBy { it.songCount }
                else filtered.sortedByDescending { it.songCount }
            }
        }

        sorted
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalItemCount: StateFlow<Int> = albums
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalDurationMs: StateFlow<Long> = combine(
        albums,
        repository.getAllSongs()
    ) { filteredAlbums, allSongs ->
        val filteredAlbumIds = filteredAlbums.map { it.id }.toSet()
        allSongs.filter { it.albumId in filteredAlbumIds }.sumOf { it.duration }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortBy(sortBy: AlbumSortBy) {
        _sortBy.value = sortBy
    }

    fun toggleSortOrder() {
        _sortOrder.value = if (_sortOrder.value == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
    }

    fun toggleLayoutView() {
        _isGridView.value = !_isGridView.value
    }

    fun playShuffleAll() {
        viewModelScope.launch {
            val currentAlbums = albums.value.map { it.id }.toSet()
            if (currentAlbums.isNotEmpty()) {
                val allSongs = repository.getAllSongs().first()
                val filteredSongs = allSongs.filter { it.albumId in currentAlbums }
                if (filteredSongs.isNotEmpty()) {
                    playbackConnection.playShuffle(filteredSongs.random(), filteredSongs)
                }
            }
        }
    }
}
