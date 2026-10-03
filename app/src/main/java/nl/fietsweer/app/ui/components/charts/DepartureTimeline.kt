package nl.fietsweer.app.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.domain.RideAssessment
import nl.fietsweer.app.ui.theme.AppTheme

@Composable
fun DepartureTimeline(
    slots: List<RideAssessment>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (slots.isEmpty()) return
    val accents = AppTheme.accents
    val format = AppTheme.format
    val outline = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current.density
    val slotCount = slots.size

    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .pointerInput(slotCount) {
                    detectTapGestures { position ->
                        onSelect(((position.x / size.width) * slotCount).toInt().coerceIn(0, slotCount - 1))
                    }
                }
                .pointerInput(slotCount) {
                    detectHorizontalDragGestures { change, _ ->
                        onSelect(((change.position.x / size.width) * slotCount).toInt().coerceIn(0, slotCount - 1))
                    }
                }
        ) {
            val slotWidth = size.width / slotCount
            val gap = (slotWidth * 0.12f).coerceAtMost(1.6f * density)
            val radius = androidx.compose.ui.geometry.CornerRadius(1.6f * density)

            slots.forEachIndexed { i, slot ->
                val x = i * slotWidth
                drawRoundRect(
                    color = accents.forRisk(slot.risk).copy(alpha = if (slot.isNight) 0.40f else 1f),
                    topLeft = Offset(x + gap / 2, 0f),
                    size = Size((slotWidth - gap).coerceAtLeast(1f), size.height),
                    cornerRadius = radius
                )
            }

            for (i in slots.indices) {
                val calendar = java.util.Calendar.getInstance().apply { timeInMillis = slots[i].departureMs }
                if (calendar.get(java.util.Calendar.MINUTE) != 0) continue
                if (calendar.get(java.util.Calendar.HOUR_OF_DAY) % 6 != 0) continue
                drawLine(
                    color = Color.White.copy(alpha = 0.55f),
                    start = Offset(i * slotWidth, 0f),
                    end = Offset(i * slotWidth, size.height),
                    strokeWidth = 1f * density
                )
            }

            val selectedX = selectedIndex.coerceIn(0, slotCount - 1) * slotWidth
            drawRoundRect(
                color = outline,
                topLeft = Offset(selectedX - 1f * density, -1f * density),
                size = Size(slotWidth + 2f * density, size.height + 2f * density),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * density),
                style = Stroke(width = 2.2f * density)
            )
        }

        Spacer(Modifier.height(5.dp))
        TimeAxis(List(AXIS_LABEL_COUNT) { i ->
            slots[(i * (slotCount - 1) / (AXIS_LABEL_COUNT - 1)).coerceIn(0, slotCount - 1)].departureMs
        })
        val selected = slots[selectedIndex.coerceIn(0, slotCount - 1)]
        Spacer(Modifier.height(6.dp))
        Text(
            "${format.dayTime(selected.departureMs)} \u00b7 ${format.percent(selected.risk)}",
            style = MaterialTheme.typography.labelLarge,
            color = accents.forRisk(selected.risk)
        )
    }
}
