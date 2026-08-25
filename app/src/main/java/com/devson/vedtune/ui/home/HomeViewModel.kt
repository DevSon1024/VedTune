package com.devson.vedtune.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtune.domain.model.Album
import com.devson.vedtune.domain.model.Playlist
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.domain.repository.MediaRepository
import com.devson.vedtune.domain.repository.SettingsRepository
import com.devson.vedtune.player.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

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

    private val allSongs = repository.getAllSongs()
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val allAlbums = repository.getAllAlbums()
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allPlaylists: StateFlow<List<Playlist>> = repository.getAllPlaylists()
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentlyAddedAlbums: StateFlow<List<Album>> = allAlbums
        .map { albums ->
            albums.sortedByDescending { it.id }.take(10)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val jumpBackInSongs: StateFlow<List<Song>> = allSongs
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

    val latestSongs: StateFlow<List<Song>> = allSongs
        .map { songs ->
            songs.sortedByDescending { it.dateAdded }.take(20)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalSongsCount: StateFlow<Int> = allSongs
        .map { it.size }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalAlbumsCount: StateFlow<Int> = allAlbums
        .map { it.size }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalArtistsCount: StateFlow<Int> = repository.getAllArtists()
        .map { it.size }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalPlaylistsCount: StateFlow<Int> = allPlaylists
        .map { it.size }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val favoriteSongsCount: StateFlow<Int> = repository.getFavoriteSongIdsFlow()
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

    fun playNext(song: Song) {
        playbackConnection.playNext(song)
    }

    fun playShuffle(song: Song) {
        val list = latestSongs.value
        if (list.isNotEmpty()) {
            playbackConnection.playShuffle(song, list)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.addSongToPlaylist(playlistId, songId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createPlaylistAndAddSong(name: String, songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newId = repository.createPlaylist(name)
                repository.addSongToPlaylist(newId, songId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
