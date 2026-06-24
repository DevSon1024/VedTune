package com.devson.vedtune.ui.artists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.components.SongArtwork
import java.util.Locale

@Composable
fun ArtistDetailsScreen(
    viewModel: ArtistDetailsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsState()
    val artistDetails by viewModel.artistDetails.collectAsState()
    val showArtwork by viewModel.showAlbumArt.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go Back"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = artistDetails?.name ?: "Artist Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (songs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No songs by this artist")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val distinctAlbums = remember(songs) {
                            songs.map { it.albumId }.distinct().take(4)
                        }

                        ElevatedCard(
                            modifier = Modifier.size(200.dp),
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            if (distinctAlbums.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "?")
                                }
                            } else if (distinctAlbums.size == 1) {
                                SongArtwork(
                                    albumId = distinctAlbums[0],
                                    modifier = Modifier.fillMaxSize(),
                                    showArtwork = showArtwork
                                )
                            } else {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(modifier = Modifier.weight(1f)) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            SongArtwork(
                                                albumId = distinctAlbums[0],
                                                modifier = Modifier.fillMaxSize(),
                                                showArtwork = showArtwork
                                            )
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            if (distinctAlbums.size > 1) {
                                                SongArtwork(
                                                    albumId = distinctAlbums[1],
                                                    modifier = Modifier.fillMaxSize(),
                                                    showArtwork = showArtwork
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.fillMaxSize())
                                            }
                                        }
                                    }
                                    Row(modifier = Modifier.weight(1f)) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            if (distinctAlbums.size > 2) {
                                                SongArtwork(
                                                    albumId = distinctAlbums[2],
                                                    modifier = Modifier.fillMaxSize(),
                                                    showArtwork = showArtwork
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.fillMaxSize())
                                            }
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            if (distinctAlbums.size > 3) {
                                                SongArtwork(
                                                    albumId = distinctAlbums[3],
                                                    modifier = Modifier.fillMaxSize(),
                                                    showArtwork = showArtwork
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.fillMaxSize())
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = artistDetails?.name ?: "Unknown Artist",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        val songsText = if (artistDetails?.songCount == 1) "1 Song" else "${artistDetails?.songCount ?: 0} Songs"
                        val albumsText = if (artistDetails?.albumCount == 1) "1 Album" else "${artistDetails?.albumCount ?: 0} Albums"
                        Text(
                            text = "$songsText • $albumsText",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.playArtist() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "Play")
                            }

                            FilledTonalButton(
                                onClick = { viewModel.shuffleArtist() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "Shuffle")
                            }
                        }
                    }
                }

                itemsIndexed(
                    items = songs,
                    key = { _, song -> song.id }
                ) { index, song ->
                    ArtistTrackItem(
                        index = index + 1,
                        song = song,
                        onClick = { viewModel.playSong(song) }
                    )
                }
            }
        }
    }
}

@Composable
fun ArtistTrackItem(
    index: Int,
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = String.format(Locale.getDefault(), "%02d", index),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.width(36.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = song.album,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = formatDuration(song.duration),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}
