package nl.fietsweer.app.data

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLon(val lat: Double, val lon: Double)

object Geo {

    private const val EARTH_RADIUS_KM = 6371.0088
    private const val DEGREES_TO_RADIANS = PI / 180.0

    private const val DETOUR_FACTOR = 1.25

    fun haversineKm(from: LatLon, to: LatLon): Double {
        val deltaLat = (to.lat - from.lat) * DEGREES_TO_RADIANS
        val deltaLon = (to.lon - from.lon) * DEGREES_TO_RADIANS
        val haversine = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(from.lat * DEGREES_TO_RADIANS) * cos(to.lat * DEGREES_TO_RADIANS) *
            sin(deltaLon / 2) * sin(deltaLon / 2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(haversine.coerceIn(0.0, 1.0)))
    }

    fun routeKm(from: LatLon, to: LatLon): Double = haversineKm(from, to) * DETOUR_FACTOR

    fun bearingDeg(from: LatLon, to: LatLon): Double {
        val y = sin((to.lon - from.lon) * DEGREES_TO_RADIANS) * cos(to.lat * DEGREES_TO_RADIANS)
        val x = cos(from.lat * DEGREES_TO_RADIANS) * sin(to.lat * DEGREES_TO_RADIANS) -
            sin(from.lat * DEGREES_TO_RADIANS) * cos(to.lat * DEGREES_TO_RADIANS) *
            cos((to.lon - from.lon) * DEGREES_TO_RADIANS)
        return (atan2(y, x) / DEGREES_TO_RADIANS + 360.0) % 360.0
    }

    fun angleDiff(firstDeg: Double, secondDeg: Double): Double {
        var difference = abs(firstDeg - secondDeg) % 360.0
        if (difference > 180) difference = 360 - difference
        return difference
    }

    private fun interpolate(from: LatLon, to: LatLon, fraction: Double) =
        LatLon(from.lat + (to.lat - from.lat) * fraction, from.lon + (to.lon - from.lon) * fraction)

    fun samplePoints(from: LatLon, to: LatLon, count: Int): List<LatLon> {
        if (count <= 1) return listOf(from)
        return (0 until count).map { interpolate(from, to, it.toDouble() / (count - 1)) }
    }

    fun midpoint(from: LatLon, to: LatLon) = interpolate(from, to, 0.5)

    fun zoomForSpan(lat: Double, km: Double, pixels: Double): Double {
        val metresPerPixel = (km * 1000.0) / pixels.coerceAtLeast(1.0)
        if (metresPerPixel <= 0) return 13.0
        return (ln(156543.03392 * cos(lat * DEGREES_TO_RADIANS) / metresPerPixel) / ln(2.0)).coerceIn(2.0, 17.0)
    }
}
