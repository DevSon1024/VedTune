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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
