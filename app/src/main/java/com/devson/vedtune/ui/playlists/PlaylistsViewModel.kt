package com.devson.vedtune.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtune.domain.model.Playlist
import com.devson.vedtune.domain.repository.MediaRepository
import com.devson.vedtune.player.PlaybackConnection
import com.devson.vedtune.ui.songs.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PlaylistSortBy {
    NAME, SONG_COUNT, DATE_CREATED
}

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val playbackConnection: PlaybackConnection
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortBy = MutableStateFlow(PlaylistSortBy.NAME)
    val sortBy: StateFlow<PlaylistSortBy> = _sortBy.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.ASCENDING)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    val playlists: StateFlow<List<Playlist>> = repository.getAllPlaylists()
        .combine(searchQuery) { list, query ->
            if (query.isBlank()) {
                list
            } else {
                list.filter { it.name.contains(query, ignoreCase = true) }
            }
        }
        .combine(combine(sortBy, sortOrder) { by, order -> Pair(by, order) }) { filtered, (sortBy, sortOrder) ->
            when (sortBy) {
                PlaylistSortBy.NAME -> {
                    if (sortOrder == SortOrder.ASCENDING) filtered.sortedBy { it.name.lowercase() }
                    else filtered.sortedByDescending { it.name.lowercase() }
                }
                PlaylistSortBy.SONG_COUNT -> {
                    if (sortOrder == SortOrder.ASCENDING) filtered.sortedBy { it.songCount }
                    else filtered.sortedByDescending { it.songCount }
                }
                PlaylistSortBy.DATE_CREATED -> {
                    if (sortOrder == SortOrder.ASCENDING) filtered.sortedBy { it.createdAt }
                    else filtered.sortedByDescending { it.createdAt }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalItemCount: StateFlow<Int> = playlists
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortBy(sortBy: PlaylistSortBy) {
        _sortBy.value = sortBy
    }

    fun toggleSortOrder() {
        _sortOrder.value = if (_sortOrder.value == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
    }

    fun toggleLayoutView() {
        _isGridView.value = !_isGridView.value
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun playShuffleAll() {
        viewModelScope.launch {
            val allSongs = repository.getAllSongs().first()
            if (allSongs.isNotEmpty()) {
                playbackConnection.playShuffle(allSongs.random(), allSongs)
            }
        }
    }
}
