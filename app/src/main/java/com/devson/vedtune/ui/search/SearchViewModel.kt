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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
    val searchResults: StateFlow<SearchResults> = searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(SearchResults())
            } else {
                combine(
                    repository.getAllSongs(),
                    repository.getAllAlbums(),
                    repository.getAllArtists(),
                    flow { emit(repository.getUniqueGenres()) }
                ) { songs, albums, artists, genres ->
                    val filteredSongs = songs.filter {
                        it.title.contains(query, ignoreCase = true) ||
                        it.artist.contains(query, ignoreCase = true) ||
                        it.album.contains(query, ignoreCase = true)
                    }
                    val filteredAlbums = albums.filter {
                        it.title.contains(query, ignoreCase = true) ||
                        it.artist.contains(query, ignoreCase = true)
                    }
                    val filteredArtists = artists.filter {
                        it.name.contains(query, ignoreCase = true)
                    }
                    val filteredGenres = genres.filter {
                        it.contains(query, ignoreCase = true)
                    }
                    SearchResults(
                        songs = filteredSongs,
                        albums = filteredAlbums,
                        artists = filteredArtists,
                        genres = filteredGenres
                    )
                }
            }
        }
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
