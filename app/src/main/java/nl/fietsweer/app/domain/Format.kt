package nl.fietsweer.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Locale-aware formatting helpers bound to the active language. */
class Fmt(private val txt: Txt) {

    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val locale: Locale get() = txt.locale

    private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

    fun time(ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(zone).format(timeFmt)

    fun hourLabel(ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(zone).format(DateTimeFormatter.ofPattern("HH", Locale.ROOT))

    fun dayWord(ms: Long): String {
        val d = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        return when (d) {
            today -> txt.today
            today.plusDays(1) -> txt.tomorrow
            else -> d.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, locale)
        }
    }

    fun dayShort(ms: Long): String {
        val d = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
        return d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale)
            .replaceFirstChar { it.uppercase(locale) }
    }

    fun dayTime(ms: Long): String = "${dayWord(ms)} ${time(ms)}"

    fun temp(c: Double): String =
        if (c.isNaN()) "–" else "${c.roundToInt()}°"

    fun tempFine(c: Double): String =
        if (c.isNaN()) "–" else String.format(locale, "%.1f°", c)

    fun mm(v: Double): String =
        if (v.isNaN()) "–" else String.format(locale, "%.1f", v)

    fun mm2(v: Double): String =
        if (v.isNaN()) "–" else String.format(locale, "%.2f", v)

    fun km(v: Double): String = String.format(locale, "%.1f", v)

    fun kmh(v: Double): String = if (v.isNaN()) "–" else "${v.roundToInt()}"

    fun percent(v: Double): String = "${(v * 100).roundToInt()}%"

    fun durationText(minutes: Int): String =
        if (minutes >= 60) txt.hoursShort(String.format(locale, "%.1f", minutes / 60.0).removeSuffix(",0").removeSuffix(".0"))
        else txt.minutesShort(minutes)

    fun windRelationWord(r: WindRelation): String = when (r) {
        WindRelation.HEAD -> txt.headwind
        WindRelation.TAIL -> txt.tailwind
        WindRelation.CROSS -> txt.crosswind
    }

    fun riskWord(risk: Double): String = when {
        risk < 0.08 -> txt.riskDry
        risk < Engine.DRY_RISK -> txt.riskAlmostDry
        risk < 0.45 -> txt.riskEither
        risk < 0.70 -> txt.riskLikelyWet
        else -> txt.riskWet
    }

    fun compass(deg: Double): String {
        if (deg.isNaN()) return "–"
        val names = if (txt.locale.language == "nl")
            listOf("N", "NO", "O", "ZO", "Z", "ZW", "W", "NW")
        else
            listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return names[((deg / 45).roundToInt() % 8 + 8) % 8]
    }

    fun daysSummary(days: Set<Int>): String = when {
        days.size == 7 -> txt.everyDay
        days == setOf(1, 2, 3, 4, 5) -> txt.weekdays
        days == setOf(6, 7) -> txt.weekend
        days.isEmpty() -> txt.neverRepeats
        else -> days.sorted().joinToString(", ") { txt.dayNamesShort[it - 1] }
    }

    fun longDate(ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(zone)
            .format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale))
            .replaceFirstChar { it.uppercase(locale) }

    fun shortDate(ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(zone)
            .format(DateTimeFormatter.ofPattern("d MMM", locale))

    fun relativeShort(ms: Long): String {
        val delta = ms - System.currentTimeMillis()
        val minutes = (abs(delta) / 60_000L).toInt()
        return when {
            minutes < 60 -> txt.minutesShort(minutes)
            else -> "${dayWord(ms)} ${time(ms)}"
        }
    }
}
