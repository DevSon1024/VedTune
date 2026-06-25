package com.devson.vedtune.ui.songs

import com.devson.vedtune.core.UiEvent
import com.devson.vedtune.core.UiState
import com.devson.vedtune.domain.model.Song

enum class SortBy {
    TITLE, ARTIST, ALBUM, DATE_ADDED
}

enum class SortOrder {
    ASCENDING, DESCENDING
}

data class SongsUiState(
    val songs: List<Song> = emptyList(),
    val searchQuery: String = "",
    val sortBy: SortBy = SortBy.TITLE,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val isGridView: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val showArtwork: Boolean = true,
    val totalItemCount: Int = 0,
    val totalDurationMs: Long = 0L
) : UiState

sealed interface SongsUiEvent : UiEvent {
    data class ShowError(val message: String) : SongsUiEvent
    data class LaunchIntentSender(val intentSender: android.content.IntentSender) : SongsUiEvent
}
