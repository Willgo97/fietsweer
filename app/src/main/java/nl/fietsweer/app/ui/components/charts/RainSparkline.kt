package nl.fietsweer.app.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

@Composable
fun RainSparkline(
    values: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
    height: Int = 26
) {
    if (values.isEmpty()) return
    val peak = max(0.3, values.maxOrNull() ?: 0.0)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height.dp)
    ) {
        val slotWidth = size.width / values.size
        values.forEachIndexed { i, value ->
            val fraction = (value / peak).coerceIn(0.0, 1.0).toFloat()
            val barHeight = max(size.height * 0.06f, size.height * fraction)
            drawRoundRect(
                color = if (value <= 0.001) color.copy(alpha = 0.18f) else color,
                topLeft = Offset(i * slotWidth + slotWidth * 0.15f, size.height - barHeight),
                size = Size(slotWidth * 0.7f, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(min(slotWidth * 0.35f, 4f))
            )
        }
    }
}
