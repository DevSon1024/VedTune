package com.devson.vedtune.ui.artists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtune.domain.model.Artist
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

enum class ArtistSortBy {
    NAME, SONG_COUNT, ALBUM_COUNT
}

@HiltViewModel
class ArtistsViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val playbackConnection: PlaybackConnection
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortBy = MutableStateFlow(ArtistSortBy.NAME)
    val sortBy: StateFlow<ArtistSortBy> = _sortBy

    private val _sortOrder = MutableStateFlow(SortOrder.ASCENDING)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView

    val artists: StateFlow<List<Artist>> = combine(
        repository.getAllArtists(),
        _searchQuery,
        _sortBy,
        _sortOrder
    ) { artists, query, sortBy, sortOrder ->
        var filtered = artists
        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
        val sorted = when (sortBy) {
            ArtistSortBy.NAME -> {
                if (sortOrder == SortOrder.ASCENDING) filtered.sortedBy { it.name.lowercase() }
                else filtered.sortedByDescending { it.name.lowercase() }
            }
            ArtistSortBy.SONG_COUNT -> {
                if (sortOrder == SortOrder.ASCENDING) filtered.sortedBy { it.songCount }
                else filtered.sortedByDescending { it.songCount }
            }
            ArtistSortBy.ALBUM_COUNT -> {
                if (sortOrder == SortOrder.ASCENDING) filtered.sortedBy { it.albumCount }
                else filtered.sortedByDescending { it.albumCount }
            }
        }

        sorted
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalItemCount: StateFlow<Int> = artists
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalDurationMs: StateFlow<Long> = combine(
        artists,
        repository.getAllSongs()
    ) { filteredArtists, allSongs ->
        val filteredArtistNames = filteredArtists.map { it.name.lowercase() }.toSet()
        allSongs.filter { it.artist.lowercase() in filteredArtistNames }.sumOf { it.duration }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortBy(sortBy: ArtistSortBy) {
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
            val currentArtistNames = artists.value.map { it.name.lowercase() }.toSet()
            if (currentArtistNames.isNotEmpty()) {
                val allSongs = repository.getAllSongs().first()
                val filteredSongs = allSongs.filter { it.artist.lowercase() in currentArtistNames }
                if (filteredSongs.isNotEmpty()) {
                    playbackConnection.playShuffle(filteredSongs.random(), filteredSongs)
                }
            }
        }
    }
}
