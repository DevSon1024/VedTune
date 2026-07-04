package com.devson.vedtune.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
fun PlayerInterfaceSettingScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val seekbarStyle by viewModel.seekbarStyle.collectAsState()
    val showRemainingTime by viewModel.showRemainingTime.collectAsState()
    val showMiniPlayerProgress by viewModel.showMiniPlayerProgress.collectAsState()
    
    val isGestureMiniPlayerEnabled by viewModel.isGestureMiniPlayerEnabled.collectAsState()
    val enableSwipeToSkip by viewModel.enableSwipeToSkip.collectAsState()
    
    val showLyricsButton by viewModel.showLyricsButton.collectAsState()
    val showSleepTimerButton by viewModel.showSleepTimerButton.collectAsState()
    val showShuffleRepeatButtons by viewModel.showShuffleRepeatButtons.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Player Interface",
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
            // SECTION 1: Seekbar & Progress
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Seekbar & Progress",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                SettingsCard(
                    title = "Visual Progress Indicators",
                    icon = Icons.Default.Palette
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
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SeekBarStyle.entries.forEachIndexed { index, style ->
                                val label = when (style) {
                                    SeekBarStyle.DEFAULT -> "Line"
                                    SeekBarStyle.SLIM -> "Dashed"
                                    SeekBarStyle.WAVY -> "Wave"
                                }
                                SegmentedButton(
                                    selected = seekbarStyle == style,
                                    onClick = { viewModel.setSeekBarStyle(style) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = SeekBarStyle.entries.size
                                    )
                                ) {
                                    Text(text = label)
                                }
                            }
                        }
                    }

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
                }
            }

            // SECTION 2: Player Controls
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Player Controls",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                SettingsCard(
                    title = "Control Visibility & Actions",
                    icon = Icons.Rounded.Tune
                ) {
                    SettingSwitchRow(
                        title = "Show Lyrics Button",
                        description = "Show button to open the lyrics panel on player controls.",
                        checked = showLyricsButton,
                        onCheckedChange = { viewModel.setShowLyricsButton(it) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    SettingSwitchRow(
                        title = "Show Sleep Timer",
                        description = "Show button to set sleep timer on player controls.",
                        checked = showSleepTimerButton,
                        onCheckedChange = { viewModel.setShowSleepTimerButton(it) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    SettingSwitchRow(
                        title = "Show Shuffle & Repeat",
                        description = "Show shuffle and repeat buttons on player controls.",
                        checked = showShuffleRepeatButtons,
                        onCheckedChange = { viewModel.setShowShuffleRepeatButtons(it) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    SettingSwitchRow(
                        title = "Gesture-Based Mini Player",
                        description = "Control playback using gestures on the mini player.",
                        checked = isGestureMiniPlayerEnabled,
                        onCheckedChange = { viewModel.setGestureMiniPlayerEnabled(it) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    SettingSwitchRow(
                        title = "Swipe to Skip Artwork",
                        description = "Swipe left/right on player album artwork to skip songs.",
                        checked = enableSwipeToSkip,
                        onCheckedChange = { viewModel.setEnableSwipeToSkip(it) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
