package nl.fietsweer.app.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.ui.components.Caption
import nl.fietsweer.app.ui.theme.AppTheme
import kotlin.math.max

data class ChartPoint(val timeMs: Long, val precipMm: Double, val tempC: Double)

data class ChartBand(val startMs: Long, val endMs: Long, val color: Color, val label: String)

@Composable
fun PrecipTempChart(
    points: List<ChartPoint>,
    bands: List<ChartBand>,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return
    val format = AppTheme.format
    val accents = AppTheme.accents
    val rainColor = accents.rain
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val tempColor = MaterialTheme.colorScheme.tertiary
    val nowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)

    val startMs = points.first().timeMs
    val endMs = points.last().timeMs
    val spanMs = max(1L, endMs - startMs).toFloat()

    val precipitationScale = max(0.6, points.maxOf { if (it.precipMm.isNaN()) 0.0 else it.precipMm } * 1.15)
    val temps = points.map { it.tempC }.filter { !it.isNaN() }
    val minTemp = temps.minOrNull()
    val maxTemp = temps.maxOrNull()
    val tempAxisMin = (minTemp ?: 0.0) - 1.0
    val tempAxisMax = (maxTemp ?: 10.0) + 1.0
    val tempAxisSpan = max(1.0, tempAxisMax - tempAxisMin)

    val density = LocalDensity.current

    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(168.dp)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val chartWidth = size.width
                val bottomPadding = 2f
                val plotHeight = size.height - bottomPadding

                fun xOf(ms: Long): Float = ((ms - startMs) / spanMs) * chartWidth

                for (band in bands) {
                    val left = xOf(band.startMs).coerceIn(0f, chartWidth)
                    val right = xOf(band.endMs).coerceIn(0f, chartWidth)
                    if (right <= left) continue
                    drawRect(
                        color = band.color.copy(alpha = 0.14f),
                        topLeft = Offset(left, 0f),
                        size = Size(right - left, plotHeight)
                    )
                    drawLine(
                        color = band.color.copy(alpha = 0.55f),
                        start = Offset(left, 0f), end = Offset(left, plotHeight),
                        strokeWidth = with(density) { 1.5.dp.toPx() }
                    )
                }

                for (i in 1..3) {
                    val y = plotHeight * i / 4f
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y), end = Offset(chartWidth, y),
                        strokeWidth = with(density) { 1.dp.toPx() },
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f))
                    )
                }

                val barWidth = (chartWidth / points.size) * 0.62f
                for (point in points) {
                    val precipitation = if (point.precipMm.isNaN()) 0.0 else point.precipMm
                    if (precipitation <= 0.005) continue
                    val barHeight = (precipitation / precipitationScale).coerceIn(0.0, 1.0).toFloat() * plotHeight * 0.72f
                    val x = xOf(point.timeMs)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(rainColor, rainColor.copy(alpha = 0.55f)),
                            startY = plotHeight - barHeight, endY = plotHeight
                        ),
                        topLeft = Offset(x - barWidth / 2, plotHeight - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2.4f)
                    )
                }

                val tempLine = Path()
                val tempArea = Path()
                var started = false
                for (point in points) {
                    if (point.tempC.isNaN()) continue
                    val x = xOf(point.timeMs)
                    val y = plotHeight * 0.10f +
                        (1f - ((point.tempC - tempAxisMin) / tempAxisSpan).toFloat()) * (plotHeight * 0.62f)
                    if (!started) {
                        tempLine.moveTo(x, y); tempArea.moveTo(x, plotHeight); tempArea.lineTo(x, y)
                        started = true
                    } else {
                        tempLine.lineTo(x, y); tempArea.lineTo(x, y)
                    }
                }
                if (started) {
                    tempArea.lineTo(xOf(points.last().timeMs), plotHeight)
                    tempArea.close()
                    drawPath(
                        tempArea,
                        Brush.verticalGradient(
                            listOf(tempColor.copy(alpha = 0.24f), tempColor.copy(alpha = 0.0f))
                        )
                    )
                    drawPath(
                        tempLine, tempColor,
                        style = Stroke(width = with(density) { 2.4.dp.toPx() }, cap = StrokeCap.Round)
                    )
                }

                val nowX = xOf(System.currentTimeMillis())
                if (nowX in 0f..chartWidth) {
                    drawLine(
                        color = nowColor,
                        start = Offset(nowX, 0f), end = Offset(nowX, plotHeight),
                        strokeWidth = with(density) { 1.5.dp.toPx() },
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                }
            }

            if (minTemp != null && maxTemp != null) {
                Caption(
                    "${format.temp(minTemp)} \u2013 ${format.temp(maxTemp)}",
                    color = tempColor,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
            val peak = points.maxOf { if (it.precipMm.isNaN()) 0.0 else it.precipMm }
            if (peak > 0.05) {
                Caption(
                    "${format.millimetres(peak)} mm",
                    color = rainColor,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        TimeAxis(List(AXIS_LABEL_COUNT) { i -> startMs + (endMs - startMs) * i / (AXIS_LABEL_COUNT - 1) })
    }
}
