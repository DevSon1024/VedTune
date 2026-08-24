package com.devson.vedtune.ui.player.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.TextFormat
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devson.vedtune.domain.model.Song
import com.devson.vedtune.ui.player.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class LrcLine(val timestamp: Long, val text: String)

fun parseLrc(lrcText: String): List<LrcLine> {
    val timeRegex = Regex("\\[(\\d+):(\\d+)(?:\\.(\\d+))?]")
    val parsedLines = mutableListOf<LrcLine>()

    for (line in lrcText.lines()) {
        val cleanLine = line.trim()
        if (cleanLine.isEmpty()) continue

        // Skip metadata tags like [ti:Title]
        if (cleanLine.startsWith("[") && !cleanLine.startsWith("[0") && !cleanLine.startsWith("[1") && !cleanLine.startsWith("[2") && !cleanLine.startsWith("[3") && !cleanLine.startsWith("[4") && !cleanLine.startsWith("[5") && !cleanLine.startsWith("[6") && !cleanLine.startsWith("[7") && !cleanLine.startsWith("[8") && !cleanLine.startsWith("[9")) {
            continue
        }

        val matches = timeRegex.findAll(cleanLine).toList()
        if (matches.isNotEmpty()) {
            val text = cleanLine.replace(timeRegex, "").trim()
            for (match in matches) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val msPart = match.groupValues.getOrNull(3)
                val ms = if (!msPart.isNullOrEmpty()) {
                    when (msPart.length) {
                        1 -> msPart.toLong() * 100
                        2 -> msPart.toLong() * 10
                        else -> msPart.substring(0, 3).toLong()
                    }
                } else 0L
                parsedLines.add(LrcLine((min * 60 * 1000) + (sec * 1000) + ms, text))
            }
        } else {
            // Unsynced line gets -1L
            parsedLines.add(LrcLine(-1L, cleanLine))
        }
    }
    return parsedLines
}

fun getActiveLyricsLineIndex(lines: List<LrcLine>, currentPosition: Long): Int {
    if (lines.isEmpty()) return -1
    // Offset +150ms compensates for AudioTrack buffer latency and UI rendering
    val effectivePosition = currentPosition + 150L
    var activeIndex = -1
    for (i in lines.indices) {
        val timestamp = lines[i].timestamp
        if (timestamp in 0L..effectivePosition) {
            activeIndex = i
        }
    }
    return activeIndex
}

@Composable
fun LyricsPanel(
    viewModel: PlayerViewModel,
    activeSong: Song,
    onToggleLyrics: () -> Unit,
    onEditLyricsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lyricsText by viewModel.currentLyrics.collectAsStateWithLifecycle()
    val fontSizeState by viewModel.lyricsFontSize.collectAsStateWithLifecycle()
    val alignmentState by viewModel.lyricsAlignment.collectAsStateWithLifecycle()
    val positionState = viewModel.playbackPosition
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    var showFormattingDialog by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }

    val lrcPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                viewModel.importLrcFile(context, uri)
            }
        }
    }

    val fontSize = when (fontSizeState) {
        "Small" -> MaterialTheme.typography.bodyMedium.fontSize
        "Big" -> MaterialTheme.typography.titleLarge.fontSize
        else -> MaterialTheme.typography.titleMedium.fontSize
    }

    val textAlign = when (alignmentState) {
        "Left" -> TextAlign.Start
        "Right" -> TextAlign.End
        else -> TextAlign.Center
    }

    val horizontalAlign = when (alignmentState) {
        "Left" -> Alignment.Start
        "Right" -> Alignment.End
        else -> Alignment.CenterHorizontally
    }

    val currentLyricsText = lyricsText
    val hasTimestamps = remember(currentLyricsText) {
        !currentLyricsText.isNullOrBlank() && currentLyricsText.contains(Regex("\\[\\d+:\\d+"))
    }
    var parsedLines by remember { mutableStateOf<List<LrcLine>>(emptyList()) }

    LaunchedEffect(lyricsText) {
        val text = lyricsText
        if (!text.isNullOrBlank()) {
            parsedLines = withContext(Dispatchers.Default) {
                parseLrc(text)
            }
        } else {
            parsedLines = emptyList()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f))
            .clickable { onToggleLyrics() }
    ) {
        // Floating Edit Lyrics bar (top left)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.medium
                )
                .clip(MaterialTheme.shapes.medium)
                .clickable { onEditLyricsClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = "Edit Lyrics",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Edit Lyrics",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Floating options bar
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.medium
                )
                .clickable(enabled = false) {},
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { showFormattingDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.TextFormat,
                    contentDescription = "Format Lyrics",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { showCopyDialog = true },
                modifier = Modifier.size(32.dp),
                enabled = !lyricsText.isNullOrBlank()
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "Copy Lyrics",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                        val initialUri = android.provider.DocumentsContract.buildDocumentUri(
                            "com.android.externalstorage.documents",
                            "primary:Documents/VedTune/Lyrics"
                        )
                        putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI, initialUri)
                    }
                    lrcPickerLauncher.launch(intent)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.UploadFile,
                    contentDescription = "Import LRC File",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                lyricsText.isNullOrBlank() -> {
                    Text(
                        text = "No lyrics available\nTap top-right to import .lrc",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center
                    )
                }

                hasTimestamps && parsedLines.isEmpty() -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                !hasTimestamps -> {
                    val nonNullLyrics = lyricsText ?: ""
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = horizontalAlign,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = nonNullLyrics,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = fontSize,
                                textAlign = textAlign
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                else -> {
                    val listState = rememberLazyListState()
                    val position by positionState.collectAsStateWithLifecycle()

                    val activeLineIndex by remember(parsedLines) {
                        derivedStateOf { getActiveLyricsLineIndex(parsedLines, position) }
                    }

                    // Snap immediately on first composition or song change, animate subsequently
                    var hasSnappedToInitial by remember(activeSong.id) { mutableStateOf(false) }

                    LaunchedEffect(activeLineIndex) {
                        if (activeLineIndex >= 0) {
                            if (!hasSnappedToInitial) {
                                listState.scrollToItem(activeLineIndex, scrollOffset = -100)
                                hasSnappedToInitial = true
                            } else {
                                listState.animateScrollToItem(activeLineIndex, scrollOffset = -100)
                            }
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = horizontalAlign,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(100.dp)) }

                        itemsIndexed(parsedLines) { index, line ->
                            val isActive = index == activeLineIndex
                            val alpha by animateFloatAsState(
                                targetValue = if (isActive) 1f else 0.4f,
                                label = "LyricsAlpha"
                            )
                            val lineScale by animateFloatAsState(
                                targetValue = if (isActive) 1.08f else 0.95f,
                                label = "LyricsScale"
                            )
                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = fontSize,
                                    textAlign = textAlign
                                ),
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(lineScale)
                                    .graphicsLayer { this.alpha = alpha }
                                    .padding(horizontal = 8.dp)
                            )
                        }

                        item { Spacer(modifier = Modifier.height(100.dp)) }
                    }
                }
            }
        }
    }

    if (showFormattingDialog) {
        LyricsFormattingDialog(
            currentSize = fontSizeState,
            currentAlignment = alignmentState,
            onDismiss = { showFormattingDialog = false },
            onSelectSize = { viewModel.setLyricsFontSize(it) },
            onSelectAlignment = { viewModel.setLyricsAlignment(it) }
        )
    }

    if (showCopyDialog && !lyricsText.isNullOrBlank()) {
        LyricsCopyDialog(
            lyricsText = lyricsText ?: "",
            parsedLines = parsedLines,
            onDismiss = { showCopyDialog = false },
            onCopy = { textToCopy ->
                clipboardManager.setText(AnnotatedString(textToCopy))
                showCopyDialog = false
            }
        )
    }
}

@Composable
fun LyricsFormattingDialog(
    currentSize: String,
    currentAlignment: String,
    onDismiss: () -> Unit,
    onSelectSize: (String) -> Unit,
    onSelectAlignment: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lyrics Formatting") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text(
                        text = "Font Size",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Small", "Medium", "Big").forEach { size ->
                            val isSelected = size == currentSize
                            Button(
                                onClick = { onSelectSize(size) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(size)
                            }
                        }
                    }
                }

                Column {
                    Text(
                        text = "Alignment",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Left", "Center", "Right").forEach { align ->
                            val isSelected = align == currentAlignment
                            val label = if (align == "Center") "Middle" else align
                            Button(
                                onClick = { onSelectAlignment(align) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun LyricsCopyDialog(
    lyricsText: String,
    parsedLines: List<LrcLine>,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Copy Lyrics") },
        text = {
            Text("Select format to copy to clipboard:")
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (parsedLines.isNotEmpty()) {
                            val cleanLyrics = parsedLines.joinToString("\n") { it.text }
                            onCopy(cleanLyrics)
                        } else {
                            onCopy(lyricsText)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Without Timestamp")
                }

                if (parsedLines.isNotEmpty()) {
                    Button(
                        onClick = {
                            val formattedLyrics = parsedLines.joinToString("\n") { line ->
                                "${com.devson.vedtune.core.formatLrcTime(line.timestamp)} ${line.text}"
                            }
                            onCopy(formattedLyrics)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("With Timestamp")
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}
