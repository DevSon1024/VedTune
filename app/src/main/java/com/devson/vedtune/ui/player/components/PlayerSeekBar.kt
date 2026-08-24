package com.devson.vedtune.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devson.vedtune.domain.model.SeekBarStyle
import com.devson.vedtune.ui.components.VedTuneSeekBar
import kotlinx.coroutines.flow.StateFlow

@Composable
fun PlayerSeekBar(
    positionState: StateFlow<Long>,
    duration: Long,
    showRemainingTime: Boolean,
    style: SeekBarStyle,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    onToggleRemainingTime: () -> Unit,
    modifier: Modifier = Modifier
) {
    val position by positionState.collectAsStateWithLifecycle()
    var isDragging by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(position.toFloat()) }
    val hapticFeedback = LocalHapticFeedback.current
    var lastHapticTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(position) {
        if (!isDragging) {
            sliderValue = position.toFloat()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        VedTuneSeekBar(
            value = sliderValue,
            onValueChange = { value ->
                isDragging = true
                sliderValue = value
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastHapticTime > 40L) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastHapticTime = currentTime
                }
                onSeek(value.toLong())
            },
            onValueChangeFinished = {
                isDragging = false
                onSeek(sliderValue.toLong())
            },
            valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
            style = style,
            isPlaying = isPlaying,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatPlayerTime(sliderValue.toLong()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onToggleRemainingTime() }
            )
            val endLabel = if (showRemainingTime) {
                "-${formatPlayerTime((duration - sliderValue.toLong()).coerceAtLeast(0L))}"
            } else {
                formatPlayerTime(duration)
            }
            Text(
                text = endLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onToggleRemainingTime() }
            )
        }
    }
}
