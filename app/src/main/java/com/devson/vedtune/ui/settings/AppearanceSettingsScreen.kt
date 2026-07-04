package com.devson.vedtune.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.AlbumArtClickAction
import com.devson.vedtune.domain.model.AlbumArtQuality

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColorsEnabled by viewModel.dynamicColorsEnabled.collectAsState()
    val showAlbumArt by viewModel.showAlbumArt.collectAsState()
    val keepScreenOnWithLyrics by viewModel.keepScreenOnWithLyrics.collectAsState()
    val albumArtClickAction by viewModel.albumArtClickAction.collectAsState()
    val playerBackgroundBlurRadius by viewModel.playerBackgroundBlurRadius.collectAsState()

    // New settings flows
    val isAmoledDark by viewModel.isAmoledDark.collectAsState()
    val albumArtQuality by viewModel.albumArtQuality.collectAsState()
    val forceSquareArtwork by viewModel.forceSquareArtwork.collectAsState()

    val systemInDark = androidx.compose.foundation.isSystemInDarkTheme()
    val showAmoledToggle = themeMode == "DARK" || (themeMode == "SYSTEM" && systemInDark)

    var showThemeDialog by remember { mutableStateOf(false) }
    var showClickActionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Appearance & Theme",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // SECTION 1: Theme Settings
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                SettingsCard(
                    title = "Theme Options",
                    icon = Icons.Default.Palette
                ) {
                    SettingsNavigationRow(
                        title = "Theme Mode",
                        description = when (themeMode) {
                            "LIGHT" -> "Light Theme"
                            "DARK" -> "Dark Theme"
                            else -> "System Default"
                        },
                        onClick = { showThemeDialog = true }
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

                    if (showAmoledToggle) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        SettingSwitchRow(
                            title = "AMOLED Dark Mode",
                            description = "Use absolute black backgrounds for dark themes.",
                            checked = isAmoledDark,
                            onCheckedChange = { viewModel.setAmoledDark(it) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // SECTION 2: Album Art Settings
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Album Art",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                SettingsCard(
                    title = "Artwork Customization",
                    icon = Icons.Default.Palette
                ) {
                    SettingSwitchRow(
                        title = "Show Album Artwork",
                        description = "Display cover art inside player screens and lists.",
                        checked = showAlbumArt,
                        onCheckedChange = { viewModel.setShowAlbumArt(it) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    SettingsNavigationRow(
                        title = "Album Art Click Action",
                        description = when (albumArtClickAction) {
                            AlbumArtClickAction.DO_NOTHING -> "Do Nothing"
                            AlbumArtClickAction.SHOW_LYRICS -> "Show Lyrics"
                            AlbumArtClickAction.VIEW_ALBUM_ART -> "View Album Art"
                            AlbumArtClickAction.PLAY_PAUSE -> "Play/Pause"
                        },
                        onClick = { showClickActionDialog = true }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    var sliderValue by remember(playerBackgroundBlurRadius) { mutableStateOf(playerBackgroundBlurRadius) }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Background Blur Intensity",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Adjust the blur radius of the player background.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${sliderValue.toInt()} dp",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            onValueChangeFinished = {
                                viewModel.setPlayerBackgroundBlurRadius(sliderValue)
                            },
                            valueRange = 10f..100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    AlbumArtQualitySelector(
                        currentQuality = albumArtQuality,
                        onQualitySelected = { viewModel.setAlbumArtQuality(it) }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    SettingSwitchRow(
                        title = "Force Square Artwork",
                        description = "Crop artwork to square. Disabling this shows the original portrait/landscape aspect ratio.",
                        checked = forceSquareArtwork,
                        onCheckedChange = { viewModel.setForceSquareArtwork(it) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // SECTION 3: Lyrics
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Lyrics Settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                SettingsCard(
                    title = "Lyrics Preferences",
                    icon = Icons.Default.Palette
                ) {
                    SettingSwitchRow(
                        title = "Keep Screen On",
                        description = "Prevent the screen from sleeping while lyrics are visible.",
                        checked = keepScreenOnWithLyrics,
                        onCheckedChange = { viewModel.setKeepScreenOnWithLyrics(it) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentMode = themeMode,
            onDismiss = { showThemeDialog = false },
            onSelectMode = { viewModel.setThemeMode(it) }
        )
    }

    if (showClickActionDialog) {
        ClickActionSelectionDialog(
            currentAction = albumArtClickAction,
            onDismiss = { showClickActionDialog = false },
            onSelectAction = { viewModel.setAlbumArtClickAction(it) }
        )
    }
}

@Composable
fun ThemeSelectionDialog(
    currentMode: String,
    onDismiss: () -> Unit,
    onSelectMode: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Text(
                text = "Theme Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    "SYSTEM" to "System Default",
                    "LIGHT" to "Light",
                    "DARK" to "Dark"
                ).forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectMode(mode)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = {
                                onSelectMode(mode)
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun ClickActionSelectionDialog(
    currentAction: AlbumArtClickAction,
    onDismiss: () -> Unit,
    onSelectAction: (AlbumArtClickAction) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Text(
                text = "Album Art Click Action",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AlbumArtClickAction.entries.forEach { action ->
                    val label = when (action) {
                        AlbumArtClickAction.DO_NOTHING -> "Do Nothing"
                        AlbumArtClickAction.SHOW_LYRICS -> "Show Lyrics"
                        AlbumArtClickAction.VIEW_ALBUM_ART -> "View Album Art"
                        AlbumArtClickAction.PLAY_PAUSE -> "Play/Pause"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectAction(action)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentAction == action,
                            onClick = {
                                onSelectAction(action)
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumArtQualitySelector(
    currentQuality: AlbumArtQuality,
    onQualitySelected: (AlbumArtQuality) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Album Art Quality",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when (currentQuality) {
                    AlbumArtQuality.SAVE_SPACE -> "Save Space: Reduces memory usage and loading times."
                    AlbumArtQuality.BALANCED -> "Balanced: Good trade-off between detail and performance."
                    AlbumArtQuality.HIGH_QUALITY -> "High Quality: Uses higher resolution images for clearer artwork."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            AlbumArtQuality.entries.forEachIndexed { index, quality ->
                val label = when (quality) {
                    AlbumArtQuality.SAVE_SPACE -> "Save Space"
                    AlbumArtQuality.BALANCED -> "Balanced"
                    AlbumArtQuality.HIGH_QUALITY -> "High Quality"
                }
                SegmentedButton(
                    selected = currentQuality == quality,
                    onClick = { onQualitySelected(quality) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = AlbumArtQuality.entries.size
                    )
                ) {
                    Text(text = label)
                }
            }
        }
    }
}
