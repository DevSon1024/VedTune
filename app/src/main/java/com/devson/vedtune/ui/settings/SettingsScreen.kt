package com.devson.vedtune.ui.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.devson.vedtune.domain.model.FolderFilterMode

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

    val context = LocalContext.current
    var queueClearedMessageVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp
            )
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Player Customization
        Text(
            text = "Player Customization",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        ThemeModeSelector(
            currentMode = themeMode,
            onModeSelected = { viewModel.setThemeMode(it) }
        )

        SettingSwitchRow(
            title = "Show Album Artwork",
            description = "Display cover art inside player screens and lists.",
            checked = showAlbumArt,
            onCheckedChange = { viewModel.setShowAlbumArt(it) }
        )

        SettingSwitchRow(
            title = "Dynamic Material You Colors",
            description = "Enable dynamic theme colors matching device wallpaper (Android 12+).",
            checked = dynamicColorsEnabled,
            onCheckedChange = { viewModel.setDynamicColorsEnabled(it) }
        )

        SettingSwitchRow(
            title = "Show Remaining Time",
            description = "Show negative countdown remaining time instead of duration.",
            checked = showRemainingTime,
            onCheckedChange = { viewModel.setShowRemainingTime(it) }
        )

        SettingSwitchRow(
            title = "Show Mini Player Progress",
            description = "Display the thin progress line along the top of the mini player card.",
            checked = showMiniPlayerProgress,
            onCheckedChange = { viewModel.setShowMiniPlayerProgress(it) }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Playback Configuration
        Text(
            text = "Playback Configuration",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        SettingSwitchRow(
            title = "Auto-resume on Startup",
            description = "Automatically resume music playback when the app is restarted.",
            checked = autoplayOnStartup,
            onCheckedChange = { viewModel.setAutoplayOnStartup(it) }
        )

        SettingSwitchRow(
            title = "Auto-sync Library on Startup",
            description = "Automatically scan and sync device audio files on app launch.",
            checked = autoSyncOnStartup,
            onCheckedChange = { viewModel.setAutoSyncOnStartup(it) }
        )

        SettingSwitchRow(
            title = "Audio Fade-in on Play/Resume",
            description = "Gradually fade in sound when starting or resuming playback.",
            checked = audioFadeInEnabled,
            onCheckedChange = { viewModel.setAudioFadeInEnabled(it) }
        )

        DefaultStartScreenSelector(
            currentScreen = defaultStartScreen,
            onScreenSelected = { viewModel.setDefaultStartScreen(it) }
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "System Equalizer",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Launch the system audio equalizer control panel to adjust sound settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
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
                }
            ) {
                Text(text = "Open Equalizer")
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Library
        Text(
            text = "Library",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        val folderFilterMode by viewModel.folderFilterMode.collectAsState()
        val folderModeLabel = when (folderFilterMode) {
            com.devson.vedtune.domain.model.FolderFilterMode.NONE -> "None"
            com.devson.vedtune.domain.model.FolderFilterMode.WHITELIST -> "Whitelist"
            com.devson.vedtune.domain.model.FolderFilterMode.BLACKLIST -> "Blacklist"
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToFolderSettings() },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
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
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Cache Management
        Text(
            text = "Cache Management",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Reset Playback Queue",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Clears all cached song items from the active queue. Restores play state to default.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
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
                )
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

        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            text = "VedTune v1.0.0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun ThemeModeSelector(
    currentMode: String,
    onModeSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
            .padding(vertical = 8.dp),
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = label, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onScreenSelected(screen) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = label, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
