package com.devson.vedtune.core

/**
 * Marker interface for state representations in Composable screens.
 * Every screen state should be immutable and implement this interface.
 */
interface UiState

/**
 * Marker interface for one-off side-effects/events (e.g. Navigation, SnackBar, Permissions).
 */
interface UiEvent
