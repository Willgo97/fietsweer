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
        nowMs: Long = System.currentTimeMillis()
    ): List<Planned> {
        val legs = when (coverage) {
            Coverage.OUTBOUND -> listOf(Leg.OUTBOUND)
            Coverage.RETURN -> listOf(Leg.RETURN)
            Coverage.BOTH -> listOf(Leg.OUTBOUND, Leg.RETURN)
        }
        // Chronological, so the ride that comes up next is always on top —
        // which after the morning ride means today's trip home before
        // tomorrow's trip to work.
        return legs.map { next(s, it, nowMs) }.sortedBy { it.departureMs }
    }

    /** The one ride of this leg that is still ahead of us, slack included. */
    fun next(s: Settings, leg: Leg, nowMs: Long = System.currentTimeMillis()): Planned {
        val zone = ZoneId.systemDefault()
        val today = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMs), zone).toLocalDate()
        val late = s.lateMinFor(leg) * 60_000L

        var departure = at(s, leg, today, zone)
        // Rolling the local date rather than adding 24 hours keeps the clock
        // time right across a daylight-saving switch.
        if (nowMs > departure + late) departure = at(s, leg, today.plusDays(1), zone)

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
