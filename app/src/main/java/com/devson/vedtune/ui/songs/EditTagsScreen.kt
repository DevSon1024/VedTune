package com.devson.vedtune.ui.songs

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.devson.vedtune.ui.components.SongArtwork
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTagsScreen(
    viewModel: EditTagsViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackPosition by viewModel.playbackPosition.collectAsState()

    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onWritePermissionGranted()
        }
    }

    val artworkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.customArtworkUri = uri
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is EditTagsUiEvent.LaunchIntentSender -> {
                    val request = IntentSenderRequest.Builder(event.intentSender).build()
                    intentSenderLauncher.launch(request)
                }
                is EditTagsUiEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is EditTagsUiEvent.SaveSuccess -> {
                    Toast.makeText(context, "Tags saved successfully!", Toast.LENGTH_SHORT).show()
                    onBackClick()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tag & Lyrics Editor") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState is EditTagsUiState.Success) {
                        IconButton(onClick = { viewModel.onSaveClick() }) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when (uiState) {
            is EditTagsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is EditTagsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is EditTagsUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Metadata Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Album Artwork selection
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { artworkLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (viewModel.customArtworkUri != null) {
                                AsyncImage(
                                    model = viewModel.customArtworkUri,
                                    contentDescription = "Selected Art",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                viewModel.song?.let { song ->
                                    SongArtwork(
                                        albumId = song.albumId,
                                        lastModified = song.dateModified,
                                        ignoreCustomArtwork = viewModel.shouldRemoveArtwork,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Art",
                                    tint = Color.White
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.TextButton(
                                onClick = { artworkLauncher.launch("image/*") }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Choose Image")
                            }

                            if (viewModel.customArtworkUri != null || (viewModel.hasExistingCustomArtwork && !viewModel.shouldRemoveArtwork)) {
                                androidx.compose.material3.TextButton(
                                    onClick = { viewModel.removeCustomArtwork() },
                                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remove Custom")
                                }
                            } else if (viewModel.shouldRemoveArtwork) {
                                androidx.compose.material3.TextButton(
                                    onClick = { viewModel.undoRemoveCustomArtwork() }
                                ) {
                                    Text("Undo Remove")
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = viewModel.title,
                        onValueChange = { viewModel.title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = viewModel.artist,
                        onValueChange = { viewModel.artist = it },
                        label = { Text("Artist") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = viewModel.album,
                        onValueChange = { viewModel.album = it },
                        label = { Text("Album") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = viewModel.albumArtist,
                        onValueChange = { viewModel.albumArtist = it },
                        label = { Text("Album Artist") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = viewModel.composer,
                        onValueChange = { viewModel.composer = it },
                        label = { Text("Composer") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = viewModel.genre,
                        onValueChange = { viewModel.genre = it },
                        label = { Text("Genre") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = viewModel.lyricist,
                        onValueChange = { viewModel.lyricist = it },
                        label = { Text("Lyricist") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = viewModel.year,
                        onValueChange = { viewModel.year = it },
                        label = { Text("Year") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = viewModel.comment,
                        onValueChange = { viewModel.comment = it },
                        label = { Text("Comment") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.track,
                            onValueChange = { viewModel.track = it },
                            label = { Text("Track Number") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = viewModel.discNo,
                            onValueChange = { viewModel.discNo = it },
                            label = { Text("Disc Number") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Embedded Lyrics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Sync Tool card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Sync",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Sync Tool",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isPlaying) {
                                            "Playing at: ${formatPlaybackTime(playbackPosition)}"
                                        } else {
                                            "Playback paused"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Button(
                                onClick = { viewModel.captureTimestamp() },
                                enabled = isPlaying
                            ) {
                                Text("Capture Timestamp")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = viewModel.lyrics,
                        onValueChange = { viewModel.lyrics = it },
                        label = { Text("Lyrics (.lrc synced format or text)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        singleLine = false
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

private fun formatPlaybackTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
