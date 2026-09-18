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

/** Which of the day's two rides an alert reports on. */
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
    /** ISO day numbers, Monday = 1 … Sunday = 7. */
    val days: Set<Int> = setOf(1, 2, 3, 4, 5),
    val coverage: Coverage = Coverage.BOTH,
    val enabled: Boolean = true,
    /** Only fire when there is actually something to bring. */
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

    /**
     * How far either side of a planned departure the ride is still on the
     * table. The late end doubles as the ride's shelf life: once it has passed,
     * Today drops that ride and shows the next day's instead.
     */
    val outboundEarlyMin: Int = 0,
    val outboundLateMin: Int = 60,
    val returnEarlyMin: Int = 60,
    val returnLateMin: Int = 60,

    /** Cycling pace in km/h **in still air** — the wind is applied on top. */
    val speedKmh: Int = 19,
    /** Let head- and tailwind change the speed, and with it the ride time. */
    val windAdjustSpeed: Boolean = true,
    /** mm per 15 minutes above which a model counts as "wet". */
    val wetThreshold: Double = 0.2,
    /** Percentage of rain risk at which the rain jacket becomes a yes. */
    val rainJacketPercent: Int = 30,
    /** Bike-feel temperature below which a vest replaces short sleeves. */
    val vestBelow: Double = 17.0,
    /** Bike-feel temperature below which a vest is no longer enough. */
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

    /** Minutes you could leave ahead of plan, clamped to something sane. */
    fun earlyMinFor(leg: Leg): Int =
        (if (leg == Leg.OUTBOUND) outboundEarlyMin else returnEarlyMin).coerceIn(0, FLEX_MAX_MIN)

    /** Minutes you could still leave after plan, and how long the ride stays. */
    fun lateMinFor(leg: Leg): Int =
        (if (leg == Leg.OUTBOUND) outboundLateMin else returnLateMin).coerceIn(0, FLEX_MAX_MIN)
}

/** Upper bound on the departure slack, in minutes. */
const val FLEX_MAX_MIN = 180

/** Used until the user has picked anything: roughly the centre of the country. */
val LatLonFallback = LatLon(52.1326, 5.2913)
