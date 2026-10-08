package nl.fietsweer.app.ui.screens.map

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

// Draws the moment between two frames: the first pushed forward along the motion field, the
// second pulled back, added together so the rain keeps its density while it moves.
internal fun DrawScope.drawBlend(
    first: MapFrame,
    second: MapFrame?,
    motion: MotionField?,
    fraction: Float,
    topLeft: Offset,
    bottomRight: Offset,
    opacity: Float
) {
    val width = bottomRight.x - topLeft.x
    val height = bottomRight.y - topLeft.y
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        val layer = native.saveLayerAlpha(null, (opacity * 255).toInt())
        val firstBitmap = first.image.asAndroidBitmap()
        native.drawBitmapMesh(
            firstBitmap, RadarMotion.COLUMNS, RadarMotion.ROWS,
            meshVertices(topLeft, width, height, motion, fraction, width / firstBitmap.width, height / firstBitmap.height),
            0, null, 0,
            Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = ((1 - fraction) * 255).toInt() }
        )
        if (second != null && fraction > 0f) {
            val secondBitmap = second.image.asAndroidBitmap()
            native.drawBitmapMesh(
                secondBitmap, RadarMotion.COLUMNS, RadarMotion.ROWS,
                meshVertices(topLeft, width, height, motion, fraction - 1, width / secondBitmap.width, height / secondBitmap.height),
                0, null, 0,
                Paint(Paint.FILTER_BITMAP_FLAG).apply {
                    alpha = (fraction * 255).toInt()
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
                }
            )
        }
        native.restoreToCount(layer)
    }
}

private fun meshVertices(
    topLeft: Offset,
    width: Float,
    height: Float,
    motion: MotionField?,
    shift: Float,
    scaleX: Float,
    scaleY: Float
): FloatArray {
    val columns = RadarMotion.COLUMNS
    val rows = RadarMotion.ROWS
    val vertices = FloatArray((columns + 1) * (rows + 1) * 2)
    for (row in 0..rows) for (column in 0..columns) {
        val vertex = row * (columns + 1) + column
        vertices[vertex * 2] = topLeft.x + width * column / columns + (motion?.dx?.get(vertex) ?: 0f) * shift * scaleX
        vertices[vertex * 2 + 1] = topLeft.y + height * row / rows + (motion?.dy?.get(vertex) ?: 0f) * shift * scaleY
    }
    return vertices
}
