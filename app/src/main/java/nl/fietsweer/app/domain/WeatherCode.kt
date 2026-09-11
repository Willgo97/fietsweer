package nl.fietsweer.app.domain

/** WMO weather interpretation codes, grouped into what a cyclist cares about. */
enum class Sky { CLEAR, PARTLY, CLOUDY, FOG, DRIZZLE, RAIN, SHOWERS, SNOW, THUNDER, UNKNOWN }

object WeatherCode {
    fun sky(code: Int): Sky = when (code) {
        0 -> Sky.CLEAR
        1, 2 -> Sky.PARTLY
        3 -> Sky.CLOUDY
        45, 48 -> Sky.FOG
        51, 53, 55, 56, 57 -> Sky.DRIZZLE
        61, 63, 65, 66, 67 -> Sky.RAIN
        80, 81, 82 -> Sky.SHOWERS
        71, 73, 75, 77, 85, 86 -> Sky.SNOW
        95, 96, 99 -> Sky.THUNDER
        else -> Sky.UNKNOWN
    }
}
