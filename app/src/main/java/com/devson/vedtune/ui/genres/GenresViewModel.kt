package com.devson.vedtune.ui.genres

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtune.domain.repository.MediaRepository
import com.devson.vedtune.player.PlaybackConnection
import com.devson.vedtune.ui.songs.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GenresViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val playbackConnection: PlaybackConnection
) : ViewModel() {

    private val _genres = MutableStateFlow<List<String>>(emptyList())
    val genres: StateFlow<List<String>> = _genres.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.ASCENDING)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadGenres()
    }

    fun loadGenres() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = repository.getUniqueGenres()
                sortAndSetGenres(list, _sortOrder.value)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun sortAndSetGenres(list: List<String>, order: SortOrder) {
        _genres.value = if (order == SortOrder.ASCENDING) {
            list.sortedWith(String.CASE_INSENSITIVE_ORDER)
        } else {
            list.sortedWith(String.CASE_INSENSITIVE_ORDER.reversed())
        }
    }

    fun toggleSortOrder() {
        viewModelScope.launch {
            val newOrder = if (_sortOrder.value == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
            _sortOrder.value = newOrder
            val currentList = repository.getUniqueGenres()
            sortAndSetGenres(currentList, newOrder)
        }
    }

    fun toggleLayoutView() {
        _isGridView.value = !_isGridView.value
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
