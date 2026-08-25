package com.devson.vedtune.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devson.vedtune.core.toFormattedDuration
import com.devson.vedtune.ui.theme.VedTuneIconSizes
import com.devson.vedtune.ui.theme.VedTuneMotion
import com.devson.vedtune.ui.theme.spacing

@Composable
fun VedTuneTopAppBar(
    title: String,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    searchPlaceholder: String = "Search...",
    showSearchAction: Boolean = true,
    showSortAction: Boolean = false,
    onSortClick: (() -> Unit)? = null,
    showLayoutToggleAction: Boolean = false,
    isGridView: Boolean = false,
    onLayoutToggleClick: (() -> Unit)? = null,
    totalItemCount: Int? = null,
    itemTypeLabel: String = "songs",
    totalDurationMs: Long? = null,
    onShuffleClick: (() -> Unit)? = null
) {
    var isSearchActive by rememberSaveable { mutableStateOf(searchQuery.isNotEmpty()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        AnimatedContent(
            targetState = isSearchActive,
            transitionSpec = {
                VedTuneMotion.FadeTransition
            },
            label = "TopAppBarSearchTransition"
        ) { active ->
            if (active) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = MaterialTheme.spacing.s),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VedTuneIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Cancel Search",
                        onClick = {
                            isSearchActive = false
                            onQueryChange("")
                        },
                        iconSize = VedTuneIconSizes.Standard,
                        tint = MaterialTheme.colorScheme.onSurface
                    )

                    val focusRequester = remember { FocusRequester() }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        placeholder = { Text(searchPlaceholder) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                VedTuneIconButton(
                                    icon = Icons.Default.Close,
                                    contentDescription = "Clear Text",
                                    onClick = { onQueryChange("") },
                                    iconSize = VedTuneIconSizes.Medium,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )

                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = MaterialTheme.spacing.l),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (showSearchAction) {
                            VedTuneIconButton(
                                icon = Icons.Default.Search,
                                contentDescription = "Search",
                                onClick = { isSearchActive = true },
                                iconSize = VedTuneIconSizes.Standard,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (showSortAction && onSortClick != null) {
                            VedTuneIconButton(
                                icon = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort Options",
                                onClick = onSortClick,
                                iconSize = VedTuneIconSizes.Standard,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (showLayoutToggleAction && onLayoutToggleClick != null) {
                            VedTuneIconButton(
                                icon = if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Filled.GridView,
                                contentDescription = "Toggle Layout",
                                onClick = onLayoutToggleClick,
                                iconSize = VedTuneIconSizes.Standard,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        if (totalItemCount != null) {
            val metadataText = buildString {
                append("$totalItemCount $itemTypeLabel")
                if (totalDurationMs != null && totalDurationMs > 0) {
                    append(" • ${totalDurationMs.toFormattedDuration()}")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = MaterialTheme.spacing.l,
                        end = MaterialTheme.spacing.l,
                        bottom = MaterialTheme.spacing.s
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = metadataText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                if (onShuffleClick != null && totalItemCount > 0) {
                    VedTuneIconButton(
                        icon = Icons.Default.Shuffle,
                        contentDescription = "Shuffle All",
                        onClick = onShuffleClick,
                        iconSize = VedTuneIconSizes.Medium,
                        touchTargetSize = 36.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
