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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.SeekBarStyle
import com.devson.vedtune.ui.components.VedTuneIconButton
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.VedTuneTextStyles
import com.devson.vedtune.ui.theme.spacing

@Composable
fun PlayerInterfaceSettingScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val seekbarStyle by viewModel.seekbarStyle.collectAsState()
    val showRemainingTime by viewModel.showRemainingTime.collectAsState()
    val showMiniPlayerProgress by viewModel.showMiniPlayerProgress.collectAsState()
    val isGestureMiniPlayerEnabled by viewModel.isGestureMiniPlayerEnabled.collectAsState()
    val enableSwipeToSkip by viewModel.enableSwipeToSkip.collectAsState()
    val showLyricsButton by viewModel.showLyricsButton.collectAsState()
    val showSleepTimerButton by viewModel.showSleepTimerButton.collectAsState()
    val showShuffleRepeatButtons by viewModel.showShuffleRepeatButtons.collectAsState()

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
                text = "Player Interface",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
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
            // Seekbar & Progress Card
            item {
                InterfaceSectionCard(
                    title = "Visual Progress Indicators",
                    icon = Icons.Default.Palette
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                        Text(
                            text = "Seekbar Style",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
                        ) {
                            SeekBarStyle.entries.forEach { style ->
                                FilterChip(
                                    selected = (seekbarStyle == style),
                                    onClick = { viewModel.setSeekBarStyle(style) },
                                    label = {
                                        Text(
                                            text = when (style) {
                                                SeekBarStyle.DEFAULT -> "Standard"
                                                SeekBarStyle.SLIM -> "Slim"
                                                SeekBarStyle.WAVY -> "Waveform"
                                            }
                                        )
                                    },
                                    shape = VedTuneShapeTokens.Pill
                                )
                            }
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
                                text = "Show Remaining Time",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Display countdown remaining time instead of total duration",
                                style = VedTuneTextStyles.Metadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showRemainingTime,
                            onCheckedChange = { viewModel.setShowRemainingTime(it) }
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
                                text = "Show Mini Player Progress",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Display thin progress line across top of mini player",
                                style = VedTuneTextStyles.Metadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showMiniPlayerProgress,
                            onCheckedChange = { viewModel.setShowMiniPlayerProgress(it) }
                        )
                    }
                }
            }

            // Controls Visibility Card
            item {
                InterfaceSectionCard(
                    title = "Controls Visibility & Gestures",
                    icon = Icons.Default.Tune
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Show Lyrics Button",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Access synchronized lyrics in player action bar",
                                style = VedTuneTextStyles.Metadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showLyricsButton,
                            onCheckedChange = { viewModel.setShowLyricsButton(it) }
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
                                text = "Show Sleep Timer Button",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Quick shortcut to configure sleep timer in player",
                                style = VedTuneTextStyles.Metadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showSleepTimerButton,
                            onCheckedChange = { viewModel.setShowSleepTimerButton(it) }
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
                                text = "Show Shuffle & Repeat",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Display playback mode buttons next to playback transport",
                                style = VedTuneTextStyles.Metadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showShuffleRepeatButtons,
                            onCheckedChange = { viewModel.setShowShuffleRepeatButtons(it) }
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
                                text = "Swipe on Artwork to Skip",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Swipe left/right on full player album artwork to skip songs",
                                style = VedTuneTextStyles.Metadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableSwipeToSkip,
                            onCheckedChange = { viewModel.setEnableSwipeToSkip(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InterfaceSectionCard(
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
