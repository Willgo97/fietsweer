package nl.fietsweer.app.domain

import nl.fietsweer.app.data.Alert
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

class CommuteTest {

    private val zone: ZoneId = ZoneId.of("Europe/Amsterdam")

    private val settings = Settings(
        outboundHour = 8, outboundMinute = 0,
        returnHour = 17, returnMinute = 30,
        outboundEarlyMinutes = 0, outboundLateMinutes = 60,
        returnEarlyMinutes = 60, returnLateMinutes = 60
    )

    @Before
    fun fixZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(zone))
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun local(ms: Long): LocalDateTime =
        Instant.ofEpochMilli(ms).atZone(zone).toLocalDateTime()

    @Test
    fun `before the morning ride both legs are today, outbound first`() {
        val rides = Commute.plannedRides(settings, Coverage.BOTH, at(2026, 3, 10, 7, 0))
        assertEquals(listOf(Leg.OUTBOUND, Leg.RETURN), rides.map { it.leg })
        assertEquals(LocalDateTime.of(2026, 3, 10, 8, 0), local(rides[0].departureMs))
        assertEquals(LocalDateTime.of(2026, 3, 10, 17, 30), local(rides[1].departureMs))
    }

    @Test
    fun `inside the slack the morning ride is still today`() {
        val rides = Commute.plannedRides(settings, Coverage.BOTH, at(2026, 3, 10, 8, 59))
        assertEquals(LocalDateTime.of(2026, 3, 10, 8, 0), local(rides[0].departureMs))
    }

    @Test
    fun `past the slack the morning ride becomes tomorrow and sorts last`() {
        val rides = Commute.plannedRides(settings, Coverage.BOTH, at(2026, 3, 10, 9, 1))
        assertEquals(listOf(Leg.RETURN, Leg.OUTBOUND), rides.map { it.leg })
        assertEquals(LocalDateTime.of(2026, 3, 10, 17, 30), local(rides[0].departureMs))
        assertEquals(LocalDateTime.of(2026, 3, 11, 8, 0), local(rides[1].departureMs))
    }

    @Test
    fun `after the evening slack both legs are tomorrow`() {
        val rides = Commute.plannedRides(settings, Coverage.BOTH, at(2026, 3, 10, 18, 31))
        assertEquals(listOf(Leg.OUTBOUND, Leg.RETURN), rides.map { it.leg })
        assertEquals(LocalDateTime.of(2026, 3, 11, 8, 0), local(rides[0].departureMs))
        assertEquals(LocalDateTime.of(2026, 3, 11, 17, 30), local(rides[1].departureMs))
    }

    @Test
    fun `the window is the planned time minus early plus late`() {
        val rides = Commute.plannedRides(settings, Coverage.BOTH, at(2026, 3, 10, 7, 0))
        val outbound = rides.first { it.leg == Leg.OUTBOUND }
        val homeward = rides.first { it.leg == Leg.RETURN }

        assertEquals(LocalDateTime.of(2026, 3, 10, 8, 0), local(outbound.earliestMs))
        assertEquals(LocalDateTime.of(2026, 3, 10, 9, 0), local(outbound.latestMs))
        assertEquals(LocalDateTime.of(2026, 3, 10, 16, 30), local(homeward.earliestMs))
        assertEquals(LocalDateTime.of(2026, 3, 10, 18, 30), local(homeward.latestMs))
    }

    @Test
    fun `zero slack rolls over the minute the ride leaves`() {
        val strict = settings.copy(outboundLateMinutes = 0)
        assertEquals(
            LocalDateTime.of(2026, 3, 10, 8, 0),
            local(Commute.nextRide(strict, Leg.OUTBOUND, at(2026, 3, 10, 8, 0)).departureMs)
        )
        assertEquals(
            LocalDateTime.of(2026, 3, 11, 8, 0),
            local(Commute.nextRide(strict, Leg.OUTBOUND, at(2026, 3, 10, 8, 1)).departureMs)
        )
    }

    @Test
    fun `a longer slack keeps the ride on screen for longer`() {
        val relaxed = settings.copy(outboundLateMinutes = 180)
        val rides = Commute.plannedRides(relaxed, Coverage.BOTH, at(2026, 3, 10, 10, 30))
        assertEquals(LocalDateTime.of(2026, 3, 10, 8, 0), local(rides[0].departureMs))
        assertTrue(rides[0].leg == Leg.OUTBOUND)
    }

    @Test
    fun `rolling into a daylight-saving switch keeps the clock time`() {
        // 25-26 Oct 2025 the clocks go back: the day is 25 hours long.
        val next = Commute.nextRide(settings, Leg.OUTBOUND, at(2025, 10, 25, 9, 30))
        assertEquals(LocalDateTime.of(2025, 10, 26, 8, 0), local(next.departureMs))
    }

    @Test
    fun `a single-leg coverage only reports that leg`() {
        val rides = Commute.plannedRides(settings, Coverage.RETURN, at(2026, 3, 10, 9, 1))
        assertEquals(listOf(Leg.RETURN), rides.map { it.leg })
        assertEquals(LocalDateTime.of(2026, 3, 10, 17, 30), local(rides[0].departureMs))
    }

    private val weekdays = settings.copy(alerts = listOf(Alert(id = "a", days = setOf(1, 2, 3, 4, 5))))

    @Test
    fun `after the last ride on Friday the next riding day is Monday`() {
        val rides = Commute.plannedRides(weekdays, Coverage.BOTH, at(2026, 3, 13, 18, 31), alertDaysOnly = true)
        assertEquals(LocalDateTime.of(2026, 3, 16, 8, 0), local(rides[0].departureMs))
        assertEquals(LocalDateTime.of(2026, 3, 16, 17, 30), local(rides[1].departureMs))
    }

    @Test
    fun `on Friday afternoon the trip home is today and the next morning is Monday`() {
        val rides = Commute.plannedRides(weekdays, Coverage.BOTH, at(2026, 3, 13, 12, 0), alertDaysOnly = true)
        assertEquals(LocalDateTime.of(2026, 3, 13, 17, 30), local(rides[0].departureMs))
        assertEquals(LocalDateTime.of(2026, 3, 16, 8, 0), local(rides[1].departureMs))
    }

    @Test
    fun `disabled alerts do not make a riding day, and none at all means every day`() {
        val disabled = settings.copy(alerts = listOf(Alert(id = "a", days = setOf(1), enabled = false)))
        val rides = Commute.plannedRides(disabled, Coverage.BOTH, at(2026, 3, 13, 18, 31), alertDaysOnly = true)
        assertEquals(LocalDateTime.of(2026, 3, 14, 8, 0), local(rides[0].departureMs))
    }

    @Test
    fun `alerts keep their own rollover without the day filter`() {
        val rides = Commute.plannedRides(weekdays, Coverage.BOTH, at(2026, 3, 13, 18, 31))
        assertEquals(LocalDateTime.of(2026, 3, 14, 8, 0), local(rides[0].departureMs))
    }
}
