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

@HiltViewModel
class SongsViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val playbackConnection: PlaybackConnection
) : BaseViewModel<SongsUiState, SongsUiEvent>(SongsUiState(isLoading = true)) {

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
                updateState {
                    it.copy(
                        songs = filteredSongs,
                        isLoading = false
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
}

