package com.devson.vedtune.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devson.vedtune.ui.components.VedTuneIconButton
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneShapeTokens
import com.devson.vedtune.ui.theme.spacing

@Composable
fun PlayerHeader(
    sleepTimerRemaining: Long,
    onBackClick: () -> Unit,
    onQueueClick: () -> Unit,
    onOptionsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Header Action Bar with perfectly centered NOW PLAYING title
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
            // Left: Back button
            VedTuneIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBackClick,
                iconSize = VedTuneIconSizes.Medium,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterStart)
            )

            // Center: Header title mathematically centered
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    letterSpacing = 1.2.sp
                )

                if (sleepTimerRemaining > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = VedTuneShapeTokens.Pill,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.s, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatPlayerTime(sleepTimerRemaining),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Right action buttons: Queue and Options
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VedTuneIconButton(
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = "Playback Queue",
                    onClick = onQueueClick,
                    iconSize = VedTuneIconSizes.Medium,
                    tint = MaterialTheme.colorScheme.onSurface
                )
                if (onOptionsClick != null) {
                    VedTuneIconButton(
                        icon = Icons.Rounded.MoreVert,
                        contentDescription = "More Options",
                        onClick = onOptionsClick,
                        iconSize = VedTuneIconSizes.Medium,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
}
