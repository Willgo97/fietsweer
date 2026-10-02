package nl.fietsweer.app.notify

import nl.fietsweer.app.data.Coverage
import nl.fietsweer.app.data.Leg
import nl.fietsweer.app.data.Settings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * One concrete ride a notification (or the Today screen) is talking about,
 * together with the slack the user allows around it.
 */
data class Planned(
    val leg: Leg,
    val departureMs: Long,
    /** Earliest departure still worth considering. */
    val earliestMs: Long,
    /** Latest one — past this the ride is history and the next day's takes over. */
    val latestMs: Long
) {
    fun isStale(nowMs: Long): Boolean = nowMs > latestMs
}

/**
 * Works out which concrete rides we are talking about. Each leg rolls over on
 * its own clock: a ride stays on screen until its slack has run out, and from
 * that moment the same leg on the next day is shown instead. So at the office,
 * an hour after you left home, the morning card has already become tomorrow's.
 */
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
        // Chronological, so the ride that comes up next is always on top —
        // which after the morning ride means today's trip home before
        // tomorrow's trip to work.
        val days = if (alertDaysOnly) alertDays(s) else ALL_DAYS
        return legs.map { next(s, it, nowMs, days) }.sortedBy { it.departureMs }
    }

    /**
     * The days some enabled alert fires on — the days you actually ride. With
     * no enabled alert at all there is nothing to go on, so every day counts.
     */
    fun alertDays(s: Settings): Set<Int> =
        s.alerts.filter { it.enabled }.flatMap { it.days }.toSet()
            .filter { it in 1..7 }.toSet().ifEmpty { ALL_DAYS }

    private val ALL_DAYS = (1..7).toSet()

    /** The one ride of this leg that is still ahead of us, slack included. */
    fun next(
        s: Settings,
        leg: Leg,
        nowMs: Long = System.currentTimeMillis(),
        days: Set<Int> = ALL_DAYS
    ): Planned {
        val zone = ZoneId.systemDefault()
        val today = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMs), zone).toLocalDate()
        val late = s.lateMinFor(leg) * 60_000L

        // Rolling the local date rather than adding 24 hours keeps the clock
        // time right across a daylight-saving switch.
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
