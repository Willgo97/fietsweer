package nl.fietsweer.app.domain

import nl.fietsweer.app.data.Coverage
import nl.fietsweer.app.data.Leg
import nl.fietsweer.app.data.Settings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class PlannedRide(
    val leg: Leg,
    val departureMs: Long,
    val earliestMs: Long,
    val latestMs: Long
)

object Commute {

    fun plannedRides(
        settings: Settings,
        coverage: Coverage,
        nowMs: Long = System.currentTimeMillis(),
        alertDaysOnly: Boolean = false
    ): List<PlannedRide> {
        val legs = when (coverage) {
            Coverage.OUTBOUND -> listOf(Leg.OUTBOUND)
            Coverage.RETURN -> listOf(Leg.RETURN)
            Coverage.BOTH -> listOf(Leg.OUTBOUND, Leg.RETURN)
        }
        val days = if (alertDaysOnly) alertDays(settings) else ALL_DAYS
        return legs.map { nextRide(settings, it, nowMs, days) }.sortedBy { it.departureMs }
    }

    fun ridesBetween(settings: Settings, fromMs: Long, untilMs: Long): List<PlannedRide> {
        val days = alertDays(settings)
        return listOf(Leg.OUTBOUND, Leg.RETURN).flatMap { leg ->
            generateSequence(nextRide(settings, leg, fromMs, days)) { nextRide(settings, leg, it.latestMs + 1, days) }
                .takeWhile { it.departureMs <= untilMs }
                .toList()
        }.sortedBy { it.departureMs }
    }

    private fun alertDays(settings: Settings): Set<Int> =
        settings.alerts.filter { it.enabled }.flatMap { it.days }.toSet()
            .filter { it in 1..7 }.toSet().ifEmpty { ALL_DAYS }

    private val ALL_DAYS = (1..7).toSet()

    fun nextRide(
        settings: Settings,
        leg: Leg,
        nowMs: Long = System.currentTimeMillis(),
        days: Set<Int> = ALL_DAYS
    ): PlannedRide {
        val zone = ZoneId.systemDefault()
        val today = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMs), zone).toLocalDate()
        val lateMs = settings.lateMinutesFor(leg) * 60_000L

        // Roll the date rather than add 24 h, so DST keeps the clock time.
        var day = today
        if (nowMs > departureOn(settings, leg, day, zone) + lateMs) day = day.plusDays(1)
        repeat(7) { if (day.dayOfWeek.value !in days) day = day.plusDays(1) }
        val departure = departureOn(settings, leg, day, zone)

        return PlannedRide(
            leg = leg,
            departureMs = departure,
            earliestMs = departure - settings.earlyMinutesFor(leg) * 60_000L,
            latestMs = departure + lateMs
        )
    }

    private fun departureOn(settings: Settings, leg: Leg, day: LocalDate, zone: ZoneId): Long =
        day.atTime(settings.hourFor(leg).coerceIn(0, 23), settings.minuteFor(leg).coerceIn(0, 59))
            .atZone(zone).toInstant().toEpochMilli()
}
