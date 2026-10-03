package nl.fietsweer.app.notify

import nl.fietsweer.app.data.Coverage
import nl.fietsweer.app.data.Leg
import nl.fietsweer.app.data.Settings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class Planned(
    val leg: Leg,
    val departureMs: Long,
    val earliestMs: Long,
    /** Past this the ride is history and the next day's takes over. */
    val latestMs: Long
) {
    fun isStale(nowMs: Long): Boolean = nowMs > latestMs
}

/** Each leg rolls over to its next riding day once its slack has passed. */
object Commute {

    fun plannedRides(
        s: Settings,
        coverage: Coverage,
        nowMs: Long = System.currentTimeMillis(),
        alertDaysOnly: Boolean = false
    ): List<Planned> {
        val legs = when (coverage) {
            Coverage.OUTBOUND -> listOf(Leg.OUTBOUND)
            Coverage.RETURN -> listOf(Leg.RETURN)
            Coverage.BOTH -> listOf(Leg.OUTBOUND, Leg.RETURN)
        }
        // Chronological, so the next ride is on top.
        val days = if (alertDaysOnly) alertDays(s) else ALL_DAYS
        return legs.map { next(s, it, nowMs, days) }.sortedBy { it.departureMs }
    }

    /** Days some enabled alert fires on; every day if there is none. */
    fun alertDays(s: Settings): Set<Int> =
        s.alerts.filter { it.enabled }.flatMap { it.days }.toSet()
            .filter { it in 1..7 }.toSet().ifEmpty { ALL_DAYS }

    private val ALL_DAYS = (1..7).toSet()

    fun next(
        s: Settings,
        leg: Leg,
        nowMs: Long = System.currentTimeMillis(),
        days: Set<Int> = ALL_DAYS
    ): Planned {
        val zone = ZoneId.systemDefault()
        val today = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMs), zone).toLocalDate()
        val late = s.lateMinFor(leg) * 60_000L

        // Roll the date rather than add 24 h, so DST keeps the clock time.
        var day = today
        if (nowMs > at(s, leg, day, zone) + late) day = day.plusDays(1)
        // Skip the days you do not ride; a week ahead always lands on one.
        repeat(7) { if (day.dayOfWeek.value !in days) day = day.plusDays(1) }
        val departure = at(s, leg, day, zone)

        return Planned(
            leg = leg,
            departureMs = departure,
            earliestMs = departure - s.earlyMinFor(leg) * 60_000L,
            latestMs = departure + late
        )
    }

    private fun at(s: Settings, leg: Leg, day: LocalDate, zone: ZoneId): Long =
        day.atTime(s.hourFor(leg).coerceIn(0, 23), s.minuteFor(leg).coerceIn(0, 59))
            .atZone(zone).toInstant().toEpochMilli()
}
