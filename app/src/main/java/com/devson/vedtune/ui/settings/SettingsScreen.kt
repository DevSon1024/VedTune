package com.devson.vedtune.ui.settings

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val showAlbumArt by viewModel.showAlbumArt.collectAsState()
    val showRemainingTime by viewModel.showRemainingTime.collectAsState()
    val showMiniPlayerProgress by viewModel.showMiniPlayerProgress.collectAsState()
    val autoplayOnStartup by viewModel.autoplayOnStartup.collectAsState()

    var queueClearedMessageVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
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

        SettingSwitchRow(
            title = "Show Album Artwork",
            description = "Display cover art inside player screens and lists.",
            checked = showAlbumArt,
            onCheckedChange = { viewModel.setShowAlbumArt(it) }
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
