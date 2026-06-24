package com.devson.vedtune.ui.albums

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtune.domain.model.Album
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.domain.repository.MediaRepository
import com.devson.vedtune.player.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AlbumDetailsViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val playbackConnection: PlaybackConnection,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val albumId: Long = checkNotNull(savedStateHandle["albumId"])

    val songs: StateFlow<List<Song>> = repository.getSongsByAlbumId(albumId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albumDetails: StateFlow<Album?> = songs.map { songList ->
        if (songList.isNotEmpty()) {
            val firstSong = songList.first()
            Album(
                id = albumId,
                title = firstSong.album,
                artist = firstSong.artist,
                songCount = songList.size
            )
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun playSong(song: Song) {
        playbackConnection.playSong(song, songs.value)
    }

    fun playAlbum() {
        val songList = songs.value
        if (songList.isNotEmpty()) {
            playbackConnection.playSong(songList.first(), songList)
        }
    }

    fun shuffleAlbum() {
        val songList = songs.value
        if (songList.isNotEmpty()) {
            val shuffled = songList.shuffled()
            playbackConnection.playSong(shuffled.first(), shuffled)
        }
    }
}
