package nl.fietsweer.app.domain

import nl.fietsweer.app.data.RouteForecast
import kotlin.math.abs

// How light the sky is: daylight runs from 0 (night) to 1 (day) over the hour around sunrise
// and sunset, and twilight peaks right at those moments.
class SkyLight(val daylight: Float, val twilight: Float) {

    companion object {
        val DAY = SkyLight(1f, 0f)
        val DUSK = SkyLight(0.5f, 1f)
        val NIGHT = SkyLight(0f, 0f)

        private const val CHANGE_MINUTES = 60f
        private const val TWILIGHT_MINUTES = 40f
        private const val CHANGING_REDRAW_MS = 10 * MINUTE_MS
        private const val CHANGE_STARTS_BEFORE_MS = 40 * MINUTE_MS

        fun at(nowMs: Long, forecast: RouteForecast): SkyLight =
            at(nowMs, SunTimes.of(forecast))

        fun at(nowMs: Long, sun: SunTimes): SkyLight {
            val (eventMs, isSunrise) = sun.nearest(nowMs) ?: return if (sun.isDay) DAY else NIGHT
            val minutesAfter = (nowMs - eventMs) / MINUTE_MS.toFloat()
            val towardsDay = if (isSunrise) minutesAfter else -minutesAfter
            return SkyLight(
                daylight = (0.5f + towardsDay / CHANGE_MINUTES).coerceIn(0f, 1f),
                twilight = (1f - abs(minutesAfter) / TWILIGHT_MINUTES).coerceIn(0f, 1f)
            )
        }

        // When a still picture of the sky is next out of date: often around sunrise and sunset,
        // otherwise not until the next one starts.
        fun nextChangeMs(nowMs: Long, sun: SunTimes): Long? {
            val (eventMs, _) = sun.nearest(nowMs) ?: return null
            if (abs(nowMs - eventMs) < CHANGE_STARTS_BEFORE_MS) return nowMs + CHANGING_REDRAW_MS
            val upcoming = sun.events.map { it.first }.filter { it > nowMs }.minOrNull() ?: return null
            return upcoming - CHANGE_STARTS_BEFORE_MS
        }
    }
}

class SunTimes(val sunrises: List<Long>, val sunsets: List<Long>, val isDay: Boolean) {

    val events: List<Pair<Long, Boolean>> get() = sunrises.map { it to true } + sunsets.map { it to false }

    fun nearest(nowMs: Long): Pair<Long, Boolean>? = events.minByOrNull { abs(it.first - nowMs) }

    companion object {
        // Open-Meteo sends sunrise and sunset as unix seconds.
        fun of(forecast: RouteForecast) = SunTimes(
            sunrises = forecast.daily["sunrise"]?.map { (it * 1000).toLong() }.orEmpty(),
            sunsets = forecast.daily["sunset"]?.map { (it * 1000).toLong() }.orEmpty(),
            isDay = forecast.current["is_day"] != 0.0
        )
    }
}
