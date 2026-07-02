package com.devson.vedtune.ui.lyrics

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devson.vedtune.domain.model.SeekBarStyle
import com.devson.vedtune.ui.components.VedTuneSeekBar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsEditorScreen(
    viewModel: LyricsEditorViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val song by viewModel.song.collectAsStateWithLifecycle()
    val rawLyrics by viewModel.rawLyrics.collectAsStateWithLifecycle()
    val parsedLines by viewModel.parsedLines.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val seekbarStyle by viewModel.seekbarStyle.collectAsStateWithLifecycle()

    var showDiscardDialog by remember { mutableStateOf(false) }
    var editingLineIndex by remember { mutableStateOf<Int?>(null) }

    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onWritePermissionGranted()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is LyricsEditorUiEvent.LaunchIntentSender -> {
                    val request = IntentSenderRequest.Builder(event.intentSender).build()
                    intentSenderLauncher.launch(request)
                }
                is LyricsEditorUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is LyricsEditorUiEvent.SaveSuccess -> {
                    Toast.makeText(context, "Lyrics saved successfully", Toast.LENGTH_SHORT).show()
                    onBackClick()
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Changes") },
            text = { Text("Discard unsaved changes?") },
            confirmButton = {
                TextButton(onClick = onBackClick) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    editingLineIndex?.let { index ->
        if (index in parsedLines.indices) {
            val line = parsedLines[index]
            LyricsEditDialog(
                line = line,
                onDismiss = { editingLineIndex = null },
                onConfirm = { text, timeMs ->
                    viewModel.updateLineTextAndTimestamp(index, text, timeMs)
                    editingLineIndex = null
                },
                onRemove = {
                    viewModel.removeLine(index)
                    editingLineIndex = null
                },
                formatLrcTimeForUi = { viewModel.formatLrcTime(it, includeBrackets = false) }
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left 'X' Close button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { showDiscardDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Center Tab Selector
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .height(40.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        val tabs = listOf("Simple", "Synced")
                        tabs.forEachIndexed { index, label ->
                            val selected = activeTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        else Color.Transparent
                                    )
                                    .clickable { viewModel.setActiveTab(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Right Checkmark Save button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { viewModel.saveLyrics() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is LyricsEditorUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is LyricsEditorUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = (uiState as LyricsEditorUiState.Error).message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.saveLyrics() }) {
                            Text("Retry")
                        }
                    }
                }
                else -> {
                    Crossfade(targetState = activeTab, label = "TabTransition") { tab ->
                        if (tab == 0) {
                            SimpleTabContent(
                                rawLyrics = rawLyrics,
                                songTitle = song?.title ?: "",
                                songArtist = song?.artist ?: "",
                                onRawLyricsChange = { viewModel.setRawLyrics(it) }
                            )
                        } else {
                            SyncedTabContent(
                                parsedLines = parsedLines,
                                viewModel = viewModel,
                                style = seekbarStyle,
                                onLineTextClick = { index -> editingLineIndex = index }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleTabContent(
    rawLyrics: String,
    songTitle: String,
    songArtist: String,
    onRawLyricsChange: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        OutlinedTextField(
            value = rawLyrics,
            onValueChange = onRawLyricsChange,
            label = { Text("Embedded lyrics") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    val query = "lyrics: $songTitle $songArtist"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}"))
                    context.startActivity(intent)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search Web"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search")
            }

            Row {
                OutlinedIconButton(
                    onClick = {
                        val clip = android.content.ClipData.newPlainText("raw lyrics", rawLyrics)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy All"
                    )
                }

                OutlinedIconButton(
                    onClick = {
                        val clipData = clipboardManager.primaryClip
                        val clipText = if (clipData != null && clipData.itemCount > 0) {
                            clipData.getItemAt(0).text?.toString() ?: ""
                        } else ""
                        onRawLyricsChange(rawLyrics + clipText)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentPaste,
                        contentDescription = "Paste"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncedTabContent(
    parsedLines: List<LrcLine>,
    viewModel: LyricsEditorViewModel,
    style: SeekBarStyle,
    onLineTextClick: (Int) -> Unit
) {
    val currentPos by viewModel.currentPosition.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 8.dp)
        ) {
            itemsIndexed(parsedLines) { index, line ->
                val formattedTime = viewModel.formatLrcTime(line.timestamp, includeBrackets = false)
                LrcLineCard(
                    line = line,
                    formattedTime = formattedTime,
                    onTimeSetClick = { viewModel.updateLineTimestamp(index, currentPos) },
                    onTextClick = { onLineTextClick(index) },
                    onPlayClick = { viewModel.seekAndPlay(line.timestamp) }
                )
            }
        }

        // Synced Bottom Player Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                var isDragging by remember { mutableStateOf(false) }
                var sliderValue by remember { mutableFloatStateOf(currentPos.toFloat()) }

                LaunchedEffect(currentPos) {
                    if (!isDragging) {
                        sliderValue = currentPos.toFloat()
                    }
                }

                VedTuneSeekBar(
                    value = sliderValue,
                    onValueChange = { value ->
                        isDragging = true
                        sliderValue = value
                        viewModel.seekToDebounced(value.toLong())
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        viewModel.seekTo(sliderValue.toLong())
                    },
                    valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                    style = style,
                    isPlaying = isPlaying,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play/Pause button
                    OutlinedIconButton(
                        onClick = { viewModel.togglePlayPause() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause"
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Rewind 10s button
                    OutlinedIconButton(
                        onClick = { viewModel.skipBackward() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FastRewind,
                            contentDescription = "Rewind 10s"
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Fast Forward 10s button
                    OutlinedIconButton(
                        onClick = { viewModel.skipForward() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FastForward,
                            contentDescription = "Fast Forward 10s"
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Timer Display
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = "Current Time",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = viewModel.formatLrcTime(sliderValue.toLong(), includeBrackets = false),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LrcLineCard(
    line: LrcLine,
    formattedTime: String,
    onTimeSetClick: () -> Unit,
    onTextClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Time Set Column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(72.dp)
            ) {
                IconButton(onClick = onTimeSetClick) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = "Set Time",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Vertical Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
                    .padding(horizontal = 8.dp)
            )

            // Center: Text Section
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTextClick() }
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = if (line.text.isBlank()) "[Empty line]" else line.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (line.text.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface
                )
            }

            // Right: Play button
            IconButton(
                onClick = onPlayClick,
                enabled = line.timestamp >= 0
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play Line",
                    tint = if (line.timestamp >= 0) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun LyricsEditDialog(
    line: LrcLine,
    onDismiss: () -> Unit,
    onConfirm: (text: String, timestamp: Long) -> Unit,
    onRemove: () -> Unit,
    formatLrcTimeForUi: (Long) -> String
) {
    var textVal by remember { mutableStateOf(line.text) }
    var timeMs by remember { mutableStateOf(line.timestamp) }
    if (timeMs < 0) timeMs = 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Time box with - and + buttons
                Text(
                    text = "Time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { timeMs = (timeMs - 25L).coerceAtLeast(0L) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(width = 64.dp, height = 48.dp)
                    ) {
                        Text("-", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .height(48.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formatLrcTimeForUi(timeMs),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { timeMs += 25L },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(width = 64.dp, height = 48.dp)
                    ) {
                        Text("+", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Text field
                OutlinedTextField(
                    value = textVal,
                    onValueChange = { textVal = it },
                    label = { Text("Text") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Custom Button Row matching the screenshot layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onRemove) {
                        Text("REMOVE", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }

                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("CANCEL", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { onConfirm(textVal, timeMs) }) {
                            Text("OK", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    )
}
