package com.devson.vedtune.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.Slider
import com.devson.vedtune.domain.model.AlbumArtClickAction
import com.devson.vedtune.domain.model.SeekBarStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColorsEnabled by viewModel.dynamicColorsEnabled.collectAsState()
    val showAlbumArt by viewModel.showAlbumArt.collectAsState()
    val seekbarStyle by viewModel.seekbarStyle.collectAsState()
    val keepScreenOnWithLyrics by viewModel.keepScreenOnWithLyrics.collectAsState()
    val albumArtClickAction by viewModel.albumArtClickAction.collectAsState()
    val playerBackgroundBlurRadius by viewModel.playerBackgroundBlurRadius.collectAsState()

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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
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

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                SeekBarStyleSelector(
                    currentStyle = seekbarStyle,
                    onStyleSelected = { viewModel.setSeekBarStyle(it) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                AlbumArtClickActionSelector(
                    currentAction = albumArtClickAction,
                    onActionSelected = { viewModel.setAlbumArtClickAction(it) }
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
            }

            // CARD 2: Lyrics Settings
            SettingsCard(
                title = "Lyrics Settings",
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

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun SeekBarStyleSelector(
    currentStyle: SeekBarStyle,
    onStyleSelected: (SeekBarStyle) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Seekbar Style",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (style in SeekBarStyle.entries) {
                val label = when (style) {
                    SeekBarStyle.DEFAULT -> "Default"
                    SeekBarStyle.SLIM -> "Slim"
                    SeekBarStyle.WAVY -> "Wavy"
                }
                val isSelected = currentStyle == style
                if (isSelected) {
                    FilledTonalButton(
                        onClick = { onStyleSelected(style) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = label)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onStyleSelected(style) },
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
fun AlbumArtClickActionSelector(
    currentAction: AlbumArtClickAction,
    onActionSelected: (AlbumArtClickAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Album Art Click Action",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            val label = when (currentAction) {
                AlbumArtClickAction.DO_NOTHING -> "Do Nothing"
                AlbumArtClickAction.SHOW_LYRICS -> "Show Lyrics"
                AlbumArtClickAction.VIEW_ALBUM_ART -> "View Album Art"
                AlbumArtClickAction.PLAY_PAUSE -> "Play/Pause"
            }
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = label)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                AlbumArtClickAction.entries.forEach { action ->
                    val actionLabel = when (action) {
                        AlbumArtClickAction.DO_NOTHING -> "Do Nothing"
                        AlbumArtClickAction.SHOW_LYRICS -> "Show Lyrics"
                        AlbumArtClickAction.VIEW_ALBUM_ART -> "View Album Art"
                        AlbumArtClickAction.PLAY_PAUSE -> "Play/Pause"
                    }
                    DropdownMenuItem(
                        text = { Text(actionLabel) },
                        onClick = {
                            onActionSelected(action)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
