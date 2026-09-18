package nl.fietsweer.app.notify

import nl.fietsweer.app.data.Coverage
import nl.fietsweer.app.data.Leg
import nl.fietsweer.app.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

/**
 * The rollover is the one bit of Fietsweer that has to reason about wall-clock
 * time, so it is pinned down here: per leg, on its own slack, across a
 * daylight-saving switch.
 */
class CommuteTest {

    private val zone: ZoneId = ZoneId.of("Europe/Amsterdam")

    /** Leaves home at 08:00 and work at 17:30, both with an hour of slack. */
    private val s = Settings(
        outboundHour = 8, outboundMinute = 0,
        returnHour = 17, returnMinute = 30,
        outboundEarlyMin = 0, outboundLateMin = 60,
        returnEarlyMin = 60, returnLateMin = 60
    )

    @Before
    fun fixZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(zone))
    }

    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int): Long =
        LocalDateTime.of(y, mo, d, h, mi).atZone(zone).toInstant().toEpochMilli()

    private fun local(ms: Long): LocalDateTime =
        Instant.ofEpochMilli(ms).atZone(zone).toLocalDateTime()

    @Test
    fun `before the morning ride both legs are today, outbound first`() {
        val rides = Commute.plannedRides(s, Coverage.BOTH, at(2026, 3, 10, 7, 0))
        assertEquals(listOf(Leg.OUTBOUND, Leg.RETURN), rides.map { it.leg })
        assertEquals(LocalDateTime.of(2026, 3, 10, 8, 0), local(rides[0].departureMs))
        assertEquals(LocalDateTime.of(2026, 3, 10, 17, 30), local(rides[1].departureMs))
    }

    @Test
    fun `inside the slack the morning ride is still today`() {
        val rides = Commute.plannedRides(s, Coverage.BOTH, at(2026, 3, 10, 8, 59))
        assertEquals(LocalDateTime.of(2026, 3, 10, 8, 0), local(rides[0].departureMs))
    }

    @Test
    fun `past the slack the morning ride becomes tomorrow and sorts last`() {
        val rides = Commute.plannedRides(s, Coverage.BOTH, at(2026, 3, 10, 9, 1))
        // Chronological: today's ride home comes before tomorrow's ride to work.
        assertEquals(listOf(Leg.RETURN, Leg.OUTBOUND), rides.map { it.leg })
        assertEquals(LocalDateTime.of(2026, 3, 10, 17, 30), local(rides[0].departureMs))
        assertEquals(LocalDateTime.of(2026, 3, 11, 8, 0), local(rides[1].departureMs))
    }

    @Test
    fun `after the evening slack both legs are tomorrow`() {
        val rides = Commute.plannedRides(s, Coverage.BOTH, at(2026, 3, 10, 18, 31))
        assertEquals(listOf(Leg.OUTBOUND, Leg.RETURN), rides.map { it.leg })
        assertEquals(LocalDateTime.of(2026, 3, 11, 8, 0), local(rides[0].departureMs))
        assertEquals(LocalDateTime.of(2026, 3, 11, 17, 30), local(rides[1].departureMs))
    }

    @Test
    fun `the window is the planned time minus early plus late`() {
        val rides = Commute.plannedRides(s, Coverage.BOTH, at(2026, 3, 10, 7, 0))
        val out = rides.first { it.leg == Leg.OUTBOUND }
        val back = rides.first { it.leg == Leg.RETURN }

        assertEquals(LocalDateTime.of(2026, 3, 10, 8, 0), local(out.earliestMs))
        assertEquals(LocalDateTime.of(2026, 3, 10, 9, 0), local(out.latestMs))
        assertEquals(LocalDateTime.of(2026, 3, 10, 16, 30), local(back.earliestMs))
        assertEquals(LocalDateTime.of(2026, 3, 10, 18, 30), local(back.latestMs))
    }

    @Test
    fun `zero slack rolls over the minute the ride leaves`() {
        val strict = s.copy(outboundLateMin = 0)
        assertEquals(
            LocalDateTime.of(2026, 3, 10, 8, 0),
            local(Commute.next(strict, Leg.OUTBOUND, at(2026, 3, 10, 8, 0)).departureMs)
        )
        assertEquals(
            LocalDateTime.of(2026, 3, 11, 8, 0),
            local(Commute.next(strict, Leg.OUTBOUND, at(2026, 3, 10, 8, 1)).departureMs)
        )
    }

    @Test
    fun `a longer slack keeps the ride on screen for longer`() {
        val relaxed = s.copy(outboundLateMin = 180)
        val rides = Commute.plannedRides(relaxed, Coverage.BOTH, at(2026, 3, 10, 10, 30))
        assertEquals(LocalDateTime.of(2026, 3, 10, 8, 0), local(rides[0].departureMs))
        assertTrue(rides[0].leg == Leg.OUTBOUND)
    }

    @Test
    fun `rolling into a daylight-saving switch keeps the clock time`() {
        // Night of 25 to 26 October 2025 the Dutch clocks go back an hour, so
        // the day is 25 hours long; adding 24 hours of millis would land at 07:00.
        val next = Commute.next(s, Leg.OUTBOUND, at(2025, 10, 25, 9, 30))
        assertEquals(LocalDateTime.of(2025, 10, 26, 8, 0), local(next.departureMs))
    }

    @Test
    fun `a single-leg coverage only reports that leg`() {
        val rides = Commute.plannedRides(s, Coverage.RETURN, at(2026, 3, 10, 9, 1))
        assertEquals(listOf(Leg.RETURN), rides.map { it.leg })
        assertEquals(LocalDateTime.of(2026, 3, 10, 17, 30), local(rides[0].departureMs))
    }

    @Test
    fun `a stale ride is one whose slack has run out`() {
        val planned = Commute.next(s, Leg.OUTBOUND, at(2026, 3, 10, 8, 30))
        assertTrue(!planned.isStale(at(2026, 3, 10, 9, 0)))
        assertTrue(planned.isStale(at(2026, 3, 10, 9, 1)))
    }
}
