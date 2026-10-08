package nl.fietsweer.app.domain

import nl.fietsweer.app.data.RouteForecast
import kotlin.math.sqrt

object Timeline {

    const val STEP_MINUTES = 5
    private const val RADAR_BLEND_MINUTES = 30
    private const val MINUTE_MS = 60_000L

    fun times(startMs: Long, endMs: Long): LongArray {
        val stepMs = STEP_MINUTES * MINUTE_MS
        val count = ((endMs - startMs) / stepMs).toInt() + 1
        return LongArray(maxOf(count, 0)) { startMs + it * stepMs }
    }

    // mm/h along the route: quarter-hour models smoothed to 5 minutes, handing over to radar
    // where it reaches, with a linear cross-fade so the seam never shows as a step.
    fun rainRate(forecast: RouteForecast, times: LongArray): DoubleArray {
        val model = modelRainCurve(forecast)
        val radar = forecast.radar.sortedBy { it.timeMs }
        val radarStartMs = radar.firstOrNull()?.timeMs
        val radarEndMs = radar.lastOrNull()?.timeMs
        val blendMs = RADAR_BLEND_MINUTES * MINUTE_MS

        return DoubleArray(times.size) { i ->
            val timeMs = times[i]
            val modelRate = model?.invoke(timeMs.toDouble()) ?: 0.0
            if (radarStartMs == null || radarEndMs == null || timeMs < radarStartMs || timeMs > radarEndMs) {
                return@DoubleArray modelRate
            }
            val radarRate = interpolateLinear(
                LongArray(radar.size) { radar[it].timeMs },
                DoubleArray(radar.size) { radar[it].mmPerHour },
                timeMs
            )
            val radarWeight = ((radarEndMs - timeMs).toDouble() / blendMs).coerceIn(0.0, 1.0)
            radarWeight * radarRate + (1 - radarWeight) * modelRate
        }
    }

    fun temperature(forecast: RouteForecast, times: LongArray): DoubleArray {
        val quarterHour = forecast.quarterHourly["temperature_2m"]
        val hourly = forecast.hourly["temperature_2m"]
        return DoubleArray(times.size) { i ->
            val fromQuarterHour = quarterHour?.let { interpolateLinear(forecast.quarterHourTimes, it, times[i]) } ?: Double.NaN
            if (!fromQuarterHour.isNaN()) fromQuarterHour
            else hourly?.let { interpolateLinear(forecast.hourlyTimes, it, times[i]) } ?: Double.NaN
        }
    }

    // Each model value is the sum over the preceding interval, so it is centred half a step back.
    private fun modelRainCurve(forecast: RouteForecast): ((Double) -> Double)? {
        val times = forecast.modelTimes
        if (times.size < 2 || forecast.models.isEmpty()) return null
        val stepMs = times[1] - times[0]
        val perHour = 3_600_000.0 / stepMs
        val centres = DoubleArray(times.size) { times[it] - stepMs / 2.0 }
        val rates = DoubleArray(times.size) { i ->
            var sum = 0.0
            var count = 0
            for (model in forecast.models) {
                for (values in model.precipitationPerPoint) {
                    if (i < values.size && !values[i].isNaN()) { sum += values[i]; count++ }
                }
            }
            if (count == 0) 0.0 else sum / count * perHour
        }
        return monotoneCubic(centres, rates)
    }

    fun interpolateLinear(times: LongArray, values: DoubleArray, timeMs: Long): Double {
        val count = minOf(times.size, values.size)
        if (count == 0 || timeMs < times[0] || timeMs > times[count - 1]) return Double.NaN
        var high = 1
        while (high < count - 1 && times[high] < timeMs) high++
        val low = high - 1
        if (count == 1 || times[high] == times[low]) return values[low]
        val fraction = (timeMs - times[low]).toDouble() / (times[high] - times[low])
        return values[low] + (values[high] - values[low]) * fraction
    }

    // Fritsch–Carlson: no overshoot, so no negative rain and no peak above what the models say.
    private fun monotoneCubic(xs: DoubleArray, ys: DoubleArray): (Double) -> Double {
        val n = xs.size
        val secants = DoubleArray(n - 1) { (ys[it + 1] - ys[it]) / (xs[it + 1] - xs[it]) }
        val tangents = DoubleArray(n) { k ->
            when {
                k == 0 -> secants[0]
                k == n - 1 -> secants[n - 2]
                secants[k - 1] * secants[k] <= 0 -> 0.0
                else -> (secants[k - 1] + secants[k]) / 2
            }
        }
        for (k in 0 until n - 1) {
            if (secants[k] == 0.0) {
                tangents[k] = 0.0
                tangents[k + 1] = 0.0
                continue
            }
            val a = tangents[k] / secants[k]
            val b = tangents[k + 1] / secants[k]
            val length = a * a + b * b
            if (length > 9) {
                val scale = 3 / sqrt(length)
                tangents[k] = scale * a * secants[k]
                tangents[k + 1] = scale * b * secants[k]
            }
        }
        return { x ->
            when {
                x <= xs[0] -> ys[0]
                x >= xs[n - 1] -> ys[n - 1]
                else -> {
                    var k = 0
                    while (xs[k + 1] < x) k++
                    val h = xs[k + 1] - xs[k]
                    val t = (x - xs[k]) / h
                    val t2 = t * t
                    val t3 = t2 * t
                    val y = (2 * t3 - 3 * t2 + 1) * ys[k] + (t3 - 2 * t2 + t) * h * tangents[k] +
                        (-2 * t3 + 3 * t2) * ys[k + 1] + (t3 - t2) * h * tangents[k + 1]
                    y.coerceAtLeast(0.0)
                }
            }
        }
    }
}
