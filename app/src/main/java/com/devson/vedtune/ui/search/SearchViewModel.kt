package com.devson.vedtune.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.domain.model.Album
import com.devson.vedtune.domain.model.Artist
import com.devson.vedtune.domain.repository.MediaRepository
import com.devson.vedtune.player.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.devson.vedtune.domain.repository.SettingsRepository

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val playbackConnection: PlaybackConnection,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val currentSongId: StateFlow<Long?> = playbackConnection.currentSongId
    val isPlaying: StateFlow<Boolean> = playbackConnection.isPlaying

    val showArtwork: StateFlow<Boolean> = settingsRepository.showAlbumArt
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    private val allSongs = repository.getAllSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allAlbums = repository.getAllAlbums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allArtists = repository.getAllArtists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allGenres = flow { emit(repository.getUniqueGenres()) }
        .flowOn(kotlinx.coroutines.Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val searchResults: StateFlow<SearchResults> = combine(
        searchQuery.debounce(150),
        allSongs,
        allAlbums,
        allArtists,
        allGenres
    ) { query, songs, albums, artists, genres ->
        if (query.isBlank()) {
            SearchResults()
        } else {
            val trimmedQuery = query.trim()
            val filteredSongs = songs.filter {
                it.title.contains(trimmedQuery, ignoreCase = true) ||
                it.artist.contains(trimmedQuery, ignoreCase = true) ||
                it.album.contains(trimmedQuery, ignoreCase = true)
            }
            val filteredAlbums = albums.filter {
                it.title.contains(trimmedQuery, ignoreCase = true) ||
                it.artist.contains(trimmedQuery, ignoreCase = true)
            }
            val filteredArtists = artists.filter {
                it.name.contains(trimmedQuery, ignoreCase = true)
            }
            val filteredGenres = genres.filter {
                it.contains(trimmedQuery, ignoreCase = true)
            }
            SearchResults(
                songs = filteredSongs,
                albums = filteredAlbums,
                artists = filteredArtists,
                genres = filteredGenres
            )
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchResults()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun playSong(song: Song) {
        playbackConnection.playSong(song, searchResults.value.songs)
    }
}

data class SearchResults(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val genres: List<String> = emptyList()
)
