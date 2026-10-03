package nl.fietsweer.app.data

import kotlinx.serialization.Serializable
import nl.fietsweer.app.domain.LatLon

@Serializable
data class Place(
    val name: String,
    val lat: Double,
    val lon: Double,
    val detail: String = ""
) {
    fun toLatLon() = LatLon(lat, lon)
}

enum class Leg { OUTBOUND, RETURN }

enum class Coverage { OUTBOUND, RETURN, BOTH }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class Lang { SYSTEM, NL, EN }

enum class MapStyle { AUTO, LIGHT, DARK, SOFT }

@Serializable
data class Alert(
    val id: String,
    val label: String = "",
    val hour: Int = 7,
    val minute: Int = 15,
    val days: Set<Int> = setOf(1, 2, 3, 4, 5), // ISO: Monday = 1
    val coverage: Coverage = Coverage.BOTH,
    val enabled: Boolean = true,
    val onlyWhenNeeded: Boolean = false
) {
    val minutesOfDay: Int get() = hour * 60 + minute
}

@Serializable
data class Settings(
    val home: Place? = null,
    val work: Place? = null,

    val outboundHour: Int = 8,
    val outboundMinute: Int = 0,
    val returnHour: Int = 17,
    val returnMinute: Int = 30,

    val outboundEarlyMin: Int = 0,
    val outboundLateMin: Int = 60,
    val returnEarlyMin: Int = 60,
    val returnLateMin: Int = 60,

    val speedKmh: Int = 19,
    val windAdjustSpeed: Boolean = true,
    val wetThreshold: Double = 0.2, // mm per 15 min
    val rainJacketPercent: Int = 30,
    val vestBelow: Double = 17.0,
    val winterCoatBelow: Double = 6.0,

    val useRadar: Boolean = true,
    val alerts: List<Alert> = emptyList(),

    val theme: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val lang: Lang = Lang.SYSTEM,
    val mapStyle: MapStyle = MapStyle.AUTO,

    val setupDone: Boolean = false,
    val lastNotifiedAt: Long = 0L
) {
    val ready: Boolean get() = home != null && work != null

    fun hourFor(leg: Leg): Int = if (leg == Leg.OUTBOUND) outboundHour else returnHour
    fun minuteFor(leg: Leg): Int = if (leg == Leg.OUTBOUND) outboundMinute else returnMinute

    fun earlyMinFor(leg: Leg): Int =
        (if (leg == Leg.OUTBOUND) outboundEarlyMin else returnEarlyMin).coerceIn(0, FLEX_MAX_MIN)

    fun lateMinFor(leg: Leg): Int =
        (if (leg == Leg.OUTBOUND) outboundLateMin else returnLateMin).coerceIn(0, FLEX_MAX_MIN)
}

const val FLEX_MAX_MIN = 180

val LatLonFallback = LatLon(52.1326, 5.2913)
