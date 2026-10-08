package nl.fietsweer.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class Formatter(private val strings: Strings) {

    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val locale: Locale get() = strings.locale

    private val clockFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

    fun time(ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(zone).format(clockFormat)

    fun dayWord(ms: Long): String {
        val date = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        return when (date) {
            today -> strings.today
            today.plusDays(1) -> strings.tomorrow
            else -> date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        }
    }

    fun isToday(ms: Long): Boolean =
        Instant.ofEpochMilli(ms).atZone(zone).toLocalDate() == LocalDate.now(zone)

    fun dayShort(ms: Long): String {
        val date = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
        return date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
            .replaceFirstChar { it.uppercase(locale) }
    }

    fun dayTime(ms: Long): String = "${dayWord(ms)} ${time(ms)}"

    fun temp(celsius: Double): String =
        if (celsius.isNaN()) "–" else "${celsius.roundToInt()}°"

    fun millimetres(value: Double): String =
        if (value.isNaN()) "–" else String.format(locale, "%.1f", value)

    fun kilometres(value: Double): String = String.format(locale, "%.1f", value)

    private fun speed(kmh: Double): String = if (kmh.isNaN()) "–" else "${kmh.roundToInt()}"

    fun speedWithUnit(kmh: Double): String = "${speed(kmh)} ${strings.speedUnit}"

    fun percent(fraction: Double): String = "${(fraction * 100).roundToInt()}%"

    fun clock(hour: Int, minute: Int): String = clock(hour * 60 + minute)

    fun clock(minuteOfDay: Int): String {
        val wrapped = ((minuteOfDay % 1440) + 1440) % 1440
        return String.format(Locale.ROOT, "%02d:%02d", wrapped / 60, wrapped % 60)
    }

    fun hoursMinutes(minutes: Int): String {
        if (minutes < 60) return strings.minutesShort(minutes)
        val hours = strings.hoursShort((minutes / 60).toString())
        val remainder = minutes % 60
        return if (remainder == 0) hours else "$hours $remainder"
    }

    fun windRelationWord(relation: WindRelation): String = when (relation) {
        WindRelation.HEAD -> strings.headwind
        WindRelation.TAIL -> strings.tailwind
        WindRelation.CROSS -> strings.crosswind
    }

    fun riskWord(risk: Double): String = when (RiskLevel.of(risk)) {
        RiskLevel.DRY -> strings.riskDry
        RiskLevel.MOSTLY_DRY -> strings.riskAlmostDry
        RiskLevel.UNCERTAIN -> strings.riskEither
        RiskLevel.LIKELY_WET -> strings.riskLikelyWet
        RiskLevel.WET -> strings.riskWet
    }

    fun compass(degrees: Double): String {
        if (degrees.isNaN()) return "–"
        return strings.compassPoints[((degrees / 45).roundToInt() % 8 + 8) % 8]
    }

    fun windAndRain(current: Map<String, Double>): String = buildString {
        val wind = current["wind_speed_10m"] ?: Double.NaN
        val precipitation = current["precipitation"] ?: 0.0
        if (!wind.isNaN()) {
            append("${strings.wind} ${speedWithUnit(wind)} ${compass(current["wind_direction_10m"] ?: Double.NaN)}")
        }
        if (precipitation > 0.02) {
            if (isNotEmpty()) append(" · ")
            append("${millimetres(precipitation)} mm")
        }
    }

    fun daysSummary(days: Set<Int>): String = when {
        days.size == 7 -> strings.everyDay
        days == setOf(1, 2, 3, 4, 5) -> strings.weekdays
        days == setOf(6, 7) -> strings.weekend
        days.isEmpty() -> strings.neverRepeats
        else -> days.sorted().joinToString(", ") { strings.dayNamesShort[it - 1] }
    }

    fun relativeShort(ms: Long): String {
        val delta = ms - System.currentTimeMillis()
        val minutes = (abs(delta) / MINUTE_MS).toInt()
        return if (minutes < 60) strings.minutesShort(minutes) else dayTime(ms)
    }
}
