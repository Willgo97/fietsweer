package nl.fietsweer.app.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

// For cycling anything from 30 km/h is hard work, so the heads step up early.
private fun headsFor(kmh: Double): Int = when {
    kmh.isNaN() || kmh < 15 -> 1
    kmh < 30 -> 2
    else -> 3
}

// Points the way the wind blows to, so it reads as flow; fromDegrees is the meteorological direction.
fun DrawScope.drawWindArrow(centre: Offset, fromDegrees: Double, kmh: Double, color: Color, length: Float) {
    if (fromDegrees.isNaN()) return
    val half = length / 2
    val head = length * 0.34f
    val stroke = Stroke(width = length * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    rotate((fromDegrees + 180).toFloat(), centre) {
        val tipY = centre.y - half
        drawLine(color, Offset(centre.x, centre.y + half), Offset(centre.x, tipY), strokeWidth = stroke.width, cap = StrokeCap.Round)
        repeat(headsFor(kmh)) { i ->
            val y = tipY + i * head * 0.85f
            val chevron = Path().apply {
                moveTo(centre.x - head * 0.7f, y + head)
                lineTo(centre.x, y)
                lineTo(centre.x + head * 0.7f, y + head)
            }
            drawPath(chevron, color, style = stroke)
        }
    }
}

@Composable
fun WindArrow(fromDegrees: Double, kmh: Double) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(14.dp)) {
        drawWindArrow(center, fromDegrees, kmh, color, size.minDimension * 0.9f)
    }
}
