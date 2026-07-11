package com.devson.vedtune.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devson.vedtune.domain.model.ViewPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewSettingsSheet(
    preferences: ViewPreferences,
    onPreferencesChange: (ViewPreferences) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "View Options",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Layout Mode Toggle
            Text(
                text = "Layout Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val options = listOf("List", "Grid")
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                options.forEachIndexed { index, label ->
                    val isSelected = (index == 0 && !preferences.isGridView) || (index == 1 && preferences.isGridView)
                    SegmentedButton(
                        selected = isSelected,
                        onClick = {
                            onPreferencesChange(preferences.copy(isGridView = index == 1))
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Grid Span count slider (visible only if Grid view is selected)
            if (preferences.isGridView) {
                Text(
                    text = "Grid Columns: ${preferences.gridSpanCount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = preferences.gridSpanCount.toFloat(),
                    onValueChange = {
                        onPreferencesChange(preferences.copy(gridSpanCount = it.toInt()))
                    },
                    valueRange = 1f..6f,
                    steps = 4, // 1 to 6 has steps: 2, 3, 4, 5 (4 intermediate values)
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Card fields visibility
            Text(
                text = "Show Fields",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            PreferenceToggleRow(
                label = "Show Artist",
                checked = preferences.showArtist,
                onCheckedChange = { onPreferencesChange(preferences.copy(showArtist = it)) }
            )
            PreferenceToggleRow(
                label = "Show Album",
                checked = preferences.showAlbum,
                onCheckedChange = { onPreferencesChange(preferences.copy(showAlbum = it)) }
            )
            PreferenceToggleRow(
                label = "Show Duration",
                checked = preferences.showDuration,
                onCheckedChange = { onPreferencesChange(preferences.copy(showDuration = it)) }
            )
            PreferenceToggleRow(
                label = "Show Album Art",
                checked = preferences.showAlbumArt,
                onCheckedChange = { onPreferencesChange(preferences.copy(showAlbumArt = it)) }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PreferenceToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
