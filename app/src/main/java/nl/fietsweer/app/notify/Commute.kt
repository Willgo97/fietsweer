package nl.fietsweer.app.notify

import nl.fietsweer.app.data.Coverage
import nl.fietsweer.app.data.Leg
import nl.fietsweer.app.data.Settings
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Works out which concrete rides a notification (or the Today screen) is
 * talking about. The reference day is the one the last covered ride still
 * lies on: before the evening ride you hear about today, after it about
 * tomorrow.
 */
object Commute {

    fun plannedRides(
        s: Settings,
        coverage: Coverage,
        nowMs: Long = System.currentTimeMillis()
    ): List<Pair<Leg, Long>> {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMs), zone)
        val nowMinutes = now.hour * 60 + now.minute

        val anchor = when (coverage) {
            Coverage.OUTBOUND -> s.outboundHour * 60 + s.outboundMinute
            else -> s.returnHour * 60 + s.returnMinute
        }
        val day = if (nowMinutes <= anchor) now.toLocalDate() else now.toLocalDate().plusDays(1)

        fun at(h: Int, m: Int): Long =
            day.atTime(h.coerceIn(0, 23), m.coerceIn(0, 59)).atZone(zone).toInstant().toEpochMilli()

        return when (coverage) {
            Coverage.OUTBOUND -> listOf(Leg.OUTBOUND to at(s.outboundHour, s.outboundMinute))
            Coverage.RETURN -> listOf(Leg.RETURN to at(s.returnHour, s.returnMinute))
            Coverage.BOTH -> listOf(
                Leg.OUTBOUND to at(s.outboundHour, s.outboundMinute),
                Leg.RETURN to at(s.returnHour, s.returnMinute)
            )
        }
    }
}
