package com.devson.vedtune.ui.player.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.devson.vedtune.ui.components.VedTuneIconButton
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.spacing

@Composable
fun ActionControlsStrip(
    isFav: Boolean,
    sleepTimerRemaining: Long,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    showLyricsButton: Boolean,
    showSleepTimerButton: Boolean,
    showShuffleRepeatButtons: Boolean,
    onSleepTimerClick: () -> Unit,
    onFavClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onInfoClick: () -> Unit,
    onOptionsClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onToggleLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Favorite pulse animation
    val favScale by animateFloatAsState(
        targetValue = if (isFav) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "FavPulse"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Favorite Button
            VedTuneIconButton(
                icon = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isFav) "Remove from Favorites" else "Add to Favorites",
                onClick = onFavClick,
                iconSize = VedTuneIconSizes.Medium,
                touchTargetSize = 44.dp,
                tint = if (isFav) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer {
                    scaleX = favScale
                    scaleY = favScale
                }
            )

            // Playlist Add Button
            VedTuneIconButton(
                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                contentDescription = "Add to Playlist",
                onClick = onPlaylistClick,
                iconSize = VedTuneIconSizes.Medium,
                touchTargetSize = 44.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Lyrics Button
            if (showLyricsButton) {
                VedTuneIconButton(
                    icon = Icons.Rounded.Description,
                    contentDescription = "Lyrics",
                    onClick = onToggleLyrics,
                    iconSize = VedTuneIconSizes.Medium,
                    touchTargetSize = 44.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Sleep Timer Button
            if (showSleepTimerButton) {
                VedTuneIconButton(
                    icon = Icons.Rounded.Timer,
                    contentDescription = "Sleep Timer",
                    onClick = onSleepTimerClick,
                    iconSize = VedTuneIconSizes.Medium,
                    touchTargetSize = 44.dp,
                    tint = if (sleepTimerRemaining > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Info Button
            VedTuneIconButton(
                icon = Icons.Rounded.Info,
                contentDescription = "Song Info",
                onClick = onInfoClick,
                iconSize = VedTuneIconSizes.Medium,
                touchTargetSize = 44.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // More Options Button
            VedTuneIconButton(
                icon = Icons.Rounded.MoreVert,
                contentDescription = "More Options",
                onClick = onOptionsClick,
                iconSize = VedTuneIconSizes.Medium,
                touchTargetSize = 44.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Shuffle & Repeat Controls
        if (showShuffleRepeatButtons) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                VedTuneIconButton(
                    icon = Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    onClick = onShuffleClick,
                    iconSize = VedTuneIconSizes.Medium,
                    touchTargetSize = 44.dp,
                    tint = if (shuffleModeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )

                // Repeat Button
                val repeatIcon = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
                val repeatTint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

                VedTuneIconButton(
                    icon = repeatIcon,
                    contentDescription = "Repeat",
                    onClick = onRepeatClick,
                    iconSize = VedTuneIconSizes.Medium,
                    touchTargetSize = 44.dp,
                    tint = repeatTint
                )
            }
        }
    }
}
