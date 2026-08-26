package com.devson.vedtune.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.AlbumArtQuality
import com.devson.vedtune.ui.components.VedTuneConfirmDialog
import com.devson.vedtune.ui.components.VedTuneIconButton
import com.devson.vedtune.ui.components.VedTuneInfoDialog
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.VedTuneTextStyles
import com.devson.vedtune.ui.theme.spacing

@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColorsEnabled by viewModel.dynamicColorsEnabled.collectAsState()
    val showAlbumArt by viewModel.showAlbumArt.collectAsState()
    val isAmoledDark by viewModel.isAmoledDark.collectAsState()
    val albumArtQuality by viewModel.albumArtQuality.collectAsState()
    val forceSquareArtwork by viewModel.forceSquareArtwork.collectAsState()
    val defaultStartScreen by viewModel.defaultStartScreen.collectAsState()

    val systemInDark = isSystemInDarkTheme()
    val showAmoledToggle = themeMode == "DARK" || (themeMode == "SYSTEM" && systemInDark)

    var showThemeDialog by remember { mutableStateOf(false) }
    var showStartTabDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = MaterialTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VedTuneIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Go Back",
                onClick = onNavigateBack,
                iconSize = VedTuneIconSizes.Medium
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.s))
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            VedTuneIconButton(
                icon = Icons.Default.RestartAlt,
                contentDescription = "Reset Appearance Settings",
                onClick = { showResetDialog = true },
                iconSize = VedTuneIconSizes.Medium,
                tint = MaterialTheme.colorScheme.error
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.l,
                end = MaterialTheme.spacing.l,
                top = MaterialTheme.spacing.s,
                bottom = MaterialTheme.spacing.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)
        ) {
            // Theme Mode & Dynamic Colors Card
            item {
                AppearanceCard(
                    title = "Theme & Palette",
                    icon = Icons.Default.Palette
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showThemeDialog = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Theme Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (themeMode) {
                                    "LIGHT" -> "Light Mode"
                                    "DARK" -> "Dark Mode"
                                    else -> "System Default"
                                },
                                style = VedTuneTextStyles.Metadata,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (showAmoledToggle) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pure Black (AMOLED)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "True pitch-black backgrounds to conserve OLED battery",
                                    style = VedTuneTextStyles.Metadata,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isAmoledDark,
                                onCheckedChange = { viewModel.setAmoledDark(it) }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dynamic Colors (Material You)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Extract theme accent colors from your system wallpaper",
                                style = VedTuneTextStyles.Metadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = dynamicColorsEnabled,
                            onCheckedChange = { viewModel.setDynamicColorsEnabled(it) }
                        )
                    }
                }
            }

            // Artwork Display Card
            item {
                AppearanceCard(
                    title = "Album Artwork",
                    icon = Icons.Default.Image
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Show Album Art",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Display embedded album artwork across lists and cards",
                                style = VedTuneTextStyles.Metadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showAlbumArt,
                            onCheckedChange = { viewModel.setShowAlbumArt(it) }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enforce Square Artwork",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Crop non-standard ratio images to clean 1:1 squares",
                                style = VedTuneTextStyles.Metadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = forceSquareArtwork,
                            onCheckedChange = { viewModel.setForceSquareArtwork(it) }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                        Text(
                            text = "Artwork Resolution & Cache Quality",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
                        ) {
                            AlbumArtQuality.entries.forEach { quality ->
                                FilterChip(
                                    selected = (albumArtQuality == quality),
                                    onClick = { viewModel.setAlbumArtQuality(quality) },
                                    label = {
                                        Text(
                                            text = when (quality) {
                                                AlbumArtQuality.SAVE_SPACE -> "Fast (128px)"
                                                AlbumArtQuality.BALANCED -> "Balanced (256px)"
                                                AlbumArtQuality.HIGH_QUALITY -> "High (512px)"
                                            }
                                        )
                                    },
                                    shape = VedTuneShapeTokens.Pill
                                )
                            }
                        }
                    }
                }
            }

            // Navigation Defaults Card
            item {
                AppearanceCard(
                    title = "Navigation & Startup",
                    icon = Icons.Default.Tab
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStartTabDialog = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Default Start Tab",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (defaultStartScreen.lowercase()) {
                                    "home_tab" -> "Home"
                                    "search_tab" -> "Search"
                                    "library_tab", "songs" -> "Library"
                                    else -> "Songs"
                                },
                                style = VedTuneTextStyles.Metadata,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    // Theme Picker Dialog
    if (showThemeDialog) {
        VedTuneInfoDialog(
            title = "Choose Theme",
            onDismiss = { showThemeDialog = false },
            confirmButtonText = "Done"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "SYSTEM" to "System Default",
                    "LIGHT" to "Light Mode",
                    "DARK" to "Dark Mode"
                ).forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(VedTuneShapeTokens.Medium)
                            .clickable {
                                viewModel.setThemeMode(mode)
                                showThemeDialog = false
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (themeMode == mode),
                            onClick = {
                                viewModel.setThemeMode(mode)
                                showThemeDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.s))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    // Start Tab Picker Dialog
    if (showStartTabDialog) {
        VedTuneInfoDialog(
            title = "Choose Default Tab",
            onDismiss = { showStartTabDialog = false },
            confirmButtonText = "Done"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "home_tab" to "Home",
                    "search_tab" to "Search",
                    "library_tab" to "Library"
                ).forEach { (route, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(VedTuneShapeTokens.Medium)
                            .clickable {
                                viewModel.setDefaultStartScreen(route)
                                showStartTabDialog = false
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (defaultStartScreen == route),
                            onClick = {
                                viewModel.setDefaultStartScreen(route)
                                showStartTabDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.s))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    // Reset Appearance Dialog
    if (showResetDialog) {
        VedTuneConfirmDialog(
            title = "Reset Appearance Settings?",
            message = "This will restore theme to System Default, disable pure black AMOLED, enable dynamic colors, and set default start tab to Home.",
            confirmText = "Reset",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                viewModel.resetAppearanceSettings()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }
}

@Composable
private fun AppearanceCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = VedTuneShapeTokens.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(VedTuneIconSizes.Medium)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            content()
        }
    }
}
