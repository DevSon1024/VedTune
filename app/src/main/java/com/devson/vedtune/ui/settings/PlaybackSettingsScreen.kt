package com.devson.vedtune.ui.settings

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val autoplayOnStartup by viewModel.autoplayOnStartup.collectAsState()
    val audioFadeInEnabled by viewModel.audioFadeInEnabled.collectAsState()
    val showRemainingTime by viewModel.showRemainingTime.collectAsState()
    val showMiniPlayerProgress by viewModel.showMiniPlayerProgress.collectAsState()
    val isGestureMiniPlayerEnabled by viewModel.isGestureMiniPlayerEnabled.collectAsState()
    val enableSwipeToSkip by viewModel.enableSwipeToSkip.collectAsState()
    val defaultStartScreen by viewModel.defaultStartScreen.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Playback Preferences",
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

                SettingSwitchRow(
                    title = "Swipe to Skip Artwork",
                    description = "Swipe left/right on player album artwork to skip songs.",
                    checked = enableSwipeToSkip,
                    onCheckedChange = { viewModel.setEnableSwipeToSkip(it) },
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

            // System Equalizer
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

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
