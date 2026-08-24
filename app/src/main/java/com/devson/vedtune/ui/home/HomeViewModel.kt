package com.devson.vedtune.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtune.domain.model.Album
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.domain.repository.MediaRepository
import com.devson.vedtune.player.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.devson.vedtune.domain.repository.SettingsRepository

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val playbackConnection: PlaybackConnection,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val currentSongId: StateFlow<Long?> = playbackConnection.currentSongId
    val isPlaying: StateFlow<Boolean> = playbackConnection.isPlaying

    val showArtwork: StateFlow<Boolean> = settingsRepository.showAlbumArt
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val recentlyAddedAlbums: StateFlow<List<Album>> = repository.getAllAlbums()
        .map { albums ->
            albums.sortedByDescending { it.id }.take(10)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val jumpBackInSongs: StateFlow<List<Song>> = repository.getAllSongs()
        .map { songs ->
            val played = songs.filter { it.lastPlayed > 0 }.sortedByDescending { it.lastPlayed }
            if (played.isNotEmpty()) played.take(12) else songs.take(12)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val latestSongs: StateFlow<List<Song>> = repository.getAllSongs()
        .map { songs ->
            songs.sortedByDescending { it.dateAdded }.take(20)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalSongsCount: StateFlow<Int> = repository.getAllSongs()
        .map { it.size }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalAlbumsCount: StateFlow<Int> = repository.getAllAlbums()
        .map { it.size }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalArtistsCount: StateFlow<Int> = repository.getAllArtists()
        .map { it.size }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalPlaylistsCount: StateFlow<Int> = repository.getAllPlaylists()
        .map { it.size }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun playSong(song: Song) {
        playbackConnection.playSong(song, latestSongs.value)
    }

    fun playJumpBackInSong(song: Song) {
        playbackConnection.playSong(song, jumpBackInSongs.value)
    }

    fun playAlbum(album: Album) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val albumSongs = repository.getSongsByAlbumId(album.id).first()
                if (albumSongs.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        playbackConnection.playSong(albumSongs.first(), albumSongs)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playAll() {
        val songs = latestSongs.value
        if (songs.isNotEmpty()) {
            playbackConnection.playSong(songs.first(), songs)
        }
    }

    fun shuffleAll() {
        val songs = latestSongs.value
        if (songs.isNotEmpty()) {
            val shuffled = songs.shuffled()
            playbackConnection.playSong(shuffled.first(), shuffled)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.synchronizeLibrary()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
