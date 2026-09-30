package com.devson.vedtune.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class QueueInfo(
    val id: Long,
    val name: String,
    val orderIndex: Int,
    val songCount: Int,
    val createdAt: Long
) {
    companion object {
        const val DEFAULT_QUEUE_ID = 1L
        const val DEFAULT_QUEUE_NAME = "All Songs"
        const val LOCAL_QUEUE_ID = -1L
    }
}
