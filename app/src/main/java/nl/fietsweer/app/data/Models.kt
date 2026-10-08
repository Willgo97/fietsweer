package nl.fietsweer.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

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

enum class WidgetStyle { CLASSIC, CHART, RIDES }

enum class AccentColor { BRAND, WALLPAPER, BLUE, GREEN, GOLD, COPPER, BORDEAUX, ROSE, PURPLE, INK }

enum class Language { SYSTEM, NL, EN }

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

    companion object {
        fun create() = Alert(id = UUID.randomUUID().toString())
    }
}

@Serializable
data class Settings(
    val home: Place? = null,
    val work: Place? = null,

    val outboundHour: Int = 8,
    val outboundMinute: Int = 0,
    val returnHour: Int = 17,
    val returnMinute: Int = 30,

    @SerialName("outboundEarlyMin") val outboundEarlyMinutes: Int = 0,
    @SerialName("outboundLateMin") val outboundLateMinutes: Int = 60,
    @SerialName("returnEarlyMin") val returnEarlyMinutes: Int = 60,
    @SerialName("returnLateMin") val returnLateMinutes: Int = 60,

    val speedKmh: Int = 19,
    val windAdjustSpeed: Boolean = true,
    val wetThreshold: Double = 0.2, // mm per 15 min
    val rainJacketPercent: Int = 30,
    val vestBelow: Double = 17.0,
    val winterCoatBelow: Double = 6.0,

    val useRadar: Boolean = true,
    val askedLocation: Boolean = false,
    val alerts: List<Alert> = emptyList(),

    val theme: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentColor = AccentColor.BRAND,
    val widgetStyle: WidgetStyle = WidgetStyle.CLASSIC,
    @SerialName("lang") val language: Language = Language.SYSTEM,
    val mapStyle: MapStyle = MapStyle.AUTO,

    val setupDone: Boolean = false
) {
    val hasRoute: Boolean get() = home != null && work != null

    val routeKm: Double
        get() = if (home == null || work == null) 0.0 else Geo.routeKm(home.toLatLon(), work.toLatLon())

    fun withAlert(alert: Alert): Settings {
        val others = alerts.filterNot { it.id == alert.id }
        return copy(alerts = (others + alert).sortedBy { it.minutesOfDay })
    }

    fun hourFor(leg: Leg): Int = if (leg == Leg.OUTBOUND) outboundHour else returnHour
    fun minuteFor(leg: Leg): Int = if (leg == Leg.OUTBOUND) outboundMinute else returnMinute

    fun earlyMinutesFor(leg: Leg): Int =
        (if (leg == Leg.OUTBOUND) outboundEarlyMinutes else returnEarlyMinutes).coerceIn(0, MAX_SLACK_MINUTES)

    fun lateMinutesFor(leg: Leg): Int =
        (if (leg == Leg.OUTBOUND) outboundLateMinutes else returnLateMinutes).coerceIn(0, MAX_SLACK_MINUTES)
}

const val MAX_SLACK_MINUTES = 180

val CenterOfNetherlands = LatLon(52.1326, 5.2913)
