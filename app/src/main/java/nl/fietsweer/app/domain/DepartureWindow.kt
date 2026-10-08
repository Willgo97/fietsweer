package nl.fietsweer.app.domain

import nl.fietsweer.app.data.RouteForecast
import kotlin.math.abs

class ChartSeries(
    val startMs: Long,
    val endMs: Long,
    val times: LongArray,
    val rainMmPerHour: DoubleArray,
    val temperatureC: DoubleArray
)

class DepartureWindow(
    val slots: List<RideAssessment>,
    val best: RideAssessment,
    val plannedDepartureMs: Long,
    val plannedArrivalMs: Long,
    val series: ChartSeries
) {
    companion object {
        const val VISIBLE_MS = 2 * HOUR_MS
    }
}

private const val REACH_MS = 4 * HOUR_MS
private const val TIE_RISK = 0.01

fun departureWindow(
    forecast: RouteForecast,
    engine: Engine,
    planned: PlannedRide,
    nowTick: Long
): DepartureWindow? {
    val stepMs = Timeline.STEP_MS
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
    val slotTimes = LongArray(slots.size) { slots[it].slotCentreMs }
    val feels = DoubleArray(slots.size) { slots[it].bikeFeelC }
    return DoubleArray(times.size) { i ->
        Timeline.interpolateLinear(slotTimes, feels, times[i].coerceIn(slotTimes.first(), slotTimes.last()))
    }
}
