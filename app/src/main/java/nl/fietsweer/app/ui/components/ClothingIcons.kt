package nl.fietsweer.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Material has no hat or scarf, so these two are drawn on its 24-unit grid.
object ClothingIcons {

    val Hat: ImageVector = ImageVector.Builder("Hat", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            // Pompom
            moveTo(12f, 2f)
            arcToRelative(2f, 2f, 0f, true, true, 0f, 4f)
            arcToRelative(2f, 2f, 0f, true, true, 0f, -4f)
            close()
            // Crown
            moveTo(4.5f, 15f)
            curveTo(4.5f, 9.8f, 7.9f, 6.8f, 12f, 6.8f)
            curveTo(16.1f, 6.8f, 19.5f, 9.8f, 19.5f, 15f)
            close()
            // Turned-up brim
            moveTo(4f, 16.2f)
            horizontalLineTo(20f)
            arcToRelative(1f, 1f, 0f, false, true, 1f, 1f)
            verticalLineTo(20f)
            arcToRelative(1f, 1f, 0f, false, true, -1f, 1f)
            horizontalLineTo(4f)
            arcToRelative(1f, 1f, 0f, false, true, -1f, -1f)
            verticalLineTo(17.2f)
            arcToRelative(1f, 1f, 0f, false, true, 1f, -1f)
            close()
        }
    }.build()

    val Scarf: ImageVector = ImageVector.Builder("Scarf", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            // The loop around the neck
            moveTo(5f, 4f)
            horizontalLineTo(19f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
            verticalLineTo(8f)
            arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
            horizontalLineTo(5f)
            arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
            verticalLineTo(6f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
            close()
            // Front end, hanging straight down
            moveTo(12.5f, 10.5f)
            horizontalLineTo(16f)
            verticalLineTo(20f)
            lineTo(14.25f, 21.5f)
            lineTo(12.5f, 20f)
            close()
            // Back end, swinging out
            moveTo(16.8f, 10.5f)
            lineTo(20f, 10.5f)
            lineTo(21.5f, 18f)
            lineTo(19.6f, 19.2f)
            lineTo(18.2f, 17.6f)
            close()
        }
    }.build()
}
