package com.devson.vedtune.ui.artists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devson.vedtune.domain.model.Artist
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.domain.repository.MediaRepository
import com.devson.vedtune.player.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import com.devson.vedtune.domain.repository.SettingsRepository

@HiltViewModel
class ArtistDetailsViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val playbackConnection: PlaybackConnection,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val showAlbumArt: StateFlow<Boolean> = settingsRepository.showAlbumArt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val artistName: String = checkNotNull(savedStateHandle["artistName"])

    val songs: StateFlow<List<Song>> = repository.getSongsByArtist(artistName)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artistDetails: StateFlow<Artist?> = songs.map { songList ->
        if (songList.isNotEmpty()) {
            val distinctAlbumsCount = songList.map { it.albumId }.distinct().size
            Artist(
                name = artistName,
                songCount = songList.size,
                albumCount = distinctAlbumsCount
            )
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun playSong(song: Song) {
        playbackConnection.playSong(song, songs.value)
    }

    fun playArtist() {
        val songList = songs.value
        if (songList.isNotEmpty()) {
            playbackConnection.playSong(songList.first(), songList)
        }
    }

    fun shuffleArtist() {
        val songList = songs.value
        if (songList.isNotEmpty()) {
            val shuffled = songList.shuffled()
            playbackConnection.playSong(shuffled.first(), shuffled)
        }
    }
}
