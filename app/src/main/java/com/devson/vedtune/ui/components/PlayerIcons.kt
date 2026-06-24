package com.devson.vedtune.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object PlayerIcons {
    val Previous: ImageVector = ImageVector.Builder(
        name = "Previous",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(6f, 6f)
            lineTo(8f, 6f)
            verticalLineTo(18f)
            horizontalLineTo(6f)
            close()
            moveTo(18f, 6f)
            lineTo(9.5f, 12f)
            lineTo(18f, 18f)
            close()
        }
    }.build()

    val Next: ImageVector = ImageVector.Builder(
        name = "Next",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(6f, 6f)
            lineTo(14.5f, 12f)
            lineTo(6f, 18f)
            close()
            moveTo(16f, 6f)
            lineTo(18f, 6f)
            verticalLineTo(18f)
            horizontalLineTo(16f)
            close()
        }
    }.build()

    val Shuffle: ImageVector = ImageVector.Builder(
        name = "Shuffle",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(10.59f, 9.17f)
            lineTo(5.41f, 4f)
            horizontalLineTo(4f)
            verticalLineTo(6f)
            horizontalLineTo(4.59f)
            lineTo(9.78f, 11.19f)
            close()
            moveTo(20f, 4f)
            horizontalLineTo(15f)
            verticalLineTo(6f)
            horizontalLineTo(18f)
            lineTo(14.12f, 9.88f)
            lineTo(15.54f, 11.3f)
            lineTo(20f, 6.83f)
            verticalLineTo(9f)
            horizontalLineTo(22f)
            verticalLineTo(4f)
            close()
            moveTo(10.59f, 14.83f)
            lineTo(14.83f, 10.59f)
            lineTo(16.24f, 12f)
            lineTo(12f, 16.24f)
            close()
            // simple diagonal connection
            moveTo(4.59f, 18f)
            lineTo(10.59f, 12f)
            lineTo(12f, 13.41f)
            lineTo(6f, 19.41f)
            horizontalLineTo(4f)
            close()
        }
    }.build()

    val Repeat: ImageVector = ImageVector.Builder(
        name = "Repeat",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(7f, 7f)
            horizontalLineTo(17f)
            verticalLineTo(10f)
            lineTo(21f, 6f)
            lineTo(17f, 2f)
            verticalLineTo(5f)
            horizontalLineTo(5f)
            verticalLineTo(11f)
            horizontalLineTo(7f)
            close()
            moveTo(17f, 17f)
            horizontalLineTo(7f)
            verticalLineTo(14f)
            lineTo(3f, 18f)
            lineTo(7f, 22f)
            verticalLineTo(19f)
            horizontalLineTo(19f)
            verticalLineTo(13f)
            horizontalLineTo(17f)
            close()
        }
    }.build()

    val RepeatOne: ImageVector = ImageVector.Builder(
        name = "RepeatOne",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(7f, 7f)
            horizontalLineTo(17f)
            verticalLineTo(10f)
            lineTo(21f, 6f)
            lineTo(17f, 2f)
            verticalLineTo(5f)
            horizontalLineTo(5f)
            verticalLineTo(11f)
            horizontalLineTo(7f)
            close()
            moveTo(17f, 17f)
            horizontalLineTo(7f)
            verticalLineTo(14f)
            lineTo(3f, 18f)
            lineTo(7f, 22f)
            verticalLineTo(19f)
            horizontalLineTo(19f)
            verticalLineTo(13f)
            horizontalLineTo(17f)
            close()
            // digit 1 in center
            moveTo(11f, 9f)
            horizontalLineTo(13f)
            verticalLineTo(15f)
            horizontalLineTo(11f)
            close()
        }
    }.build()

    val Timer: ImageVector = ImageVector.Builder(
        name = "Timer",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 2f)
            curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
            curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
            curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
            close()
            moveTo(12.5f, 13f)
            horizontalLineTo(7f)
            verticalLineTo(11.5f)
            horizontalLineTo(11f)
            verticalLineTo(7f)
            horizontalLineTo(12.5f)
            close()
        }
    }.build()
}
