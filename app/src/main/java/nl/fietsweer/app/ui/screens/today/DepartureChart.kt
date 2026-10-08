package nl.fietsweer.app.ui.screens.today

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.data.RouteForecast
import nl.fietsweer.app.domain.Engine
import nl.fietsweer.app.domain.PlannedRide
import nl.fietsweer.app.domain.RideAssessment
import nl.fietsweer.app.domain.Timeline
import nl.fietsweer.app.ui.components.SoftDivider
import nl.fietsweer.app.ui.components.charts.ChartMarker
import nl.fietsweer.app.ui.components.charts.ChartSeries
import nl.fietsweer.app.ui.components.charts.TimeChart
import nl.fietsweer.app.ui.theme.AppTheme
import kotlin.math.abs

private const val SLOT_MS = Engine.GRID_MINUTES * 60_000L
private const val HALF_HOUR_MS = 30 * 60_000L
private const val HOUR_MS = 60 * 60_000L
private const val VISIBLE_MS = 2 * HOUR_MS
private const val REACH_MS = 4 * HOUR_MS
private const val TIE_RISK = 0.01

internal class DepartureWindow(
    val slots: List<RideAssessment>,
    val best: RideAssessment,
    val plannedDepartureMs: Long,
    val plannedArrivalMs: Long,
    val series: ChartSeries
)

internal fun departureWindow(
    forecast: RouteForecast,
    engine: Engine,
    planned: PlannedRide,
    nowTick: Long
): DepartureWindow? {
    val stepMs = Timeline.STEP_MINUTES * 60_000L
    val firstMs = maxOf(planned.earliestMs, ((nowTick + stepMs - 1) / stepMs) * stepMs)
    val candidates = generateSequence(firstMs) { it + stepMs }
        .takeWhile { it <= planned.latestMs }
        .map { engine.assess(it, planned.leg) }
        .filter { it.modelCount > 0 }
        .toList()
    if (candidates.isEmpty()) return null
    val lowestRisk = candidates.minOf { it.risk }
    val best = candidates.filter { it.risk <= lowestRisk + TIE_RISK }
        .minBy { abs(it.departureMs - planned.departureMs) }

    val centreMs = planned.departureMs
    val slots = engine.scanWindow(planned.leg, centreMs - REACH_MS, centreMs + REACH_MS)
    val startMs = centreMs - REACH_MS
    val endMs = maxOf(centreMs + REACH_MS, best.arrivalMs)
    val times = Timeline.times(startMs, endMs)
    return DepartureWindow(
        slots, best, centreMs, engine.assess(planned.departureMs, planned.leg).arrivalMs,
        ChartSeries(startMs, endMs, times, Timeline.rainRate(forecast, times), bikeFeelAt(slots, times))
    )
}

private fun bikeFeelAt(slots: List<RideAssessment>, times: LongArray): DoubleArray {
    if (slots.isEmpty()) return DoubleArray(times.size) { Double.NaN }
    val slotTimes = LongArray(slots.size) { slots[it].departureMs + SLOT_MS / 2 }
    val feels = DoubleArray(slots.size) { slots[it].bikeFeelC }
    return DoubleArray(times.size) { i ->
        Timeline.interpolateLinear(slotTimes, feels, times[i].coerceIn(slotTimes.first(), slotTimes.last()))
    }
}

@Composable
internal fun DepartureChart(window: DepartureWindow, modifier: Modifier = Modifier) {
    val accents = AppTheme.accents
    val format = AppTheme.format
    val best = window.best
    val bestColor = accents.forRisk(best.risk)
    val plannedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val slotColors = window.slots.map { accents.forRisk(it.risk) }

    Column(modifier.fillMaxWidth()) {
        Spacer(Modifier.height(12.dp))
        SoftDivider()
        Spacer(Modifier.height(10.dp))

        TimeChart(
            series = window.series,
            visibleMs = VISIBLE_MS,
            homeMs = (window.plannedDepartureMs + window.plannedArrivalMs) / 2,
            homeAt = 0.5f,
            axisStepMs = HALF_HOUR_MS,
            axisLabel = format::time,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 84.dp),
            markers = listOf(ChartMarker(best.departureMs, format.time(best.departureMs), bestColor)),
            background = { xOf ->
                val stops = window.slots.mapIndexed { i, slot ->
                    (xOf(slot.departureMs + SLOT_MS / 2) / size.width).coerceIn(0f, 1f) to slotColors[i].copy(alpha = 0.16f)
                }
                if (stops.isNotEmpty()) {
                    drawRect(if (stops.size == 1) SolidColor(stops[0].second) else Brush.horizontalGradient(*stops.toTypedArray()))
                }

                val departX = xOf(best.departureMs)
                val arriveX = xOf(best.arrivalMs).coerceAtMost(size.width)
                val band = Path().apply {
                    addRoundRect(RoundRect(departX, 0f, arriveX, size.height, CornerRadius(4.dp.toPx())))
                }
                drawPath(band, bestColor.copy(alpha = 0.22f))
                clipPath(band) {
                    drawLine(
                        bestColor.copy(alpha = 0.8f),
                        Offset(departX, 0f), Offset(departX, size.height),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            },
            foreground = { xOf ->
                val outline = 1.5.dp.toPx()
                val left = xOf(window.plannedDepartureMs) + outline / 2
                val right = xOf(window.plannedArrivalMs).coerceAtMost(size.width) - outline / 2
                drawRoundRect(
                    color = plannedColor,
                    topLeft = Offset(left, outline / 2),
                    size = Size(right - left, size.height - outline),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = Stroke(width = outline, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
                )
            }
        )
    }
}
