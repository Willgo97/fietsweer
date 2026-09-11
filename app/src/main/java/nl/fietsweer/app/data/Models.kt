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

    /** Cycling pace in km/h **in still air** — the wind is applied on top. */
    val speedKmh: Int = 19,
    /** Let head- and tailwind change the speed, and with it the ride time. */
    val windAdjustSpeed: Boolean = true,
    /** mm per 15 minutes above which a model counts as "wet". */
    val wetThreshold: Double = 0.2,
    /** Percentage of rain risk at which the rain jacket becomes a yes. */
    val rainJacketPercent: Int = 30,
    /** Bike-feel temperature below which the warm jacket becomes a yes. */
    val warmJacketTemp: Double = 11.0,

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
}

/** Used until the user has picked anything: roughly the centre of the country. */
val LatLonFallback = LatLon(52.1326, 5.2913)
