package com.devson.vedtune.ui.settings

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.FolderFilterMode
import com.devson.vedtune.ui.components.VedTuneTopAppBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onNavigateToFolderSettings: () -> Unit = {}
) {
    val showAlbumArt by viewModel.showAlbumArt.collectAsState()
    val showRemainingTime by viewModel.showRemainingTime.collectAsState()
    val showMiniPlayerProgress by viewModel.showMiniPlayerProgress.collectAsState()
    val autoplayOnStartup by viewModel.autoplayOnStartup.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColorsEnabled by viewModel.dynamicColorsEnabled.collectAsState()
    val autoSyncOnStartup by viewModel.autoSyncOnStartup.collectAsState()
    val audioFadeInEnabled by viewModel.audioFadeInEnabled.collectAsState()
    val defaultStartScreen by viewModel.defaultStartScreen.collectAsState()
    val isGestureMiniPlayerEnabled by viewModel.isGestureMiniPlayerEnabled.collectAsState()

    val context = LocalContext.current
    var queueClearedMessageVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        VedTuneTopAppBar(
            title = "Settings",
            searchQuery = "",
            onQueryChange = {},
            showSearchAction = false,
            showSortAction = false,
            showLayoutToggleAction = false,
            modifier = Modifier.statusBarsPadding()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

        // CARD 1: Appearance & Theme
        SettingsCard(
            title = "Appearance & Theme",
            icon = Icons.Default.Palette
        ) {
            ThemeModeSelector(
                currentMode = themeMode,
                onModeSelected = { viewModel.setThemeMode(it) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            SettingSwitchRow(
                title = "Dynamic Material You Colors",
                description = "Match theme colors with device wallpaper (Android 12+).",
                checked = dynamicColorsEnabled,
                onCheckedChange = { viewModel.setDynamicColorsEnabled(it) },
                modifier = Modifier.padding(vertical = 4.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            SettingSwitchRow(
                title = "Show Album Artwork",
                description = "Display cover art inside player screens and lists.",
                checked = showAlbumArt,
                onCheckedChange = { viewModel.setShowAlbumArt(it) },
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // CARD 2: Playback Configuration
        SettingsCard(
            title = "Playback Preferences",
            icon = Icons.Default.PlayArrow
        ) {
            SettingSwitchRow(
                title = "Auto-resume on Startup",
                description = "Automatically resume playback when app is restarted.",
                checked = autoplayOnStartup,
                onCheckedChange = { viewModel.setAutoplayOnStartup(it) },
                modifier = Modifier.padding(vertical = 4.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            SettingSwitchRow(
                title = "Audio Fade-in on Play/Resume",
                description = "Gradually fade in sound when starting or resuming playback.",
                checked = audioFadeInEnabled,
                onCheckedChange = { viewModel.setAudioFadeInEnabled(it) },
                modifier = Modifier.padding(vertical = 4.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            SettingSwitchRow(
                title = "Show Remaining Time",
                description = "Show negative countdown remaining time instead of duration.",
                checked = showRemainingTime,
                onCheckedChange = { viewModel.setShowRemainingTime(it) },
                modifier = Modifier.padding(vertical = 4.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            SettingSwitchRow(
                title = "Show Mini Player Progress",
                description = "Display the thin progress line along the top of the mini player card.",
                checked = showMiniPlayerProgress,
                onCheckedChange = { viewModel.setShowMiniPlayerProgress(it) },
                modifier = Modifier.padding(vertical = 4.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            SettingSwitchRow(
                title = "Gesture-Based Mini Player",
                description = "Control playback using gestures (swipe left/right to next/previous, double-tap to play/pause).",
                checked = isGestureMiniPlayerEnabled,
                onCheckedChange = { viewModel.setGestureMiniPlayerEnabled(it) },
                modifier = Modifier.padding(vertical = 4.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            DefaultStartScreenSelector(
                currentScreen = defaultStartScreen,
                onScreenSelected = { viewModel.setDefaultStartScreen(it) }
            )
        }

        // CARD 3: Library & Sync
        SettingsCard(
            title = "Library & Folders",
            icon = Icons.Default.Folder
        ) {
            SettingSwitchRow(
                title = "Auto-sync Library on Startup",
                description = "Scan and sync device audio files on app launch.",
                checked = autoSyncOnStartup,
                onCheckedChange = { viewModel.setAutoSyncOnStartup(it) },
                modifier = Modifier.padding(vertical = 4.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            val folderFilterMode by viewModel.folderFilterMode.collectAsState()
            val folderModeLabel = when (folderFilterMode) {
                FolderFilterMode.NONE -> "None"
                FolderFilterMode.WHITELIST -> "Whitelist"
                FolderFilterMode.BLACKLIST -> "Blacklist"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigateToFolderSettings() }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Folder Visibility",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Filter mode: $folderModeLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // CARD 4: System Equalizer
        SettingsCard(
            title = "System Equalizer",
            icon = Icons.Default.Tune
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Launch system audio equalizer control panel to adjust sound settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "System Equalizer not supported on this device.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Open Equalizer")
                }
            }
        }

        // CARD 5: Cache Management
        SettingsCard(
            title = "Cache & Storage",
            icon = Icons.Default.Delete
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Clears all cached song items from the active queue. Restores play state to default.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ElevatedButton(
                    onClick = {
                        viewModel.clearPlaybackQueue()
                        queueClearedMessageVisible = true
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(3000)
                            queueClearedMessageVisible = false
                        }
                    },
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Clear Playback Queue")
                }

                if (queueClearedMessageVisible) {
                    Text(
                        text = "Playback queue cache cleared successfully.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "VedTune v1.0.0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // Bottom spacer to offset the mini-player and navigation bar padding cleanly
        Spacer(modifier = Modifier.height(contentPadding.calculateBottomPadding()))
    }
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

@Composable
fun ThemeModeSelector(
    currentMode: String,
    onModeSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Theme Mode",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark").forEach { (mode, label) ->
                val isSelected = currentMode == mode
                if (isSelected) {
                    FilledTonalButton(
                        onClick = { onModeSelected(mode) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = label)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onModeSelected(mode) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = label)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun DefaultStartScreenSelector(
    currentScreen: String,
    onScreenSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Default Start Screen",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("songs" to "Songs", "albums" to "Albums", "artists" to "Artists", "playlists" to "Playlists").forEach { (screen, label) ->
                val isSelected = currentScreen == screen
                if (isSelected) {
                    FilledTonalButton(
                        onClick = { onScreenSelected(screen) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Text(text = label, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onScreenSelected(screen) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Text(text = label, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
