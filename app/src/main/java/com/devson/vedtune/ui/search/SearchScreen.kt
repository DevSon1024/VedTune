package com.devson.vedtune.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.Album
import com.devson.vedtune.domain.model.Artist
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.components.SongArtwork
import com.devson.vedtune.ui.components.VedTuneEmptyState
import com.devson.vedtune.ui.components.VedTuneSongRow
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.spacing

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToGenre: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val currentSongId by viewModel.currentSongId.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val showArtwork by viewModel.showArtwork.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Search Input Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { query: String -> viewModel.setSearchQuery(query) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.l, vertical = MaterialTheme.spacing.m),
            placeholder = { Text("Search songs, albums, artists, genres...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            } else null,
            shape = VedTuneShapeTokens.Search,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        val hasResults = results.songs.isNotEmpty() ||
                results.albums.isNotEmpty() ||
                results.artists.isNotEmpty() ||
                results.genres.isNotEmpty()

        if (searchQuery.isBlank()) {
            VedTuneEmptyState(
                icon = Icons.Default.Search,
                title = "Search your library",
                description = "Find any song, album, artist, or genre in your collection.",
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )
        } else if (!hasResults) {
            VedTuneEmptyState(
                icon = Icons.Default.SearchOff,
                title = "No Results Found",
                description = "No audio files matching \"$searchQuery\" were found.",
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(
                    bottom = contentPadding.calculateBottomPadding() + MaterialTheme.spacing.l
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.l)
            ) {
                // 1. Songs Section
                if (results.songs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Songs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                horizontal = MaterialTheme.spacing.l,
                                vertical = MaterialTheme.spacing.xs
                            )
                        )
                    }
                    items(
                        items = results.songs,
                        key = { "song_${it.id}" }
                    ) { song ->
                        VedTuneSongRow(
                            song = song,
                            isCurrentSong = song.id == currentSongId,
                            isPlaying = isPlaying,
                            showArtwork = showArtwork,
                            onClick = { viewModel.playSong(song) },
                            onOptionsClick = null
                        )
                    }
                }

                // 2. Albums Section
                if (results.albums.isNotEmpty()) {
                    item {
                        Text(
                            text = "Albums",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                horizontal = MaterialTheme.spacing.l,
                                vertical = MaterialTheme.spacing.xs
                            )
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.l),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)
                        ) {
                            items(
                                items = results.albums,
                                key = { "album_${it.id}" }
                            ) { album ->
                                SearchAlbumRowItem(
                                    album = album,
                                    onClick = { onNavigateToAlbum(album.id) },
                                    showArtwork = showArtwork
                                )
                            }
                        }
                    }
                }

                // 3. Artists Section
                if (results.artists.isNotEmpty()) {
                    item {
                        Text(
                            text = "Artists",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                horizontal = MaterialTheme.spacing.l,
                                vertical = MaterialTheme.spacing.xs
                            )
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.l),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)
                        ) {
                            items(
                                items = results.artists,
                                key = { "artist_${it.name}" }
                            ) { artist ->
                                SearchArtistRowItem(
                                    artist = artist,
                                    onClick = { onNavigateToArtist(artist.name) }
                                )
                            }
                        }
                    }
                }

                // 4. Genres Section
                if (results.genres.isNotEmpty()) {
                    item {
                        Text(
                            text = "Genres",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                horizontal = MaterialTheme.spacing.l,
                                vertical = MaterialTheme.spacing.xs
                            )
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.l),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)
                        ) {
                            items(
                                items = results.genres,
                                key = { "genre_$it" }
                            ) { genre ->
                                SearchGenreRowItem(
                                    genreName = genre,
                                    onClick = { onNavigateToGenre(genre) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchAlbumRowItem(
    album: Album,
    onClick: () -> Unit,
    showArtwork: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(120.dp)
            .clip(VedTuneShapeTokens.Card)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(VedTuneShapeTokens.Card)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            SongArtwork(
                albumId = album.id,
                modifier = Modifier.fillMaxSize(),
                showArtwork = showArtwork
            )
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SearchArtistRowItem(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(100.dp)
            .clip(VedTuneShapeTokens.Medium)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = artist.name,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SearchGenreRowItem(
    genreName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = VedTuneShapeTokens.Medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = modifier
            .width(140.dp)
            .height(70.dp)
            .clip(VedTuneShapeTokens.Medium)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(MaterialTheme.spacing.m),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = genreName,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.s))
                Text(
                    text = genreName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

