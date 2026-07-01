package com.devson.vedtune.ui.songs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.components.SongArtwork
import com.devson.vedtune.data.metadata.AudioMetadataExtractor
import com.devson.vedtune.data.metadata.ExtractedMetadata
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AudioMetadataExtractorEntryPoint {
    fun getAudioMetadataExtractor(): AudioMetadataExtractor
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongInfoBottomSheet(
    song: Song,
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToLocation: (Long) -> Unit,
    onNavigateToEditTags: (Long) -> Unit,
    onClearHistory: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val extractor = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AudioMetadataExtractorEntryPoint::class.java
        ).getAudioMetadataExtractor()
    }

    var metadata by remember { mutableStateOf<ExtractedMetadata?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showClearConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(song.id) {
        isLoading = true
        withContext(Dispatchers.IO) {
            metadata = extractor.extractMetadata(song.id)
        }
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val meta = metadata ?: ExtractedMetadata(
                composer = "", genre = "", lyricist = "", trackNumber = "", discNumber = "",
                comment = "", year = "", bitrate = "", sampleRate = "", bitsPerSample = "",
                format = "", encodingType = "", channels = "", fileSizeMb = 0.0, filePath = ""
            )

            val fileName = remember(meta.filePath) {
                if (meta.filePath.isNotEmpty()) {
                    File(meta.filePath).name
                } else {
                    song.title
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SongArtwork(
                            albumId = song.albumId,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = truncateMiddle(fileName),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = meta.filePath.ifEmpty { "Unknown location" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        onNavigateToLocation(song.id)
                                        onDismiss()
                                    }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                }

                item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) }

                // Section 2: Metadata Tags
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Metadata Tags",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                onNavigateToEditTags(song.id)
                                onDismiss()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Tags",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        InfoRowItem(label = "Title", value = song.title)
                        
                        // Clickable Album Row
                        ClickableRowItem(
                            label = "Album",
                            value = song.album,
                            onClick = {
                                onNavigateToAlbum(song.albumId)
                                onDismiss()
                            }
                        )

                        // Clickable Artist Row
                        ClickableRowItem(
                            label = "Artist",
                            value = song.artist,
                            onClick = {
                                onNavigateToArtist(song.artist)
                                onDismiss()
                            }
                        )

                        InfoRowItem(label = "Composer", value = meta.composer)
                        InfoRowItem(label = "Genre", value = meta.genre)
                        InfoRowItem(label = "Lyricist", value = meta.lyricist)
                        InfoRowItem(label = "Track Number", value = meta.trackNumber.ifEmpty { song.track.takeIf { it > 0 }?.toString() ?: "" })
                        InfoRowItem(label = "Disc Number", value = meta.discNumber)
                        InfoRowItem(label = "Year", value = meta.year.ifEmpty { song.year.takeIf { it > 0 }?.toString() ?: "" })
                        InfoRowItem(label = "Comment", value = meta.comment)
                    }
                }

                item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) }

                // Section 3: Technical Specs
                item {
                    Text(
                        text = "Technical Specifications",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                TechSpecItem(label = "Duration", value = formatDuration(song.duration))
                                TechSpecItem(label = "Format", value = meta.format.uppercase())
                                TechSpecItem(label = "Bitrate", value = if (meta.bitrate.isNotEmpty()) "${meta.bitrate} kbps" else "")
                                TechSpecItem(label = "Channels", value = meta.channels)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                TechSpecItem(label = "File Size", value = if (meta.fileSizeMb > 0.0) String.format(Locale.US, "%.2f MB", meta.fileSizeMb) else "")
                                TechSpecItem(label = "Encoding", value = meta.encodingType)
                                TechSpecItem(label = "Sample Rate", value = if (meta.sampleRate.isNotEmpty()) "${meta.sampleRate} Hz" else "")
                                TechSpecItem(label = "Bits Per Sample", value = if (meta.bitsPerSample.isNotEmpty() && meta.bitsPerSample != "0") "${meta.bitsPerSample} bit" else "")
                            }
                        }
                    }
                }

                item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) }

                // Section 4: Statistics & History
                item {
                    Text(
                        text = "Playback Stats & History",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        InfoRowItem(label = "Date Added", value = formatTimestamp(song.dateAdded, isMediaStoreTime = true))
                        InfoRowItem(label = "Date Modified", value = formatTimestamp(song.dateModified, isMediaStoreTime = true))
                        InfoRowItem(label = "Play Count", value = song.playCount.toString())
                        InfoRowItem(
                            label = "Last Played",
                            value = if (song.lastPlayed > 0) formatTimestamp(song.lastPlayed, isMediaStoreTime = false) else "Never"
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedButton(
                            onClick = { showClearConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                            )
                        ) {
                            Text("Clear Playback History")
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Reset Stats") },
            text = { Text("Are you sure you want to clear playback history and reset play count for this song?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory(song.id)
                        showClearConfirm = false
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun InfoRowItem(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        )
    }
}

@Composable
fun ClickableRowItem(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value.ifBlank { "Unknown" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Go to $label",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun TechSpecItem(
    label: String,
    value: String
) {
    if (value.isBlank()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0:00"
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

private fun formatTimestamp(timestamp: Long, isMediaStoreTime: Boolean): String {
    if (timestamp <= 0) return "Unknown"
    val millis = if (isMediaStoreTime) timestamp * 1000 else timestamp
    val date = Date(millis)
    val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
    return sdf.format(date)
}

private fun truncateMiddle(name: String, maxLength: Int = 36): String {
    if (name.length <= maxLength) return name
    val half = (maxLength - 3) / 2
    return name.take(half) + "..." + name.takeLast(half)
}
