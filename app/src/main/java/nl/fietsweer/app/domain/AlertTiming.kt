package nl.fietsweer.app.domain

import nl.fietsweer.app.data.Alert
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

fun Alert.nextTriggerMs(): Long? {
    if (!enabled || days.isEmpty()) return null
    val fromMs = System.currentTimeMillis()
    val zone = ZoneId.systemDefault()
    val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(fromMs), zone)
    for (daysAhead in 0..8L) {
        val date = now.toLocalDate().plusDays(daysAhead)
        if (date.dayOfWeek.value !in days) continue
        val triggerAt = date.atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            .atZone(zone)
        val triggerMs = triggerAt.toInstant().toEpochMilli()
        if (triggerMs > fromMs + 1000) return triggerMs
    }
    return null
}
