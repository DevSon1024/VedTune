package com.devson.vedtune.ui.artists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtune.domain.model.Artist
import com.devson.vedtune.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ArtistsViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val artists: StateFlow<List<Artist>> = combine(
        repository.getAllArtists(),
        _searchQuery
    ) { artists, query ->
        if (query.isBlank()) {
            artists
        } else {
            artists.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
}
